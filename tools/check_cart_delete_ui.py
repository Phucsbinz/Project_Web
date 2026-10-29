from pathlib import Path
import sys,time,urllib.request,os,json
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882'
for _ in range(50):
 try:urllib.request.urlopen(base+'/api/system/health',timeout=2);break
 except OSError:time.sleep(1)
with sync_playwright() as p:
 b=p.chromium.launch(channel='msedge',headless=True)
 for signed in [False,True]:
  c=b.new_context();t=c.new_page();errors=[];replies=[];t.on('pageerror',lambda e:errors.append(str(e)));t.on('response',lambda r:replies.append((r.url.split(base)[-1],r.status)) if '/cart/' in r.url else None)
  def post(path,data):
   csrf=c.request.get(base+'/products/csrf').json();r=c.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token'],'Accept':'application/json'});assert r.ok,(path,r.status,r.text()[:200]);return r.json()
  if signed:post('/auth/login',{'email':'u1@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']});post('/cart/clear',{})
  for sugar in [50,0]:post('/cart/draft/items',{'variantId':1,'sugar':sugar,'ice':50,'toppingIds':[1],'quantity':1,'note':''})
  t.goto(base+'/cart',wait_until='networkidle');before=t.locator('.cart-line').count();t.locator('.delete-item').first.click();t.wait_for_timeout(2000);after=t.locator('.cart-line').count();print(json.dumps({'signed':signed,'before':before,'after':after,'errors':errors,'responses':replies,'message':t.locator('#order-message').inner_text()},ensure_ascii=True));assert after==before-1
  t.locator('#clear-cart').click();t.wait_for_selector('.modal.show');t.locator('.modal.show [data-confirm-accept]').click();t.wait_for_selector('.cart-empty');assert c.request.get(base+'/cart/data').json()['items']==[]
  assert not errors,errors
 b.close()
