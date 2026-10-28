"""Validate repository-scoped agent skills without modifying personal Codex settings."""
from pathlib import Path
import re,hashlib,json
root=Path(__file__).resolve().parent.parent
items=[]
for file in sorted((root/'.agents/skills').glob('*/SKILL.md')):
 text=file.read_text(encoding='utf-8-sig');assert text.startswith('---\n'),file
 name=re.search(r'^name:\s*(.+)$',text,re.M).group(1).strip();description=re.search(r'^description:\s*(.+)$',text,re.M).group(1).strip()
 assert name==file.parent.name and description,file
 items.append({'name':name,'path':file.relative_to(root).as_posix(),'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'description':description})
assert len(items)==15
folder=root/'.local';folder.mkdir(exist_ok=True)
(folder/'agent-skills-check.json').write_text(json.dumps(items,ensure_ascii=False,indent=2),encoding='utf-8')
print('Repository skills validated:',len(items),'No global configuration overwritten.')
