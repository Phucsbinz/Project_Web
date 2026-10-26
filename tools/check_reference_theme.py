from pathlib import Path
import sys,json,time,urllib.request,os
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8080';out=root/'docs/demo/reference-20261026';checks=[];errors=[]
for _ in range(40):
 try:urllib.request.urlopen(base+'/auth/login',timeout=2);break
 except OSError:time.sleep(1)
else:raise RuntimeError('WAR not ready')
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 page=browser.new_page(viewport={'width':1440,'height':1000});page.on('pageerror',lambda e:errors.append(str(e)))
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
  assert page.goto(base+'/',wait_until='networkidle').status==200
  page.evaluate("document.fonts.ready")
  assert page.evaluate("document.fonts.check('14px \"Be Vietnam Pro\"')")
  assert page.locator('.footer-logo').evaluate('(e)=>getComputedStyle(e).color')=='rgb(255, 249, 232)'
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
  page.screenshot(path=str(out/('home-desktop.png' if width>500 else 'home-mobile.png')),full_page=True)
  checks.append({'scenario':'Final shared theme and readable footer logo','width':width})
 page.set_viewport_size({'width':1440,'height':1000})
 assert page.goto(base+'/menu',wait_until='networkidle').status==200
 page.screenshot(path=str(out/'menu-desktop.png'),full_page=True)
 page.locator('.product-card .card-footer a').first.click();page.wait_for_selector('#drink-modal.show')
 frame=page.frame_locator('#drink-modal-frame');frame.locator('#drink-form').wait_for()
 frame.locator('.drink-choices').first.locator('button').last.click()
 assert frame.locator('.drink-choices').first.locator('button').last.get_attribute('aria-pressed')=='true'
 frame.locator('#quantity').fill('2');frame.locator('#quantity').dispatch_event('input')
 assert 'Tạm tính' in frame.locator('#quote-result').inner_text()
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
  assert frame.locator('body').evaluate('(e)=>e.ownerDocument.documentElement.scrollWidth<=e.ownerDocument.defaultView.innerWidth')
  page.screenshot(path=str(out/('popup-desktop.png' if width>500 else 'popup-mobile.png')))
  submit=frame.get_by_role('button',name='Thêm vào giỏ hàng',exact=True)
  submit.scroll_into_view_if_needed();assert submit.is_visible() and submit.is_enabled()
  page.screenshot(path=str(out/('popup-options-desktop.png' if width>500 else 'popup-options-mobile.png')))
  checks.append({'scenario':'Popup size/quantity/price preview and reachable submit; no add-to-cart or checkout submitted','width':width})
 if os.environ.get('UTETRA_DEMO_EMAIL') and os.environ.get('UTETRA_DEMO_PASSWORD'):
  page.goto(base+'/auth/login',wait_until='networkidle')
  page.locator('#email').fill(os.environ['UTETRA_DEMO_EMAIL']);page.locator('#password').fill(os.environ['UTETRA_DEMO_PASSWORD'])
  page.locator('form[data-endpoint="/auth/login"] button[type=submit]').click();page.wait_for_url('**/account/profile')
  for width in [1440,390]:
   page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
   assert page.goto(base+'/admin/reports',wait_until='networkidle').status==200
   assert page.locator('.sidebar-note p').evaluate('(e)=>getComputedStyle(e).color')=='rgb(220, 231, 209)'
   assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
   page.screenshot(path=str(out/('admin-admin-reports.png' if width>500 else 'admin-reports-mobile.png')),full_page=True)
   checks.append({'scenario':'Readable workspace sidebar note and reports','width':width})
 assert not errors,errors
 browser.close()
(out/'final-verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'scope':'Final WAR after footer contrast correction; no cart/order/provider mutation'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Final theme checks passed:',len(checks))
