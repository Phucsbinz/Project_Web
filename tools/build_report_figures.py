"""Create code-native report figures from current source; never read configuration secrets."""
from pathlib import Path
import json
import textwrap
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'docs/report-assets'
OUT.mkdir(exist_ok=True)

def font(size, mono=False):
    candidates = [Path('C:/Windows/Fonts/consola.ttf' if mono else 'C:/Windows/Fonts/arial.ttf'),
                  Path('/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf' if mono else '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')]
    for path in candidates:
        if path.is_file():
            return ImageFont.truetype(str(path), size)
    raise SystemExit('Install Arial/Consolas or DejaVu fonts to build Vietnamese figures.')

manifest = []
def save(im, name, caption, source=None):
    im.save(OUT / name)
    manifest.append(dict(file=name, caption=caption, source=source, basis='working tree 24/10/2026; source illustration, not runtime screenshot'))

def code(name, title, relative, marker, notes):
    lines = (ROOT / relative).read_text(encoding='utf-8').splitlines()
    start = next(i for i, line in enumerate(lines) if marker in line)
    rendered = []
    for i in range(start, min(len(lines), start + 23)):
        pieces = textwrap.wrap(lines[i].expandtabs(4), 112, replace_whitespace=False, drop_whitespace=False) or ['']
        rendered.extend((str(i + 1) if j == 0 else '…', piece) for j, piece in enumerate(pieces))
    height = max(740, 220 + len(rendered) * 27)
    im = Image.new('RGB', (1800, height), 'white'); d = ImageDraw.Draw(im)
    d.text((55, 35), title, font=font(34), fill='#173c32')
    d.text((55, 90), relative, font=font(20), fill='#40564b')
    for row, (number, text) in enumerate(rendered):
        y = 145 + row * 27
        d.text((45, y), number, font=font(19, True), fill='#718078')
        d.text((112, y), text, font=font(20, True), fill='#17291f')
    y = 145 + len(rendered) * 27 + 20
    for note in notes:
        for line in textwrap.wrap(note, 140):
            if y + 26 > im.height:
                enlarged = Image.new('RGB', (1800, im.height + 100), 'white'); enlarged.paste(im, (0, 0));im=enlarged;d=ImageDraw.Draw(im)
            d.text((55, y), line, font=font(21), fill='#46574d');y+=28
    if y + 40 > im.height:
        padded=Image.new('RGB',(1800,y+45),'white');padded.paste(im,(0,0));im=padded
    save(im,name,title,dict(path=relative,first_line=start+1,excerpt_lines=min(23,len(lines)-start),notes=notes))

def diagram(name, title, nodes, subtitle):
    im=Image.new('RGB',(1800,260+len(nodes)*155),'white');d=ImageDraw.Draw(im)
    d.text((70,35),title,font=font(36),fill='#173c32')
    for i,(heading,body) in enumerate(nodes):
        y=115+i*155
        d.rounded_rectangle((90,y,1710,y+112),radius=12,fill='#f4f7f3',outline='#8da398',width=2)
        d.text((120,y+14),heading,font=font(27),fill='#173c32')
        for j,line in enumerate(textwrap.wrap(body,130)):
            d.text((120,y+53+j*25),line,font=font(21),fill='#293b31')
        if i<len(nodes)-1 and not (name=='application-architecture.png' and i==4):
            d.line((900,y+115,900,y+145),fill='#173c32',width=4)
            d.polygon([(900,y+150),(890,y+136),(910,y+136)],fill='#173c32')
    if name=='application-architecture.png':
        sy=115+2*155+56;ty=115+5*155+56
        d.line([(1710,sy),(1760,sy),(1760,ty),(1713,ty)],fill='#173c32',width=3)
        d.polygon([(1710,ty),(1723,ty-9),(1723,ty+9)],fill='#173c32')
    for j,line in enumerate(textwrap.wrap(subtitle,145)):
        d.text((70,130+len(nodes)*155+j*27),line,font=font(21),fill='#46574d')
    save(im,name,title)

