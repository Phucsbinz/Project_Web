# Thiết lập bản tích hợp UTETra

> Cập nhật 27/10: source có 17 Flyway migrations, mới nhất 2026102705. DB cloud utetra_db đã áp dụng CSKH 2704 và bỏ plaintext 2705 theo yêu cầu; giữ 23 tài khoản/hash. Không sửa checksum migration cũ hoặc chạy reset password tool để cập nhật schema. CSKH dùng JWT/WebSocket hiện có, không thêm secret/provider. Setup/contract: features/customer-support/README.md.

Cập nhật 24/10/2026. Đây là hướng dẫn bản integration có code catalog, identity và order; thay hướng dẫn baseline ba bảng cũ. Không coi việc khởi động thành công là đã nghiệm thu email, media hoặc thanh toán thật.

## 1. Chuẩn bị và nhận source

- JDK 21, Git, Maven Wrapper có sẵn trong repo; Python 3 dùng các script hỗ trợ. Build WAR bằng Spring Boot 3.5.13, JSP/JSTL, SiteMesh và Bootstrap chung.
- Nhận toàn bộ bản integration đã được nhóm công bố, không chỉ copy JSP/config/migration. Source đang sửa chưa push sẽ không xuất hiện trên máy khác khi pull.
- Đọc AGENTS.md, SHARED_UI, PROJECT_PLAN, DATABASE_DESIGN, USE_CASES và hồ sơ nhánh. Không chạy code cũ song song để demo inventory/JTI mới trên cùng DB.

```powershell
git status --short
git fetch origin
git switch integration/utetra
git pull --ff-only origin integration/utetra
java -version
.\mvnw.cmd -B -ntp clean verify
```

Chỉ pull khi working tree sạch hoặc đã lưu công việc của mình. Không dùng reset/force push để bỏ thay đổi thành viên. Nếu JAVA_HOME đang trỏ JDK khác, đặt lại đường dẫn JDK 21 thực trên máy trước khi chạy Maven.

## 2. Database

Profile mặc định `mysql`, database riêng `utetra_db`. Thông tin kết nối nằm trong `src/main/resources/application-mysql.properties` theo cấu hình chung của repo; không copy credential vào báo cáo/ảnh/log. Không dùng database `project_idv`.

- Flyway quản lý migration, `ddl-auto=validate`, `clean-disabled=true`. Bản có carrier theo shop dùng migration đến `2026102403` và tổng 9 migration. Không sửa checksum các migration đã chạy.
- Không dùng create-drop/update trên cloud để chữa lỗi thiếu cột. Nếu schema lỗi, ghi migration/version và sửa có kiểm soát.
- DB thực là TiDB tương thích MySQL. Nếu thầy yêu cầu đúng MySQL Server, nhóm cần xác nhận/kiểm thêm trên MySQL riêng. CHECK trên cluster từng được đọc là OFF; vẫn phải kiểm validation và cập nhật có điều kiện, không suy diễn mọi CHECK đã được DB thực thi.
- Profile test dùng H2 in-memory. Fixture có guard chặn reset cloud; không đổi test datasource sang cloud để tạo dữ liệu demo.

## 3. Chạy và dừng ứng dụng

```powershell
java -jar target/utetra.war
```

Mở `http://localhost:8080/`. Health: `/api/system/health`. Nếu dùng script:

```powershell
py -3 tools/start_web.py 8080
py -3 tools/stop_web.py
```

Script tìm JDK 21 từ JAVA_HOME/UTETRA_JAVA_HOME hoặc bộ công cụ của workspace; không mặc định máy thành viên có đường dẫn JDK giống máy người khác. Dừng WAR trước khi đóng gói lại trên Windows để tránh file WAR đang bị khóa.

## 4. Tài khoản và dữ liệu cần có

Không có tài khoản demo production tự tạo kèm hướng dẫn này. Nhóm cần tài khoản active/verified đúng role và scope, không ghi mật khẩu vào báo cáo.

1. Admin có quyền toàn hệ thống; Manager được phân công shop; quyền catalog dùng chung phải được cấp `CATALOG_ADMIN` nếu báo cáo chức năng đó.
2. User đăng ký shop tạo hồ sơ chờ duyệt. Admin chuẩn bị policy chiết khấu phù hợp, duyệt và kích hoạt shop; chủ shop đăng nhập lại để nhận quyền Vendor. Vendor vẫn có luồng mua hàng như User.
3. Chuẩn bị danh mục, sản phẩm, size, topping và ảnh; shop đúng giờ/đang nhận đơn. Muốn demo finite inventory, nhập số ly theo size; NULL là chưa bật theo dõi số lượng hữu hạn.
4. Chuẩn bị khu vực giao, carrier và biểu phí. Manager tạo carrier riêng của shop; carrier toàn hệ thống do Admin quản lý. Carrier riêng không được dùng ở shop khác.
5. User có địa chỉ thuộc chính mình; voucher đúng shop/global và còn quota. Tài khoản test cần dữ liệu hợp lệ trước khi thử checkout.
6. Top 20/phân trang cần đủ dữ liệu; Guest hiện diễn giải bán >10 theo số ly từ đơn DELIVERED/PAID, cần thầy xác nhận. Dữ liệu thử phải ghi rõ là demo; không giả là giao dịch thật.

