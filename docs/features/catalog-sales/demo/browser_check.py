"""Disposable H2 fixture checks. Never use these test identities against a real DB."""
import base64, hashlib, hmac, json, os, pathlib, subprocess, time, urllib.request, uuid
import websocket

root = pathlib.Path(__file__).resolve().parents[4]
base = 'http://127.0.0.1:8087'
out = root / 'docs/features/catalog-sales/assets/update-20261024'
out.mkdir(parents=True, exist_ok=True)
profile = root / 'tmp/catalog-browser-profile'
browser = subprocess.Popen([
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', '--headless=new',
    '--disable-gpu', '--no-first-run', '--remote-debugging-port=9239',
    '--remote-debugging-address=127.0.0.1', '--remote-allow-origins=http://localhost:9239',
    '--user-data-dir=' + str(profile), 'about:blank'], creationflags=subprocess.CREATE_NO_WINDOW)
connection = None
checks, errors = [], []

def token(user, role):
    props = dict(line.split('=', 1) for line in (root / 'src/test/resources/application-test.properties').read_text().splitlines() if '=' in line)
    def encode(data):
        return base64.urlsafe_b64encode(json.dumps(data, separators=(',', ':')).encode()).decode().rstrip('=')
    now = int(time.time())
    payload = {'iss':'utetra', 'aud':['utetra-web'], 'sub':str(user), 'sid':str(user), 'role':'ROLE_' + role,
               'iat':now, 'nbf':now, 'exp':now+3600, 'jti':str(uuid.uuid4())}
    value = encode({'alg':'HS256','typ':'JWT'}) + '.' + encode(payload)
    signature = hmac.new(base64.b64decode(props['security.jwt.secret-key']), value.encode(), hashlib.sha256).digest()
    return value + '.' + base64.urlsafe_b64encode(signature).decode().rstrip('=')

sequence = 0
def cdp(method, params=None):
    global sequence
    sequence += 1
    connection.send(json.dumps({'id':sequence,'method':method,'params':params or {}}))
    while True:
        result = json.loads(connection.recv())
        if result.get('method') == 'Runtime.exceptionThrown':
            errors.append(result['params']['exceptionDetails'].get('text', 'JavaScript exception'))
        if result.get('id') == sequence:
            if 'error' in result: raise RuntimeError(result['error'])
            return result.get('result', {})

def evaluate(expression):
    result = cdp('Runtime.evaluate', {'expression':expression, 'awaitPromise':True, 'returnByValue':True})
    if result.get('exceptionDetails'): raise AssertionError(result['exceptionDetails'].get('text'))
    return result.get('result', {}).get('value')

def wait(expression):
    deadline = time.time() + 12
    while time.time() < deadline:
        if evaluate(expression): return
        time.sleep(.1)
    raise AssertionError('Timed out: ' + expression)

def navigate(path, ready):
    cdp('Page.navigate', {'url':base+path})
    wait("location.pathname === " + json.dumps(path.split('?')[0]) + " && document.readyState === 'complete'")
    wait(ready)

