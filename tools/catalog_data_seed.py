"""Explicit catalog-only JDBC import. Defaults to preview; never changes app configuration."""
from pathlib import Path
import argparse,os,subprocess,sys,xml.etree.ElementTree as E
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--mode',choices=['preview','apply','rollback-preview','rollback','reconcile'],default='preview')
p.add_argument('--manifest',default='docs/data-seeding/2026-10-28/manifest.json')
p.add_argument('--output',default='.local/catalog-seeding-20261028/preview.json')
p.add_argument('--ledger');p.add_argument('--confirm-demo',action='store_true');p.add_argument('--self-test',action='store_true')
a=p.parse_args()
if a.mode in ['apply','rollback'] and not a.confirm_demo:p.error('Mutation requires explicit --confirm-demo after environment authorization.')
props={x.get('name'):x.get('value') for x in E.parse(root/'target/surefire-reports/TEST-com.utetra.order.OrderCommerceIntegrationTest.xml').findall('./properties/property')}
cp=props.get('surefire.test.class.path') or props['java.class.path'];java=java_executable();classes=root/'.local/catalog-seeding-20261028/classes';classes.mkdir(parents=True,exist_ok=True)
r=subprocess.run([str(java.with_name('javac.exe')),'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(root/'tools/data/CatalogSeed.java'),str(root/'tools/data/CatalogSeedTest.java')],cwd=root,capture_output=True,text=True)
if r.returncode:print(r.stderr);raise SystemExit(r.returncode)
cmd=[str(java),'-Xmx128m','-cp',str(classes)+os.pathsep+cp]
cmd+=['CatalogSeedTest',a.manifest] if a.self_test else ['CatalogSeed',a.mode,a.manifest,a.output,a.ledger or '']
r=subprocess.run(cmd,cwd=root,capture_output=True,text=True);print(r.stdout);print(r.stderr if r.returncode else '');raise SystemExit(r.returncode)
