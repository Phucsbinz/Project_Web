# UTETra — Hướng dẫn setup chung và ghi kết quả thực tế

> Trạng thái: khung hướng dẫn trước khi dựng B0. Repository tại lúc soạn bộ tài liệu chưa có mã nguồn Spring Boot/pom/Wrapper. Các lệnh build/chạy dưới đây là mục tiêu cần xác minh sau B0, không phải kết quả đã chạy thành công.

## 1. Thông tin môi trường cần ghi

| Thành phần | Yêu cầu theo baseline | Phiên bản thực/đường dẫn/kết quả |
| --- | --- | --- |
| OS/IDE | Windows hoặc môi trường nhóm chọn | [CẦN BỔ SUNG] |
| JDK | Java 21 | [CẦN BỔ SUNG] |
| Build | Maven Wrapper dùng chung | [CẦN BỔ SUNG] |
| Database chính | SQL Server; database/tài khoản được tạo trước | [CẦN BỔ SUNG] |
| Database kiểm thử | MySQL, PostgreSQL theo profile riêng | [CẦN BỔ SUNG] |
| Dịch vụ ảnh | Cloudinary của nhóm | [CẦN BỔ SUNG — không điền secret] |
| Email | SMTP test và cấu hình gửi thật khi cần | [CẦN BỔ SUNG] |
| Baseline | Commit main dùng để setup | [CẦN BỔ SUNG] |

## 2. Lấy mã và chọn nhánh

Clone từ URL repository thật do nhóm cung cấp; không bịa URL. `git switch <nhanh-duoc-giao>` rồi `git merge main` sau khi B0 hoàn thành. Kiểm tra hướng dẫn ở gốc nhánh, hồ sơ module và contract. Điền URL repo/commit/tag ở bản báo cáo; không đưa credential vào URL.

## 3. Biến môi trường chung

| Biến | Nội dung | Owner/phạm vi |
| --- | --- | --- |
| SPRING_PROFILES_ACTIVE | sqlserver hoặc mysql hoặc postgresql, chọn đúng một | B0 |
| DB_URL, DB_USERNAME, DB_PASSWORD | URL JDBC/tài khoản tương ứng database đã tạo | B0 |
| JWT_SECRET, JWT_ISSUER, JWT_AUDIENCE | Theo cấu hình Security/JWT chung | TV2 |
| CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET | Tài khoản ảnh của nhóm; secret chỉ local/secret store | TV1 |
| MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM | SMTP reset mật khẩu | TV2 |
| APP_BASE_URL | Địa chỉ website thực đang chạy | B0/TV2 |
| APP_TIME_ZONE | Asia/Ho_Chi_Minh | B0 |

Không thêm biến tính khoảng cách/Maps API. Phí giao đọc store_delivery_areas theo đặc tả, không tự thay bằng phí cố định. Biến phải được ánh xạ thực trong application config. Không commit file .env/password/key, không chụp secret trong hình setup.

## 4. Database và migration

1. Tạo database và tài khoản đủ quyền theo hệ quản trị được chọn.
2. Chọn profile và URL JDBC đúng; đảm bảo service DB đang chạy, port và quyền truy cập hợp lệ.
3. Flyway áp dụng migration của dialect; ddl-auto=validate; không để mỗi người tự tạo schema khác bằng update.
4. Chỉ bật seed demo ở môi trường thử. Seed đủ ba vai trò, hai Customer, Staff hai chi nhánh, danh mục/món/size/topping/khu vực, tình trạng bán và đơn/ledger mẫu nhất quán.
5. Ghi lệnh thực, thứ tự migration, kết quả và lỗi sửa được. Kiểm thử các dialect còn lại trước khi xác nhận hỗ trợ ba CSDL.

## 5. Build và chạy sau B0

```powershell
$env:SPRING_PROFILES_ACTIVE = 'sqlserver'
# Thiết lập các biến local còn lại bằng giá trị của môi trường mình.
.\mvnw.cmd clean verify
java -jar target/utetra.war
```

Tên WAR phụ thuộc finalName=utetra đã chốt. Điền port/context path/URL truy cập thực sau khi chạy; không mặc định chúng đã được cấu hình. Kiểm tra trang JSP qua SiteMesh ở bản WAR, không chỉ IDE. Local HTTP cần cookie Secure theo profile local; triển khai dùng HTTPS/WSS theo tài liệu bảo mật.

## 6. Setup và demo theo nhánh

| Nhánh | Điều kiện riêng | Luồng chạy để kiểm chứng |
| --- | --- | --- |
| catalog-sales | Cloudinary, data catalog và chi nhánh/khu vực, quyền Admin/Staff | CRUD món/upload → menu → chọn size/topping/chi nhánh |
| identity-membership | JWT, session/reset schema, SMTP test, tài khoản phân công | Đăng ký → đăng nhập → refresh/reset → địa chỉ → kiểm tra quyền/điểm |
| order-payment | Hai module trước hoặc contract mock trong test, dữ liệu giá/quyền/điểm | Giỏ → checkout COD → xử lý → thu tiền → realtime/điểm/báo cáo |

Mỗi người bổ sung hướng dẫn cụ thể tại IMPLEMENTATION của mình và file riêng nếu dài. Contract mock chỉ hỗ trợ test module, không dùng làm bằng chứng demo ứng dụng tích hợp.

## 7. Nhật ký setup và xử lý lỗi

| Ngày/người | Commit/profile | Lệnh/thao tác thực | Kết quả | Lỗi và cách sửa | Hình/log minh chứng |
| --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [chưa chạy/qua/lỗi] | [điền] | [điền] |

Trước bàn giao chạy từ môi trường sạch, thực hiện backup/restore test và kiểm chứng không thiếu secret/config ngoài tài liệu. Báo cáo nêu rõ CSDL đã kiểm thử, phiên bản và môi trường thực tế.
