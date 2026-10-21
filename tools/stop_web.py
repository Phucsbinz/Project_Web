"""Stop only the actual WAR launched by start_web.py, after checking its command line."""
from pathlib import Path
import subprocess,json,os,signal
root=Path(__file__).resolve().parent.parent
info=json.loads((root/'.local/web-process.json').read_text(encoding='utf-8'))
jcmd=next((root/'.tools/jdk21').glob('*/bin/jcmd.exe'))
result=subprocess.run([str(jcmd),str(info['pid']),'VM.command_line'],capture_output=True,text=True,creationflags=subprocess.CREATE_NO_WINDOW)
if result.returncode or str(root/'target/utetra.war').lower() not in result.stdout.lower():raise SystemExit('The recorded PID is not this workspace WAR; no process stopped.')
os.kill(info['pid'],signal.SIGTERM)
print('Stopped verified workspace WAR PID',info['pid'])
