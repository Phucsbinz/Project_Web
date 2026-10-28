"""Support visibility/login/quick-view regression; disposable AllUiServer H2 only."""
from pathlib import Path
import os,sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright,expect
base='http://127.0.0.1:8882';out=root/'.local/support-widget-ui';out.mkdir(parents=True,exist_ok=True)
for _ in range(45):
    try:
        urllib.request.urlopen(base+'/api/system/health',timeout=2)
        if (root/'.local/all-ui-server.json').exists():break
    except OSError:pass
    time.sleep(1)
else:raise RuntimeError('Isolated H2 fixture not ready')
checks=[];failures=[];errors=[]
def check(name,action):
    try:action();checks.append({'name':name,'status':'PASS'});return True
    except Exception as e:failures.append({'name':name,'error':type(e).__name__});checks.append({'name':name,'status':'FAIL'});return False
def ensure(value):assert value
with sync_playwright() as p:
    browser=p.chromium.launch(channel='msedge',headless=True)
    guest=browser.new_context();customer=browser.new_context()
    c=customer.request.get(base+'/products/csrf').json()
    assert customer.request.post(base+'/auth/login',data={'email':'u1@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']},headers={c['headerName']:c['token']}).ok
    page=guest.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
    calls=[];page.on('request',lambda r:calls.append(r.url) if '/api/support' in r.url else None)
    for width in [390,768,1440]:
        page.set_viewport_size({'width':width,'height':900});page.goto(base+'/menu',wait_until='domcontentloaded')
        visible=check(f'Guest launcher visible at {width}',lambda:expect(page.locator('.support-widget-launcher')).to_be_visible(timeout=1500))
        if visible:
            page.locator('.support-widget-launcher').click();page.wait_for_selector('.modal.show')
            check(f'Guest prompted to log in at {width}',lambda:ensure('đăng nhập' in page.locator('.modal.show').inner_text().lower()))
            page.locator('.modal.show [data-bs-dismiss=modal]').last.click();page.wait_for_selector('.modal.show',state='hidden')
            check(f'Guest can dismiss prompt at {width}',lambda:ensure('/menu' in page.url and page.locator('#support-widget-panel').is_hidden()))
            page.locator('.support-widget-launcher').click();page.locator('.modal.show [data-confirm-accept]').click();page.wait_for_url('**/auth/login')
            checks.append({'name':f'Guest acceptance opens login at {width}','status':'PASS'})
    check('Guest widget makes no support API calls',lambda:ensure(not calls))
    check('Guest cannot access support backend',lambda:ensure(guest.request.get(base+'/api/support/conversations').status==401))
    user=customer.new_page();user.on('pageerror',lambda e:errors.append(str(e)))
    user.goto(base+'/products/1?quick=1',wait_until='domcontentloaded');expect(user.locator('#drink-form')).to_be_visible()
    check('Direct quick-view has no widget markup',lambda:ensure(user.locator('[data-support-widget]').count()==0))
    for width in [390,768,1440]:
        user.set_viewport_size({'width':width,'height':900});user.goto(base+'/menu',wait_until='domcontentloaded')
        expect(user.locator('.support-widget-launcher')).to_be_visible()
        before=sum(i['quantity'] for i in customer.request.get(base+'/cart/data').json()['items'])
        user.locator('.product-card .card-footer a[href$="/products/1"]').first.click()
        user.wait_for_selector('#drink-modal.show')
        frame=user.frame_locator('#drink-modal-frame');expect(frame.locator('#drink-form')).to_be_visible()
        check(f'No widget inside quick-view iframe at {width}',lambda:ensure(frame.locator('[data-support-widget]').count()==0))
        check(f'Parent chat hidden while modal open at {width}',lambda:expect(user.locator('[data-support-widget]')).to_be_hidden(timeout=1500))
        expect(frame.locator('[data-product-submit]')).to_be_enabled()
        frame.locator('[data-product-submit]').click();user.wait_for_selector('#drink-modal.show',state='hidden')
        expect(user.locator('.support-widget-launcher')).to_be_visible()
        after=sum(i['quantity'] for i in customer.request.get(base+'/cart/data').json()['items'])
        check(f'Add to cart succeeds and chat returns at {width}',lambda:ensure(after==before+1))
    user.locator('.support-widget-launcher').click();user.locator('#support-widget-body').fill('Regression support visibility')
    user.locator('.support-widget-compose button').click();expect(user.locator('.support-widget-messages article.mine')).to_be_visible()
    checks.append({'name':'Logged-in customer can still send support message','status':'PASS'})
    user.locator('[data-chat-close]').click();user.screenshot(path=str(out/'support-launcher-desktop.png'))
    user.goto(base+'/products/1',wait_until='domcontentloaded')
    check('Regular product page retains chat',lambda:expect(user.locator('.support-widget-launcher')).to_be_visible())
    browser.close()
(out/'verification.json').write_text(json.dumps({'checks':checks,'failures':failures,'pageErrors':errors,'fixture':'Native Edge/Tomcat/SiteMesh; isolated H2 on8882'},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'checks':len(checks),'failures':failures,'pageErrors':errors},ensure_ascii=False))
assert not failures and not errors
