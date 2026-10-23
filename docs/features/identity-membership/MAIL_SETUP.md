# Gửi mail sau khi pull nhánh identify

Nhánh có đầy đủ `MailOtpDelivery`, dependency Spring Mail, template HTML/plain text
và cấu hình SMTP Gmail qua STARTTLS cổng 587. Mail được dùng cho OTP đăng ký
và khôi phục mật khẩu. Mã có hiệu lực 5 phút; gửi lại cách nhau tối thiểu 60 giây.

## Thiết lập một lần trên máy mới

Chạy tại thư mục gốc repository, với Python 3:

```powershell
python tools/configure_mail.py --email your-address@gmail.com
```

Nhập **App Password 16 chữ cái của đúng Gmail người gửi**, không phải mật khẩu
đăng nhập Gmail. Có thể dùng Gmail riêng của bạn hoặc Gmail của nhóm nếu chủ
tài khoản đã cấp App Password riêng qua kênh riêng. Gmail cần bật xác minh hai
bước và cho phép tạo App Password. Script không hiện mật khẩu và ghi cấu hình
vào `.local/mail.properties`, được Git bỏ qua. Nếu dùng Gmail của nhóm hiện tại,
có thể chạy `python tools/configure_mail.py` để dùng địa chỉ mặc định.

Không có Python: tạo thư mục `.local`, copy `config/mail.properties.example`
thành `.local/mail.properties`, rồi sửa địa chỉ và App Password trong file mới.
Không sửa mật khẩu vào file mẫu hoặc `src/main/resources/application.properties`.

## Chạy ứng dụng

Cài JDK 21, đặt `JAVA_HOME`, sau đó chạy từ thư mục gốc:

```powershell
.\mvnw.cmd -B -ntp verify
java -jar target/utetra.war
```

Ứng dụng đọc `.local/mail.properties` theo thư mục chạy hiện tại. Nếu chạy WAR
từ thư mục khác, dùng đường dẫn tuyệt đối đến file cấu hình:

```powershell
java -jar target/utetra.war --spring.config.import=optional:file:C:/utetra/.local/mail.properties
```

Khởi động lại ứng dụng sau khi thay cấu hình. Mở `/auth/register` hoặc
`/auth/forgot`, yêu cầu OTP cho email bạn kiểm soát và kiểm tra Inbox/Spam.
Dùng mã trong email mới nhất. Không gửi OTP thử trong lúc build/test tự động.

## Cấu hình bằng biến môi trường

Có thể dùng bốn biến `UTETRA_MAIL_ENABLED=true`, `UTETRA_MAIL_USERNAME`,
`UTETRA_MAIL_FROM`, `UTETRA_MAIL_APP_PASSWORD` thay cho file. Khi dùng cách này,
không giữ `.local/mail.properties` cũ vì file được import có độ ưu tiên cao hơn.
Không ghi giá trị bí mật vào Git hay lịch sử lệnh chia sẻ.

Pull code không tự chuyển App Password từ máy người khác. Thiết lập một lần
ở trên là bắt buộc để gửi mail thật trên máy mới. Nếu gặp lỗi, kiểm tra địa chỉ
khớp App Password, mail đã bật, quyền truy cập SMTP 587 và cấu hình đã được nạp.
Ứng dụng báo lỗi khi SMTP thất bại; không trả OTP qua API hoặc log.

## Chia sẻ cho thành viên nhóm

Thanh chỉ cần chia sẻ repository và hướng dẫn này. Mỗi bạn chạy script cấu hình trên máy mình rồi khởi động lại ứng dụng; không cần máy Thanh đang bật. File `.local/mail.properties` không đi theo Git. Ưu tiên mỗi người dùng Gmail và App Password của chính mình. Nếu dùng chung Gmail nhóm, chủ tài khoản cấp App Password riêng cho từng người qua kênh riêng, có thể thu hồi riêng khi ngừng sử dụng. Không gửi cả thư mục `.local`, vì có thể chứa cấu hình và dữ liệu cục bộ khác.

Chạy từ thư mục gốc project để nạp đúng file. Không commit App Password, không dùng mật khẩu đăng nhập Gmail. Test tự động dùng mail giả lập; cần thử đăng ký và khôi phục bằng hộp thư thật để xác nhận SMTP trên từng máy.
