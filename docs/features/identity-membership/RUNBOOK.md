# Chạy và bàn giao identity-membership

## Bàn giao nhánh identify

Mã bàn giao được push lên `identify`. Sau khi pull, thiết lập Gmail một lần theo
[MAIL_SETUP.md](MAIL_SETUP.md), rồi build/chạy bằng JDK 21. App Password chỉ nằm
trên máy chạy ứng dụng; Git có đầy đủ code gửi OTP và mẫu cấu hình không chứa mật khẩu.

Nhánh: `feature/identity-membership`. Đã merge main/catalog-sales/order-payment theo IDN-AI-003; chưa push. Phần hướng dẫn thiết kế sau đây được giữ để tham khảo; trạng thái schema và runtime hiện hành ở INTEGRATION_20261020. Java 21, Maven Wrapper, Boot/WAR/JSP/SiteMesh/Bootstrap/JPA và cấu hình MySQL từ main được giữ nguyên.

## Build và test

```powershell
# Dùng JDK 21 đã cài trên máy. Trong workspace phiên này:
$env:JAVA_HOME=(Get-ChildItem .tools/jdk21 -Directory | Select-Object -First 1).FullName
$env:Path=$env:JAVA_HOME+'/bin;'+$env:Path
.\mvnw.cmd -B -ntp verify
```

WAR nằm tại `target/utetra.war`. Test profile dùng H2 isolated, không ghi TiDB. Các trình chạy và seed demo đã gỡ; chỉ giữ fixtures isolated cho automated tests, không nằm trong WAR.

## Schema và cấu hình

`V2026101701__identity_membership.sql` tạo 12 bảng identity còn thiếu và các index, unique/FK nội bộ. Giữ nguyên checksum V1. Hibernate có metadata Entity cho cả 12 bảng để validate schema. Main cần review/điều phối version trước khi đưa lên database chung; `INTEGRATION_FOREIGN_KEYS.sql` là đề xuất FK liên module sau khi bảng catalog/order đã tồn tại, không tự chạy file này trước integration.

Datasource, credential, JWT key và OTP HMAC key dùng các properties kế thừa từ main. Không đổi database/TLS hoặc copy credential vào tài liệu. Gửi OTP thật cần `app.mail.enabled=true` và SMTP hợp lệ; cấu hình hiện tại còn tắt mail. Cloudinary dùng cấu hình hiện có, chưa upload nghiệm thu thật trong phiên này. Khi provider thất bại trả lỗi, không trả OTP qua API/log hoặc giả báo upload thành công.

OTP: 6 chữ số CSPRNG; HMAC theo id/purpose/email; TTL 5 phút; 5 lần sai; cooldown 60 giây; 10 lần gửi/email/giờ và limiter IP. Reset grant ngẫu nhiên chỉ trả cho người đã verify OTP reset, lưu hash và dùng một lần trong 5 phút. Email chưa có tài khoản cũng nhận challenge không liên kết User và cùng luồng gửi/giới hạn để phản hồi gửi không tiết lộ tài khoản; challenge đó không thể cấp quyền reset. IP limiter hiện lưu trong bộ nhớ từng process; triển khai nhiều instance cần limiter chia sẻ.

Cookie JWT/refresh là HttpOnly/SameSite=Lax theo secure flag của main. CSRF vẫn bật cho toàn bộ request thay đổi. Token được materialize trước SiteMesh; JWT stateless không làm xoay CSRF ở mỗi request, đăng nhập/đăng xuất/reset/đổi mật khẩu xóa cookie CSRF để trang tiếp theo tạo token mới. Không lưu auth token vào localStorage.

## Các màn hình và route

| Màn hình | Route chính | Quyền |
| --- | --- | --- |
| Đăng ký / OTP / đăng nhập | `/auth/register`, `/auth/otp`, `/auth/login` | Guest |
| Quên / reset mật khẩu | `/auth/forgot`, `/auth/reset` | Guest |
| Refresh / logout | POST `/auth/refresh`, `/auth/logout` | Refresh cookie hoặc phiên hợp lệ + CSRF |
| Hồ sơ / avatar / mật khẩu | `/account/profile`, POST `/account/profile/avatar`, `/account/password` | Mọi tài khoản đăng nhập |
| Địa chỉ | `/account/addresses`; POST/PUT/DELETE theo id | User/Vendor, owner |
| Thích / đã xem | `/favorites`, `/viewed`; POST/DELETE `/favorites/{id}`, POST `/viewed/{id}` | User/Vendor |
| Review / comment | `/reviews`, `/comments`; POST multipart, PUT/DELETE theo id | Đã mua, owner |
| Nội dung public/rating | GET `/products/{id}/community` | Product public; chỉ VISIBLE |
| Điểm / lịch sử / hạng | `/account/loyalty` | User/Vendor, owner |
| Tìm / khóa user | `/manager/users` | Manager theo membership và shop có khách/owner liên quan |
| Role / user / phân công | `/admin/users`; POST `/admin/users/{id}/assignments` | Admin |
| Ẩn nội dung | POST `/admin/reviews/{id}/hide`, `/admin/comments/{id}/hide` | Admin + audit |

