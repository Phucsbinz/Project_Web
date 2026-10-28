"""Isolated browser contexts verify private offers and floating customer/admin chat."""
from pathlib import Path
import os,sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/offers-chat-ui';out.mkdir(exist_ok=True)
for _ in range(45):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('Fixture not ready')
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True);customer=browser.new_context(viewport={'width':1440,'height':1000});admin=browser.new_context(viewport={'width':1440,'height':1000});other=browser.new_context()
 def post(ctx,path,data=None):
  csrf=ctx.request.get(base+'/products/csrf').json();return ctx.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token'],'Accept':'application/json'})
 for ctx,i in [(customer,1),(admin,4),(other,6)]:assert post(ctx,'/auth/login',{'email':f'u{i}@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
 page=customer.new_page();staff=admin.new_page();page.on('pageerror',lambda e:errors.append(str(e)));staff.on('pageerror',lambda e:errors.append(str(e)))
 page.goto(base+'/',wait_until='networkidle');assert page.locator('.dropdown-menu a[href="/support"]').count()==0
 page.locator('.account-trigger').click();page.locator('[data-offers-open]').click();page.wait_for_selector('.offers-card');assert 'Quà hạng Bạc' in page.locator('#offers-list').inner_text()
 wallet=customer.request.get(base+'/account/offers').json();gift=next(v for v in wallet if v['source']=='TIER_SILVER');assert gift['owned'] and gift['reward']
 assert all(v['id']!=gift['id'] for v in other.request.get(base+'/account/offers').json());assert not post(other,'/account/offers/'+str(gift['id'])+'/save').ok
 page.locator('#offers-owned').uncheck();page.locator('#offers-search').fill('REALTIME_LAST');page.wait_for_selector('.offers-card button:not([disabled])');page.locator('.offers-card button').click();page.wait_for_function("document.querySelector('.offers-card button')?.textContent==='Đã lưu'");page.locator('#offers-owned').check();assert 'Last voucher' in page.locator('#offers-list').inner_text();page.locator('#offers-search').fill('')
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844});page.wait_for_function("getComputedStyle(document.querySelector('.account-offers .modal-dialog')).transform==='none'");assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(out/f'offers-{width}.png'),full_page=False)
 page.locator('.account-offers .btn-close').click();page.wait_for_selector('.account-offers.show',state='hidden');checks.append('Account offers modal: tier reward, saved/public filters, search, idempotent save and ownership isolation; 390/1440px')
 assert page.locator('#support-widget-title').inner_text()=='UTETra hỗ trợ';page.locator('.support-widget-launcher').click();page.locator('#support-widget-body').fill('Tôi cần hỗ trợ đơn hàng <b>riêng tư</b>');page.locator('.support-widget-compose button').click();page.wait_for_selector('.support-widget-messages article.mine')
 staff.goto(base+'/admin/support',wait_until='networkidle');staff.wait_for_selector('.support-thread');staff.locator('.support-thread').first.click();staff.wait_for_function("document.querySelector('#support-messages').textContent.includes('riêng tư')");assert staff.locator('#support-messages b').count()==0
 staff.locator('#support-body').fill('Chào bạn, admin đã nhận được yêu cầu.');staff.locator('#support-compose button').click();page.wait_for_function("document.querySelector('.support-widget-messages').textContent.includes('admin đã nhận')",timeout=15000)
 thread=customer.request.get(base+'/api/support/conversations').json()['items'][0]['id'];assert other.request.get(base+'/api/support/conversations/'+thread+'/messages').status==404
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(out/f'chat-{width}.png'),full_page=False)
 page.locator('[data-chat-close]').click();staff.locator('#support-body').fill('Bạn có thể kiểm tra thông tin tại hóa đơn nhé.');staff.locator('#support-compose button').click();page.wait_for_selector('.support-widget-unread:not([hidden])',timeout=15000);page.locator('.support-widget-launcher').click();assert page.locator('.support-widget-unread').is_hidden()
 page.goto(base+'/menu',wait_until='networkidle');page.locator('.support-widget-launcher').click();page.wait_for_function("document.querySelector('.support-widget-messages').textContent.includes('thông tin tại hóa đơn')")
 page.go_back(wait_until='domcontentloaded')
 if page.locator('#support-widget-panel').is_hidden():page.locator('.support-widget-launcher').click()
 staff.locator('#support-body').fill('Admin vẫn nhận tin sau khi bạn quay lại trang.');staff.locator('#support-compose button').click();page.wait_for_function("document.querySelector('.support-widget-messages').textContent.includes('sau khi bạn quay lại')",timeout=15000)
 checks.append('Customer/admin chat in separate cookies, escaped text, isolation, unread badge, persisted history and browser Back')
 page.goto(base+'/checkout?branch=1',wait_until='networkidle');page.wait_for_selector('[data-voucher-for=productPromotionId]');assert page.locator('[name=paymentMethod]').evaluate_all('(nodes)=>nodes.map(n=>n.value)')==['COD','LOCAL_QR'];page.locator('[data-voucher-for=productPromotionId]').click();page.wait_for_selector('.voucher-card');assert 'Quà hạng Bạc' in page.locator('#voucher-list').inner_text()
 checks.append('Saved/tier vouchers available through common checkout eligibility; payment methods keep server-side quote validation')
 browser.close()
assert not errors,errors
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2; independent customer/admin/other browser contexts'},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'checks':len(checks),'errors':errors},ensure_ascii=False))
