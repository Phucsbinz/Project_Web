"""Read-only public page/quote checks on the existing localhost app; does not submit carts/orders/payments."""
from pathlib import Path
import urllib.request,urllib.error,urllib.parse,http.cookiejar,json,re,time,sys
root=Path(__file__).resolve().parents[3]
folder=Path(__file__).resolve().parent
base='http://127.0.0.1:8080'
checks=[]
requests=[]
stage="startup"
jar=http.cookiejar.CookieJar();opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
def request(path,data=None,headers=None):
 req=urllib.request.Request(base+path,data=data,headers=headers or {})
 started=time.monotonic();entry={"path":path.split("?")[0],"method":"POST" if data else "GET"};requests.append(entry)
 try:
  with opener.open(req,timeout=45) as r:
   body=r.read().decode('utf-8');entry.update(status=r.status,seconds=round(time.monotonic()-started,2));return r.status,body
 except urllib.error.HTTPError as e:
  entry.update(status=e.code,seconds=round(time.monotonic()-started,2));return e.code,e.read().decode('utf-8')
def get(path):
 status,body=request(path);assert status==200,(path,status);return body
try:
 get('/api/system/health');checks.append('Existing localhost app health200')
 ledger=json.loads((folder/'applied-ledger.json').read_text(encoding='utf-8'))
 new_products=[x for x in ledger['created'] if x['table']=='products']
 for row in new_products:
  product=row['snapshot'];html=get('/products/'+str(row['id']));assert product['name'] in html and 'catalog-placeholder' in html and '\ufffd' not in html
  sizes=re.findall(r'<option[^>]*data-extra-price="([^"]+)"[^>]*>\s*([SML])',html)
  want={'M','L'} if 'mango-milk' in row['key'] else {'M'}
  assert {size for extra,size in sizes}==want
  checks.append('Product'+str(row['id'])+': UTF8/name/placeholder/source-specific sizes200')
 for sid in [1,2,3]:
  query=urllib.parse.urlencode({'keyword':'Trà sữa xoài','storeId':sid});html=get('/menu?'+query);assert 'Trà sữa xoài' in html
  get('/menu?'+urllib.parse.urlencode({'categoryId':6,'storeId':sid,'page':0}))
 checks.append('Search and category filters across three existing stores200')
 pages=[]
 for page in [0,1]:
  html=get('/menu?'+urllib.parse.urlencode({'storeId':1,'page':page}));ids=set(re.findall(r'href="/products/(\d+)(?:\?quick=1)?"',html));assert ids;pages.append(ids)
 assert not pages[0]&pages[1];checks.append('Two menu pages have distinct product links under fixed read data')
 if '--pages-only' not in sys.argv:
  candidate=json.loads((folder/'data-checks.json').read_text(encoding='utf-8'))['existingQuoteCandidate']
  csrf=json.loads(get('/products/csrf'));headers={'Content-Type':'application/json',csrf['headerName']:csrf['token'],'Accept':'application/json'}
  if candidate:
   item=candidate[0];body=json.dumps({'variantId':item['variant_id'],'quantity':2,'sugarLevel':50,'iceLevel':50,'toppingIds':[item['topping_id']]}).encode()
   status,text=request('/products/'+str(item['product_id'])+'/quote',body,headers);assert status==200,(status,'quote failed');result=json.loads(text)
   expected=2*(int(item['base_price'])+int(item['extra_price'])+10000)
   assert int(result['total'])==expected,(result.keys(),expected)
   checks.append('Existing finite-stock drink + new herbal jelly quote for2 cups equals server formula; no order created')
  else:checks.append('Quote not run: no existing active finite-stock candidate')
  first=new_products[0];variant=next(x for x in ledger['created'] if x['table']=='product_variants' and x['key'].startswith(first['key']))
  status,_=request('/products/'+str(first['id'])+'/quote',json.dumps({'variantId':variant['id'],'quantity':1,'sugarLevel':50,'iceLevel':50,'toppingIds':[]}).encode(),headers)
  assert status in [400,409];checks.append('New unknown-stock product quote rejected; availability guard retained')
 result={'checks':checks,'errors':[],'browserVerification':'Unavailable: CUA browser inventory empty; these are HTTP/JSP response and public quote checks, not a native browser render','noCartOrderPaymentCreated':True,'requests':requests,'quoteVerification':'not run in pages-only mode' if '--pages-only' in sys.argv else 'passed'}
except Exception as e:
 result={'checks':checks,'errors':[type(e).__name__+': '+str(e)],'browserVerification':'HTTP only','requests':requests}
(folder/('http-pages-only.json' if '--pages-only' in sys.argv else 'http-checks.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print('HTTP checks:',len(checks),'errors:',len(result['errors']))
if result['errors']:print(result['errors']);raise SystemExit(1)