Lần kiểm tra trước, DB có 9 tài khoản active/verified nhưng không có shop/product/carrier/policy đang hoạt động. Không lấy kết quả test H2 làm bằng chứng DB cloud đã đủ dữ liệu.

## 5. Email OTP

Code đăng ký/kích hoạt, đăng nhập/đăng xuất và reset OTP đã triển khai; tài liệu baseline nói chưa có code không còn đúng.

```powershell
py -3 tools/configure_mail.py --email your-address@gmail.com
```

Nhập App Password của tài khoản gửi do chủ Gmail cấp, không dùng mật khẩu đăng nhập Gmail. Script và cấu hình bàn giao theo [MAIL_SETUP](features/identity-membership/MAIL_SETUP.md). Trên máy kiểm tra chưa có SMTP local; phải cấu hình và khởi động lại trước demo nhận OTP thật. Không gửi OTP thử cho người không tham gia kiểm thử.

## 6. JWT và WebSocket

JWT dùng cookie HttpOnly, CSRF cho mutation và kiểm account/role/session DB. Session lưu thêm JTI/hash/hạn access token, refresh thay token hiện hành; đăng nhập lại hoặc refresh hợp lệ sau khi nâng schema mới.

WebSocket `/ws` dùng STOMP; topic order kiểm quyền riêng. Checkout nghe thay đổi stock/voucher sau commit và có polling 15 giây. DB transaction/khóa quyết định ai đặt thành công. Xem [REALTIME_CHECKOUT_JWT](features/order-payment/REALTIME_CHECKOUT_JWT.md).

## 7. Cloudinary

Adapter upload/xóa ảnh đã có và có biên bản thử PNG thật trước đó. Đọc `CloudinaryConfig` và file properties hiện tại; không chụp khóa lên báo cáo. Bình luận hỗ trợ ảnh/video nhưng cần kiểm video thực qua provider trên máy báo cáo; test mock/PNG không thay nghiệm thu MP4. Media provider lỗi không được ghi thành upload thành công.

## 8. Các phương thức thanh toán

- COD: không cần merchant ngân hàng. Kiểm giao hàng và xác nhận thu tiền theo quyền; chưa thu đủ không được coi đơn hoàn tất.
- QR Techcombank: [BANK_TRANSFER](features/order-payment/BANK_TRANSFER.md). QR tài khoản tĩnh, khách nhập số tiền/nội dung theo đơn; Admin đối soát và xác nhận. Không tự chuyển tiền/không tự xác nhận từ ảnh chụp.
- VNPAY: đã có code ký URL và kiểm IPN, vẫn tắt ở cấu hình thật khi thiếu merchant. Xem [VNPAY](features/order-payment/VNPAY.md) để thiết lập, kiểm sandbox, public IPN, hủy/hoàn/đối soát. Không dùng key test trong mã kiểm thử làm tài khoản merchant thật.
- MOMO: chưa triển khai; nếu đã chọn VNPAY thì không bắt buộc làm thêm cả MOMO chỉ vì đề viết VNPAY hoặc MOMO.

## 9. Checklist máy báo cáo

- Build và health đạt, schema đúng version, chỉ một layout và một Bootstrap.
- Đăng nhập được đúng role/scope; Vendor mua như User, Manager không sửa shop khác.
- Nhận OTP đăng ký/reset thật; upload ảnh/video đúng quyền và định dạng.
- Có shop/món/địa chỉ/carrier/rate/policy/voucher và dữ liệu top hợp lệ.
- Demo hai người tranh tồn/voucher và trạng thái socket, kể cả khi không nhận được socket thì server vẫn chặn vượt quota.
- Demo COD/QR và VNPAY sandbox khi merchant đã cấp; phân biệt chữ ký IPN và ReturnUrl.
- Demo giao/thu/trả/hoàn và ledger, chụp desktop/mobile/tablet đúng phiên bản.
- Hoàn thiện tên/MSSV/phân công, khảo sát, hình source/activity/UI, prompt, DOCX và diễn tập; không tự xác nhận đã nộp/chấm đạt.

Hạn nộp/link nộp theo thông báo lớp; văn bản được cung cấp chưa có ngày cụ thể.
