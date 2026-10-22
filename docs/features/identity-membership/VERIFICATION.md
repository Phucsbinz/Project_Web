# Kiểm chứng identity-membership — 17/10/2026

Đã cập nhật main, checkout nhánh remote `feature/identity-membership`, merge `origin/main` thành `8a18438`. Workspace sạch trước khi làm. Chưa commit/push mã chức năng; chưa có reviewer do nhóm xác nhận. [Inventory](DOCUMENT_INVENTORY.json) ghi 88 file Markdown tracked được đọc, số dòng và SHA-256; không tính Markdown từ dependency tải về.

## Kết quả thực

| Kiểm tra | Kết quả |
| --- | --- |
| Maven Wrapper/JDK 21 `verify` | BUILD SUCCESS, WAR `target/utetra.war` |
| Tổng Surefire | 42 test, 0 failure/error/skipped |
| BaselineSecurityTest / BaselineViewTest | 6 + 3 đạt; giữ JWT/role/CSRF và JSP/SiteMesh |
| JwtServiceTest / SessionIdentityServiceTest | 5 + 2 đạt |
| IdentityIntegrationTest | 22 đạt: OTP, SMTP rollback, refresh race, grant, địa chỉ, profile, community, media compensation, điểm, quản trị/scope |
| IdentityViewTest | 2 đạt; render 12 trang trên Tomcat; hồi quy cookie CSRF và POST sau background GET |
| IdentityMigrationTest | 1 đạt; chạy file migration thật trên H2 MySQL mode và kiểm unique/check |
| IdentitySocketLifecycleTest | 1 đạt; đóng idle socket khi phiên bị thu hồi |
| Edge headless/Playwright | 16 check đạt, 0 lỗi JavaScript; login, địa chỉ mặc định, logout, màn hình User/Manager/Admin, không tràn ngang ở 390/768/1440px |
| Diagram/source/UI | 16 bộ activity.mmd + SVG + ảnh đoạn source thực; ảnh UI tại `assets/browser`; chi tiết trong từng UC |

Bằng chứng trình duyệt có thể đọc máy: [verification.json](assets/browser/verification.json). Source test ở `src/test/java/com/utetra/identity`; lệnh tái chạy và contract ở [RUNBOOK](RUNBOOK.md). Build log trong workspace là `tmp-identity-verify.log` (ignored); kết quả số lượng đối chiếu được với Surefire XML.

## Lỗi đã phát hiện và sửa

1. Sau khi chuyển repository sang EntityManager, binding Timestamp qua Hibernate lệch 7 giờ trên H2 có timezone. Đã normalize parameter thành UTC OffsetDateTime; test kiểm hạn grant thực và consume/reset/replay.
2. CSRF cookie bị tạo sau thời điểm SiteMesh commit headers. Thêm CsrfTokenMaterializationFilter trước servlet render.
3. Với JWT stateless, chiến lược session mặc định xoay CSRF trên các request authenticated, làm meta token trên trang cũ khác cookie sau background GET. Dùng NullAuthenticatedSessionStrategy riêng cho CSRF, xoay rõ ở login/logout/reset/password; test HTTP thật và Playwright xác nhận POST hoạt động, request thiếu CSRF vẫn 403.
4. Giới hạn khóa User trên đúng dòng users trước join roles để tránh khóa role hàng loạt; bảo vệ Admin cuối vẫn dùng khóa role Admin riêng.

## Phạm vi đã có và phần nghiệm thu còn phụ thuộc

Đủ code cho 16 UC của nhánh: Entity mapping/Repository/DTO, Service transaction/permission, Controller và JSP/JS, test và hồ sơ. H2 fixtures cung cấp schema catalog/order rút gọn trong test, không phải nghiệp vụ catalog/order production.

