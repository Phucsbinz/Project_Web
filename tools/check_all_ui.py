"""Run the complete UI page audit against an isolated H2 server, then stop it.
Requires a prior Maven package with OrderCommerceIntegrationTest and installed Playwright.
"""
from pathlib import Path
import os,re,socket,subprocess,xml.etree.ElementTree as ET
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
os.chdir(root)
with socket.socket() as probe:
    if probe.connect_ex(('127.0.0.1',8882))==0:
        raise SystemExit('Port 8882 is occupied; no existing process was stopped.')
report=root/'target/surefire-reports/TEST-com.utetra.order.OrderCommerceIntegrationTest.xml'
if not report.is_file():raise SystemExit('Run Maven package with OrderCommerceIntegrationTest first.')
xml=ET.parse(report)
cp=next(p.get('value') for p in xml.findall('./properties/property') if p.get('name')=='java.class.path')
java=java_executable();folder=root/'.local/ui-audit-classes';folder.mkdir(parents=True,exist_ok=True)
subprocess.run([str(java.with_name('javac.exe' if os.name=='nt' else 'javac')),'-cp',cp,'-d',str(folder),str(root/'tools/ui/AllUiServer.java')],check=True)
(root/'.local/all-ui-server.json').unlink(missing_ok=True)
fixture=(root/'src/test/java/com/utetra/order/OrderCommerceIntegrationTest.java').read_text(encoding='utf-8')
match=re.search(r'passwords\.encode\("([^"\n]+)"\)',fixture)
if not match:raise SystemExit('Test fixture credential format changed; audit needs updating.')
env=os.environ.copy();env['UTETRA_UI_TEST_PASSWORD']=match.group(1)
with (root/'.local/all-ui.stdout.log').open('wb') as out,(root/'.local/all-ui.stderr.log').open('wb') as err:
    process=subprocess.Popen([str(java),'-Xmx192m','-XX:ActiveProcessorCount=2','-cp',str(folder)+os.pathsep+cp,'com.utetra.order.AllUiServer'],cwd=root,stdout=out,stderr=err,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    try:
        result=subprocess.run(['py','-3',str(root/'tools/check_all_module_ui.py')] if os.name=='nt' else ['python3',str(root/'tools/check_all_module_ui.py')],cwd=root,env=env)
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=15)
raise SystemExit(result.returncode)
