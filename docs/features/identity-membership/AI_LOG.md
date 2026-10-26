# IDN — Nhật ký AI của nhánh feature/identity-membership

Trạng thái hiện tại: đã có prompt triển khai `IDN-AI-001` ngày 17/10/2026. Các ghi chú “chưa có” bên dưới là lịch sử trước phiên này. Prompt nguyên văn nằm tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Cập nhật 20/10/2026: prompt `IDN-AI-002`, rà validation và chạy demo. Kết quả đầy đủ ở [VALIDATION_REVIEW_20261020](VALIDATION_REVIEW_20261020.md); 53 test và 23 check trình duyệt đạt, demo H2 chạy cục bộ. [Phản hồi](ai-responses/IDN-AI-002.md). Chưa commit/push.

Ghi mọi prompt thực dùng cho 16 UC P0 của identity-membership theo IMPLEMENTATION hiện tại: code/JWT/CSRF/ledger, sửa lỗi, sơ đồ, code/UI, setup, kiểm tra quyền và review. Dùng [mẫu prompt](../../templates/PROMPT_ENTRY.md), ID IDN-AI-001... Lưu response, file/hình và commit; che secret/token; nêu nhóm sửa và kiểm chứng. Không ghi prompt mẫu như đã gửi.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |
| IDN-AI-001 | 17/10/2026; chủ workspace/Codex | Đủ 16 UC identity | [Prompt](../../AI_PROMPT_HISTORY.md), [kết quả](ai-responses/IDN-AI-001.md) | Module identity, 16 bộ diagram/code và ảnh UI | verify 42 test; Playwright 16 check; nhóm chưa review thủ công | Chưa commit/push code; merge nền 8a18438 |

## Kết quả phiên 17/10/2026

Source/test và sơ đồ/UI thực đã được bổ sung; xem [VERIFICATION](VERIFICATION.md), [RUNBOOK](RUNBOOK.md), [CONTRACT_REVIEW](CONTRACT_REVIEW.md). Lỗi UTC và CSRF được phát hiện bằng test rồi sửa, không bỏ test. Test dùng H2 và provider mock trong test; production giữ adapter SMTP/Cloudinary thật và lỗi rõ khi chưa sẵn sàng. Không sửa cấu hình DB hoặc tài liệu của thành viên khác. Tên/reviewer/PR chưa được nhóm xác nhận; không tự điền.

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.


## Tích hợp thực tế 20/10/2026 — IDN-AI-003

Đã merge catalog-sales/order-payment vào identity, thống nhất UI/quyền/ledger và chạy WAR với TiDB thật. Các giới hạn H2/demo/integration của các phần lịch sử phía trên được thay bằng trạng thái hiện tại tại [INTEGRATION_20261020](INTEGRATION_20261020.md). Không push.


## IDN-AI-004 — Khởi tạo tài khoản 20/10/2026

Tạo 8 tài khoản tổng hợp trên TiDB theo yêu cầu người dùng: Admin/Manager/Vendor và 5 User. BCrypt riêng, kích hoạt thủ công có audit, credential trong .local Git ignore. Xem [chi tiết](ACCOUNTS_20261020.md) và báo cáo kiểm chứng tài khoản; không thêm seed vào app/migration.


## Cập nhật IDN-AI-005 — UI và tài khoản

Đã nâng cấp layout/ảnh thương hiệu, trang chủ, menu, sản phẩm, cửa hàng, auth/account và các màn hình mua hàng/quản lý; mật khẩu 8 tài khoản khởi tạo được đổi sang dạng dễ nhớ, BCrypt và revoke session cũ. Xem [báo cáo thiết kế/kiểm chứng](UI_REDESIGN_20261020.md), [audit](UI_REDESIGN_AUDIT_20261020.json). Mật khẩu hiện hành ở .local/TAI_KHOAN_UTETRA.md, không ghi vào Git.


## IDN-AI-006 — Tài khoản và Gmail OTP

Đã thay thế liên kết kích hoạt riêng bằng luồng tự chuyển sau đăng ký; quên/đặt lại mật khẩu bắt buộc OTP. Đồng bộ UI 5 trang. Gmail người gửi luxiphot123456@gmail.com đã bật và kiểm tra gửi thật; cấu hình bí mật chỉ ở .local/mail.properties, Git bỏ qua. Xem [báo cáo và bằng chứng kiểm tra](AUTH_FLOW_20261020.md). Thông tin này thay thế trạng thái SMTP chưa cấu hình trong các báo cáo cũ.


## IDN-AI-007 — OTP và email