try:
    for _ in range(50):
        try:
            request = urllib.request.Request('http://127.0.0.1:9239/json/new?about:blank', method='PUT')
            target = json.load(urllib.request.urlopen(request, timeout=1))
            break
        except OSError: time.sleep(.1)
    else: raise RuntimeError('Browser did not start')
    connection = websocket.create_connection(target['webSocketDebuggerUrl'], origin='http://localhost:9239', timeout=15)
    cdp('Page.enable'); cdp('Runtime.enable'); cdp('Network.enable')
    cdp('Network.setExtraHTTPHeaders', {'headers':{'Authorization':'Bearer ' + token(1,'ADMIN')}})
    for width in [1440, 768, 390]:
        cdp('Emulation.setDeviceMetricsOverride', {'width':width,'height':1000,'deviceScaleFactor':1,'mobile':False})
        navigate('/admin/products?storeId=1', "document.querySelector('#catalog-list-title')?.textContent === 'Sản phẩm (4)'")
        assert evaluate("document.querySelector('[data-catalog-tab][aria-current=page]')?.dataset.catalogTab === 'products'")
        assert evaluate("document.querySelector('#managed-store').value === '1'")
        assert evaluate("document.documentElement.scrollWidth <= innerWidth")
        screenshot = cdp('Page.captureScreenshot', {'format':'png','captureBeyondViewport':True})
        (out / f'admin-products-{width}.png').write_bytes(base64.b64decode(screenshot['data']))
        checks.append({'route':'/admin/products?storeId=1','width':width,'correctSection':True,'overflow':False})
    evaluate("document.querySelector('[data-catalog-tab=promotions]').click()")
    wait("document.querySelector('#catalog-list-title')?.textContent === 'Khuyến mãi (1)'")
    assert evaluate("location.search === '?storeId=1'")
    cdp('Page.reload'); wait("document.querySelector('#catalog-list-title')?.textContent === 'Khuyến mãi (1)'")
    checks.append({'navigation':'tabs + reload','selectedStoreRetained':True})
    navigate('/admin/stores', "document.querySelector('#catalog-list-title')?.textContent === 'Shop (2)'")
    evaluate("[...document.querySelectorAll('#management-list button')].find(b => b.textContent === 'Duyệt').click()")
    wait("document.querySelector('[data-review-form] select')?.options.length === 2")
    assert evaluate("document.querySelector('[data-review-form] select').textContent.includes('Demo policy')")
    screenshot = cdp('Page.captureScreenshot', {'format':'png','captureBeyondViewport':True})
    (out / 'approval-policy-390.png').write_bytes(base64.b64decode(screenshot['data']))
    checks.append({'approvalPolicyPicker':'active policies only','noRawIdPrompt':True})
    evaluate("[...document.querySelectorAll('#management-list button')].find(b => b.textContent === 'Từ chối').click()")
    wait("document.querySelector('[data-review-form]') && !document.querySelector('[data-review-form] select')")
    evaluate("document.querySelector('[data-review-form] textarea').value = 'Cần bổ sung địa chỉ <script>history-check</script>'; document.querySelector('[data-review-form]').requestSubmit()")
    wait("document.querySelector('#management-message').textContent.includes('Đã xử lý hồ sơ')")
    cdp('Network.setExtraHTTPHeaders', {'headers':{'Authorization':'Bearer ' + token(2,'VENDOR')}})
    navigate('/account/shop-registration', "document.body.textContent.includes('Hồ sơ bị từ chối')")
    assert evaluate("document.querySelector('label[for=shop-slug]').textContent.startsWith('Đường dẫn') && document.querySelector('label[for=shop-phone]').textContent === 'Điện thoại'")
    assert evaluate("document.body.textContent.includes('Cần bổ sung địa chỉ <script>history-check</script>') && ![...document.scripts].some(s => s.textContent.includes('history-check'))")
    screenshot = cdp('Page.captureScreenshot', {'format':'png','captureBeyondViewport':True})
    (out / 'registration-history-390.png').write_bytes(base64.b64decode(screenshot['data']))
    checks.append({'registrationFeedback':'visible to applicant','escaped':True})
    cdp('Network.setExtraHTTPHeaders', {'headers':{'Authorization':'Bearer ' + token(3,'MANAGER')}})
    navigate('/manager/stock?storeId=1', "document.querySelector('#manage-toppingId') !== null")
    assert evaluate("document.querySelector('[data-catalog-tab][aria-current=page]')?.dataset.catalogTab === 'stock'")
    checks.append({'route':'/manager/stock?storeId=1','correctSection':True})
    assert not errors, errors
    report = {'mode':'disposable H2 + test-issued JWT; no real login or cloud provider', 'checks':checks, 'javascriptErrors':errors}
    (out / 'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Passed', len(checks), 'catalog browser checks; no JavaScript errors.')
finally:
    if connection:
        try: cdp('Browser.close')
        except Exception: pass
        connection.close()
    try: browser.wait(timeout=5)
    except subprocess.TimeoutExpired: browser.terminate()
