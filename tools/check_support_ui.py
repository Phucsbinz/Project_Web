"""Isolated H2 acceptance: private realtime CSKH and typed checkout address."""
from pathlib import Path
import os,sys,json,time,urllib.request
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'docs/demo/support-20261027';out.mkdir(parents=True,exist_ok=True)
password=os.environ['UTETRA_UI_TEST_PASSWORD'];checks=[];errors=[];frames={role:[] for role in ['user','manager','admin','other']}
for _ in range(40):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
else:raise RuntimeError('UI fixture not ready')
def post(context,path,data):
 csrf=context.request.get(base+'/products/csrf').json()
 return context.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token'],'Accept':'application/json'})
def capture(role,packet):
 payload=packet if isinstance(packet,str) else packet.decode('utf-8')
 if payload.lstrip().startswith('MESSAGE\n') and 'conversationId' in payload:
  data=json.loads(payload.split('\n\n',1)[1].rstrip('\0'))
  assert set(data)=={'conversationId','messageId'},'private hint contains message data'
  frames[role].append(data)
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True)
 contexts={};pages={}
 for role,user_id,path in [('user',1,'/support'),('manager',3,'/manager/support'),('admin',4,'/admin/support'),('other',6,'/support')]:
  context=browser.new_context(viewport={'width':1440,'height':1000});contexts[role]=context
  assert post(context,'/auth/login',{'email':'u'+str(user_id)+'@test.invalid','password':password}).ok
  page=context.new_page();pages[role]=page;page.on('pageerror',lambda e,role=role:errors.append({'role':role,'error':str(e)}))
  page.on('websocket',lambda ws,role=role:ws.on('framereceived',lambda packet,role=role:capture(role,packet)))
  assert page.goto(base+path,wait_until='networkidle').status==200
  page.wait_for_function("document.querySelector('#support-connection').textContent==='Đã kết nối realtime'")
 user=pages['user'];manager=pages['manager'];admin=pages['admin'];other=pages['other']
 user.locator('#support-target').select_option('1');user.locator('#support-start button').click()
 user.wait_for_function("!document.querySelector('#support-body').disabled")
 branch=user.locator('.support-thread[aria-current=true]').get_attribute('data-thread')
 manager.wait_for_function("!document.querySelector('#support-body').disabled")
 text='<img src=x onerror=window.supportXss=true> Nhờ chi nhánh kiểm tra đơn giúp mình.'
 before=len(frames['manager']);user.locator('#support-body').fill(text);user.locator('#support-compose button').click()
 manager.locator('#support-messages').get_by_text(text,exact=True).wait_for();assert len(frames['manager'])>before
 assert manager.locator('.support-message img').count()==0
 assert manager.evaluate('window.supportXss===undefined')
 manager.locator('#support-body').fill('Chi nhánh đã nhận thông tin và đang hỗ trợ bạn.');manager.locator('#support-compose button').click()
 user.locator('#support-messages').get_by_text('Chi nhánh đã nhận thông tin và đang hỗ trợ bạn.',exact=True).wait_for()
 assert not frames['admin'] and not frames['other']
 assert contexts['other'].request.get(base+'/api/support/conversations/'+branch+'/messages').status==404
 assert contexts['admin'].request.get(base+'/api/support/conversations/'+branch+'/messages').status==404
 assert post(contexts['manager'],'/api/support/conversations',{'storeId':None}).status==403
 assert post(contexts['admin'],'/api/support/conversations',{'storeId':1}).status==403
 checks.append('User/assigned Manager exchange with private STOMP hints; other User/Admin cannot read branch thread; staff cannot start chats; HTML stays text')
 user.locator('#support-target').select_option('ADMIN');user.locator('#support-start button').click()
 user.wait_for_function("document.querySelector('#support-title').textContent==='Admin UTETra' && !document.querySelector('#support-body').disabled")
 user.locator('#support-body').fill('Mình cần Admin hỗ trợ chính sách hoàn tiền.');user.locator('#support-compose button').click()
 admin.locator('#support-messages').get_by_text('Mình cần Admin hỗ trợ chính sách hoàn tiền.',exact=True).wait_for()
 admin.locator('#support-body').fill('Admin đã tiếp nhận yêu cầu của bạn.');admin.locator('#support-compose button').click()
 user.locator('#support-messages').get_by_text('Admin đã tiếp nhận yêu cầu của bạn.',exact=True).wait_for()
 assert frames['admin'] and not frames['other']
 checks.append('Separate User/Admin conversation and reply; unrelated customer receives no hints')
 denied=other.evaluate(r'''async () => {
  const csrf=await(await fetch('/products/csrf')).json();
  return await new Promise(resolve=>{let rejected=false;const ws=new WebSocket(utetra.websocketUrl());const timeout=setTimeout(()=>{ws.close();resolve(false);},5000);
   ws.onopen=()=>ws.send('CONNECT\naccept-version:1.2\nhost:'+location.host+'\nheart-beat:0,0\n'+csrf.headerName+':'+csrf.token+'\n\n\0');
   ws.onmessage=e=>{if(e.data.startsWith('CONNECTED'))ws.send('SUBSCRIBE\nid:forbidden\ndestination:/user/1/queue/support\n\n\0');if(e.data.startsWith('ERROR')){rejected=true;clearTimeout(timeout);ws.close();resolve(true);}};
   ws.onclose=()=>{clearTimeout(timeout);resolve(rejected);};});
 }''')
 assert denied,'forged subscription was not rejected'
 checks.append('Broker rejects forged subscription to another user queue')
 for role in ['user','manager','admin']:
  page=pages[role]
  for width in [1440,390]:
   page.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
   assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),(role,'overflow',width)
   assert page.locator('.site-header').count()==page.locator('.site-footer').count()==1
   page.evaluate("document.activeElement?.blur();window.scrollTo(0,0)");page.wait_for_timeout(80)
   page.screenshot(path=str(out/(role+'-support-'+str(width)+'.png')),full_page=True)
 checks.append('User/Manager/Admin chat pages: one decorator, no horizontal overflow at 390/1440px')
 checkout=contexts['user'].new_page();checkout.on('pageerror',lambda e:errors.append({'role':'checkout','error':str(e)}))
 assert checkout.goto(base+'/checkout?branch=1',wait_until='networkidle').status==200
 checkout.locator('[name=recipientName]').fill('Khách nhập địa chỉ mới');checkout.locator('[name=phone]').fill('0909876543')
 address='12 Võ Văn Ngân, Thủ Đức, TP.HCM';checkout.locator('#delivery-address').fill(address)
 for field in ['areaId','carrierId']:
  label=checkout.locator('select[name='+field+'] option[value="1"]').inner_text().strip()
  checkout.locator('input[data-choice='+field+']').fill(label);checkout.locator('input[data-choice='+field+']').press('Tab')
 checkout.locator('input[name=paymentMethod][value=COD]').check()
 checkout.wait_for_function("document.querySelector('#quote-detail').dataset.state==='ready' && !document.querySelector('#confirm-order').disabled",timeout=15000)
 new_id=int(checkout.locator('input[name=addressId]').input_value());assert new_id>2
 saved=contexts['user'].request.get(base+'/account/addresses/data').json()
 assert any(a['address_detail']==address for a in saved)
 for width in [1440,390]:
  checkout.set_viewport_size({'width':width,'height':1000 if width>500 else 844})
  assert checkout.evaluate('document.documentElement.scrollWidth<=innerWidth')
  checkout.evaluate("document.activeElement?.blur();window.scrollTo(0,0)");checkout.wait_for_timeout(80)
  checkout.screenshot(path=str(out/('typed-address-'+str(width)+'.png')),full_page=True)
 checkout.locator('#confirm-order').click();checkout.wait_for_url('**/orders/*');checkout.wait_for_load_state('networkidle')
 order_id=checkout.url.rsplit('/',1)[1];order=contexts['user'].request.get(base+'/orders/'+order_id+'/data').json()
 assert address in json.dumps(order,ensure_ascii=False)
 assert contexts['other'].request.get(base+'/orders/'+order_id+'/data').status in [403,404]
 checks.append('Typed address saved to buyer, quoted and snapshotted in H2 COD order; other buyer cannot read it')
 assert not errors,errors
 for context in contexts.values():context.close()
 browser.close()
(out/'verification.json').write_text(json.dumps({'fixture':'isolated H2; no email, Cloudinary, bank or merchant transaction','checks':checks,'pageErrors':errors,'supportHintCounts':{role:len(value) for role,value in frames.items()},'limits':'Text CSKH and address/COD test, not real support or provider transactions.'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Support/address browser checks passed:',len(checks))
