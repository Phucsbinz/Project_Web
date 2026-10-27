from pathlib import Path
import sys,json,os,time,urllib.request,uuid
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/shopping-ui';out.mkdir(exist_ok=True)
for _ in range(40):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('UI server not ready')
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 def post(ctx,path,data):
  csrf=ctx.request.get(base+'/products/csrf').json()
  return ctx.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token'],'Accept':'application/json'})
 def page(ctx):
  value=ctx.new_page();value.on('pageerror',lambda e:errors.append(str(e)));return value
 def pay_local(merchant):
  link=merchant.locator('#open-local-wallet');merchant.wait_for_function("!document.querySelector('#open-local-wallet').hidden")
  assert link.get_attribute('href').startswith(base+'/local-wallet/pay/')
  from urllib.parse import urlparse,parse_qs
  address=urlparse(link.get_attribute('href'));public_id=address.path.rsplit('/',1)[1];capability=parse_qs(address.fragment)['token'][0]
  assert post(guest,'/local-wallet/api/'+public_id+'/status',{'token':capability}).ok
  assert guest.request.post(base+'/local-wallet/api/'+public_id+'/confirm',data={'token':capability,'expectedAmount':1,'acknowledged':True}).status==403
  with merchant.expect_popup() as event:link.click()
  wallet_page=event.value;wallet_page.wait_for_selector('#wallet-amount');wallet_page.wait_for_function("document.querySelector('#wallet-amount').textContent.length>0");assert wallet_page.locator('#wallet-confirm').is_disabled();wallet_page.locator('#wallet-acknowledged').check();wallet_page.locator('#wallet-confirm').click();wallet_page.wait_for_function("document.querySelector('#wallet-state').textContent.includes('đã thanh toán')||document.querySelector('#wallet-state').textContent.includes('hoàn tất')")
  wallet_page.close();merchant.bring_to_front()
 guest=browser.new_context();gp=page(guest);gp.goto(base+'/menu',wait_until='networkidle')
 gp.locator('.product-card .card-footer a').first.click();gp.wait_for_selector('#drink-modal.show')
 frame=gp.frame_locator('#drink-modal-frame');frame.get_by_role('button',name='Thêm vào giỏ hàng',exact=True).click()
 gp.wait_for_selector('#drink-modal',state='hidden');gp.wait_for_function("document.querySelector('[data-cart-count]').textContent==='1'")
 assert gp.locator('#site-toast').is_visible();assert '1 giờ' in gp.locator('#site-toast').inner_text();checks.append('Guest popup updates badge and shows success with one-hour retention reminder')
 gp.goto(base+'/cart',wait_until='networkidle');assert gp.locator('.cart-line').count()==1;assert gp.locator('.cart-line img').count()==1
 gp.get_by_role('button',name='Tăng số ly',exact=True).click();gp.wait_for_load_state('networkidle');gp.wait_for_function("document.querySelector('.quantity').value==='2'")
 assert gp.locator('.nav-actions [data-cart-count]').inner_text()=='1'
 checks.append('Stepper updates quantity and totals; badge counts lines rather than cups')
 assert post(guest,'/cart/draft/items',{'variantId':1,'sugar':0,'ice':0,'toppingIds':[],'quantity':1,'note':'Later'}).ok
 gp.reload(wait_until='networkidle');gp.locator('.cart-item-select').last.uncheck();gp.wait_for_function("!document.querySelector('.cart-item-select').disabled")
 rows=guest.request.get(base+'/cart/data').json()['items'];assert sum(1 for item in rows if item['selected'])==1
 assert gp.locator('[data-cart-countdown]').inner_text().startswith(('Còn 59:','Còn 60:'))
 checks.append('Checkbox persists selected subset; countdown uses server time rather than the fixture/client clock difference')
 gp.on('dialog',lambda dialog:dialog.accept());gp.locator('#clear-cart').click();gp.locator('.modal [data-bs-dismiss=modal]').last.click();gp.wait_for_selector('.modal',state='detached');assert gp.locator('.cart-line').count()>0;gp.locator('#clear-cart').click();gp.locator('[data-confirm-accept]').click();gp.wait_for_selector('.cart-empty');assert gp.locator('.nav-actions [data-cart-count]').is_hidden()
 checks.append('Clearing guest cart updates empty state and hides badge')
 contexts=[]
 for user in [1,6,2]:
  ctx=browser.new_context();result=post(ctx,'/auth/login',{'email':'u'+str(user)+'@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']})
  assert result.ok
  assert result.json()['redirect']==('/' if user!=2 else '/account/profile')
  assert post(ctx,'/cart/clear',{}).ok
  assert post(ctx,'/cart/draft/items',{'variantId':1,'sugar':50,'ice':50,'toppingIds':[1],'quantity':1,'note':'No straw'}).ok
  contexts.append(ctx)
 checks.append('Customer login returns home; vendor destination preserved')
 host,member,vendor=contexts
 created=post(host,'/group-orders',{});assert created.ok;group=created.json();token=group['token']
 def state():return host.request.get(base+'/group-orders/'+token+'/data').json()
 assert state()['memberCount']==1 and state()['participants']==0
 # Joining never copies any personal cart: only explicit picks inside the group count.
 for ctx in [member,vendor]:
  result=post(ctx,'/group-orders/'+token+'/join',{'version':state()['version']});assert result.ok,result.text()
 assert state()['memberCount']==3 and state()['participants']==0
 for ctx in [host,member,vendor]:
  assert post(ctx,'/group-orders/'+token+'/items',{'version':state()['version'],'item':{'variantId':1,'sugar':50,'ice':50,'toppingIds':[1],'quantity':1,'note':''}}).ok
 assert state()['participants']==3 and state()['percent']==5
 assert post(member,'/group-orders/'+token+'/lock',{'version':state()['version']}).status==403
 hp=page(host);hp.goto(base+'/group-orders/'+token,wait_until='networkidle')
 assert hp.locator('.account-display-name').inner_text()=='User 1';assert hp.locator('.account-display-name').get_attribute('title')=='User 1';assert hp.locator('.account-display-name').evaluate("n=>getComputedStyle(n).whiteSpace==='nowrap'&&getComputedStyle(n).textOverflow==='ellipsis'");checks.append('Authenticated header shows account full name with single-line ellipsis and full-name title')
 for width in [1440,390]:
  hp.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert hp.evaluate('document.documentElement.scrollWidth<=innerWidth');hp.screenshot(path=str(out/('group-'+str(width)+'.png')),full_page=True)
 checks.append('Three distinct contributors, host-only finalization and group page responsive')
 assert post(host,'/group-orders/'+token+'/lock',{'version':0}).status==409
 result=post(host,'/group-orders/'+token+'/lock',{'version':state()['version']});assert result.ok,result.text()
 hp.goto(base+'/cart',wait_until='networkidle')
 for width in [1440,390]:
  hp.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert hp.locator('.cart-line img').evaluate('(img)=>img.complete&&img.naturalWidth>0');assert hp.evaluate('document.documentElement.scrollWidth<=innerWidth');hp.screenshot(path=str(out/('cart-'+str(width)+'.png')),full_page=True)
 assert host.request.get(base+'/cart/count').json()=={'count':1,'cups':3}
 checks.append('Finalized cart has real images/prices and responsive summary at 390/1440px')
 cart=host.request.get(base+'/cart/data').json();assert post(host,'/checkout/branch',{'storeId':1,'version':cart['version']}).ok
 draft={'key':str(uuid.uuid4()),'addressId':1,'areaId':1,'carrierId':1,'productPromotionId':None,'shippingPromotionId':None,'points':0,'quoteHash':'','note':'H2 UI test','paymentMethod':'COD'}
 quote=post(host,'/checkout/quote',draft);assert quote.ok,quote.text();q=quote.json();assert q['group']['participants']==3 and q['group']['discount']==6000
 draft['quoteHash']=q['hash'];order=post(host,'/checkout',draft);assert order.ok,order.text()
 assert state()['status']=='ORDERED'
 hp.goto(base+'/cart',wait_until='networkidle');assert hp.locator('[data-cart-count]').last.is_hidden()
 checks.append('Group discount is applied by checkout, order saved in isolated H2, badge clears after checkout')
 # A member keeps their personal cart; checkout of selected lines must preserve unchecked items.
 assert post(member,'/cart/draft/items',{'variantId':1,'sugar':0,'ice':0,'toppingIds':[],'quantity':1,'note':'Keep later'}).ok
 for notice in member.request.get(base+'/group-orders/payment-notices').json():member.add_init_script('localStorage.setItem('+json.dumps('utetra.group.notice.'+notice['token']+'.'+str(notice['member_id']))+',"shown")')
 mp=page(member);mp.goto(base+'/cart',wait_until='networkidle');mp.locator('.cart-item-select').last.uncheck();mp.wait_for_function("!document.querySelector('.cart-item-select').disabled")
 state_cart=member.request.get(base+'/cart/data').json();assert post(member,'/checkout/branch',{'storeId':1,'version':state_cart['version']}).ok
 mp.goto(base+'/checkout?branch=1',wait_until='networkidle');
 if mp.locator('.modal.show #utetra-confirm-title').count():mp.locator('.modal.show [data-bs-dismiss=modal]').last.click()
 address_form=mp.locator('#checkout-form');address_form.locator('[name=recipientName]').fill('Nguyễn Minh Anh');address_form.locator('[name=phone]').fill('0901234567');address_form.locator('[name=addressDetail]').fill('12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM')
 mp.locator('[data-choice=areaId]').fill('Khu vực không tồn tại');assert not mp.locator('[data-choice=areaId]').evaluate('e=>e.checkValidity()')
 mp.locator('[data-choice=areaId]').fill('Area A');mp.locator('[data-choice=carrierId]').fill('Carrier');mp.locator('#checkout-form [name=points]').fill('0');mp.locator('#checkout-form [name=note]').fill('Gọi trước khi giao');mp.locator('[data-choice=productPromotionId]').fill('Không dùng');mp.locator('[data-choice=shippingPromotionId]').fill('Không dùng');mp.locator('[name=paymentMethod][value=LOCAL_QR]').check();assert mp.locator('[name=paymentMethod]:checked').input_value()=='LOCAL_QR'
 try:mp.wait_for_selector('#quote-detail[data-state=ready]',timeout=10000)
 except Exception:
  print('CHECKOUT_DIAGNOSTIC',json.dumps({'message':mp.locator('#order-message').text_content(),'form':mp.locator('#checkout-form').evaluate("f=>({values:Object.fromEntries(new FormData(f)),invalid:[...f.elements].filter(e=>e.validity&&!e.validity.valid).map(e=>({name:e.name,message:e.validationMessage}))})"),'errors':errors},ensure_ascii=True))
  mp.screenshot(path=str(out/'checkout-failure.png'),full_page=True);raise
 assert mp.locator('#confirm-order').is_enabled();assert mp.locator('[name=addressId]').input_value()!='';assert mp.locator('[name=recipientName]').input_value()=='Nguyễn Minh Anh'
 mp.locator('[name=addressDetail]').click();mp.wait_for_selector('#address-suggestions:not([hidden])');mp.locator('#address-suggestions button').filter(has_text='12 Nguyễn Văn Bảo').first.click();assert mp.locator('[name=phone]').input_value()=='0901234567'
 saved_options=mp.locator('#address-suggestions button').count();mp.locator('[name=note]').fill('Gọi trước khi giao, giữ nguyên địa chỉ');assert mp.locator('#confirm-order').is_disabled();assert mp.locator('#quote').count()==0;mp.wait_for_selector('#quote-detail[data-state=ready]');assert mp.locator('#address-suggestions button').count()==saved_options
 for width in [1440,390]:
  mp.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert mp.evaluate('document.documentElement.scrollWidth<=innerWidth');mp.screenshot(path=str(out/('checkout-input-'+str(width)+'.png')),full_page=True)
 mp.locator('#confirm-order').click();
 try:mp.wait_for_url('**/orders/*/qr-payment')
 except Exception:
  print('AUTOQUOTE_DIAGNOSTIC',json.dumps({'errors':errors,'panel':mp.locator('#quote-detail').inner_text(),'form':mp.locator('#checkout-form').evaluate('f=>({values:Object.fromEntries(new FormData(f)),invalid:[...f.elements].filter(e=>!e.validity.valid).map(e=>e.name)})')},ensure_ascii=True));raise
 left=member.request.get(base+'/cart/data').json()['items'];assert len(left)==1 and left[0]['note']=='Keep later' and not left[0]['selected']
 mp.wait_for_selector('.local-qr-image svg');assert mp.locator('#local-qr-expiry').inner_text()!='';mp.set_viewport_size({'width':1440,'height':1000});mp.screenshot(path=str(out/'qr-payment-1440.png'),full_page=True);mp.set_viewport_size({'width':390,'height':844});mp.screenshot(path=str(out/'qr-payment-390.png'),full_page=True);assert mp.evaluate('document.documentElement.scrollWidth<=innerWidth');mp.set_viewport_size({'width':1440,'height':1000});pay_local(mp);mp.wait_for_function("document.querySelector('#local-qr-state').textContent.includes('hoàn tất')");checks.append('Personal local QR uses tokenized wallet link, optional promotions and automatic quote; unchecked cart lines preserved')
 mp.evaluate("Object.defineProperty(window.crypto,'randomUUID',{value:undefined,configurable:true})");assert mp.evaluate("/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(utetra.uuid())")
 checks.append('UUID fallback supports LAN HTTP where crypto.randomUUID is unavailable')
 # Personal COD flow starts from the actual cart link and uses the same automatic bill panel.
 mp.goto(base+'/cart',wait_until='networkidle');mp.locator('.cart-item-select').first.check();mp.wait_for_function("!document.querySelector('.cart-item-select').disabled");mp.locator('#cart-checkout').click();mp.wait_for_url('**/checkout')
 mp.locator('[data-choice=branch]').fill('Shop 1 — Address');mp.locator('#branch-form > button').click();mp.wait_for_url('**/checkout?branch=1');mp.wait_for_load_state('networkidle')
 assert mp.locator('#quote-detail').is_visible();assert mp.locator('#quote').count()==0;assert mp.locator('#confirm-order').is_disabled();assert mp.locator('[name=paymentMethod][value=COD]').is_checked()
 mp.locator('[name=addressDetail]').click();mp.locator('#address-suggestions button').first.click();mp.locator('[data-choice=areaId]').fill('Area A');mp.locator('[data-choice=carrierId]').fill('Carrier');mp.wait_for_selector('#quote-detail[data-state=ready]')
 assert '50.000' in mp.locator('[data-bill=total]').inner_text();assert mp.locator('[data-choice=productPromotionId]').is_visible();mp.locator('#confirm-order').click();mp.wait_for_url('**/orders/*');assert member.request.get(base+'/cart/data').json()['items']==[]
 checks.append('Personal COD checkout from cart uses same automatic bill as group: incomplete form blocks submit, saved contact fills, total auto-updates and order clears only purchased cart')
 receipt_id=int(mp.url.rsplit('/',1)[1])
 assert post(member,'/orders/'+str(receipt_id)+'/received',{}).status==409
 for status in ['CONFIRMED','PICKED_UP','DELIVERING','DELIVERED']:
  state=vendor.request.get(base+'/vendor/orders/'+str(receipt_id)+'/data').json()
  assert post(vendor,'/vendor/orders/'+str(receipt_id)+'/status',{'status':status,'version':state['version'],'reason':'','cashCollected':status=='DELIVERED'}).ok
 mp.wait_for_selector('#confirm-received',timeout=22000)
 mp.locator('#confirm-received').click();mp.wait_for_selector('.modal.show [data-confirm-accept]');mp.locator('.modal.show [data-confirm-accept]').click();mp.wait_for_load_state('networkidle');mp.wait_for_function("document.body.textContent.includes('Bạn đã xác nhận nhận hàng lúc')")
 mp.get_by_role('link',name='Đã nhận hàng',exact=True).click();mp.wait_for_url('**/orders?status=RECEIVED');assert mp.locator('tbody tr').count()==1
 checks.append('Buyer sees operator delivery update automatically, confirms own delivered order, and finds it in Received orders')

 # Full UI flow: homepage -> create -> join first -> group menu -> host chooses COD split -> one order.
 hp.goto(base+'/',wait_until='networkidle');assert hp.locator('.hero-actions a').all_text_contents()==['Khám phá thực đơn →','Đặt đơn','Đặt đơn nhóm'];assert hp.locator('#highlights-title em').count()==0;hp.get_by_role('link',name='Đặt đơn nhóm',exact=True).click();hp.wait_for_url('**/group-orders/new')
 hp.locator('#group-payment-mode').select_option('INDIVIDUAL');hp.locator('#create-group').click();hp.wait_for_url('**/group-orders/*')
 hp.wait_for_selector('.group-page');newtoken=hp.locator('.group-page').get_attribute('data-token')
 def newstate():return host.request.get(base+'/group-orders/'+newtoken+'/data').json()
 assert newstate()['memberCount']==1 and newstate()['participants']==0
 mp.goto(base+'/group-orders/'+newtoken,wait_until='networkidle');mp.get_by_role('button',name='Tham gia nhóm',exact=True).click();mp.wait_for_load_state('networkidle');mp.wait_for_selector('a[href*="/menu?group="]')
 assert newstate()['participants']==0
 mp.get_by_role('link',name='Chọn món cho tôi',exact=True).click();mp.wait_for_selector('.product-card');assert mp.locator('input[name=group]').input_value()==newtoken
 mp.locator('.product-card').filter(has_text='Milk tea 1').locator('.card-footer a').click();mp.wait_for_selector('#drink-modal.show');gf=mp.frame_locator('#drink-modal-frame');gf.locator('[name=toppingIds]').first.check();gf.get_by_role('button',name='Thêm vào nhóm',exact=True).click()
 try:mp.wait_for_selector('#drink-modal',state='hidden',timeout=10000)
 except Exception:
  print('GROUP_ADD_DIAGNOSTIC',json.dumps({'result':gf.locator('#quote-result').inner_text(),'errors':errors,'group':newstate()},ensure_ascii=True));raise
 assert newstate()['participants']==1;assert member.request.get(base+'/cart/count').json()['cups']==0
 for ctx in [host,vendor]:
  if ctx==vendor:assert post(ctx,'/group-orders/'+newtoken+'/join',{'version':newstate()['version']}).ok
  assert post(ctx,'/group-orders/'+newtoken+'/items',{'version':newstate()['version'],'item':{'variantId':1,'sugar':50,'ice':50,'toppingIds':[1],'quantity':1,'note':''}}).ok
 hp.goto(base+'/group-orders/'+newtoken,wait_until='networkidle');hp.on('dialog',lambda d:d.accept());hp.get_by_role('button',name='Chốt nhóm và thanh toán',exact=True).click();hp.locator('[data-confirm-accept]').click();hp.wait_for_url('**/checkout')
 hp.locator('[data-choice=branch]').fill('Shop 1 — Address');hp.locator('#branch-form > button').click();hp.wait_for_url('**/checkout?branch=1');hp.wait_for_load_state('networkidle')
 assert hp.locator('[name=paymentMethod][value=LOCAL_QR]').count()==1;assert hp.locator('[data-choice=productPromotionId]').count()==0;assert hp.locator('[name=productPromotionId]').input_value()=='';assert hp.locator('.checkout-group-discount').is_visible()
 hp.locator('[name=addressDetail]').click();hp.locator('#address-suggestions [data-address-id="1"]').click();hp.locator('[data-choice=areaId]').fill('Area A');hp.locator('[data-choice=carrierId]').fill('Carrier');hp.locator('[name=paymentMethod][value=LOCAL_QR]').check();hp.wait_for_selector('#quote-detail[data-state=ready]')
 for width in [1440,390]:
  hp.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert hp.evaluate('document.documentElement.scrollWidth<=innerWidth')
 hp.locator('#confirm-order').click();hp.wait_for_url('**/orders/*/qr-payment');hp.wait_for_selector('.local-qr-image svg');bill=newstate();assert bill['status']=='ORDERED';assert bill['paymentMode']=='INDIVIDUAL'
 assert all(m['amountDue']==43000 and m['discount']==2000 for m in bill['members'])
 mp.bring_to_front();
 try:mp.wait_for_selector('.modal.show #utetra-confirm-title',timeout=20000)
 except Exception:
  print('NOTICE_DIAGNOSTIC',json.dumps({'page':mp.url,'state':mp.evaluate('({role:document.body.dataset.currentRole,hidden:document.hidden,modals:[...document.querySelectorAll(".modal.show")].map(n=>n.id),storage:{...localStorage},confirm:typeof window.utetraConfirm})'),'notices':member.request.get(base+'/group-orders/payment-notices').text(),'errors':errors},ensure_ascii=True));raise
 assert 'Phần của bạn' in mp.locator('.modal.show .modal-body').inner_text();mp.locator('.modal.show [data-confirm-accept]').click();mp.wait_for_url('**/orders/*/qr-payment');mp.wait_for_selector('.local-qr-image svg');checks.append('Split QR group creates order without member confirmation, host sees own QR share, member receives popup on another customer page and opens own QR instructions')
 # Every participant must pay their own immutable server-priced share before an operator confirms.
 order_id=int(hp.url.split('/orders/')[1].split('/')[0])
 def group_order_state():return vendor.request.get(base+'/vendor/orders/'+str(order_id)+'/data').json()
 def try_confirm():return post(vendor,'/vendor/orders/'+str(order_id)+'/status',{'status':'CONFIRMED','version':group_order_state()['version'],'reason':'','cashCollected':False})
 assert try_confirm().status==409
 pay_local(hp);assert try_confirm().status==409;pay_local(mp);assert try_confirm().status==409
 vp=page(vendor);vp.goto(base+'/orders/'+str(order_id)+'/qr-payment',wait_until='networkidle');vp.wait_for_selector('.local-qr-image svg');pay_local(vp)
 hp.bring_to_front();hp.wait_for_url("**/group-orders/*");hp.wait_for_function("document.querySelector('.group-page')?.dataset.paidCount==='3'")
 assert group_order_state()['paymentStatus']=='PAID';assert try_confirm().ok
 checks.append('Local wallet requires explicit simulation consent; first/second member payments cannot confirm order, all three verified shares allow confirmation')
 mp.goto(base+'/orders',wait_until='networkidle');mp.locator('tbody a[href="/orders/'+str(order_id)+'"]').click();mp.wait_for_url('**/orders/'+str(order_id));assert mp.locator('.status-pill[data-status=CONFIRMED]').count()>0
 assert mp.locator('#cancel,#confirm-received,#request-return').count()==0
 for status in ['PICKED_UP','DELIVERING']:
  assert post(vendor,'/vendor/orders/'+str(order_id)+'/status',{'status':status,'version':group_order_state()['version'],'reason':'','cashCollected':False}).ok
 mp.wait_for_selector('.status-pill[data-status=DELIVERING]',timeout=22000)
 assert member.request.get(base+'/orders/'+str(order_id)+'/data').json()['status']=='DELIVERING'
 checks.append('Billed member finds group order in personal orders, reads confirmed status/history, auto-updates delivery state, and has no owner action controls')

 # Multiple independently identifiable groups, host badge, cancellation and expiry notices.
 first=post(host,'/group-orders',{}).json();second=post(host,'/group-orders',{}).json()
 for g in [first,second]:assert post(member,'/group-orders/'+g['token']+'/join',{'version':g['version']}).ok
 hp.goto(base+'/group-orders/new',wait_until='networkidle');cards=hp.locator('.group-summary-card');assert cards.count()>=2
 for g in [first,second]:assert hp.locator('.group-summary-card[href$="'+g['token']+'"]').count()==1
 assert hp.locator('.group-summary-card[href$="'+first['token']+'"]').inner_text()!=hp.locator('.group-summary-card[href$="'+second['token']+'"]').inner_text()
 hp.goto(base+'/group-orders/'+first['token'],wait_until='networkidle');assert hp.locator('.group-member .badge').filter(has_text='Chủ nhóm').count()==1
 mp.goto(base+'/group-orders/'+first['token'],wait_until='networkidle')
 hp.locator('[data-group-action=cancel]').click();hp.locator('[data-confirm-accept]').click();hp.wait_for_url('**/group-orders/new')
 mp.bring_to_front();mp.wait_for_selector('.modal.show #utetra-confirm-title',timeout=20000);assert mp.locator('.modal.show .modal-title').inner_text()=='Nhóm đã được hủy';assert first['code'] in mp.locator('.modal.show .modal-body').inner_text();mp.locator('.modal.show [data-bs-dismiss=modal]').last.click();mp.wait_for_selector('#utetra-confirm-title',state='detached');mp.wait_for_url('**/group-orders/new');assert mp.locator('.group-page').count()==0;mp.goto(base+'/group-orders/'+first['token'],wait_until='networkidle');mp.wait_for_url('**/group-orders/new')
 checks.append('Distinct group cards show code/host/time/status, host badge is next to member name, host cancel sends popup to member even on a payment page')
 assert post(host,'/api/ui-fixture/advance',{'seconds':90000}).ok
 for ctx,user in [(host,1),(member,6)]:
  ctx.clear_cookies();assert post(ctx,'/auth/login',{'email':'u'+str(user)+'@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
 mp.goto(base+'/menu',wait_until='networkidle');mp.wait_for_selector('.modal.show #utetra-confirm-title',timeout=20000);assert mp.locator('.modal.show .modal-title').inner_text()=='Nhóm đã hết hạn';assert second['code'] in mp.locator('.modal.show .modal-body').inner_text();mp.locator('.modal.show [data-confirm-accept]').click();mp.wait_for_url('**/group-orders/new');assert mp.locator('.group-summary-card[href$="'+second['token']+'"]').count()==0;mp.goto(base+'/group-orders/'+second['token'],wait_until='networkidle');mp.wait_for_url('**/group-orders/new')
 checks.append('Real H2 clock expiry sends popup to member after relogin, opens expired group and blocks further menu choices')
 assert not errors,errors
 browser.close()
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2 only; no cloud orders or provider calls'},ensure_ascii=False,indent=2))
print('Shopping UI checks passed:',len(checks))
