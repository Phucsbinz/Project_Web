# Test-only Playwright smoke; tokens are supplied by OrderSmokeHarness, never committed.
from pathlib import Path
import json,re,os
from playwright.sync_api import sync_playwright
root=Path(__file__).resolve().parents[3]/'docs/features/order-payment/assets'
base=os.environ.get('ORDER_BROWSER_BASE_URL','http://127.0.0.1:8082')
for uc in ['UC-ORD-03','UC-ORD-09','UC-ORD-10','UC-VND-04','UC-VND-06','UC-MGR-06','UC-MGR-09','UC-ADM-06','UC-ADM-09','UC-SHP-01','UC-SHP-02','UC-SHP-03']:(root/uc).mkdir(parents=True,exist_ok=True)
tokens=json.loads(Path(os.environ.get('ORDER_BROWSER_TOKENS_FILE','/tmp/utetra-order-smoke-tokens.json')).read_text())
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path=os.environ.get('ORDER_BROWSER_CHROMIUM','/usr/bin/chromium'),args=['--disable-dev-shm-usage'])
 def context(role,viewport=None):
  c=browser.new_context(viewport=viewport or {'width':1365,'height':900})
  c.add_cookies([{'name':'UTETRA_JWT','value':tokens[role],'url':base,'httpOnly':True,'sameSite':'Lax'}])
  return c
 buyer=context('ROLE_USER');page=buyer.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
 assert page.goto(base+'/cart').status==200
 page.screenshot(path=str(root/'UC-ORD-03/ui-cart.png'),full_page=True)
 assert page.goto(base+'/checkout').status==200
 page.select_option('[name=addressId]','1');page.select_option('[name=areaId]','1');page.select_option('[name=carrierId]','1')
 page.click('#quote');page.wait_for_selector('#quote-detail:not([hidden])')
 assert '95000' in page.locator('#quote-detail').inner_text()
 page.screenshot(path=str(root/'UC-ORD-03/ui-checkout-quote.png'),full_page=True)
 page.click('#confirm-order');page.wait_for_url(re.compile(r'.*/orders/\d+$'));order_id=int(page.url.rsplit('/',1)[1]);page.screenshot(path=str(root/'UC-ORD-03/ui-order-created.png'),full_page=True)
 result=page.evaluate(r'''async()=>new Promise(resolve=>{const socket=new WebSocket((location.protocol==='https:'?'wss://':'ws://')+location.host+(document.querySelector('meta[name="app-context"]').content||'')+'/ws');window.orderSmokeSocket=socket;window.orderSmokeEvents=[];let finished=false;const finish=value=>{if(!finished){finished=true;resolve(value);}};socket.onopen=()=>{const header=document.querySelector('meta[name="_csrf_header"]').content;const token=document.querySelector('meta[name="_csrf"]').content;socket.send('CONNECT\naccept-version:1.2\nhost:127.0.0.1\n'+header+':'+token+'\n\n\0');};socket.onmessage=e=>{if(e.data.startsWith('CONNECTED')){socket.send('SUBSCRIBE\nid:own-orders\ndestination:/user/queue/orders\nack:auto\n\n\0');finish('connected');}else if(e.data.startsWith('MESSAGE'))window.orderSmokeEvents.push(e.data);else if(e.data.startsWith('ERROR'))finish('error');};socket.onclose=()=>finish('closed');setTimeout(()=>finish('timeout'),10000);})''')
 print('STOMP connect:',result)
 assert result=='connected',result
 vendor=context('ROLE_VENDOR');operate=vendor.new_page();operate.on('pageerror',lambda e:errors.append(str(e)))
 operate.goto(f'{base}/vendor/orders/{order_id}');operate.fill('#shipper-id','5')
 with operate.expect_navigation(wait_until='networkidle'):operate.click('#assign')
 shipping=context('ROLE_SHIPPER');ship=shipping.new_page();ship.on('pageerror',lambda e:errors.append(str(e)))
 ship.goto(base+'/shipper/orders');ship.screenshot(path=str(root/'UC-SHP-01/ui-assigned-orders.png'),full_page=True)
 for state in ['CONFIRMED','PICKED_UP','DELIVERING','DELIVERED']:
  active=operate if state=='CONFIRMED' else ship
  role='vendor' if state=='CONFIRMED' else 'shipper'
  assert active.goto(f'{base}/{role}/orders/{order_id}').status==200
  assert active.locator('#next-status').input_value()==state
  if state=='DELIVERED':active.check('#cash-collected')
  with active.expect_navigation(wait_until='networkidle'):active.click('#transition')
 ship.screenshot(path=str(root/'UC-SHP-02/ui-delivered-cod.png'),full_page=True)
 assert 'DELIVERED' in ship.locator('body').inner_text()
 page.wait_for_function('window.orderSmokeEvents.length>0')
 print('After-commit own-user STOMP event received')
 assert page.goto(f'{base}/orders/{order_id}').status==200
 page.fill('#return-reason','Ảnh hưởng chất lượng đồ uống; đây là dữ liệu test.')
 with page.expect_navigation(wait_until='networkidle'):page.click('#request-return')
 page.screenshot(path=str(root/'UC-ORD-09/ui-return-request.png'),full_page=True)
 operate.goto(f'{base}/vendor/orders/{order_id}')
 operate.fill('#decision-reason','Kiểm chứng trong bộ dữ liệu test, chấp nhận hoàn cả đơn.');operate.check('#waive')
 with operate.expect_navigation(wait_until='networkidle'):operate.click('#approve')
 operate.fill('#refund-evidence','TEST-CASH-RECEIPT-001')
 with operate.expect_navigation(wait_until='networkidle'):operate.click('#refund')
 assert 'REFUNDED' in operate.locator('body').inner_text()
 page.goto(f'{base}/orders/{order_id}');page.screenshot(path=str(root/'UC-ORD-09/ui-refunded.png'),full_page=True)
 mobile=context('ROLE_USER',{'width':390,'height':844});mobile_page=mobile.new_page();mobile_page.goto(f'{base}/orders/{order_id}');mobile_page.screenshot(path=str(root/'UC-ORD-09/ui-refunded-mobile.png'),full_page=True)
 assert mobile_page.evaluate('document.documentElement.scrollWidth <= innerWidth'), 'Mobile horizontal overflow'
 tablet=context('ROLE_USER',{'width':768,'height':1024});tablet_page=tablet.new_page();tablet_page.goto(f'{base}/orders/{order_id}');assert tablet_page.evaluate('document.documentElement.scrollWidth <= innerWidth')
 for role,path,uc in [('ROLE_USER','/notifications','UC-ORD-10'),('ROLE_VENDOR','/vendor/orders','UC-VND-04'),('ROLE_VENDOR','/vendor/revenue','UC-VND-06'),('ROLE_MANAGER','/manager/order-tools','UC-MGR-06'),('ROLE_MANAGER','/manager/reports','UC-MGR-09'),('ROLE_ADMIN','/admin/order-tools','UC-ADM-06'),('ROLE_ADMIN','/admin/reports','UC-ADM-09'),('ROLE_SHIPPER','/shipper/reports','UC-SHP-03')]:
  role_page=context(role).new_page();role_page.on('pageerror',lambda e:errors.append(str(e)));assert role_page.goto(base+path).status==200;role_page.screenshot(path=str(root/uc/'ui-page.png'),full_page=True)
 print('Browser COD -> assignment -> shipper delivery -> return -> cash refund passed; desktop/mobile/tablet and role pages passed')
 assert not errors,errors
 browser.close()
