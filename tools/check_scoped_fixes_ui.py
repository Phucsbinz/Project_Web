"""Native Tomcat/SiteMesh checks for the three scoped audit fixes, using only an isolated H2 fixture.
Run after Maven tests/package with JDK21: py -3 tools/check_scoped_fixes_ui.py
"""
from pathlib import Path
import json, os, sys, time, urllib.request

root = Path(__file__).resolve().parent.parent
out = root / '.local/scoped-fixes-20261028/ui'
out.mkdir(parents=True, exist_ok=True)

if not os.environ.get('UTETRA_SCOPED_UI_RUNNING'):
    # Reuse the established lifecycle, preserving every existing fixture and audit script.
    fixture = (root / 'tools/ui/AllUiServer.java').read_text(encoding='utf-8')
    seed = '''  long purchaseOrder=com.utetra.TestRows.insert(test.jdbc,"orders",java.util.Map.of("user_id",1,"store_id",1,"status","DELIVERED","payment_status","PAID","points_used",0,"points_earned",0,"subtotal",50000,"product_discount",0,"points_discount",0));
  long oldestPurchase=0;
  for(int n=0;n<101;n++){long item=com.utetra.TestRows.insert(test.jdbc,"order_items",java.util.Map.of("order_id",purchaseOrder,"variant_id",1,"product_name","Purchase "+n,"quantity",1));if(n==0)oldestPurchase=item;}
  Files.writeString(Path.of(".local/scoped-fixes-20261028/ui-fixture.json"),"{\\"oldestPurchase\\":"+oldestPurchase+"}");
'''
    fixture = fixture.replace('public class AllUiServer', 'public class ScopedFixUiServer')
    fixture = fixture.replace('  Files.writeString(Path.of(".local/all-ui-server.json")', seed + '  Files.writeString(Path.of(".local/all-ui-server.json")')
    generated = root / '.local/scoped-fixes-20261028/ScopedFixUiServer.java'
    generated.write_text(fixture, encoding='utf-8')
    source = (root / 'tools/check_all_ui.py').read_text(encoding='utf-8')
    source = source.replace("folder=root/'.local/ui-audit-classes'", "folder=root/'.local/scoped-fixes-20261028/ui-classes'")
    source = source.replace("root/'tools/ui/AllUiServer.java'", "root/'.local/scoped-fixes-20261028/ScopedFixUiServer.java'")
    source = source.replace("'com.utetra.order.AllUiServer'", "'com.utetra.order.ScopedFixUiServer'")
    source = '\n'.join(('audit=' + repr(str(Path(__file__).resolve()))) if line.startswith('audit=') else line for line in source.splitlines())
    os.environ['UTETRA_SCOPED_UI_RUNNING'] = '1'
    exec(compile(source, str(root / 'tools/check_all_ui.py'), 'exec'), {'__file__': str(root / 'tools/check_all_ui.py'), '__name__': '__main__'})
    raise SystemExit(0)

sys.path.insert(0, str(root / '.tools/browser'))
from playwright.sync_api import sync_playwright
base = 'http://127.0.0.1:8882'
for _ in range(45):
    try:
        urllib.request.urlopen(base + '/api/system/health', timeout=2)
        if (root / '.local/all-ui-server.json').exists():
            break
    except OSError:
        pass
    time.sleep(1)
else:
    raise RuntimeError('Isolated fixture not ready')

class Checks(list):
    def append(self, value):
        super().append(value)
        print('PASS:', value, flush=True)