Các trang danh sách đọc JSON tại route `/data`, có `page` từ 0 và `size` 1–100. Route OTP/forgot/reset thực ở bảng này thay route dự kiến trong phần thiết kế lịch sử. Form xác minh dùng email+purpose để chọn challenge mới nhất phía server, không nhận userId/challenge owner từ client. JSP/JS dùng textContent và form có label, loading/empty/error; không render nội dung người dùng bằng innerHTML.

## Contract để ghép catalog/order

- `AccountService.addressSnapshot(principal,id)`: trả recipient_name/phone/address_detail thuộc người hiện tại; Order copy vào snapshot bất biến.
- `IdentityStoreAccessPolicy.canOperate(principal,storeId)`: kiểm active/verified/session; Vendor owner, Manager membership, Admin global. Áp dụng luôn cho WebSocket baseline.
- `CommunityService.favorite`, `viewed`, `publicContent`: catalog gọi endpoint theo bảng trên sau khi product tồn tại. GET không ghi lượt xem; catalog JS phải POST `/viewed/{productId}` kèm CSRF khi người dùng đã đăng nhập. Nút thích gọi POST/DELETE `/favorites/{id}`.
- `CommerceIdentityAdapter`: đọc product/store, order item/variant/order theo DATABASE_DESIGN. Không tạo bảng hoặc giả bằng chứng mua hàng trong production. Test fixtures hiện dùng schema JPA hợp nhất; đã gỡ schema rút gọn của identity.
- `LoyaltyService.apply(orderId,type)`: chỉ gọi từ transaction Order (`MANDATORY`), không có HTTP endpoint cho client cộng/trừ điểm. Khóa order → User → account. REDEEM kiểm trần 20%/số dư/nợ; EARN chỉ DELIVERED+PAID; REFUND/REVERSAL theo trạng thái hoàn/hủy; unique order/type chống lặp; điểm kiếm sau trả nợ trước. Order phải gọi REFUND và REVERSAL khi hoàn thành refund cả đơn. Đã nối OrderLoyaltyBridge với ledger chung; VNPAY vẫn tắt.
- `AuditService.record(actor,Action,Target,id)`: cùng transaction, append-only, enum tạo summary cố định không có raw body/secret. Module gọi phải kiểm quyền nghiệp vụ trước.
- Ảnh JPEG/PNG dùng `MediaService` chung; comment/review thêm MP4 qua `CommunityMediaService`. Kiểm MIME/signature/size và Cloudinary phải decode video; tối đa 5 tệp, ảnh 5MB, MP4 20MB. Bù tài sản mới sau rollback, dọn tài sản cũ sau commit. Lỗi cleanup ghi cảnh báo cần retry vận hành; chưa có hàng đợi retry bền vững qua restart.

## Chạy website thật sau tích hợp

Hướng dẫn và trạng thái hiện hành: [INTEGRATION_20261020](INTEGRATION_20261020.md). Các launcher/demo H2 đã gỡ theo IDN-AI-003.

```powershell
python tools/run_verify.py
python tools/start_web.py
python tools/web_browser_check.py
python tools/stop_web.py
```

Mở http://localhost:8080. Website dùng profile mysql và users/password_hash trong utetra_db. Không có seed tài khoản tự động. Sau IDN-AI-004 đã khởi tạo 8 tài khoản thủ công; thông tin truy cập chỉ ở .local/TAI_KHOAN_UTETRA.md, xem ACCOUNTS_20261020.md. SMTP phải được cấu hình để đăng ký/OTP thật; đã kiểm tra đăng nhập 8/8 tài khoản khởi tạo trực tiếp trong DB. Không đổi biến GLOBAL của cụm TiDB để bật CHECK vì cụm còn phục vụ database khác; xem giới hạn đã ghi ở báo cáo tích hợp.


## Cập nhật IDN-AI-005 — UI và tài khoản

Đã nâng cấp layout/ảnh thương hiệu, trang chủ, menu, sản phẩm, cửa hàng, auth/account và các màn hình mua hàng/quản lý; mật khẩu 8 tài khoản khởi tạo được đổi sang dạng dễ nhớ, BCrypt và revoke session cũ. Xem [báo cáo thiết kế/kiểm chứng](UI_REDESIGN_20261020.md), [audit](UI_REDESIGN_AUDIT_20261020.json). Mật khẩu hiện hành ở .local/TAI_KHOAN_UTETRA.md, không ghi vào Git.


## IDN-AI-006 — Tài khoản và Gmail OTP

Đã thay thế liên kết kích hoạt riêng bằng luồng tự chuyển sau đăng ký; quên/đặt lại mật khẩu bắt buộc OTP. Đồng bộ UI 5 trang. Gmail người gửi luxiphot123456@gmail.com đã bật và kiểm tra gửi thật; cấu hình bí mật chỉ ở .local/mail.properties, Git bỏ qua. Xem [báo cáo và bằng chứng kiểm tra](AUTH_FLOW_20261020.md). Thông tin này thay thế trạng thái SMTP chưa cấu hình trong các báo cáo cũ.


## IDN-AI-007 — OTP và email

Xem [điều tra, thay đổi và kiểm chứng](OTP_MAIL_20261020.md). Đã thêm email HTML + plain UTF-8, giữ TTL 5 phút; xử lý dán OTP và phân biệt thông báo hết hạn/đã dùng sau khi mã khớp. 39 tests đạt. Tái hiện trên TiDB: mã đúng được chấp nhận sau 10 giây; chưa tái hiện lỗi cụ thể từ người dùng.
