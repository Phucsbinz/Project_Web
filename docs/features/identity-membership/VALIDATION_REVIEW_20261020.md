# Rà validation và chạy demo — 20/10/2026

Nhánh `feature/identity-membership`; giữ thay đổi của phiên trước, không pull/merge đè workspace đang có source chưa commit. Đã đọc lại corpus Markdown hiện tại, đối chiếu 16 UC với DTO, Service, Controller, JSP/JS và migration. Danh sách file/hash và kết quả kiểm thử đọc máy ở [VALIDATION_AUDIT_20261020.json](VALIDATION_AUDIT_20261020.json).

## Các lỗi đã sửa

| Phạm vi | Validation hiện tại và bản sửa |
| --- | --- |
| Đăng ký / login | Username 3–50 ký tự theo pattern; email hợp lệ, không nhận display name/header injection; mật khẩu mới ít nhất 8 Unicode codepoint, không trống/control character, tối đa 72 byte UTF-8. Service cũng kiểm DTO, không chỉ dựa Controller. |
| OTP / reset | Email/OTP/purpose có giới hạn; resend thu hồi cả reset grant cũ. Reset bằng challenge anonymous/không gắn User trả 401 thay vì NPE/500. Grant vẫn kiểm mục đích, hash, hạn, consumed/revoked và user active/verified. |
| Profile / địa chỉ | Full name, người nhận, địa chỉ phải có nội dung sau trim và đúng độ dài. Phone cần 8–15 chữ số, tối đa 20 ký tự, chỉ nhận dấu + ở đầu và dấu phân cách cho phép; profile được bỏ trống phone. Chuỗi chỉ có dấu ngoặc/gạch không được lưu. |
| Version / ID / boolean | Profile, quản trị user, sửa nội dung bắt buộc version không âm. JSON số thập phân/chuỗi/overflow không được ép thành version/ID; cờ phân công Manager bắt buộc gửi boolean thật. ID âm/0, kiểu URL sai và page/size ngoài giới hạn trả lỗi 400 JSON. |
| Review / comment | Purchase evidence phải đúng author/product/order item và DELIVERED+PAID. Sao 1–5; mỗi order item một review. Comment 50–2000 codepoint sau trim, review tối đa 2000; DTO và Service thống nhất Unicode. Owner edit/delete và HIDDEN/version được kiểm; text được render bằng textContent. |
| Ảnh / video | Kiểm bytes trước provider, kể cả provider mock: JPEG/PNG phải decode được, MIME khớp nội dung, tối đa 5MB/6000×6000. MP4 tối đa 20MB, kiểm ftyp/brand/box size rồi yêu cầu provider trả duration và video codec; không chỉ nhận magic bytes. Tệp named-empty, hơn 5 tệp và quá dung lượng bị từ chối; empty browser placeholder không tính là attachment. |
| Điểm thành viên | Reject type null/sai; snapshot tiền phải nguyên VND, không âm, discount và points_used×1000 nhất quán. REVERSAL chỉ sau payment REFUNDED; hủy chưa trả tiền chỉ REFUND điểm redeem. Giữ transaction/locking/idempotency, balance/debt và lifetime earned. |
| Manager / Admin | Giữ scope, role allowlist, last active Admin, audit và revoke. API assignment/edit được @Valid; request thiếu flag/version không tự mặc định làm thay đổi dữ liệu. |
| Client | Kiểm password byte/phone/codepoint/files trước POST; sửa input invalid thì bỏ custom validity để gửi lại được. Pattern HTML đã escape theo browser hiện tại. |
| Database | Thêm migration `V2026102001__identity_validation_guards.sql`: purpose/attempt/expiry OTP, membership role, address required, ledger sign/balance/type, status, view count và media type. Không sửa checksum V1 hoặc migration 17/10; chờ main review trước database chung. |

## Kết quả thực

- `verify`: **53 test**, 0 failure/error/skipped; WAR tạo thành công. Test mới gồm ma trận nhiều input sai trong mỗi test, không chỉ 53 request.
- Trình duyệt Edge: **23 check đạt**, 0 JavaScript exception. Kiểm từ form đến API: password byte, phone invalid rồi sửa đúng, version thiếu/decimal, ID âm, comment 49/50/Unicode, review trùng/sai sao, XSS escaped, ảnh giả, owner/Vendor/Admin và logout.
- Bằng chứng: [browser verification](assets/validation-20261020/verification.json), ảnh trong cùng thư mục. Test sources: `IdentityIntegrationTest`, `IdentityMigrationTest` và các test baseline/JSP/CSRF cũ.
- Máy thiếu native memory khi fork JVM, nên dùng một JVM, SerialGC/C1 và heap giới hạn. Không hạ Java, đổi pom, bỏ test hoặc đổi datasource để chữa môi trường.

```powershell
$env:MAVEN_OPTS='-Xms32m -Xmx384m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m'
.\mvnw.cmd -B -ntp '-DforkCount=0' verify
```

## Demo để thử trực tiếp

URL: <http://127.0.0.1:8087/auth/login>. Demo chạy nền, chỉ bind loopback và dùng H2 in-memory; có banner dữ liệu thử nghiệm. Fixture chặn profile/datasource ngoài H2 trước khi khởi tạo DB. Có sản phẩm, đơn đã mua, favorite/view/review/comment và ledger điểm; dữ liệu được đặt lại khi restart.

| Vai trò | Email demo |
| --- | --- |
| User | `browser@example.test` |
| Vendor | `vendor.browser@example.test` |
| Manager | `manager.browser@example.test` |
| Admin | `admin.browser@example.test` |

Mật khẩu **chỉ cho dữ liệu demo**: `BrowserTest42!`. Không phải credential database hoặc provider. Thử sửa hồ sơ, thêm địa chỉ/default, review/comment sản phẩm đã mua, điểm/lịch sử, rồi quản trị user theo vai trò.

```powershell
python tools/start_identity_demo.py
python tools/stop_identity_demo.py
```

Launcher dùng JDK21/Maven hiện có, chạy nền với heap/luồng thấp; thông tin tiến trình ở `.local/identity-demo-process.json`. Stop script kiểm main class bằng jcmd trước khi dừng đúng PID demo.

## Giới hạn nghiệm thu

SMTP vẫn tắt và Cloudinary chưa nghiệm thu thật; demo không giả gửi OTP/upload thành công. Test provider dùng mock riêng trong test. H2/MySQL mode không chứng minh TiDB/TLS/CHECK trên database thật; migration mới và FK liên module cần main review. Chưa ghép checkout/payment/refund của các nhánh khác. Code chưa commit/push trong phiên này.