checks, errors, network = Checks(), [], []
logout_verified = False
logout_responses = []
expected_lookup_failure = [False]
fixture = json.loads((root / '.local/scoped-fixes-20261028/ui-fixture.json').read_text(encoding='utf-8'))
with sync_playwright() as pw:
    browser = pw.chromium.launch(channel='msedge', headless=True)
    contexts = []
    def post(ctx, path, data):
        csrf = ctx.request.get(base + '/products/csrf').json()
        return ctx.request.post(base + path, data=data, headers={csrf['headerName']: csrf['token'], 'Accept': 'application/json'})
    def login(user):
        ctx = browser.new_context(viewport={'width': 1440, 'height': 1000})
        assert post(ctx, '/auth/login', {'email': f'u{user}@test.invalid', 'password': os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
        contexts.append(ctx)
        return ctx
    def page(ctx):
        pg = ctx.new_page()
        pg.on('pageerror', lambda err: errors.append(str(err)))
        pg.on('console', lambda msg: errors.append(msg.text) if msg.type == 'error' and not expected_lookup_failure[0] else None)
        pg.on('response', lambda res: logout_responses.append(res.status) if res.url == base + '/auth/logout' and res.request.method == 'POST' else None)
        pg.on('requestfailed', lambda req: network.append({'path': req.url.split('?')[0], 'failure': req.failure}))
        return pg
    def render(pg, path, name):
        for width in [390, 768, 1440]:
            pg.set_viewport_size({'width': width, 'height': 1000})
            assert pg.goto(base + path, wait_until='networkidle').status == 200
            assert pg.locator('.site-header').count() == 1
            assert pg.evaluate('document.documentElement.scrollWidth <= innerWidth + 1')
            pg.screenshot(path=str(out / f'{name}-{width}.png'), full_page=True)
        checks.append(name + ': native render at 390/768/1440')

    user = login(1)
    pg = page(user)
    for path in ['/reviews', '/comments']:
        render(pg, path, path[1:])
        pg.wait_for_function("!document.querySelector('#orderItemId').disabled")
        pg.set_viewport_size({'width': 390 if path == '/reviews' else 768, 'height': 1000})
        seen = []
        for index in range(6):
            options = pg.locator('#orderItemId option[data-product]')
            ids = options.evaluate_all('(options)=>options.map(o=>Number(o.value))')
            assert len(ids) == (20 if index < 5 else 1), (index, len(ids))
            seen += ids
            if index < 5:
                pg.locator('#purchase-next').click()
                pg.wait_for_function('(n)=>document.querySelector("#purchase-page-label").textContent.includes("Trang "+n) && !document.querySelector("#orderItemId").disabled', arg=index + 2)
        assert len(seen) == len(set(seen)) == 101
        assert seen[-1] == fixture['oldestPurchase']
        assert pg.locator('#purchase-next').is_disabled()
        pg.select_option('#orderItemId', str(fixture['oldestPurchase']))
        pg.locator('#content').fill('Trải nghiệm thử nghiệm hợp lệ từ dòng mua cũ. ' * 2)
        with pg.expect_response(lambda r: r.request.method == 'POST' and r.url == base + path) as saved:
            pg.locator('#community-form button[type=submit],#community-form > button').last.click()
        assert saved.value.ok
        pg.locator('#purchase-prev').click()
        pg.wait_for_function("document.querySelector('#purchase-page-label').textContent.includes('Trang 5') && !document.querySelector('#orderItemId').disabled")
        assert pg.locator('#orderItemId option[data-product]').count() == 20
        assert pg.locator('#community-form [name=productId]').input_value() == ''
        checks.append(path + ': 101 choices across six bounded pages, old purchase submitted, back page resets selection')
    # A failed purchase lookup stays visible and can be retried without submitting an unknown ID.
    expected_lookup_failure[0] = True
    pg.route('**/reviews/purchases?*', lambda route: route.fulfill(status=503, content_type='application/json', body='{"message":"Fixture lookup unavailable"}'))
    pg.goto(base + '/comments', wait_until='networkidle')
    assert pg.locator('#purchase-retry').is_visible()
    assert pg.locator('#orderItemId').is_disabled()
    pg.unroute('**/reviews/purchases?*')
    pg.locator('#purchase-retry').click()
    pg.wait_for_function("!document.querySelector('#orderItemId').disabled")
    expected_lookup_failure[0] = False
    checks.append('Purchase lookup failure is explicit and retry restores choices')
    empty = login(2); ep = page(empty);render(ep, '/reviews', 'empty-review')
    assert ep.locator('#orderItemId option[data-product]').count() == 0
    assert ep.locator('#orderItemId').is_disabled()
    assert 'Chưa có' in ep.locator('#purchase-status').inner_text()
    checks.append('Empty eligible purchase state disables choice')

    host, member, vendor = user, login(6), empty
    def version(token): return host.request.get(base + '/group-orders/' + token + '/data').json()['version']
    for mode in ['HOST', 'INDIVIDUAL']:
        assert post(host, '/cart/clear', {}).ok
        created = post(host, '/group-orders', {'paymentMode': mode}).json(); token = created['token']
        for ctx in [member, vendor]:
            assert post(ctx, '/group-orders/' + token + '/join', {'version': version(token)}).ok
            assert post(ctx, '/group-orders/' + token + '/items', {'version': version(token), 'item': {'variantId': 1, 'sugar': 50, 'ice': 50, 'toppingIds': [1], 'quantity': 1, 'note': 'No straw'}}).ok
        assert post(host, '/group-orders/' + token + '/lock', {'version': version(token)}).ok
        cart = host.request.get(base + '/cart/data').json()
        assert post(host, '/checkout/branch', {'storeId': 1, 'version': cart['version']}).ok
        cp = page(host)
        cp.goto(base + '/checkout?branch=1', wait_until='networkidle')
        cp.locator('[name=recipientName]').fill('Khách kiểm thử')
        cp.locator('[name=phone]').fill('0901234567')
        cp.locator('[name=addressDetail]').fill('Địa chỉ giao hàng thử nghiệm')
        for field in ['areaId', 'carrierId']:
            label = cp.locator('select[name=' + field + '] option[value="1"]').inner_text()
            cp.locator('input[data-choice=' + field + ']').fill(label)
            cp.locator('input[data-choice=' + field + ']').press('Tab')
        cp.locator('[name=paymentMethod][value=LOCAL_QR]').check()
        cp.wait_for_function("!document.querySelector('#confirm-order').disabled")
        cp.locator('#confirm-order').click()
        cp.wait_for_url('**/qr-payment' if mode == 'HOST' else '**/group-orders/*/payment')
        group = host.request.get(base + '/group-orders/' + token + '/data').json()
        assert group['participants'] == 2
        assert group['payerCount'] == (1 if mode == 'HOST' else 2)
        assert cp.locator('.local-qr-page' if mode == 'HOST' else '.group-payment-page').count() == 1
        render(cp, '/group-orders/' + token, mode.lower() + '-host-empty-group')
        assert cp.locator('a[href="/orders/' + str(group['orderId']) + '"]').count() > 0
        render(cp, '/group-orders/' + token + '/payment', mode.lower() + '-host-empty-payment')
        render(cp, '/orders/' + str(group['orderId']), mode.lower() + '-host-empty-tracking')
        mp = page(member);mp.goto(base + '/group-orders/' + token + '/payment', wait_until='networkidle')
        assert mp.locator('.local-qr-page' if mode == 'INDIVIDUAL' else '.group-payment-page').count() == 1
        cp.close();mp.close()
        checks.append(mode + ': actual checkout redirect, reload/direct host tracking/payment and member payment preserve payer count')
    # Logout from header keeps only the pre-login Guest session cart, never account rows.
    guest = browser.new_context();contexts.append(guest)
    assert post(guest, '/cart/items', {'variantId': 1, 'sugar': 0, 'ice': 0, 'toppingIds': [], 'quantity': 3, 'note': 'Guest only'}).ok
    assert post(guest, '/auth/login', {'email': 'u1@test.invalid', 'password': os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
    gp = page(guest);gp.goto(base + '/account/profile', wait_until='networkidle')
    gp.locator('.account-trigger').click()
    gp.locator('[data-site-logout]').first.click();gp.wait_for_url('**/auth/login')
    data = guest.request.get(base + '/cart/data').json()
    assert len(data['items']) == 1 and data['items'][0]['quantity'] == 3
    assert guest.request.get(base + '/account/profile/data', headers={'Accept': 'application/json'}).status == 401
    assert 200 in logout_responses
    logout_verified = True
    checks.append('Header logout revokes account access; only independent pre-login Guest cart remains')
    browser.close()

# The existing shared logout handler navigates after fetch resolves its 200 headers,
# which may abort the response body. Accept only this exact navigation cancellation,
# after asserting HTTP200, login destination, Guest isolation and protected API401.
expected_aborts = [r for r in network if logout_verified and r['path'] == base + '/auth/logout' and r['failure'] == 'net::ERR_ABORTED']
unexpected = [r for r in network if r not in expected_aborts]
result = {'fixture': 'Isolated H2 only; no cloud/provider calls', 'checks': checks, 'pageErrors': errors, 'requestFailures': unexpected, 'expectedNavigationAborts': expected_aborts, 'logoutHttpStatuses': logout_responses}
(out / 'verification.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print('Scoped native UI checks:', len(checks), 'console/JS errors:', len(errors), 'unexpected network failures:', len(unexpected), 'verified navigation aborts:', len(expected_aborts))
if errors or unexpected: raise SystemExit(1)
