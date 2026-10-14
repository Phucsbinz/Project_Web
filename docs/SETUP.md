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

Java default17 không build release21 → đặt JAVA_HOME. PowerShell truyền Maven -D có dấu chấm phải quote tham số. SiteMesh 3.2.2 mặc định thêm prefix `/WEB-INF/decorators/`. Main map các đường dẫn đầy đủ `/WEB-INF/views/layouts/*.jsp`, nên phải `builder.setDecoratorPrefix("")` trước mapping. Bản sửa này loại đường dẫn ghép sai gây 404/500; vẫn giữ dispatcher REQUEST/ASYNC/ERROR và firewall nghiêm ngặt với request ngoài. API/assets/ws/payments loại khỏi decorator; filter JWT đăng ký một lần trong chain, bean servlet registration disabled.

Các lỗi DNS/TLS/TiDB/credentials phải kiểm cấu hình trước, không đổi DB khác. Nếu 8080 đã dùng, đổi server.port trực tiếp chung hoặc dùng --server.port khi chạy thử và ghi rõ; không để thành viên tự đổi source main không review.

## 6. Setup theo module và nhật ký

Catalog cần Cloudinary/catalog/store/promo seed; Identity cần SMTP/OTP/session/address/community/loyalty; Order cần contract giá/quyền/điểm/promo và merchant sandbox/carrier/commission. Mỗi branch có hướng dẫn riêng và UC note.

| Ngày/người | Commit/profile | Lệnh/URL/dữ liệu | Kết quả thật | Lỗi/cách sửa | Hình/log |
| --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền] |

## 7. Linux và cloud

Cần JDK 21 đầy đủ, không chỉ JRE; `java -version` và `javac -version` cùng nền 21. Wrapper không cần chmod file tracked: dùng `bash mvnw`. Nếu thư mục người dùng chỉ đọc, đặt Maven cache trong `/workspace`.

```bash
cd /workspace/Project_Web
export JAVA_HOME=/workspace/onboarding-utetra/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_USER_HOME=/workspace/.m2
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
bash mvnw -s /workspace/onboarding-utetra/settings.xml -B -ntp -Dmaven.repo.local=/workspace/.m2/repository verify
```

Các đường dẫn trên là của môi trường cloud đã chuẩn bị; máy khác dùng JDK/cache của máy mình. `settings.xml` ngoài checkout được tạo từ HTTPS_PROXY của máy bởi install_script trong cấu hình Codex; không lưu thông tin proxy/credential vào Git. Dùng truststore hệ thống có CA môi trường, không tắt TLS. install_script cài JDK với SHA-256 kiểm từ Oracle và tạo settings/cache, start_skill ghi bước khởi động; chúng là cấu hình Codex, không tự push lên GitHub.

Smoke test isolated với H2, giữ nguyên file properties tracked:

```bash
bash mvnw -s /workspace/onboarding-utetra/settings.xml -B -ntp -Dmaven.repo.local=/workspace/.m2/repository spring-boot:run -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.profiles=test '-Dspring-boot.run.arguments=--server.port=8081 --spring.config.additional-location=file:./src/test/resources/'
```

Kiểm HTTP nội bộ: home/login 200 với layout render, health 200 JSON UTETra/UP, Bootstrap 200 CSS, admin không xác thực 401. H2 disposable không chạy migration MySQL và không chứng minh TiDB. TiDB cần DNS và TCP native đến host trong application-mysql.properties cổng 4000; cloud hiện lỗi DNS, probe HTTPS CONNECT nhận 403. Không sửa DB hoặc hạ TLS để vượt lỗi mạng. Sau khi mạng hỗ trợ, chạy WAR với profile mysql và kiểm Flyway/JPA/TLS thật.
