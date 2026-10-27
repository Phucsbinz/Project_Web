"""Validate feedback recipe coverage and prevent coffee/tea template mixups."""
from pathlib import Path
import json,unicodedata
root=Path(__file__).resolve().parent.parent
menu=json.loads((root/'tools/demo/menu.json').read_text(encoding='utf-8'))
assert len(menu)==30 and len({m['slug'] for m in menu})==30
assert len({note for drink in menu for note in drink['tasting_notes']})==90
for drink in menu:
 notes=drink['tasting_notes'];assert len(notes)==3 and len(set(notes))==3,drink['slug']
 for note in notes:
  assert unicodedata.normalize('NFC',note)==note and '\ufffd' not in note,drink['slug']
  sample='Mình chọn '+drink['name']+'. '+note+' Size L, 100% đường và 50% đá. Nội dung demo.'
  assert 50<=len(sample)<=1000,drink['slug']
  if drink['category']=='Cà phê':
   assert 'trà' not in note.lower(),drink['slug']
   assert 'cà phê' in note.lower() or 'bạc xỉu' in note.lower(),drink['slug']
print('Feedback recipes: 30 drinks, 90 distinct product-specific notes; coffee context and Unicode checks passed')
