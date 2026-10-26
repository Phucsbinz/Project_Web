# Gửi mail sau khi pull nhánh identify

Gmail gửi OTP mặc định của nhóm là **jskosiskeekdj@gmail.com**, thay địa chỉ cũ. App Password được giữ trong `.local/mail.properties` trên máy chạy, không đưa lên Git.

## Thiết lập trên máy mới

Chạy tại thư mục gốc repository:

```powershell
python tools/configure_mail.py
```

Nhập App Password 16 chữ cái của Gmail người gửi được chủ tài khoản cấp riêng. Script ẩn nội dung khi nhập và ghi file cấu hình cục bộ. Muốn dùng Gmail khác, thêm `--email your-address@gmail.com`. Không dùng mật khẩu đăng nhập Gmail.

Sau đó dùng JDK 21, build và chạy từ thư mục gốc:

```powershell
.\mvnw.cmd -B -ntp package -DskipTests
java -jar target/utetra.war
```

Spring đọc `.local/mail.properties`; không cần máy Thanh đang bật. Máy chạy phải truy cập được SMTP Gmail cổng 587 qua STARTTLS. Khởi động lại server sau khi thay cấu hình. Chạy từ thư mục khác thì truyền `--spring.config.import=optional:file:C:/utetra/.local/mail.properties` với đường dẫn thực tế.

Mail dùng cho OTP đăng ký và khôi phục mật khẩu, có bản HTML và plain text. OTP có hiệu lực 5 phút, gửi lại cách nhau tối thiểu 60 giây. Kiểm tra Inbox/Spam khi thử đăng ký hoặc quên mật khẩu. Test tự động dùng mail giả lập, không tự gửi OTP thật.

## Chia sẻ cho nhóm

Pull code cập nhật địa chỉ gửi mặc định, không chuyển App Password. Mỗi máy cần cấu hình riêng một lần. Chủ tài khoản cấp thông tin qua kênh riêng; có thể thu hồi App Password trong tài khoản Google khi ngừng sử dụng. Các file trong `.local` tiếp tục được Git bỏ qua.
