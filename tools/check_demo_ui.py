from pathlib import Path
import sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8080';out=root/'docs/demo/ui';out.mkdir(exist_ok=True)
checks=[];errors=[]
for i in range(30):
 try:
  urllib.request.urlopen(base+'/auth/login',timeout=3);break
 except Exception:time.sleep(1)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless=True)
 page=browser.new_page(viewport={'width':1440,'height':1000})
 page.on('pageerror',lambda error:errors.append(str(error)))
 def shot(route,name,width=1440):
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
  r=page.goto(base+route,wait_until='networkidle');assert r.status==200,(route,r.status)
  page.evaluate("document.querySelectorAll('img[loading=lazy]').forEach(i=>i.loading='eager')")
  page.wait_for_function("Array.from(document.images).every(i=>i.complete)",timeout=20000)
  assert page.evaluate("Array.from(document.images).filter(i=>i.getAttribute('src')).every(i=>i.naturalWidth>0)"),('broken image',route)
  page.screenshot(path=str(out/(name+'.png')),full_page=True)
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),('overflow',route,width)
  checks.append({'route':route,'width':width,'status':r.status})
 shot('/','home-desktop')
 assert page.locator('.product-card').count()>0,'Home best sellers empty'
 shot('/menu','menu-desktop')
 page.wait_for_function("document.querySelector('#menu-cart-items') && !document.querySelector('#menu-cart-items').textContent.includes('Đang tải')")
 product=page.locator('.product-card .card-footer a').first.get_attribute('href')
 shot(product,'product-desktop')
 page.wait_for_function("document.querySelector('#product-community [role=status]').textContent.includes('Đã bán')")
 assert page.locator('#product-community [data-list=reviews] article').count()>0
 assert page.locator('#product-community [data-list=comments] article').count()>0
 shot('/','home-mobile',390);shot('/menu','menu-mobile',390);shot(product,'product-mobile',390)
 shot('/menu','menu-tablet',768)
 page.set_viewport_size({'width':1440,'height':1000})
 for role,routes in [('thanh',['/account/profile','/account/security','/orders']),('admin',['/admin/reports','/admin/orders','/admin/products']),('manager',['/manager/reports','/manager/orders','/manager/products']),('vendor',['/vendor/revenue','/vendor/orders','/vendor/products'])]:
  page.context.clear_cookies();page.goto(base+'/auth/login',wait_until='networkidle')
  page.locator('#email').fill('demo.'+role+'@example.test');page.locator('#password').fill('123456');page.locator('form[data-endpoint="/auth/login"] button[type=submit]').click();page.wait_for_url('**/account/profile',timeout=20000)
  for route in routes:shot(route,role+'-'+route.strip('/').replace('/','-'))
  if role=='admin':
   shot('/admin/orders','admin-orders-mobile',390)
   shot('/admin/reports','admin-reports-mobile',390)
  if role=='thanh':
   page.goto(base+'/account/security',wait_until='networkidle');assert page.get_by_role('link',name='Quên mật khẩu hiện tại?').count()==1
  page.set_viewport_size({'width':1440,'height':1000})
 assert not errors,errors
 browser.close()
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors},ensure_ascii=False,indent=2),encoding='utf8')
print('Browser checks passed:',len(checks))
