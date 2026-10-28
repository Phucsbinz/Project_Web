"""Partial group cancellation / refund UI audit; isolated H2 fixture only."""
from pathlib import Path
import sys,os,time,json,uuid,re,html,urllib.request
from urllib.parse import urlparse,parse_qs
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/partial-group-ui';out.mkdir(exist_ok=True)
for _ in range(40):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('Isolated H2 not ready')
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 def post(ctx,path,body):
  csrf=ctx.request.get(base+'/products/csrf').json();return ctx.request.post(base+path,data=body,headers={csrf['headerName']:csrf['token']})
 def login(user):
  ctx=browser.new_context(viewport={'width':1440,'height':1000});assert post(ctx,'/auth/login',{'email':'u'+str(user)+'@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok;return ctx
 host,member,third,admin=[login(user) for user in [1,6,2,4]];guest=browser.new_context()
 group=post(host,'/group-orders',{'paymentMode':'INDIVIDUAL'}).json();token=group['token']
 def state():return host.request.get(base+'/group-orders/'+token+'/data').json()
 for ctx in [host,member,third]:
  if ctx!=host:assert post(ctx,'/group-orders/'+token+'/join',{'version':state()['version']}).ok
  assert post(ctx,'/group-orders/'+token+'/items',{'version':state()['version'],'item':{'variantId':1,'sugar':50,'ice':50,'toppingIds':[],'quantity':1,'note':''}}).ok
 assert post(host,'/group-orders/'+token+'/lock',{'version':state()['version']}).ok
 hp=host.new_page();hp.on('pageerror',lambda error:errors.append(str(error)));hp.goto(base+'/checkout',wait_until='networkidle')
 for width in [1440,390]:
  hp.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert hp.evaluate('document.documentElement.scrollWidth<=innerWidth')
  assert hp.locator('.choice-toggle').first.evaluate("button=>{const input=button.parentElement.querySelector('input'),a=button.getBoundingClientRect(),b=input.getBoundingClientRect(),css=getComputedStyle(button);return css.display==='flex'&&css.alignItems==='center'&&css.justifyContent==='center'&&Math.abs(a.y+a.height/2-b.y-b.height/2)<2&&getComputedStyle(button,'::after').content!=='none';}")
  hp.screenshot(path=str(out/('centered-arrow-'+str(width)+'.png')),full_page=True)
 checks.append('Choice arrow is flex-centered and geometrically centered in control at 1440px and 390px')
 cart=host.request.get(base+'/cart/data').json();assert post(host,'/checkout/branch',{'storeId':1,'version':cart['version']}).ok
 request={'key':str(uuid.uuid4()),'addressId':1,'areaId':1,'carrierId':1,'productPromotionId':None,'shippingPromotionId':None,'points':0,'note':'','paymentMethod':'LOCAL_QR','quoteHash':''}
 quote=post(host,'/checkout/quote',request);assert quote.ok,quote.status;request['quoteHash']=quote.json()['hash'];created=post(host,'/checkout',request);assert created.ok,created.status;order=created.json()['id']
 def payment(ctx):
  data=ctx.request.get(base+'/orders/'+str(order)+'/qr-payment/status').json();markup=ctx.request.get(base+'/orders/'+str(order)+'/qr-payment').text();link=html.unescape(re.search(r'data-wallet-url="([^"]+)"',markup).group(1));data['token']=parse_qs(urlparse(link).fragment)['token'][0];data['url']=link;return data
 values=[payment(ctx) for ctx in [host,member,third]]
 for value in values[:2]:assert post(guest,'/local-wallet/api/'+value['id']+'/confirm',{'token':value['token'],'expectedAmount':value['amount'],'acknowledged':True}).ok
 due=sum(value['amount'] for value in values[:2]);wallet=guest.new_page();wallet.on('pageerror',lambda error:errors.append(str(error)));wallet.goto(values[2]['url'],wait_until='networkidle');wallet.locator('#wallet-acknowledged').check();assert wallet.locator('#wallet-confirm').is_enabled()
 for ctx in [member,third]:
  for notice in ctx.request.get(base+'/group-orders/payment-notices').json():ctx.add_init_script("localStorage.setItem("+json.dumps('utetra.group.notice.'+notice['token']+'.'+str(notice['member_id']))+",'shown');")
 mp=member.new_page();mp.goto(base+'/orders/'+str(order),wait_until='networkidle');tp=third.new_page();tp.goto(base+'/orders/'+str(order),wait_until='networkidle')
 hp.goto(base+'/group-orders/'+token,wait_until='networkidle');hp.locator('[data-group-action=cancel]').click();hp.wait_for_selector('.modal.show');assert '30 phút' in hp.locator('.modal.show .modal-body').inner_text();assert state()['status']=='ORDERED'
 hp.locator('.modal.show [data-bs-dismiss=modal]').last.click();hp.wait_for_selector('.modal.show',state='hidden');assert state()['status']=='ORDERED'
 hp.locator('[data-group-action=cancel]').click();hp.locator('.modal.show [data-confirm-accept]').click();hp.wait_for_url('**/group-orders/new')
 mp.bring_to_front();mp.wait_for_selector('.modal.show',timeout=18000);assert mp.locator('.modal.show .modal-title').inner_text()=='Nhóm đã bị hủy';assert '30 phút' in mp.locator('.modal.show .modal-body').inner_text();mp.locator('.modal.show [data-bs-dismiss=modal]').last.click()
 tp.bring_to_front();tp.wait_for_selector('.modal.show',timeout=18000);assert tp.locator('.modal.show .modal-title').inner_text()=='Nhóm đã bị hủy';assert '30 phút' not in tp.locator('.modal.show .modal-body').inner_text();tp.locator('.modal.show [data-bs-dismiss=modal]').last.click()
 checks.append('Owner cancellation requires explicit confirmation; dismiss leaves group active; paid member gets 30-minute estimate, unpaid member gets cancellation only')
 wallet.bring_to_front();wallet.wait_for_function("document.querySelector('#wallet-state').textContent.includes('hủy')",timeout=12000);assert wallet.locator('#wallet-confirm').is_disabled()
 assert post(guest,'/local-wallet/api/'+values[2]['id']+'/confirm',{'token':values[2]['token'],'expectedAmount':values[2]['amount'],'acknowledged':True}).status==409
 checks.append('With two of three paid, owner cancellation blocks the remaining wallet immediately, including server-side confirm')
 data=member.request.get(base+'/orders/'+str(order)+'/data').json();assert data['paymentStatus']=='REFUND_PENDING' and data['qrRefundDue']==due
 paid=post(guest,'/local-wallet/api/'+values[1]['id']+'/status',{'token':values[1]['token']}).json();assert paid['state']=='REFUND_PENDING'
 ap=admin.new_page();ap.on('pageerror',lambda error:errors.append(str(error)));ap.goto(base+'/admin/orders/'+str(order),wait_until='networkidle');assert 'Chờ hoàn' in ap.locator('main').inner_text();ap.locator('#refund-evidence').fill('INTERNAL-CANCEL-UI');ap.locator('#refund').click();ap.wait_for_load_state('networkidle');ap.wait_for_function("document.querySelector('[data-status=REFUNDED]')!==null")
 result=member.request.get(base+'/orders/'+str(order)+'/data').json();assert result['status']=='CANCELLED' and result['paymentStatus']=='REFUNDED' and result['qrRefunded']==due and result['qrRefundDue']==0
 assert post(guest,'/local-wallet/api/'+values[1]['id']+'/status',{'token':values[1]['token']}).json()['state']=='REFUNDED'
 assert post(guest,'/local-wallet/api/'+values[2]['id']+'/status',{'token':values[2]['token']}).json()['state']=='CANCELLED'
 assert post(admin,'/admin/orders/'+str(order)+'/refund',{'version':result['version'],'evidence':'DUPLICATE'}).status==409
 checks.append('Admin refunds exactly collected shares; paid member sees Refunded, unpaid member owes nothing, repeat refund is rejected')
 browser.close()
assert not errors,errors
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2 only'},ensure_ascii=False,indent=2),encoding='utf-8')
print('Partial group UI checks passed:',len(checks))
