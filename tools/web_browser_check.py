"""Read-only UI checks of the actual MySQL-backed website; never seeds data."""
from pathlib import Path
import sys,json,os,time,urllib.request
root=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(root/'.tools/browser'))
from playwright.sync_api import sync_playwright
base=os.environ.get('UTETRA_BASE_URL','http://localhost:8080')
out=root/'docs/features/identity-membership/assets/integration-20261020';out.mkdir(parents=True,exist_ok=True)
checks=[];errors=[]
for attempt in range(30):
 try:
  with urllib.request.urlopen(base+'/api/system/health',timeout=2) as response:
   if response.status==200:break
 except (OSError,urllib.error.URLError):pass
 time.sleep(1)
else:raise SystemExit('The actual web server is not ready; inspect .local/web.stdout.log')
with sync_playwright() as p:
 browser=p.chromium.launch(channel='msedge',headless=True,args=['--disable-gpu'])
 page=browser.new_page(viewport={'width':1440,'height':1000})
 page.on('pageerror',lambda error:errors.append(str(error)))
 for script in ['identity.js','catalog.js','product-community.js','orders.js']:
  source=(root/'src/main/resources/static/assets/js'/script).read_text(encoding='utf-8')
  page.evaluate('(source) => {new Function(source);}',source)
 for route in ['/','/menu','/stores','/auth/login','/auth/register','/auth/forgot','/cart']:
  response=page.goto(base+route,wait_until='networkidle')
  assert response.status==200,(route,response.status)
  assert page.locator('.navbar-brand').inner_text()=='UTETra'
  assert page.locator('.site-footer').count()==1
  text=page.locator('body').inner_text().lower()
  assert 'bản demo' not in text and 'spring mvc' not in text
  assert not any(token in text for token in ['chæ','pháº','á»','ã³']),('Vietnamese encoding',route)
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),route
  checks.append({'route':route,'status':response.status,'viewport':'desktop'})
  if route in ['/','/auth/login','/cart']:page.screenshot(path=str(out/(('home' if route=='/' else route.strip('/').replace('/','-'))+'-desktop.png')),full_page=True)
 for route in ['/menu','/auth/login','/auth/register','/cart']:
  page.set_viewport_size({'width':390,'height':844});page.goto(base+route,wait_until='networkidle')
  assert page.evaluate('document.documentElement.scrollWidth<=innerWidth'),route
  page.get_by_role('button',name='Mở menu').click()
  assert page.locator('.navbar').get_by_role('link',name='Thực đơn',exact=True).is_visible()
  checks.append({'route':route,'viewport':'mobile','overflow':False})
  if route=='/auth/login':page.screenshot(path=str(out/'login-mobile.png'),full_page=True)
 assert not errors,errors
 # A nonexistent account must never receive an authentication cookie on the real DB.
 page.goto(base+'/auth/login',wait_until='networkidle')
 csrf=page.request.get(base+'/products/csrf').json()
 rejected=page.request.post(base+'/auth/login',data={'email':'absent-web-check@example.invalid','password':'Invalid-credential-for-check'},headers={csrf['headerName']:csrf['token']})
 assert rejected.status==401,rejected.status
 assert 'UTETRA_JWT=' not in rejected.headers.get('set-cookie','')
 checks.append({'route':'POST /auth/login','nonexistentAccount':401,'authCookieIssued':False})
 blocked=page.request.post(base+'/auth/login',data={'email':'absent-web-check@example.invalid','password':'Invalid-credential-for-check'})
 assert blocked.status==403,blocked.status
 checks.append({'route':'POST /auth/login','missingCsrf':403})
 browser.close()
report={'baseUrl':base,'mode':'mysql-no-seeding','checks':checks,'javascriptErrors':errors}
(out/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Passed {len(checks)} actual-web browser checks; no JavaScript errors.')