Xem [điều tra, thay đổi và kiểm chứng](OTP_MAIL_20261020.md). Đã thêm email HTML + plain UTF-8, giữ TTL 5 phút; xử lý dán OTP và phân biệt thông báo hết hạn/đã dùng sau khi mã khớp. 39 tests đạt. Tái hiện trên TiDB: mã đúng được chấp nhận sau 10 giây; chưa tái hiện lỗi cụ thể từ người dùng.

## IDN-AI-008 — Bàn giao identify và cấu hình mail trên máy mới

Theo prompt IDN-AI-008 trong AI_PROMPT_HISTORY, tạo nhánh `identify` từ bản
identity đã tích hợp catalog/order, bàn giao toàn bộ thay đổi đang có.
Giữ `MailOtpDelivery`, HTML/plain template và SMTP Gmail STARTTLS. Bổ sung
`config/mail.properties.example`, [MAIL_SETUP](MAIL_SETUP.md), liên kết runbook
và tùy chọn `--email` cho `tools/configure_mail.py` để người nhận nhập App Password
một lần trên máy họ. Mật khẩu SMTP không được commit; `.local/` tiếp tục bị bỏ qua.

Kiểm chứng: 114 tests đạt, 0 failure/error/skipped, bằng `python tools/run_verify.py`
(JDK 21, Mockito premain agent, giới hạn bộ nhớ; log cục bộ tmp-integration-verify.log).
Lần Maven Wrapper mặc định trước đó lỗi Mockito self-attachment/fork trên máy này.
Script cấu hình mail được chạy thử với thư mục tạm và input giả, kiểm tra đúng
sender/password; không đổi cấu hình thật, không gửi mail thật trong phiên bàn giao.
Quét 539 file dự kiến bàn giao không thấy App Password SMTP cục bộ.
`git diff --cached --check` đạt. Tham chiếu commit bàn giao bằng
`git log -1 origin/identify` sau khi fetch; không dùng báo cáo cũ để khẳng định
đã thử SMTP trên máy của người nhận.

Đóng gói WAR cũng đạt BUILD SUCCESS với heap 512 MB, `-DskipTests package`
sau khi 114 test đã đạt. Dùng POM tạm chỉ đổi output sang `tmp/handoff-build`
để tránh WAR tại target đang bị khóa; POM tạm đã xóa, pom.xml không đổi.
Log đóng gói: `tmp/handoff-package.log`. Lần verify heap 192 MB trước đó
đạt toàn bộ test nhưng thiếu heap khi tạo WAR; lần package ở target với 512 MB
bị lỗi rename file. Không ghi các lần này là verify thành công toàn bộ.

## 23/10/2026 — Thanh: đồng bộ và rà soát

Yêu cầu: đọc toàn bộ Markdown, lấy code GitHub mới nhất, xử lý conflict, rà validation khắt khe cho identity-membership, hướng dẫn chia sẻ mail local và chỉnh email chuyên nghiệp.

Đã đọc corpus Markdown trước đồng bộ và tài liệu bổ sung sau đồng bộ. Fetch tất cả remote; nhánh identify cập nhật fast-forward từ 78facd2 tới dac7ad0 (origin/integration/utetra), không có conflict. Nhánh integration chứa cập nhật các module và Shared UI. Gán owner Thanh trong hướng dẫn identity; chỉnh lời email đăng ký/khôi phục và test tương ứng; bổ sung cách cài mail trên máy thành viên. Không chuyển credential local vào Git. Shared UI checker đạt. Kết quả Maven xem REVIEW_20261023.md. Không commit/push trong phiên này.

## 23/10/2026 — Bố cục hồ sơ và xem trước avatar

Thanh phản hồi bố cục lệch, cần thấy ảnh trước khi đổi và thông tin đầy đủ hơn. Cân hai cột desktop, mobile xếp dọc bằng CSS scope riêng identity-profile.css, không sửa theme/layout chung. Thêm ảnh hiện tại/chữ cái thay thế và preview ảnh local trước upload; từ chối MIME/dung lượng sai phía client, giữ validation server. Thông tin tóm tắt có username/email/phone/email verified/ngày tham gia từ API profile; không thêm dữ liệu giả. Shared UI checker đạt; Maven verify Java 21 đạt (bao gồm render JSP/Tomcat). Chưa kiểm thủ công ảnh preview trên trình duyệt. Server MySQL được khởi động lại sau build.

## 23/10/2026 — Sửa lại hồ sơ theo phản hồi ảnh thứ hai

