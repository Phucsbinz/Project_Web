from pathlib import Path
import sys,subprocess,xml.etree.ElementTree as E
root=Path(__file__).resolve().parent.parent;sys.path.insert(0,str(root/'tools'));from runtime import java_executable
p={x.get('name'):x.get('value') for x in E.parse(root/'target/surefire-reports/TEST-com.utetra.order.OrderCommerceIntegrationTest.xml').findall('./properties/property')};cp=p.get('surefire.test.class.path') or p['java.class.path'];folder=root/'.local/catalog-seeding-20261028/classes';java=java_executable()
r=subprocess.run([str(java.with_name('javac.exe')),'-encoding','UTF-8','-cp',cp,'-d',str(folder),str(root/'tools/data/CatalogSurvey.java'),str(root/'tools/data/CatalogPostCheck.java')],capture_output=True,text=True)
if r.returncode:print(r.stderr);sys.exit(r.returncode)
r=subprocess.run([str(java),'-cp',str(folder)+';'+cp,'CatalogPostCheck'],capture_output=True,text=True);print(r.stdout);print(r.stderr if r.returncode else '');sys.exit(r.returncode)
