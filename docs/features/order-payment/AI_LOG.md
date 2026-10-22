# ORD — Nhật ký AI của nhánh feature/order-payment

## ORD-AI-011 — Đồng bộ catalog/identity — 21/10/2026

Prompt nguyên văn: `hoàn chỉnh phần code bên nhánh order của tôi và xem code của cách nhánh chức năng khác rồi chình cho đồng bộ`.

Đọc catalog 812e26e và identify 78facd2; lưu sửa media thành 5882d52 rồi merge identify bằng 28e8c18. Đồng bộ ledger, khóa user, scope, danh mục tắt, JSON và validation số. Sửa helper Java để dùng JDK 21/Maven Wrapper trên máy thành viên. Thêm 5 test HTTP contract; clean verify 130 tests đạt, WAR mysql validate 6 migration không chạy mới và 7 URL trả 200. Phát hiện class policy dư trong WAR cũ, xử lý bằng clean build. Đã dừng process kiểm chứng. [Kết quả/giới hạn/lệnh](INTEGRATION_20261021.md); không ghi secret và không push sang nhánh thành viên khác.

## ORD-AI-010 — Rà bổ sung trên máy Windows

Prompt nguyên văn: `xem còn thiếu sót gì thì làm đi`.

Đã rà code giỏ/checkout/COD, quyền operator/shipper, trả hàng và media; sửa kiểm tra nội dung ảnh JPEG/PNG khớp MIME, ảnh hỏng/vượt giới hạn, phản hồi validation tiếng Việt và kiểm kết quả xóa Cloudinary. UI kiểm lý do/định dạng/dung lượng trước gửi; server vẫn kiểm độc lập. Thêm regression tests cho file giả MIME, hỏng, quá kích thước/dung lượng, provider delete lỗi và cleanup không che lỗi gốc. Maven Wrapper/JDK 21: verify đạt 62 tests, WAR thành công; upload/xóa PNG thật đạt. Chưa chạy lại full browser/native DB; chưa push các sửa code này. Chi tiết ở VERIFICATION.md; không đưa secret vào nhật ký.


Trạng thái khởi tạo hồ sơ (trước triển khai): chưa có prompt triển khai thật từ thành viên. Yêu cầu khởi tạo hồ sơ đã ghi tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi mọi prompt thực dùng cho 21 UC P0 và 3 UC Shipper P1 của order-payment theo IMPLEMENTATION hiện tại: giỏ/checkout/COD/transaction/socket/report, sửa lỗi, sơ đồ, ảnh diễn giải code/UI, setup và review. Dùng [mẫu prompt](../../templates/PROMPT_ENTRY.md), ID ORD-AI-001... Lưu response/code/nguồn hình/ảnh, cách nhóm sửa, testcase và commit. Không ghi QR gateway đã tích hợp hoặc test đã qua nếu chưa thực hiện.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.

## ORD-AI-001 đến ORD-AI-008 — triển khai và yêu cầu push ngày 14/10/2026

Prompt nguyên văn tập trung tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md#ord-ai-001). Người dùng yêu cầu làm đầy đủ order-payment trên chính nhánh feature, hoãn thanh toán ngân hàng, thay cửa sổ trả từ 48 giờ sang **3 giờ**, hoàn **cả đơn**, rồi yêu cầu push đúng nhánh. Không tự gán tên thành viên/reviewer hoặc giờ gửi prompt.

Phản hồi thực hiện: bổ sung Entity/JPA/DTO/Repository → Service → Controller → JSP/JS và test cho 20 UC bắt buộc (VNPAY hoãn) + 3 Shipper có flag. Code ở src/main/java/com/utetra/order; migration V2 tạo 19 bảng own; contract/schema module khác ghi ở SCHEMA_PROPOSAL, không tự migrate bảng của nhánh khác. OrderLoyaltyBridge là adapter cần review owner identity, không phải xác nhận identity module đã triển khai.

| Phần | Công việc thật | Kiểm chứng |
| --- | --- | --- |
| Giỏ/checkout | DB + session, merge/version/UUID, giá và snapshot server, quota/điểm, atomic rollback | Cart/checkout/merge/quote/concurrency tests |
| COD/trả–hoàn | State machine, thu tiền đúng quyền, return < 3h, review/receive/waive, evidence COD, full refund, reversal/debt | Workflow và cutoff/refund/point debt tests |
| Vận hành | Carrier/rate scope, hoa hồng mới giữ snapshot, doanh thu ngày thu/hoàn, CSV, assignment/failure/notification owner | Scoped operations, commission/report/shipper tests |
| Realtime/media | Durable SQL notification, after commit STOMP, revoked-session guard, reconnect/polling; upload rollback cleanup | Socket guard/event-delivery/media adapter tests; WebSocket trình duyệt thật, chưa test Cloudinary thật |
| Giao diện | JSP chung Bootstrap, cart/quote/order/refund, tools/report/notifications và Shipper | Playwright thao tác thật; desktop/mobile/tablet; ảnh test trong assets |

### Lỗi đã phát hiện và sửa qua kiểm chứng

- Entity generator đọc nhầm header tiếng Việt thành cột: sửa parser và mapping trước test; native JPA validate xác nhận mapping/schema.
- Persist entity mới trước khi điền trường bắt buộc; stale resource V2 cũ trong target; normalize BigDecimal để quote hash không đổi vì scale; sửa và clean build.
- Cart version không tăng với đồng hồ cố định; guest merge key bị mất sau sửa giỏ; đổi timestamp microsecond và giữ merge key.
- Cart/checkout khóa khác thứ tự có nguy cơ deadlock: thống nhất store trước catalog, user serialize mutation giỏ/checkout.
- JSP EL diễn giải template literal JS; thay nối chuỗi và kiểm JSP chạy thật.
- CSRF cookie tạo quá muộn sau SiteMesh; materialize trước decorator. JWT bị SessionManagementFilter coi là login mới ở mỗi asset request, liên tục xoay cookie: dùng NullSecurityContextRepository và bỏ filter quản lý login tự động; giữ CSRF/XOR và DisableEncodeUrlFilter. Thêm test GET → tải assets → POST hợp lệ và header giả 403.
- MySQL REPEATABLE READ đọc snapshot cũ sau chờ user lock, tạo duplicate checkout trong test native dù H2 pass: mutation dùng READ_COMMITTED; quota program có current locking read. Native concurrency/idempotency/quota chạy lại.
- Mã đơn dài tràn màn hình mobile: text-break; kiểm 390px và 768px.
- Return bị từ chối vẫn có thể gửi lại dù unique một request/đơn; assignment còn ACTIVE sau hủy; sửa ràng buộc và regression test.
- Broker lỗi sau commit không được báo purchase thất bại; giữ thông báo SQL/polling và test broker outage/rollback không phát event.

[VERIFICATION](VERIFICATION.md) ghi lệnh, kết quả cuối và giới hạn. Build không đóng gói test helper/token vào WAR. Không sửa main hoặc feature của thành viên khác; commit code `29aac50`; commit tài liệu và push feature/order-payment theo ORD-AI-008, xác nhận SHA remote sau push. Review người thật, ghép integration, DOCX/ảnh code có callout và provider thật còn cần nhóm thực hiện.

Commit code thực: [29aac50](https://github.com/Phucsbinz/Project_Web/commit/29aac509163236b88f44834ad45c4036cca5a89f). Minh chứng test tóm tắt: [test-results.txt](assets/test-results.txt).
