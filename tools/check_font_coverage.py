"""Verify bundled font cmap coverage for the complete Vietnamese vowel/tone alphabet."""
from pathlib import Path
import struct,unicodedata
root=Path(__file__).resolve().parent.parent
letters={unicodedata.normalize('NFC',v+tone) for v in 'aăâeêioôơuưyAĂÂEÊIOÔƠUƯY' for tone in ['', '\u0300','\u0301','\u0309','\u0303','\u0323']}|{'đ','Đ'}
points={ord(letter) for letter in letters}
def u16(data,offset):return struct.unpack_from('>H',data,offset)[0]
def u32(data,offset):return struct.unpack_from('>I',data,offset)[0]
def covered(data,cp):
 tables={data[12+16*i:16+16*i]:u32(data,20+16*i) for i in range(u16(data,4))}
 cmap=tables[b'cmap']
 for i in range(u16(data,cmap+2)):
  record=cmap+4+8*i;platform=u16(data,record);encoding=u16(data,record+2)
  if platform!=0 and not(platform==3 and encoding in [1,10]):continue
  offset=cmap+u32(data,record+4);fmt=u16(data,offset)
  if fmt==12:
   for n in range(u32(data,offset+12)):
    start,end,glyph=struct.unpack_from('>III',data,offset+16+12*n)
    if start<=cp<=end and glyph+cp-start:return True
  if fmt==4 and cp<=65535:
   count=u16(data,offset+6)//2;ends=offset+14;starts=ends+2*count+2;deltas=starts+2*count;ranges=deltas+2*count
   for n in range(count):
    if u16(data,ends+2*n)<cp:continue
    start=u16(data,starts+2*n)
    if cp<start:break
    delta=u16(data,deltas+2*n);jump=u16(data,ranges+2*n)
    if not jump:return bool((cp+delta)&65535)
    glyph=u16(data,ranges+2*n+jump+2*(cp-start))
    if glyph and ((glyph+delta)&65535):return True
    break
 return False
for weight in ['Regular','Medium','SemiBold','Bold']:
 path=root/'src/main/resources/static/assets/vendor/fonts/be-vietnam-pro'/('BeVietnamPro-'+weight+'.ttf');data=path.read_bytes()
 missing=sorted(cp for cp in points if not covered(data,cp))
 if missing:raise SystemExit(weight+' missing: '+','.join('U+'+format(cp,'04X') for cp in missing))
 print(weight+': '+str(len(points))+' Vietnamese glyphs verified')
