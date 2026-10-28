"""Audit regression assertions against the isolated H2 AllUiServer only."""
from pathlib import Path
import json, os, sys, time, urllib.request

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / '.tools/browser'))
from playwright.sync_api import sync_playwright

base = 'http://127.0.0.1:8882'
out = root / '.local/catalog-regressions'
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
    raise RuntimeError('Isolated H2 fixture not ready')

results, errors = [], []
with sync_playwright() as p:
    browser = p.chromium.launch(channel='msedge', headless=True)
    ctx = browser.new_context(viewport={'width': 1440, 'height': 1000})

    def write(path, body, method='POST'):
        csrf = ctx.request.get(base + '/products/csrf').json()
        return ctx.request.fetch(base + path, method=method, data=body,
                                 headers={csrf['headerName']: csrf['token'], 'Accept': 'application/json'})

    assert write('/auth/login', {'email': 'u4@test.invalid',
                                 'password': os.environ['UTETRA_UI_TEST_PASSWORD']}).ok

    def page(path):
        value = ctx.new_page()
        value.on('pageerror', lambda error: errors.append(str(error)))
        assert value.goto(base + path, wait_until='networkidle').status == 200
        return value

    def promo_list():
        return ctx.request.get(base + '/api/admin/catalog/promotions').json()

    def global_promotion():
        body = {'storeId': None, 'name': 'Regression GLOBAL scoped', 'code': 'REGLOBAL',
                'kind': 'PRODUCT_PERCENT', 'percent': 10, 'maxDiscount': 100000,
                'minOrder': 0, 'usageLimit': 100, 'perUserLimit': 10,
                'startsAt': '2026-10-13T00:00:00Z', 'endsAt': '2026-12-31T00:00:00Z',
                'active': True, 'productIds': [1], 'version': 0}
        created = write('/api/admin/catalog/promotions', body)
        assert created.ok, f'Create promotion status {created.status}'
        pid = created.json()['id']
        pg = page('/admin/promotions')
        if pg.locator('#managed-store').input_value() != '':
            with pg.expect_response(lambda r: r.request.method == 'GET' and r.url.endswith('/catalog/promotions')):
                pg.locator('#managed-store').select_option('')
        card = pg.locator(f'[data-record-id="{pid}"]')
        card.wait_for()
        card.get_by_role('button', name='Chỉnh sửa', exact=True).click()
        selected = pg.locator('#manage-productIds').evaluate('(s)=>[...s.selectedOptions].map(o=>Number(o.value))')
        assert selected == [1], f'Existing GLOBAL selection lost: {selected}'
        pg.locator('#manage-name').fill('Renamed without changing eligible products')
        with pg.expect_response(lambda r: r.request.method == 'PUT' and r.url.endswith(f'/promotions/{pid}')) as saved:
            pg.locator('#management-form button').first.click()
        assert saved.value.ok
        pg.wait_for_function("document.querySelector('#manage-name').value === ''")
        assert next(x for x in promo_list() if x['id'] == pid)['productIds'] == [1]
        # Choosing all products remains intentional and supported.
        pg.locator(f'[data-record-id="{pid}"]').get_by_role('button', name='Chỉnh sửa', exact=True).click()
        pg.locator('#manage-productIds').select_option([])
        with pg.expect_response(lambda r: r.request.method == 'PUT' and r.url.endswith(f'/promotions/{pid}')) as saved:
            pg.locator('#management-form button').first.click()
        assert saved.value.ok
        pg.wait_for_function("document.querySelector('#manage-name').value === ''")
        assert next(x for x in promo_list() if x['id'] == pid)['productIds'] == []
        # Restore an explicit selection, then simulate a failed product lookup.
        current = next(x for x in promo_list() if x['id'] == pid)
        body.update({'name': current['name'], 'productIds': [1], 'version': current['version']})
        assert write(f'/api/admin/catalog/promotions/{pid}', body, 'PUT').ok
        pg.reload(wait_until='networkidle')
        pg.locator(f'[data-record-id="{pid}"]').get_by_role('button', name='Chỉnh sửa', exact=True).click()
        pg.route('**/api/admin/catalog/products?storeId=*', lambda route: route.abort())
        pg.locator('#catalog-reload').click()
        pg.wait_for_function("document.querySelector('#management-message').classList.contains('alert-danger')")
        assert pg.locator('#management-form button').first.is_disabled(), 'Save must be blocked when product lookup failed'
        assert next(x for x in promo_list() if x['id'] == pid)['productIds'] == [1]
        pg.close()

    def optional_catalog_key():
        pg = page('/admin/products?storeId=1')
        pg.locator('[data-record-id="1"]').get_by_role('button', name='Chỉnh sửa', exact=True).click()
        key = pg.locator('#manage-catalogKey')
        key.fill('')
        assert key.evaluate('(i)=>i.checkValidity()'), 'Optional empty catalogKey is rejected by HTML validation'
        assert pg.locator('#management-form').evaluate('(f)=>f.checkValidity()')
        with pg.expect_response(lambda r: r.request.method == 'PUT' and r.url.endswith('/products/1')) as saved:
            pg.locator('#management-form button').first.click()
        assert saved.value.ok
        pg.wait_for_function("document.querySelector('#manage-name').value === ''")
        assert next(x for x in ctx.request.get(base + '/api/admin/catalog/products?storeId=1').json() if x['id'] == 1)['catalogKey'] is None
        # Create a private product with no cross-branch key through the actual form.
        pg.locator('#management-create').click()
        pg.locator('#manage-name').fill('Regression private tea')
        pg.locator('#manage-slug').fill('regression-private-tea')
        pg.locator('#manage-basePrice').fill('30000')
        with pg.expect_response(lambda r: r.request.method == 'POST' and r.url.endswith('/catalog/products')) as created:
            pg.locator('#management-form button').first.click()
        assert created.value.ok
        assert created.value.json()['catalogKey'] is None
        pg.close()

    def stale_form():
        pg = page('/admin/toppings')
        pg.locator('[data-record-id="1"]').get_by_role('button', name='Chỉnh sửa', exact=True).click()
        old = next(t for t in ctx.request.get(base + '/api/admin/catalog/toppings').json() if t['id'] == 1)
        assert write('/api/admin/catalog/toppings/1', {'name': old['name'], 'price': int(old['price']),
                     'active': False, 'version': old.get('version', 0)}, 'PUT').ok
        pg.locator('#manage-price').fill('6000')
        with pg.expect_response(lambda r: r.request.method == 'PUT' and r.url.endswith('/toppings/1')) as saved:
            pg.locator('#management-form button').first.click()
        assert saved.value.status == 409, 'Stale browser form must conflict'
        current = next(t for t in ctx.request.get(base + '/api/admin/catalog/toppings').json() if t['id'] == 1)
        assert current['active'] is False and int(current['price']) == int(old['price'])
        pg.close()

    cases = {'global': global_promotion, 'key': optional_catalog_key, 'version': stale_form}
    selected = os.environ.get('UTETRA_CATALOG_CASE', 'global,key,version').split(',')
    for name in selected:
        try:
            cases[name]()
            results.append({'case': name, 'passed': True})
        except Exception as error:
            results.append({'case': name, 'passed': False, 'error': str(error)[:300]})
    browser.close()

(out / 'verification.json').write_text(json.dumps({'cases': results, 'pageErrors': errors,
    'fixture': 'Isolated H2/Tomcat/SiteMesh only'}, ensure_ascii=False, indent=2), encoding='utf-8')
for result in results:
    print(result['case'], 'PASS' if result['passed'] else 'FAIL: ' + result['error'])
assert all(result['passed'] for result in results) and not errors, 'Catalog browser regressions failed'
