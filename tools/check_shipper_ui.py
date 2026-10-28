"""Exercise delivery role end to end on the isolated H2 fixture; no real deliveries."""
from pathlib import Path
import json, os, sys, time, urllib.request, uuid
from decimal import Decimal

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / '.tools/browser'))
from playwright.sync_api import sync_playwright

base = 'http://127.0.0.1:8882'
out = root / '.local/shipper-ui'
out.mkdir(exist_ok=True)
for _ in range(40):
    try:
        urllib.request.urlopen(base + '/api/system/health', timeout=2)
        if (root / '.local/all-ui-server.json').exists():
            break
    except OSError:
        pass
    time.sleep(1)
else:
    raise RuntimeError('Isolated H2 not ready')

checks, errors = [], []
with sync_playwright() as p:
    browser = p.chromium.launch(channel='msedge', headless=True)

    def post(ctx, path, body):
        csrf = ctx.request.get(base + '/products/csrf').json()
        return ctx.request.post(base + path, data=body,
                                headers={csrf['headerName']: csrf['token'], 'Accept': 'application/json'})

    def login(user):
        ctx = browser.new_context(viewport={'width': 1440, 'height': 1000})
        assert post(ctx, '/auth/login', {'email': f'u{user}@test.invalid',
                                        'password': os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
        return ctx

    buyer, manager, shipper, stranger = [login(user) for user in [1, 3, 5, 6]]
    cart = buyer.request.get(base + '/cart/data').json()
    assert post(buyer, '/checkout/branch', {'storeId': 1, 'version': cart['version']}).ok
    body = {'key': str(uuid.uuid4()), 'addressId': 1, 'areaId': 1, 'carrierId': 1,
            'productPromotionId': None, 'shippingPromotionId': None, 'points': 0,
            'note': 'Shipper UI test', 'paymentMethod': 'COD', 'quoteHash': ''}
    quote = post(buyer, '/checkout/quote', body)
    assert quote.ok, quote.status
    body['quoteHash'] = quote.json()['hash']
    created = post(buyer, '/checkout', body)
    assert created.ok, created.status
    order = created.json()['id']
    route = f'/shipper/orders/{order}'
    assert shipper.request.get(base + route + '/data').status == 404
    assert stranger.request.get(base + '/shipper/orders/data').status == 403
    checks.append('Unassigned shipper cannot read the order; buyer cannot access delivery endpoints')

    mp, sp = manager.new_page(), shipper.new_page()
    for page in [mp, sp]:
        page.on('pageerror', lambda e: errors.append(str(e)))
    response = mp.goto(base + f'/manager/orders/{order}', wait_until='networkidle')
    assert response.status == 200
    mp.locator('#shipper-id').select_option('5')
    with mp.expect_navigation(wait_until='networkidle'):
        mp.locator('#assign').click()
    sp.goto(base + route, wait_until='networkidle')
    assert sp.locator('#transition').count() == 0
    assert sp.locator('#cancel,#assign,#refund,#approve').count() == 0
    checks.append('Manager assigns through eligible shipper dropdown; NEW order has no forbidden shipper actions')

    with mp.expect_navigation(wait_until='networkidle'):
        mp.locator('#transition').click()
    sp.goto(base + '/shipper/orders', wait_until='networkidle')
    assert sp.locator(f'.delivery-card a[href$="/{order}"]').count() == 2
    assert 'Delivery address' in sp.locator('.shipper-page').inner_text()
    for width in [1440, 390]:
        sp.set_viewport_size({'width': width, 'height': 1000 if width > 500 else 844})
        if not sp.evaluate('document.documentElement.scrollWidth<=innerWidth'):
            sp.screenshot(path=str(out / f'overflow-{width}.png'), full_page=True)
            raise AssertionError(sp.evaluate("[...document.querySelectorAll('main *')].filter(e=>e.getBoundingClientRect().right>innerWidth+1).map(e=>({tag:e.tagName,cls:e.className,text:e.textContent.slice(0,60),right:e.getBoundingClientRect().right})).slice(0,12)"))
        assert sp.locator('#workspace-navigation').count() == 1
        sp.screenshot(path=str(out / f'shipper-list-{width}.png'), full_page=True)
    sp.goto(base + route, wait_until='networkidle')
    with sp.expect_navigation(wait_until='networkidle'):
        sp.locator('#transition').click()
    assert shipper.request.get(base + route + '/data').json()['status'] == 'PICKED_UP'
    sp.locator('#failure-reason').fill('Recipient temporarily unavailable')
    with sp.expect_navigation(wait_until='networkidle'):
        sp.locator('#failure').click()
    assert 'Recipient temporarily unavailable' in sp.locator('main').inner_text()
    with sp.expect_navigation(wait_until='networkidle'):
        sp.locator('#transition').click()
    assert shipper.request.get(base + route + '/data').json()['status'] == 'DELIVERING'
    sp.locator('#transition').click()
    sp.wait_for_function("document.querySelector('#order-message').textContent.includes('COD')")
    assert shipper.request.get(base + route + '/data').json()['status'] == 'DELIVERING'
    sp.locator('#cash-collected').check()
    with sp.expect_navigation(wait_until='networkidle'):
        sp.locator('#transition').click()
    delivered = shipper.request.get(base + route + '/data').json()
    assert delivered['status'] == 'DELIVERED' and delivered['paymentStatus'] == 'PAID'
    assert sp.locator('#transition,#failure').count() == 0
    checks.append('Pickup, failure log, retry and delivering work; COD completion requires cash, terminal order is read-only')

    for path, name in [(route, 'detail'), ('/shipper/reports?from=2026-10-14&to=2026-10-14', 'report')]:
        response = sp.goto(base + path, wait_until='networkidle')
        assert response.status == 200
        for width in [1440, 390]:
            sp.set_viewport_size({'width': width, 'height': 1000 if width > 500 else 844})
            assert sp.evaluate('document.documentElement.scrollWidth<=innerWidth')
            sp.screenshot(path=str(out / f'shipper-{name}-{width}.png'), full_page=True)
    assert 'COD đã thu khi giao' in sp.locator('main').inner_text()
    assert Decimal(sp.locator('main strong[data-money]').get_attribute('data-money')) == Decimal(str(delivered['total']))
    checks.append('Assigned list, delivery details and own COD report render through shared Tomcat/SiteMesh layout at 1440/390px')
    assert not errors, errors
    browser.close()

(out / 'verification.json').write_text(json.dumps({'checks': checks, 'errors': errors,
    'fixture': 'Isolated H2 only; no cloud orders, provider calls or actual cash'}, ensure_ascii=False, indent=2), encoding='utf-8')
print('Shipper UI checks passed:', len(checks))