- Chưa áp migration mới hoặc chạy Hibernate/TLS trên TiDB thật trong phiên này. Main cần review migration timestamp và FK liên module trước khi đưa vào database chung; V1, datasource và TLS không bị sửa.
- SMTP hiện tắt theo cấu hình main; chưa có bằng chứng gửi OTP email thật. Test chỉ mock OtpDelivery trong test sources, production trả lỗi khi mail disabled/lỗi.
- Cloudinary có cấu hình kế thừa; chưa kiểm upload/delete cloud thật. MP4 được kiểm preflight và provider decode; trường hợp provider cleanup lỗi cần retry vận hành, chưa có retry queue bền vững.
- Chưa ghép luồng checkout/COD/VNPAY/refund thật. Order owner phải gọi LoyaltyService trong cùng transaction và ghép snapshot địa chỉ; Catalog owner phải gọi favorite/viewed/publicContent vào trang sản phẩm.
- Socket idle revoke có scheduler và unit test; chưa nghiệm thu kết nối STOMP/broker xuyên suốt với sự kiện đơn hàng thật.
- Ảnh UI là ứng dụng chạy thật với dữ liệu H2 giả lập, gồm các màn hình rỗng khi chưa có đơn mua. Không thay cho khảo sát cửa hàng thực, báo cáo DOCX cuối kỳ hoặc review/PR của nhóm.

Các giới hạn này không được đánh dấu đã nghiệm thu production. Danh mục use case lịch sử trong tài liệu main vẫn là đặc tả; xem phần cập nhật 17/10 trong từng hồ sơ identity để phân biệt code hiện tại và chứng cứ thực.

## Kiểm chứng tiếp ngày 20/10/2026

Kết quả mới: **53 test**, 0 failure/error/skipped, WAR tạo thành công; **23 check trình duyệt** đạt. Đã thêm các ca invalid input, type coercion, phiên/owner, reset grant, media decode, Unicode và snapshot điểm; giữ các test baseline cũ. Test chạy với `forkCount=0` và SerialGC/C1 do máy thiếu native memory, không bỏ test. Demo có seed sản phẩm/đơn mua/ledger và các vai trò, dùng H2 cục bộ; không thay kiểm chứng provider hoặc TiDB. Xem [ma trận validation và demo](VALIDATION_REVIEW_20261020.md) và [JSON evidence](VALIDATION_AUDIT_20261020.json).


## Tích hợp thực tế 20/10/2026 — IDN-AI-003

Đã merge catalog-sales/order-payment vào identity, thống nhất UI/quyền/ledger và chạy WAR với TiDB thật. Các giới hạn H2/demo/integration của các phần lịch sử phía trên được thay bằng trạng thái hiện tại tại [INTEGRATION_20261020](INTEGRATION_20261020.md). Không push.


## Cập nhật IDN-AI-005 — UI và tài khoản

Đã nâng cấp layout/ảnh thương hiệu, trang chủ, menu, sản phẩm, cửa hàng, auth/account và các màn hình mua hàng/quản lý; mật khẩu 8 tài khoản khởi tạo được đổi sang dạng dễ nhớ, BCrypt và revoke session cũ. Xem [báo cáo thiết kế/kiểm chứng](UI_REDESIGN_20261020.md), [audit](UI_REDESIGN_AUDIT_20261020.json). Mật khẩu hiện hành ở .local/TAI_KHOAN_UTETRA.md, không ghi vào Git.


## IDN-AI-006 — Tài khoản và Gmail OTP

Đã thay thế liên kết kích hoạt riêng bằng luồng tự chuyển sau đăng ký; quên/đặt lại mật khẩu bắt buộc OTP. Đồng bộ UI 5 trang. Gmail người gửi luxiphot123456@gmail.com đã bật và kiểm tra gửi thật; cấu hình bí mật chỉ ở .local/mail.properties, Git bỏ qua. Xem [báo cáo và bằng chứng kiểm tra](AUTH_FLOW_20261020.md). Thông tin này thay thế trạng thái SMTP chưa cấu hình trong các báo cáo cũ.


## IDN-AI-007 — OTP và email

Xem [điều tra, thay đổi và kiểm chứng](OTP_MAIL_20261020.md). Đã thêm email HTML + plain UTF-8, giữ TTL 5 phút; xử lý dán OTP và phân biệt thông báo hết hạn/đã dùng sau khi mã khớp. 39 tests đạt. Tái hiện trên TiDB: mã đúng được chấp nhận sau 10 giây; chưa tái hiện lỗi cụ thể từ người dùng.
