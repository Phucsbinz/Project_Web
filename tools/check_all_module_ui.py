"""Audit all module pages on the isolated AllUiServer H2 fixture (port 8882)."""
from pathlib import Path
import sys,json,os,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'docs/demo/all-modules-20261026';out.mkdir(parents=True,exist_ok=True)
password=os.environ.get('UTETRA_UI_TEST_PASSWORD')
if not password:raise SystemExit('Set UTETRA_UI_TEST_PASSWORD for the isolated fixture; no credentials are logged.')
for _ in range(35):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
  time.sleep(1)
 except OSError:time.sleep(1)
else:raise RuntimeError('H2 UI server not ready')
metadata=json.loads((root/'.local/all-ui-server.json').read_text())
assert metadata['port']==8882
order=str(metadata['orderId'])
routes={
 'guest':['/','/menu','/stores','/stores/1','/products/1','/cart','/auth/login','/auth/register','/auth/forgot','/auth/otp','/auth/reset'],
 'user':['/','/cart','/checkout','/checkout?branch=1','/orders','/orders/'+order,'/account/profile','/account/security','/account/addresses','/account/loyalty','/favorites','/viewed','/reviews','/comments','/account/shop-registration','/notifications'],
 'vendor':['/vendor','/vendor/products','/vendor/stores','/vendor/shop','/vendor/promotions','/vendor/areas','/vendor/stock','/vendor/orders','/vendor/orders/'+order,'/vendor/revenue','/cart','/account/profile','/account/shop-registration','/notifications'],
 'manager':['/manager','/manager/products','/manager/categories','/manager/toppings','/manager/promotions','/manager/stores','/manager/areas','/manager/stock','/manager/users','/manager/order-tools','/manager/orders','/manager/orders/'+order,'/manager/reports','/notifications','/account/profile','/account/security'],
 'admin':['/admin','/admin/products','/admin/categories','/admin/toppings','/admin/promotions','/admin/stores','/admin/areas','/admin/stock','/admin/memberships','/admin/users','/admin/order-tools','/admin/orders','/admin/orders/'+order,'/admin/reports','/notifications','/account/profile','/account/security'],
 'shipper':['/shipper/orders','/shipper/reports','/notifications','/account/profile','/account/security']}
ids={'user':1,'vendor':2,'manager':3,'admin':4,'shipper':5}
checks=[];errors=[];failures=[]
shots={'/auth/register','/auth/forgot','/account/addresses','/account/loyalty','/favorites','/viewed','/reviews','/comments','/account/shop-registration','/notifications','/checkout?branch=1','/manager/users','/manager/order-tools','/admin/memberships','/admin/toppings','/admin/areas','/admin/stock','/shipper/reports','/shipper/orders'}
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 for role,paths in routes.items():
  context=browser.new_context(viewport={'width':1440,'height':1000})
  page=context.new_page();page.on('pageerror',lambda e,role=role:errors.append({'role':role,'error':str(e)}))
  page.on('response',lambda r,role=role:errors.append({'role':role,'url':r.url.split(base)[-1],'status':r.status}) if r.status>=400 and ('/api/' in r.url or '/assets/' in r.url) else None)
  if role!='guest':
   csrf=context.request.get(base+'/products/csrf').json()
   response=context.request.post(base+'/auth/login',data={'email':'u'+str(ids[role])+'@test.invalid','password':password},headers={csrf['headerName']:csrf['token']})
   assert response.ok,(role,'fixture login',response.status)
  for width in [1440,390]:
   page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
   for path in paths:
    result={'role':role,'path':path,'width':width}
    try:
     target=path+('?storeId=1' if path in ['/admin/products','/admin/areas','/admin/stock','/admin/memberships'] else '')
     response=page.goto(base+target,wait_until='networkidle');result['status']=response.status
     if page.locator('#management-message').count():assert 'alert-danger' not in (page.locator('#management-message').get_attribute('class') or ''),page.locator('#management-message').inner_text()
     if path in ['/admin/products','/admin/areas','/admin/stock','/admin/memberships']:assert page.locator('#management-form input,#management-form select').count()>0,'selected shop form missing'
     assert response.status==200,('HTTP',response.status)
     assert page.locator('.site-header').count()==1 and page.locator('.site-footer').count()==1,'missing or duplicate shared layout'
     assert page.locator('main#main-content').count()==1,'missing decorated content'
     assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),('horizontal overflow',page.evaluate('document.documentElement.scrollWidth'))
     page.evaluate('document.fonts.ready')
     assert page.evaluate("document.fonts.check('14px \"Be Vietnam Pro\"')"),'font unavailable'
     assert page.locator('body').inner_text().find('<%@')==-1,'uncompiled JSP'
     if role in ['admin','manager','vendor'] and path.startswith('/'+role):
      for suffix in ['/areas','/stock']:
       assert page.locator('.workspace-sidebar a[href="/'+role+suffix+'"]').count()==1,'missing sidebar module '+suffix
     if role=='admin' and path.startswith('/admin'):
      assert page.locator('.workspace-sidebar a[href="/admin/memberships"]').count()==1,'missing manager delegation UI'
     if path=='/shipper/reports':assert page.locator('.report-metrics article').count()==4
     result['passed']=True
     if path in shots and (role in ['guest','user','shipper'] or path.startswith('/'+role)):
      file=role+'-'+path.strip('/').replace('/','-').replace('?','-').replace('=','-')+'-'+str(width)+'.png'
      page.screenshot(path=str(out/file),full_page=True);result['screenshot']=file
    except Exception as e:
     result['passed']=False;result['error']=str(e);failures.append(result)
    checks.append(result)
  context.close()
 browser.close()
(out/'verification.json').write_text(json.dumps({'fixture':'Isolated H2, port 8882; shipper enabled only in fixture; Manager catalog delegation assigned in fixture','checks':checks,'pageErrors':errors,'failures':failures,'limits':'Render, layout, navigation and responsive checks; not all mutation/provider workflows; no cloud reset or provider call.'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('UI checks:',len(checks),'Failures:',len(failures),'JS errors:',len(errors))
for f in failures:print(f['role'],f['path'],f['width'],f['error'][:220])
assert not failures and not errors
