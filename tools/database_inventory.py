"""Read-only table counts and metadata from configured utetra_db; never prints data rows."""
from pathlib import Path
import argparse,json,subprocess,xml.etree.ElementTree as ET,collections
from runtime import java_executable
root=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--output',default='.local/database-inventory.json')
args=parser.parse_args()
report=root/'target/surefire-reports/TEST-com.utetra.order.OrderCommerceIntegrationTest.xml'
if report.is_file():
 props={p.get('name'):p.get('value') for p in ET.parse(report).findall('./properties/property')}
 cp=props.get('surefire.test.class.path') or props.get('java.class.path')
else:
 file=root/'.local/identity-classpath.txt'
 if not file.is_file():raise SystemExit('Build/test first or prepare .local/identity-classpath.txt with Maven dependency:build-classpath.')
 cp=file.read_text(encoding='utf-8').strip()
output=(root/args.output).resolve()
subprocess.run([str(java_executable()),'-Xmx128m','-cp',cp,'tools/DatabaseInventory.java',str(output)],cwd=root,check=True)
r=json.loads(output.read_text(encoding='utf-8'));names=collections.defaultdict(list);structures=collections.defaultdict(list)
for table in r['tables']:
 names[table['name'].lower()].append(table['name'])
 structures[json.dumps({'columns':table['columns'],'foreignKeys':table['foreignKeys']},sort_keys=True)].append(table['name'])
r['caseInsensitiveDuplicateNameGroups']=[v for v in names.values() if len(v)>1]
r['identicalColumnAndForeignKeyGroups']=[v for v in structures.values() if len(v)>1]
r['emptyTables']=[t['name'] for t in r['tables'] if not t['rows']]
r['emptyTableCount']=len(r['emptyTables'])
output.write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Application tables:',r['applicationTableCount'],'Empty:',r['emptyTableCount'])
for table in r['emptyTables']:print('EMPTY',table)
