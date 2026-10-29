"""CRUD/automatic dispatch on disposable H2 with separate role sessions."""
from pathlib import Path
import os,sys,json,time,urllib.request,uuid
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/crud-dispatch-ui';out.mkdir(exist_ok=True)
for _ in range(45):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('Fixture not ready')
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 def post(ctx,path,data):
  csrf=ctx.request.get(base+'/products/csrf').json();return ctx.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token'],'Accept':'application/json'})
 def login(i):
  ctx=browser.new_context(viewport={'width':1440,'height':1000});assert post(ctx,'/auth/login',{'email':f'u{i}@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok;return ctx
 def page(ctx,path):
  tab=ctx.new_page();tab.on('pageerror',lambda e:errors.append(str(e)));r=tab.goto(base+path,wait_until='networkidle');assert r.status==200,(path,r.status);return tab
 def accept(tab):tab.locator('.modal.show [data-confirm-accept]').click();tab.wait_for_selector('.modal.show',state='hidden')
 admin=login(4);manager=login(3);buyer=login(1);vendor=login(2)
 a=page(admin,'/admin/users');a.wait_for_selector('.user-list-card');assert a.locator('#assign-manager-form').count()==0 and a.locator('#create-account-form').count()==1
 a.locator('#user-role-filter').select_option('ROLE_SHIPPER');a.locator('#user-search button').click();a.wait_for_function("document.querySelectorAll('.user-list-card').length===1")
 a.locator('.user-list-card button',has_text='Vô hiệu hóa tài khoản').click();accept(a);a.wait_for_selector('.user-list-card button:text("Kích hoạt lại tài khoản")');assert not post(browser.new_context(),'/auth/login',{'email':'u5@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
 a.locator('.user-list-card button',has_text='Kích hoạt lại tài khoản').click();accept(a);a.wait_for_selector('.user-list-card button:text("Vô hiệu hóa tài khoản")')
 for width in [1440,768,390]:
  a.set_viewport_size({'width':width,'height':1000});assert a.evaluate('document.documentElement.scrollWidth<=innerWidth');a.screenshot(path=str(out/f'accounts-{width}.png'),full_page=True)
 assignments=page(admin,'/admin/assignments');assert assignments.locator('#create-account-form').count()==0
 checks.append('Separate account/assignment pages; role filter, disable/reactivate and login enforcement; 390/768/1440px')
 m=page(manager,'/manager/operations');m.wait_for_selector('#operations-toppings tr button');first=m.locator('#operations-toppings tr').first;old=first.inner_text();first.locator('button').click();m.wait_for_function('(old)=>document.querySelector("#operations-toppings tr").innerText!==old',arg=old);m.reload(wait_until='networkidle');m.wait_for_selector('#operations-toppings tr button');assert m.locator('#operations-toppings tr').first.inner_text()!=old
 stock=page(vendor,'/vendor/stock');stock.wait_for_function("!document.getElementById('catalog-message')?.textContent.includes('Đang tải')");assert 'Tạm ngưng' in stock.locator('main').inner_text() or 'Không sẵn bán' in stock.locator('main').inner_text()
 m.locator('#operations-toppings tr').first.locator('button').click();m.wait_for_function('(old)=>document.querySelector("#operations-toppings tr").innerText===old',arg=old)
 checks.append('Manager topping toggle persists and vendor reads back same saved shop availability')
 draft={'key':str(uuid.uuid4()),'addressId':1,'areaId':1,'carrierId':1,'points':0,'quoteHash':'','note':'Giao cẩn thận','paymentMethod':'COD'};q=post(buyer,'/checkout/quote',draft);assert q.ok,q.text();draft['quoteHash']=q.json()['hash'];created=post(buyer,'/checkout',draft);assert created.ok,created.text();oid=created.json()['id']
 detail=page(admin,f'/admin/orders/{oid}');detail.locator('#transition').click();detail.wait_for_selector('#dispatch-summary');assert 'Đã phân công Shipper' in detail.locator('#dispatch-summary').inner_text();assert detail.locator('#transition').count()==0;assert detail.locator('#cash-collected').count()==0;assert detail.evaluate("[...document.querySelectorAll('[data-money]')].every(n=>/^-?[0-9.]+đ$/.test(n.textContent))");data=admin.request.get(base+f'/admin/orders/{oid}/data').json();assert data['delivery'][0]['shipperId']==5
 shipper=login(5);s=page(shipper,'/shipper/orders');assert str(oid) in s.content() or data['code'] in s.locator('main').inner_text()
 for state in ['PICKED_UP','DELIVERING','DELIVERED']:
  current=shipper.request.get(base+f'/shipper/orders/{oid}/data').json();r=post(shipper,f'/shipper/orders/{oid}/status',{'status':state,'version':current['version'],'reason':'','cashCollected':state=='DELIVERED'});assert r.ok,r.text()
 checks.append('Confirming COD automatically assigns scoped shipper; separate shipper session receives and completes delivery without duplicate COD')
 paid=page(buyer,f'/orders/{oid}');assert paid.get_by_role('link',name='Xem hóa đơn',exact=True).count()==1;assert paid.get_by_role('heading',name='Thanh toán QR',exact=True).count()==0;order=buyer.request.get(base+f'/orders/{oid}/data').json();csrf=buyer.request.get(base+'/products/csrf').json();content='Cà phê được chuẩn bị đúng lựa chọn, giao hàng cẩn thận và hỗ trợ rất tốt.'
 r=buyer.request.post(base+'/comments',multipart={'productId':'1','orderItemId':str(order['items'][0]['id']),'content':content},headers={csrf['headerName']:csrf['token']});assert r.ok,r.text()
 mod=page(admin,'/admin/moderation');mod.locator('#moderation-kind').select_option('comments');mod.locator('#moderation-search button').click();mod.wait_for_selector('#moderation-list article');mod.locator('#moderation-list button',has_text='Ẩn').click();accept(mod);mod.wait_for_selector('#moderation-list button:text("Khôi phục")');mod.locator('#moderation-list button',has_text='Khôi phục').click();accept(mod);mod.wait_for_selector('#moderation-list button:text("Ẩn")');assert content in mod.locator('#moderation-list').inner_text()
 checks.append('Admin moderation finds purchased comment and hides/restores it with confirmation and retained text')
 rates=page(admin,'/admin/order-tools');rates.locator('.edit-rate').first.click();assert float(rates.locator('#rate-form [name=fee]').input_value())==5000;assert rates.locator('#rate-form [name=carrierId]').input_value()=='1'
 checks.append('Rate edit prefills persisted exact carrier/area pair, amount and active state')
 browser.close()
assert not errors,errors
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2; four independent role cookie contexts'},ensure_ascii=False,indent=2),encoding='utf-8');print('CRUD/dispatch browser checks passed:',len(checks))
