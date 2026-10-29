"""Run the complete UI page audit against an isolated H2 server, then stop it.
Requires a prior Maven package with OrderCommerceIntegrationTest and installed Playwright.
"""
from pathlib import Path
import os,re,socket,subprocess,xml.etree.ElementTree as ET
import argparse
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
os.chdir(root)
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--cart-delete',action='store_true',help='Audit cart deletion and clearing')
parser.add_argument('--crud-dispatch',action='store_true',help='Audit account CRUD, topping readback, moderation and automatic shipping')
parser.add_argument('--offers-chat',action='store_true',help='Audit account voucher wallet and floating customer/admin chat')
parser.add_argument('--vouchers',action='store_true',help='Audit voucher picker and invoice flow')
parser.add_argument('--shopping',action='store_true',help='Audit cart badges and group checkout rather than all module pages')
parser.add_argument('--partial',action='store_true',help='Audit partially paid group cancellation and refunds')
parser.add_argument('--admin',action='store_true',help='Audit administration usability and visible dropdown menus')
parser.add_argument('--catalog-regressions',action='store_true',help='Audit GLOBAL promotion selection, optional product key and stale forms')
parser.add_argument('--shipper',action='store_true',help='Audit assigned delivery, COD collection, own reports and responsive views')
parser.add_argument('--support',action='store_true',help='Audit CSKH realtime, isolation and manual checkout address')
args=parser.parse_args()
audit='check_cart_delete_ui.py' if args.cart_delete else 'check_crud_dispatch_ui.py' if args.crud_dispatch else 'check_offers_chat_ui.py' if args.offers_chat else 'check_voucher_invoice_ui.py' if args.vouchers else 'check_catalog_regressions_ui.py' if args.catalog_regressions else 'check_shipper_ui.py' if args.shipper else 'check_partial_group_ui.py' if args.partial else 'check_admin_ui.py' if args.admin else 'check_support_ui.py' if args.support else 'check_shopping_ui.py' if args.shopping else 'check_all_module_ui.py'
with socket.socket() as probe:
    if probe.connect_ex(('127.0.0.1',8882))==0:
        raise SystemExit('Port 8882 is occupied; no existing process was stopped.')
report=root/'target/surefire-reports/TEST-com.utetra.order.OrderCommerceIntegrationTest.xml'
if not report.is_file():raise SystemExit('Run Maven package with OrderCommerceIntegrationTest first.')
xml=ET.parse(report)
properties={p.get('name'):p.get('value') for p in xml.findall('./properties/property')}
cp=properties.get('surefire.test.class.path') or properties.get('java.class.path')
if not cp:raise SystemExit('Test classpath missing from Maven report.')
java=java_executable();folder=root/'.local/ui-audit-classes';folder.mkdir(parents=True,exist_ok=True)
subprocess.run([str(java.with_name('javac.exe' if os.name=='nt' else 'javac')),'-cp',cp,'-d',str(folder),str(root/'tools/ui/AllUiServer.java')],check=True)
(root/'.local/all-ui-server.json').unlink(missing_ok=True)
fixture=(root/'src/test/java/com/utetra/order/OrderCommerceIntegrationTest.java').read_text(encoding='utf-8')
match=re.search(r'passwords\.encode\("([^"\n]+)"\)',fixture)
if not match:raise SystemExit('Test fixture credential format changed; audit needs updating.')
env=os.environ.copy();env['UTETRA_CRUD_UI']='1' if args.crud_dispatch else '0';env['UTETRA_VOUCHER_UI']='1' if args.vouchers else '0';env['UTETRA_UI_TEST_PASSWORD']=match.group(1)
with (root/'.local/all-ui.stdout.log').open('wb') as out,(root/'.local/all-ui.stderr.log').open('wb') as err:
    process=subprocess.Popen([str(java),'-Xmx192m','-XX:ActiveProcessorCount=2','-cp',str(folder)+os.pathsep+cp,'com.utetra.order.AllUiServer'],cwd=root,env=env,stdout=out,stderr=err,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    try:
        result=subprocess.run(['py','-3',str(root/'tools'/audit)] if os.name=='nt' else ['python3',str(root/'tools'/audit)],cwd=root,env=env)
    finally:
        if process.poll() is None:
            process.terminate()
            process.wait(timeout=15)
raise SystemExit(result.returncode)
