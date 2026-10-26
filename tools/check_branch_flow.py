from pathlib import Path
import sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8080';out=root/'docs/demo/branch-flow';out.mkdir(exist_ok=True)
checks=[];errors=[]
for attempt in range(30):
 try:urllib.request.urlopen(base+'/auth/login',timeout=2);break
 except OSError:time.sleep(1)
else:raise RuntimeError('Server not ready')
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless=True)
 page=browser.new_page(viewport={'width':1440,'height':1000});page.on('pageerror',lambda e:errors.append(str(e)))
 def post(path,data):
  csrf=page.request.get(base+'/products/csrf').json()
  return page.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token']})
 page.goto(base+'/menu',wait_until='networkidle')
 assert page.locator('#storeId').count()==0
 names=page.locator('.product-card h3').all_inner_texts();assert len(names)==len(set(names))
 assert '30 món' in page.locator('.catalog-pagination').inner_text()
 page.locator('.product-card .card-footer a').first.click();page.wait_for_selector('#drink-modal.show')
 frame=page.frame_locator('#drink-modal-frame');frame.locator('#drink-form').wait_for()
 assert frame.get_by_text('Xem cửa hàng bán món này').count()==0
 page.screenshot(path=str(out/'quick-view-desktop.png'))
 page.set_viewport_size({'width':390,'height':844});page.screenshot(path=str(out/'quick-view-mobile.png'))
 assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
 assert frame.locator('body').evaluate('(body)=>body.ownerDocument.documentElement.scrollWidth<=body.ownerDocument.defaultView.innerWidth')
 checks.append('Quick view is usable without horizontal overflow at 390px')
 page.set_viewport_size({'width':1440,'height':1000})
 frame.get_by_role('button',name='Thêm vào giỏ hàng').click();page.wait_for_selector('#drink-modal',state='hidden')
 page.wait_for_function("document.querySelector('#menu-cart-items').textContent.includes('Size')")
 checks.append('Guest selects a drink with no branch; quick view adds to actual cart')
 page.goto(base+'/auth/login',wait_until='networkidle');page.locator('#email').fill('demo.quanghung@example.test');page.locator('#password').fill('123456');page.locator('button[type=submit]').click();page.wait_for_url('**/account/profile')
 original=page.request.get(base+'/cart/data').json()
 try:
  for item in original['items']:
   current=page.request.get(base+'/cart/data').json();assert post('/cart/items/'+str(item['id'])+'/delete',{'version':current['version']}).ok
  page.goto(base+'/cart',wait_until='networkidle');page.locator('#merge-cart').click();page.wait_for_load_state('networkidle')
  page.wait_for_selector('.delete-item')
  draft=page.request.get(base+'/cart/data').json();assert len(draft['items'])==1
  checks.append('Guest choices survive login through draft merge')
  page.get_by_role('link',name='Tiến hành thanh toán').click();page.wait_for_selector('#branch-form')
  assert page.locator('#checkout-form').count()==0
  branch=page.locator('#branch-form option').filter(has_text='Bình Thạnh').get_attribute('value')
  stale=post('/checkout/branch',{'storeId':int(branch),'version':draft['version']+1});assert stale.status==409,stale.text()
  assert page.request.get(base+'/cart/data').json()==draft
  checks.append('Stale branch selection rejected without changing the cart')
  page.screenshot(path=str(out/'choose-branch.png'))
  page.locator('#branch-form select').select_option(branch);page.locator('#branch-form button').click();page.wait_for_url('**/checkout?branch=*')
  page.wait_for_selector('#checkout-form')
  selected=page.request.get(base+'/cart/data').json();assert str(selected['storeId'])==branch
  assert selected['items'][0]['name']==draft['items'][0]['name']
  assert selected['items'][0]['size']==draft['items'][0]['size']
  assert selected['items'][0]['quantity']==draft['items'][0]['quantity']
  for name in ['addressId','areaId','carrierId']:
   select=page.locator('#checkout-form select[name='+name+']');select.select_option(select.locator('option').nth(1).get_attribute('value'))
  page.locator('#quote').click();page.wait_for_function("!document.querySelector('#confirm-order').disabled")
  assert 'Tổng thanh toán' in page.locator('#quote-detail').inner_text()
  page.screenshot(path=str(out/'branch-and-quote.png'))
  checks.append('Branch chosen only at checkout; sizes/options preserved and fresh quote confirmed')
 finally:
  post('/cart/clear',{})
  for item in original['items']:post('/cart/draft/items',{k:item[k] for k in ['variantId','sugar','ice','toppingIds','quantity','note']})
 assert not errors,errors
 browser.close()
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors},ensure_ascii=False,indent=2),encoding='utf8')
print('Branch flow checks passed:',len(checks))
