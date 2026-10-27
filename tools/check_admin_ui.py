"""Read-only admin UX audit on the isolated H2 UI fixture."""
from pathlib import Path
import os,sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/admin-ui';out.mkdir(exist_ok=True)
for _ in range(40):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('Isolated fixture not ready')
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 def login(user):
  ctx=browser.new_context(viewport={'width':1440,'height':1000});csrf=ctx.request.get(base+'/products/csrf').json();assert ctx.request.post(base+'/auth/login',data={'email':user,'password':os.environ['UTETRA_UI_TEST_PASSWORD']},headers={csrf['headerName']:csrf['token']}).ok;return ctx
 admin=login('u4@test.invalid');page=admin.new_page();page.on('pageerror',lambda error:errors.append(str(error)))
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
  for route in ['/admin','/admin/products?storeId=1','/admin/users','/admin/orders','/admin/order-tools','/admin/reports','/admin/categories','/admin/promotions']:
   assert page.goto(base+route,wait_until='networkidle').status==200
   assert page.locator('.site-header').count()==1 and page.locator('.site-footer').count()==1
   assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),(width,route)
   assert page.locator('select.form-select:not([multiple])').evaluate_all("nodes=>nodes.filter(e=>e.getBoundingClientRect().width).every(e=>getComputedStyle(e).backgroundImage!=='none'&&parseFloat(getComputedStyle(e).paddingRight)>=40)")
   if route=='/admin':
    assert page.locator('.admin-shortcuts a').count()==4
    if width==390:
     toggle=page.locator('.workspace-menu-toggle');assert toggle.get_attribute('aria-expanded')=='false';toggle.click();assert page.locator('.workspace-sidebar a').first.is_visible();toggle.click();assert toggle.get_attribute('aria-expanded')=='false'
   if route.startswith('/admin/products'):
    page.wait_for_selector('#management-list article');total=page.locator('#management-list article').count();assert total>0
    page.locator('#management-search').fill('no-such-product-90876');assert page.locator('#management-list article:visible').count()==0
    page.locator('#management-search').fill('');assert page.locator('#management-list article:visible').count()==total
    page.locator('#management-list article').first.get_by_role('button',name='Chỉnh sửa',exact=True).click();assert page.locator('.record-editing').count()==1;assert 'Cập nhật' in page.locator('#catalog-form-title').inner_text()
    page.locator('#management-create').click();assert page.locator('.record-editing').count()==0
   if route in ['/admin','/admin/products?storeId=1','/admin/users']:page.evaluate('window.scrollTo(0,0)');page.screenshot(path=str(out/(route.split('?')[0].strip('/').replace('/','-')+'-'+str(width)+'.png')),full_page=True)
   checks.append({'width':width,'route':route})
 user=login('u1@test.invalid');choice=user.new_page();choice.on('pageerror',lambda error:errors.append(str(error)));choice.goto(base+'/checkout',wait_until='networkidle');choice.wait_for_selector('.choice-toggle')
 toggle=choice.locator('#branch-form .choice-toggle');toggle.click();assert toggle.get_attribute('aria-expanded')=='true';menu=choice.locator('#branch-form .choice-options');assert menu.is_visible() and menu.locator('button').count()>0
 menu.locator('button').first.click();assert toggle.get_attribute('aria-expanded')=='false';assert choice.locator('#branch-form select').input_value()!=''
 toggle.click();choice.locator('[data-choice=branch]').press('ArrowDown');choice.keyboard.press('Escape');assert menu.is_hidden()
 checks.append({'dropdown':'Visible toggle opens all choices; selection synchronizes server ID; keyboard Escape closes menu'})
 browser.close()
assert not errors,errors
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2; read-only UI interactions'},ensure_ascii=False,indent=2),encoding='utf-8')
print('Admin UI checks passed:',len(checks))
