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
