"""Publish only QR payment routes through a Cloudflare Quick Tunnel.
Run: python tools/qr_tunnel.py start | status | stop
No bank credentials, application login, or admin routes are exposed.
"""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from http.client import HTTPConnection
from urllib.parse import urlsplit
import json, os, re, subprocess, sys, time, threading
ROOT = Path(__file__).resolve().parent.parent
LOCAL = ROOT / '.local'
STATE = LOCAL / 'qr-tunnel.json'
PUBLIC = LOCAL / 'qr-tunnel.properties'
STOP = LOCAL / 'qr-tunnel.stop'
ASSETS = {'/assets/css/utetra.css','/assets/css/local-qr.css','/assets/js/utetra.js','/assets/js/site-ui.js','/assets/js/local-qr.js','/assets/images/utetra-mark.svg','/assets/vendor/bootstrap/bootstrap.min.css','/assets/vendor/bootstrap/bootstrap.bundle.min.js'} | {'/assets/vendor/fonts/be-vietnam-pro/BeVietnamPro-'+w+'.ttf' for w in ('Regular','Medium','SemiBold','Bold')}
UUID = r'[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}'
def allowed(method, target):
    url = urlsplit(target)
    if url.scheme or url.netloc or '%' in target or '\\' in target or '#' in target: return False
    if url.query and not (url.path in ASSETS and re.fullmatch(r'v=[A-Za-z0-9_-]{1,80}',url.query)): return False
    return (method == 'GET' and (url.path in ASSETS or url.path == '/local-wallet/health' or re.fullmatch('/local-wallet/pay/'+UUID,url.path))) or (method == 'POST' and re.fullmatch('/local-wallet/api/'+UUID+'/(status|confirm)',url.path))
class Gateway(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.0'
    def log_message(self,*args): pass # Payment capabilities must never enter logs.
    def do_GET(self): self.proxy()
    def do_POST(self): self.proxy()
    def proxy(self):
        self.connection.settimeout(15)
        if not allowed(self.command,self.path): self.send_error(404); return
        if self.path == '/local-wallet/health':
            payload=b'{"status":"UP","scope":"qr-only"}'
            self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload);return
        try:
            if self.headers.get('Transfer-Encoding'): self.send_error(400);return
            length=int(self.headers.get('Content-Length','0'))
            if length<0 or length>4096: self.send_error(413);return
            if self.command=='GET' and length: self.send_error(400);return
            body=self.rfile.read(length) if length else None
            headers={k:self.headers[k] for k in ('Accept','Content-Type','X-XSRF-TOKEN','X-CSRF-TOKEN') if self.headers.get(k)}
            # Forward only CSRF/session cookies; never JWT/Authorization credentials.
            cookies=[part.strip() for part in self.headers.get('Cookie','').split(';') if part.strip().startswith(('XSRF-TOKEN=','JSESSIONID='))]
            if cookies: headers['Cookie']='; '.join(cookies)
            conn=HTTPConnection('127.0.0.1',8080,timeout=15)
            try:
                conn.request(self.command,self.path,body,headers);res=conn.getresponse();payload=res.read(2*1024*1024)
                if 300<=res.status<400: self.send_error(502);return
                self.send_response(res.status)
                for name,value in res.getheaders():
                    if name.lower() in ('content-type','set-cookie','x-content-type-options','content-security-policy'): self.send_header(name,value)
                self.send_header('Cache-Control','no-store');self.send_header('Referrer-Policy','no-referrer');self.send_header('Content-Length',str(len(payload)));self.end_headers();self.wfile.write(payload)
            finally: conn.close()
        except (ValueError,OSError): self.send_error(502)
def run():
    LOCAL.mkdir(exist_ok=True);STOP.unlink(missing_ok=True)
    server=ThreadingHTTPServer(('127.0.0.1',8086),Gateway);server.daemon_threads=True
    threading.Thread(target=server.serve_forever,daemon=True).start()
    binary=ROOT/'.tools/cloudflared/cloudflared.exe'
    with (LOCAL/'qr-tunnel.log').open('w',encoding='utf-8') as log:
        process=subprocess.Popen([str(binary),'tunnel','--no-autoupdate','--url','http://127.0.0.1:8086','--protocol','http2'],cwd=ROOT,stdout=log,stderr=log,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        try:
            STATE.write_text(json.dumps({'pid':process.pid,'gatewayPid':os.getpid(),'url':None}),encoding='utf-8')
            for _ in range(90):
                if process.poll() is not None or STOP.exists(): return
                match=re.search(r'https://[a-z0-9-]+\.trycloudflare\.com',(LOCAL/'qr-tunnel.log').read_text(encoding='utf-8',errors='replace'))
                if match:
                    url=match.group(0);PUBLIC.write_text('url='+url+'\npid='+str(process.pid)+'\n',encoding='ascii');STATE.write_text(json.dumps({'pid':process.pid,'gatewayPid':os.getpid(),'url':url}),encoding='utf-8');break
                time.sleep(1)
            else: return
            while process.poll() is None and not STOP.exists(): time.sleep(1)
        finally:
            PUBLIC.unlink(missing_ok=True)
            if process.poll() is None: process.terminate();process.wait(timeout=10)
            server.shutdown();server.server_close();STATE.unlink(missing_ok=True);STOP.unlink(missing_ok=True)
def main():
    action=sys.argv[1] if len(sys.argv)>1 else 'status'
    if action=='_run': run();return
    if action=='stop': STOP.touch();print('Requested QR tunnel shutdown.');return
    if action=='status': print(STATE.read_text(encoding='utf-8') if STATE.exists() else 'QR tunnel is stopped.');return
    if action!='start': raise SystemExit('Use start, status, or stop.')
    if STATE.exists(): raise SystemExit('QR tunnel state already exists. Check status or stop first.')
    if not (ROOT/'.tools/cloudflared/cloudflared.exe').exists(): raise SystemExit('Install the official cloudflared Windows binary at .tools/cloudflared/cloudflared.exe first.')
    LOCAL.mkdir(exist_ok=True)
    with (LOCAL/'qr-gateway.log').open('w') as log:
        subprocess.Popen([sys.executable,str(Path(__file__).resolve()),'_run'],cwd=ROOT,stdout=log,stderr=log,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    print('Starting QR-only gateway and Cloudflare tunnel. Run status to see its public URL.')
if __name__=='__main__': main()