Thanh yêu cầu avatar ở đầu trang và thông tin dễ đọc, tham khảo website chuyên nghiệp. Tham khảo hướng dẫn tài khoản Google/Microsoft (https://support.google.com/accounts/answer/27442 và https://support.microsoft.com/en-gb/accounts-billing/manage/change-your-microsoft-account-picture). Đưa avatar hiện tại/preview vào banner đầu trang, cạnh tên/email và thao tác upload. Hai cột dưới chứa form và thông tin tài khoản. Nguyên nhân chữ khó đọc: .profile-summary của theme ép nền xanh !important; đổi sang class riêng account-facts với nền trắng, chữ tương phản. Thêm phiên bản URL CSS để tránh cache. Shared UI checker và IdentityViewTest với Tomcat thật đạt; verify tạo WAR thành công. Chưa có kiểm screenshot thủ công sau sửa. Không thay layout chung.

## 23/10/2026 — Nhấn mạnh thông tin tài khoản

Thanh yêu cầu highlight khối thông tin tài khoản. Chỉnh CSS scope riêng: nền xanh trà nhạt, viền trên xanh đậm, tiêu đề lớn và giá trị đậm; giữ chữ tối dễ đọc. Không đổi JSP/layout hoặc logic. Shared UI checker đạt; package thành công (skip tests do chỉ đổi CSS). Server được khởi động lại. Chưa kiểm hình ảnh thủ công.

## 23/10/2026 — Nền xanh template và lưu avatar cùng hồ sơ

Thanh đính chính dùng nền xanh trà đậm như template ban đầu. Khôi phục màu thẻ xanh bằng class account-facts riêng, chữ sáng và đường phân cách tương phản. Thanh yêu cầu chọn ảnh nằm trong Hồ sơ của bạn, chỉ lưu ảnh khi bấm Lưu hồ sơ: chuyển file input vào profile form, giữ preview ở banner, bỏ nút upload riêng. Thêm endpoint multipart /account/profile/save gọi profileWithAvatar trong một transaction; giữ API JSON cũ. Nếu validation/upload thất bại thì rollback thông tin, giữ cơ chế cleanup tài nguyên. Không chọn ảnh vẫn lưu được thông tin. Shared UI checker đạt; IdentityViewTest và IdentityIntegrationTest đạt; test mới combinedProfileSaveRollsBackDetailsWhenAvatarIsInvalid chạy riêng đạt, kiểm rollback version/tên khi ảnh sai và lưu không có ảnh. WAR build thành công. Chưa kiểm upload provider thật bằng thao tác tay.

## 23/10/2026 — Mật khẩu và bảo mật

Thanh yêu cầu nhấn mạnh giá trị data và tổ chức đổi mật khẩu tương tự mục bảo mật Facebook. Tăng độ đậm/cỡ chữ giá trị sáng trên nền xanh, tiêu đề màu kem. Gom bảo mật thành khu vực riêng với mục đổi mật khẩu mở rộng, nhập lại mật khẩu, checkbox hiện/ẩn và thông báo revoke phiên hiện có. Bỏ nút gia hạn thủ công khỏi hồ sơ, giữ đăng xuất. Confirmation kiểm client và không gửi vào JSON; server vẫn kiểm password/quyền/CSRF và thu hồi phiên như trước. Không thêm chức năng 2FA/thiết bị giả. Shared UI checker đạt; IdentityViewTest (Tomcat) và verify đạt. Server khởi động lại. Chưa kiểm thao tác trình duyệt thủ công.

## 23/10/2026 — Trang security riêng và hỗ trợ quên mật khẩu

Thanh yêu cầu bấm Mật khẩu và bảo mật chuyển trang riêng, hỗ trợ khi quên mật khẩu hiện tại. Thêm /account/security dùng layout SiteMesh chung, cho mọi tài khoản đã xác thực; profile và navigation liên kết sang trang này. Chuyển form đổi mật khẩu sang security, có liên kết /auth/forgot dùng OTP email và reset grant sẵn có. Security không gọi API /data không tồn tại. Shared UI checker đạt; BaselineSecurityTest và IdentityViewTest đạt. Bổ sung route security và nội dung forgot/confirm vào test Tomcat; chạy lại 2 IdentityViewTest đạt, WAR build thành công. Server khởi động lại. Chưa gửi OTP SMTP thật trong phiên này.

## 23/10/2026 — Đưa đăng xuất về profile

Theo yêu cầu Thanh, chuyển khối Phiên đăng nhập/nút Đăng xuất từ security về profile, giữ handler đăng xuất hiện có. Shared UI checker và IdentityViewTest với Tomcat đạt; verify build thành công, server mở lại.

## 23/10/2026 — Thông báo lỗi đăng nhập

Thanh gửi ảnh Failed to fetch khi nhập sai password. Đây là lỗi transport không nhận response, không coi mặc định là sai mật khẩu. Request helper bắt lỗi fetch và hiển thị thông báo kết nối tiếng Việt. Riêng /auth/login trả 401 hiện Email hoặc mật khẩu không đúng, gợi ý Quên mật khẩu; không phân biệt email tồn tại. Shared UI checker và IdentityIntegrationTest đạt, verify tạo WAR thành công. Server khởi động lại. Chưa kiểm transport failure bằng trình duyệt.

## 26/10/2026 — Merge source mới nhất vào identify, chưa push

Thanh yêu cầu merge code mới nhất vào identify và sau đó dặn chưa push, kiểm thử trước. Workspace sạch tại 4d0dc73. Fetch toàn bộ remote; merge origin/integration/utetra tới 5e5708f rồi origin/main tới 3ceff77 bằng fast-forward, không conflict. Tất cả đầu remote hiện tại là ancestor của HEAD. Không thay cấu hình local mail hoặc database để merge.

Kiểm chứng local: Shared UI checker đạt; git diff --check đạt; Maven verify Java 21 đạt 161 tests, 0 failures/errors/skipped và tạo WAR thành công (bao gồm test HTTP/Tomcat/JSP, identity/catalog/order và migration H2 isolated). Dùng giới hạn bộ nhớ và Mockito premain như phiên trước. Chưa smoke-test database/provider thật trong phiên này. Chưa push GitHub theo yêu cầu Thanh; identify local hiện chứa 5 commit mới so với origin/identify. Không khởi động hoặc áp migration database thật chỉ để nghiệm thu merge.

## 26/10/2026 — Bộ dữ liệu và giao diện mẫu

Thanh yêu cầu logo lá trên ảnh ly; đầy đủ data tài khoản Việt/mật khẩu dễ nhập, món tham khảo thương hiệu lớn, mỗi món có ảnh logo UTETra, lượt mua/rating/comment theo đơn hợp lệ. Sau đó chốt 30 món, cho phép tên Việt giả lập, và yêu cầu UI theo ảnh tham chiếu mới nhưng giữ các chức năng cũ.

Dùng built-in imagegen tạo 30 ảnh món và sửa thumbnail ba ly; tất cả asset/prompt lưu trong repo. Seed thêm 12 tài khoản demo, 3 chi nhánh, 6 danh mục, 90 sản phẩm theo shop, 270 size, topping/phí giao, 106 đơn (99 COD delivered, 7 trạng thái/thanh toán), 90 review/comment, 2 voucher. Tài khoản cũ không đổi. Các đơn qua service checkout/chuyển trạng thái; cộng điểm/stock/snapshot theo ứng dụng. Không phát SMTP hoặc gọi thanh toán thật. Seed có flag apply, guard database, không xóa dữ liệu/tắt ràng buộc. Dữ liệu/tài khoản ghi rõ demo trong docs/demo/README.md.

UI vẫn một layout chung. Thêm ảnh danh mục vào DTO công khai, thống kê soldUnits/commentCount từ bảng đơn/review thật cho trang chi tiết. Sửa fixture test order_items có quantity=0 mặc định thành quantity=2 để kiểm lượt bán có ý nghĩa. Đối chiếu database bằng VerifyDemo: 12 kiểm tra không có vi phạm. Kiểm browser phát hiện overflow thanh category ở 390px, đã sửa trước lượt kiểm lại. Kết quả cuối ghi ở docs/demo/README.md và ui/verification.json. Không commit/push trong phiên này.

Kết quả cuối của đợt UI/data: 161 test đạt, build WAR thành công; 18 lượt Edge desktop/tablet/mobile đạt với User/Admin/Manager/Vendor, không JS exception; 12 đối chiếu SQL không có vi phạm. Source ảnh/prompt/seed/test report ở docs/demo. Server mở lại bản cuối, chưa commit/push.

## 26/10/2026 — Thanh đổi luồng chọn chi nhánh

Yêu cầu: chỉ chọn chi nhánh khi lên đơn/thanh toán; chọn nước không hiển thị chi nhánh; giao diện theo ảnh tham chiếu có popup tùy chọn. Đã thêm giỏ draft không ràng buộc branch trong UI, bước chọn branch ở checkout đối chiếu toàn giỏ transaction/version/stock rồi lấy báo giá. Dùng catalog_key explicit cùng owner/danh mục để gộp 90 bản ghi demo thành 30 món; giữ dữ liệu và đơn cũ. Có form quản trị mã món, popup nút size/đường/đá/topping và giỏ tạm tính thực. Chi tiết migration/API/test ở docs/features/order-payment/BRANCH_SELECTION_20261026.md. 167 tests toàn bộ đạt, thêm smoke JSP cuối đạt 6; kết quả browser lưu docs/demo. Không commit/push.
