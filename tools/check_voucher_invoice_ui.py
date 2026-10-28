"""Voucher modal / scan-only QR / invoice audit on isolated H2."""
from pathlib import Path
import os,sys,json,time,urllib.request
from urllib.parse import urlparse,parse_qs
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base='http://127.0.0.1:8882';out=root/'.local/voucher-invoice-ui';out.mkdir(exist_ok=True)
for _ in range(40):
 try:
  urllib.request.urlopen(base+'/api/system/health',timeout=2)
  if (root/'.local/all-ui-server.json').exists():break
 except OSError:pass
 time.sleep(1)
checks=[];errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True);ctx=browser.new_context(viewport={'width':1440,'height':1000});guest=browser.new_context()
 def post(context,path,data):
  csrf=context.request.get(base+'/products/csrf').json();return context.request.post(base+path,data=data,headers={csrf['headerName']:csrf['token']})
 assert post(ctx,'/auth/login',{'email':'u1@test.invalid','password':os.environ['UTETRA_UI_TEST_PASSWORD']}).ok
 assert post(ctx,'/cart/clear',{}).ok;assert post(ctx,'/cart/draft/items',{'variantId':1,'sugar':50,'ice':50,'toppingIds':[],'quantity':1,'note':''}).ok
 page=ctx.new_page();page.on('pageerror',lambda error:errors.append(str(error)));page.goto(base+'/checkout',wait_until='networkidle');page.locator('[data-choice=branch]').fill('Shop 1 — Address');page.locator('#branch-form > button').click();page.wait_for_url('**/checkout?branch=1');page.wait_for_load_state('networkidle')
 page.locator('[name=recipientName]').fill('Nguyễn Test');page.locator('[name=phone]').fill('0901234567');page.locator('[name=addressDetail]').fill('58 Lê Văn Việt, Thủ Đức');page.locator('[data-choice=areaId]').fill('Area A');page.locator('[data-choice=carrierId]').fill('Carrier');page.locator('[name=note]').fill('Gọi trước khi giao');page.wait_for_selector('#quote-detail[data-state=ready]')
 page.locator('[data-voucher-for=productPromotionId]').click();
 try:page.wait_for_selector('.voucher-card',timeout=10000)
 except Exception:
  print('VOUCHER_DIAGNOSTIC',json.dumps({'message':page.locator('#voucher-results').inner_text(),'errors':errors},ensure_ascii=True));raise
 page.locator('#voucher-search').fill('CODE90');card=page.locator('.voucher-card').filter(has_text='Đơn từ 500k');assert card.locator('input').is_disabled();assert 'Không đủ điều kiện áp dụng' in card.locator('.text-danger').inner_text()
 page.locator('#voucher-search').fill('Ưu đãi tạm ngừng');assert page.locator('.voucher-card input').is_disabled();page.locator('#voucher-search').fill('Last voucher');page.locator('.voucher-card input').check();page.locator('#voucher-apply').click();page.wait_for_selector('.voucher-picker.show',state='hidden');assert page.locator('[name=productPromotionId]').input_value()=='1';page.wait_for_selector('#quote-detail[data-state=ready]');checks.append('Search by name/code; minimum-order and inactive vouchers disabled with red reason; eligible voucher applies via authoritative quote')
 page.locator('[data-voucher-for=shippingPromotionId]').click();page.wait_for_selector('.voucher-card');page.locator('#voucher-search').fill('CODE92');page.locator('.voucher-card input').check();page.locator('#voucher-apply').click();page.wait_for_selector('.voucher-picker.show',state='hidden');page.wait_for_selector('#quote-detail[data-state=ready]');assert page.locator('[name=shippingPromotionId]').input_value()=='92'
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844});page.locator('[data-voucher-for=shippingPromotionId]').click();page.wait_for_selector('.voucher-card');assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.wait_for_function("getComputedStyle(document.querySelector('.voucher-picker .modal-dialog')).transform==='none'");page.screenshot(path=str(out/('vouchers-'+str(width)+'.png')),full_page=False);page.locator('.voucher-picker .btn-close').click();page.wait_for_selector('.voucher-picker.show',state='hidden')
 checks.append('Shipping vouchers share the same accessible, responsive picker; dismiss preserves selection')
 page.locator('[name=paymentMethod][value=LOCAL_QR]').check();page.wait_for_selector('#quote-detail[data-state=ready]');page.locator('#confirm-order').click();page.wait_for_url('**/qr-payment');assert page.locator('#open-local-wallet').count()==0;assert 'Chi tiết hóa đơn' in page.locator('aside').inner_text();address=page.locator('.local-qr-page').get_attribute('data-wallet-url');parts=urlparse(address);token=parse_qs(parts.fragment)['token'][0];payment_id=parts.path.rsplit('/',1)[1];data=post(guest,'/local-wallet/api/'+payment_id+'/status',{'token':token}).json();assert post(guest,'/local-wallet/api/'+payment_id+'/confirm',{'token':token,'expectedAmount':data['amount'],'acknowledged':True}).ok
 page.bring_to_front();page.wait_for_url('**/invoice*',timeout=15000);page.wait_for_selector('.modal.show');assert 'để ý điện thoại' in page.locator('.modal.show .modal-body').inner_text();page.locator('.modal.show [data-confirm-accept]').click();page.wait_for_selector('.modal.show',state='hidden');text=page.locator('.invoice-page').inner_text();assert all(v in text for v in ['Nguyễn Test','0901234567','58 Lê Văn Việt','Gọi trước khi giao']);assert page.locator('a[href="/orders"]').count()>0 and page.locator('.invoice-actions a[href="/"]').count()==1
 page.wait_for_function("!document.querySelector('.modal-backdrop')")
 for width in [1440,390]:
  page.set_viewport_size({'width':width,'height':1000 if width>500 else 844});assert page.evaluate('document.documentElement.scrollWidth<=innerWidth');page.screenshot(path=str(out/('invoice-'+str(width)+'.png')),full_page=True)
 checks.append('Scan-only QR has full bill; payment triggers success popup and invoice with immutable contact/note, print and navigation links')
 browser.close()
assert not errors,errors
(out/'verification.json').write_text(json.dumps({'checks':checks,'errors':errors,'fixture':'Isolated H2'},ensure_ascii=False,indent=2),encoding='utf-8');print('Voucher/invoice UI checks passed:',len(checks))
