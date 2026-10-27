"""Build CSKH architecture/activity and source figures from repository code only."""
from pathlib import Path
import textwrap,json
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parent.parent;out=root/'docs/features/customer-support/assets';out.mkdir(parents=True,exist_ok=True)
fonts=root/'src/main/resources/static/assets/vendor/fonts/be-vietnam-pro'
def font(size,bold=False):return ImageFont.truetype(str(fonts/('BeVietnamPro-Bold.ttf' if bold else 'BeVietnamPro-Regular.ttf')),size)
ink='#263b2c';green='#315638';cream='#f5f2e8'
items=[('01 · Xác thực và kênh hỗ trợ','Cookie JWT → Spring Security → User chọn Admin hoặc chi nhánh có Manager phụ trách.'),('02 · Presentation','SupportController và JSP dùng chung app.jsp; client không được chọn senderId hoặc customerId.'),('03 · Service và phạm vi','SupportService kiểm phiên/role/scope; chỉ User tạo hội thoại. Nhân viên chỉ trả lời khách trong phạm vi.'),('04 · JPA / MySQL','SupportRepository lưu hội thoại và nội dung tin; khóa hội thoại và UUID retry ngăn nhân đôi tin nhắn.'),('05 · Sau khi COMMIT','SupportEvents phát hint tới /user/queue/support của đúng người có quyền; không broadcast nội dung.'),('06 · Cập nhật và lịch sử','User/Manager/Admin nhận WebSocket rồi đọc lịch sử qua API kiểm quyền. Reload vẫn lấy tin từ DB.')]
image=Image.new('RGB',(1440,1280),cream);draw=ImageDraw.Draw(image)
draw.text((70,35),'CSKH UTETra · Kiến trúc và luồng hoạt động',font=font(31,True),fill=green)
for i,(title,body) in enumerate(items):
 y=115+i*177;draw.rounded_rectangle((65,y,1375,y+145),radius=20,fill='#fffef9',outline='#d5dec6',width=2)
 draw.text((95,y+20),title,font=font(25,True),fill=green)
 for j,line in enumerate(textwrap.wrap(body,92)):draw.text((95,y+64+j*31),line,font=font(22),fill=ink)
 if i<5:draw.line((720,y+148,720,y+169),fill=green,width=3);draw.polygon([(713,y+162),(727,y+162),(720,y+173)],fill=green)
draw.text((70,1210),'Minh họa từ source · không phải giao dịch hoặc dữ liệu khách hàng thật',font=font(18),fill=ink)
image.save(out/'flow-support.png')
rows=[];sources=[]
for path,needle in [('src/main/java/com/utetra/support/SupportService.java','public void require('),('src/main/java/com/utetra/support/SupportService.java','private Map<String,Object> allowed('),('src/main/java/com/utetra/support/SupportRepository.java','private static final String ACCESS=')]:
 lines=(root/path).read_text(encoding='utf-8').splitlines();number=next(i for i,line in enumerate(lines,1) if needle in line)
 rows.extend(['',path+':'+str(number)]);rows.extend(textwrap.wrap(lines[number-1],105,break_long_words=True,replace_whitespace=False));sources.append({'path':path,'line':number})
height=260+len(rows)*27+160;image=Image.new('RGB',(1440,height),cream);draw=ImageDraw.Draw(image)
draw.text((65,35),'Giải thích source · Quyền CSKH được kiểm ở backend',font=font(29,True),fill=green)
y=105
for line in rows:draw.text((65,y),line,font=font(18),fill=ink);y+=27
y+=35
for line in ['1. JWT phải còn phiên hợp lệ; chỉ User, Manager, Admin được dùng CSKH.','2. Hội thoại không đúng phạm vi trả 404; không tin ID người gửi do client cung cấp.','3. Nhánh của Manager được kiểm bằng membership đang hoạt động; Admin chỉ kênh ADMIN.']:
 for wrapped in textwrap.wrap(line,100):draw.text((65,y),wrapped,font=font(20),fill=green);y+=30
image.save(out/'source-access-control.png')
(out/'manifest.json').write_text(json.dumps({'basis':'integration working tree 27/10/2026; code-native illustrations, not runtime screenshots','files':['flow-support.png','source-access-control.png'],'sources':sources},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Created 2 CSKH report figures; no configuration secrets read.')
