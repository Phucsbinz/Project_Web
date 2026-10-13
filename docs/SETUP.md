# UTETra — Hướng dẫn chạy baseline chung

## 1. Bộ phiên bản và cấu hình thật

Java 21; Boot 3.5.13; Maven Wrapper 3.9.16; WAR utetra; package com.utetra. Đọc pom.xml và src/main/resources/application.properties, application-mysql.properties. Cấu hình ghi trực tiếp và được theo dõi Git theo yêu cầu nhóm; không import .env. Mật khẩu chỉ tại file cấu hình cần thiết, không sao chép vào báo cáo/prompt/log.

MySQL Connector nối TiDB Cloud cổng 4000, database riêng utetra_db, sslMode=VERIFY_IDENTITY. Database đã tạo riêng; không đổi hoặc migration project_idv. TLS dùng truststore JDK; không hạ xuống useSSL=false/trust-all khi gặp lỗi kết nối cloud. JPA validate và Flyway chỉ chạy migration version mới.

## 2. Lấy main và chạy

```powershell
git switch main
java -version
.\mvnw.cmd -v
.\mvnw.cmd clean verify
java -jar target/utetra.war
```

PATH/JAVA_HOME phải trỏ Java21, không Java17 mặc định máy hiện tại. Nếu cần trên máy đang làm:

```powershell
$env:JAVA_HOME='D:/java/.tools/jdk21/jdk-21.0.12.1+1'
$env:Path=$env:JAVA_HOME+'/bin;'+$env:Path
```

Máy khác điền JDK21 của máy mình; không tự sửa pom thành Java17/26. Mở http://localhost:8080/; health JSON /api/system/health. Trang home hiện chỉ kiểm chứng JSP/JSTL/SiteMesh/Bootstrap và ghi rõ tính năng còn chờ feature. /auth/login là màn hình thông tin, chưa luồng đăng nhập/OTP thật; không có tài khoản demo tự tạo.

## 3. Schema và test

V1 hiện thực roles/users/auth_sessions, seed 5 role; flyway_schema_history là bảng kỹ thuật. Không tạo 43 bảng bằng Hibernate update; chúng là thiết kế để triển khai migration tiếp theo. Thứ tự/owner/FK theo DATABASE_DESIGN. Main chốt migration, không mỗi nhánh tạo V2 khác nhau.

Test profile test dùng H2 isolated, create-drop chỉ database in-memory để thử JWT/role/CSRF/session; không trỏ test create-drop lên cloud. Migration/JPA/TLS kiểm chứng bằng WAR trên MySQL/TiDB thật; bằng chứng trong BASELINE_VERIFICATION.md.

## 4. Cấu hình provider

| Nhóm properties | Cần điền/bật | Trạng thái |
| --- | --- | --- |
| security.jwt.* | Đã có key ngẫu nhiên, issuer/audience/15m/7d; không tự thay khác giữa nhánh | Hạ tầng ký/verify; login/refresh nghiệp vụ cần feature |
| app.otp.* và spring.mail.* | SMTP/from/auth/TLS, enabled; OTP HMAC/TTL5m/5attempts/60s theo contract | Chưa có SMTP thật; OTP nghiệp vụ chưa code |
| cloudinary.* | cloud-name/api-key/api-secret; enabled=true khi có credentials | Bean/adapter có kiểm ảnh, hiện disabled |
| payment.vnpay.* | tmn-code/hash-secret/return URL/IPN HTTPS công khai; enabled | Chưa merchant credentials/handler; không báo payment thành công |
| features.shipper.enabled | Bật khi có module assignment/permission/test | Phần thêm, hiện false |

Không lấy credentials mẫu trong PDF để dùng thật. Provider disabled không nghĩa là tích hợp đã hoàn tất. Cloudinary baseline hỗ trợ ảnh JPEG/PNG5MB; feature phải mở rộng video đúng kiểm MIME/signature/20MB cho comments, và cleanup theo commit/rollback. Chỉ mở IPN khi có handler verify; CSRF exception giới hạn callback thật, không disable toàn app.

## 5. Lỗi cấu hình đã gặp và cách giữ nhất quán

Java default17 không build release21 → đặt JAVA_HOME. PowerShell truyền Maven -D có dấu chấm phải quote tham số. SiteMesh 3.2.2 forward decorator có URL nội bộ bị StrictHttpFirewall chặn → giới hạn Security dispatcher REQUEST/ASYNC/ERROR trong properties; request ngoài vẫn qua firewall, không cho phép malicious // toàn cục. API/assets/ws/payments loại khỏi decorator; filter JWT đăng ký một lần trong chain, bean servlet registration disabled.

Các lỗi DNS/TLS/TiDB/credentials phải kiểm cấu hình trước, không đổi DB khác. Nếu 8080 đã dùng, đổi server.port trực tiếp chung hoặc dùng --server.port khi chạy thử và ghi rõ; không để thành viên tự đổi source main không review.

## 6. Setup theo module và nhật ký

Catalog cần Cloudinary/catalog/store/promo seed; Identity cần SMTP/OTP/session/address/community/loyalty; Order cần contract giá/quyền/điểm/promo và merchant sandbox/carrier/commission. Mỗi branch có hướng dẫn riêng và UC note.

| Ngày/người | Commit/profile | Lệnh/URL/dữ liệu | Kết quả thật | Lỗi/cách sửa | Hình/log |
| --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền] |
