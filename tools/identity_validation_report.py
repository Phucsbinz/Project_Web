"""Inventory the complete Markdown corpus and record observed validation evidence."""
from pathlib import Path
import hashlib,json,subprocess,xml.etree.ElementTree as ET

root=Path('docs/features/identity-membership')
paths=subprocess.check_output(['rg','--files','--hidden','-g','*.md','-g','!.git','-g','!.tools','-g','!target','-g','!node_modules'],text=True,encoding='utf-8').splitlines()
inventory=[]
for name in paths:
    p=Path(name); data=p.read_bytes(); text=data.decode('utf-8-sig')
    inventory.append({'path':p.as_posix(),'lines':len(text.splitlines()),'sha256':hashlib.sha256(data).hexdigest()})
tests=[]
for p in sorted(Path('target/surefire-reports').glob('TEST-*.xml')):
    record=ET.parse(p).getroot()
    tests.append({key:record.attrib.get(key,'0') for key in ['name','tests','failures','errors','skipped','time','timestamp']})
browser_path=root/'assets/validation-20261020/verification.json'
report={'date':'2026-10-20','branch':'feature/identity-membership','markdown_count':len(inventory),'markdown':inventory,'surefire':tests,
    'browser':json.loads(browser_path.read_text(encoding='utf-8')) if browser_path.exists() else {'status':'not completed'}}
(root/'VALIDATION_AUDIT_20261020.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'markdown':len(inventory),'tests':sum(int(x['tests']) for x in tests),'failures':sum(int(x['failures'])+int(x['errors']) for x in tests),'browser_checks':len(report['browser'].get('checks',[]))}))