diagram('application-architecture.png','Kiến trúc ứng dụng UTETra',[
    ('Trình duyệt','Bootstrap 5.3.3 và JavaScript gọi HTTP và STOMP. Người dùng nhận trang JSP đã render qua một SiteMesh layout.'),
    ('Tầng trình bày','Spring MVC Controller và JSP JSTL. Spring Security xác thực JWT, phiên DB và CSRF trước thao tác được bảo vệ.'),
    ('Tầng nghiệp vụ','Service kiểm role và scope, validation, tính tiền, transaction, stock, voucher, trạng thái đơn và ledger điểm.'),
    ('Tầng truy cập dữ liệu','Repository JPA và JDBC thực hiện truy vấn có tham số, khóa và cập nhật có điều kiện. Không dùng WebSocket để thay DB lock.'),
    ('MySQL tương thích TiDB','Flyway quản lý migration và Hibernate validate schema. Store, order, item, payment, redemption và auth session là dữ liệu thật.'),
    ('Tích hợp bên ngoài','Service gọi SMTP và Cloudinary; VNPAY ký URL và kiểm IPN khi merchant được cấu hình. Socket phát sau commit, polling phục hồi khi mất tín hiệu.')
], 'Mũi tên mô tả luồng xử lý chính. JWT và transaction là cơ chế xuyên tầng; không có kết nối trực tiếp browser tới database. VNPAY thật vẫn cần merchant sandbox.')
diagram('activity-vnpay.png','Hoạt động thanh toán VNPAY',[
    ('Chọn và tạo đơn','User hoặc Vendor chọn VNPAY, nhận báo giá rồi tạo order NEW và payment PENDING. Tồn và voucher được giữ trong transaction.'),
    ('Tạo URL','Service kiểm chủ đơn, trạng thái, merchant và hạn 15 phút; ký tham số bằng HMAC SHA512 rồi chuyển khách tới cổng.'),
    ('Nhận IPN','Endpoint công khai nhưng yêu cầu chữ ký merchant hợp lệ. Return trên trình duyệt chỉ chuyển về lịch sử đơn.'),
    ('Kiểm chứng và khóa','Kiểm TmnCode, txnRef, amount và trạng thái. Khóa order rồi payment; sai chữ ký hoặc tiền không được sửa đơn.'),
    ('Ghi kết quả một lần','Thành công chuyển PAID; tiền đến sau khi hủy chuyển REFUND_PENDING, không mở lại đơn. Callback trùng không cộng tiền lần hai.'),
    ('Hoàn tất và đối soát','Socket gửi sau commit. Cần merchant thật để thử sandbox; trạng thái nghi ngờ hoặc chưa hoàn tất được giữ REVIEW, không cho trả lại ngay.')
], 'Sơ đồ tổng quan; trường hợp failed, review, hủy và hoàn thủ công được giải thích trong source và testcase. Đây không phải ảnh thanh toán ngân hàng thật.')
code('code-vnpay-ipn.png','Xác minh IPN trước khi ghi thanh toán','src/main/java/com/utetra/order/service/VnpayService.java','public Map<String,String> ipn(',['Chữ ký phải hợp lệ trước khi đọc và sửa giao dịch.','Amount phải khớp cả payment snapshot và order. Order lock đi trước payment lock để cùng thứ tự với hủy và hoàn tiền.'])
code('code-manager-carrier.png','Manager quản lý carrier theo cửa hàng','src/main/java/com/utetra/order/service/OperationsService.java','public Carrier carrier(',['Manager phải có cửa hàng và quyền vận hành đúng scope.','Carrier toàn hệ thống vẫn thuộc Admin; không được đổi scope của carrier bằng cách thay storeId trong request.'])
code('code-jwt-registry.png','Lưu fingerprint JWT vào phiên DB','src/main/java/com/utetra/repository/AccessTokenRegistry.java','public void register(',['DB lưu JTI, hash và hạn dùng của access token, không lưu bearer token nguyên văn.','Refresh thay fingerprint; filter HTTP và guard WebSocket kiểm phiên và token hiện hành.'])
code('code-inventory.png','Giữ tồn hữu hạn trong transaction','src/main/java/com/utetra/order/service/OrderInventoryService.java','public Set<Long> reserve(',['Tổng số ly phải được gom theo variant trước khi giữ tồn.','Cập nhật có điều kiện ngăn số âm; checkout lỗi rollback toàn bộ, sự kiện chỉ gửi sau commit.'])
code('code-community.png','Bình luận kiểm quyền và điều kiện mua','src/main/java/com/utetra/identity/CommunityService.java','public long comment(',['Chỉ người có bằng chứng mua hợp lệ được bình luận.','Độ dài và media phải qua validation; ảnh code là trích source, không phải kết quả test provider.'])
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Created {len(manifest)} report figures in docs/report-assets; no credentials read.')
