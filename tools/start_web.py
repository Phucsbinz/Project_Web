"""Start the actual WAR with the configured MySQL database, without any test seed data."""
from pathlib import Path
from runtime import java_executable
import subprocess,socket,json,time,sys
root=Path(__file__).resolve().parent.parent
port=int(sys.argv[1]) if len(sys.argv)>1 else 8080
with socket.socket() as probe:
 try:probe.bind(('127.0.0.1',port))
 except OSError:raise SystemExit(f'Port {port} is already occupied; no process was stopped.')
java=java_executable()
local=root/'.local';local.mkdir(exist_ok=True)
with (local/'web.stdout.log').open('wb') as out,(local/'web.stderr.log').open('wb') as err:
 p=subprocess.Popen([str(java),'-Xms24m','-Xmx192m','-Xss512k','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:TieredStopAtLevel=1','-XX:ReservedCodeCacheSize=24m','-XX:HeapBaseMinAddress=8g','-XX:MaxDirectMemorySize=24m','-jar',str(root/'target/utetra.war'),'--spring.profiles.active=mysql',f'--server.port={port}','--server.address=127.0.0.1','--server.tomcat.threads.max=16','--server.tomcat.threads.min-spare=2'],cwd=root,stdout=out,stderr=err,creationflags=subprocess.CREATE_NO_WINDOW)
(local/'web-process.json').write_text(json.dumps({'pid':p.pid,'port':port,'mode':'mysql','url':f'http://localhost:{port}'}),encoding='utf-8')
print(f'Started actual mysql WAR PID {p.pid} at http://localhost:{port}; logs: .local/web.stdout.log')
