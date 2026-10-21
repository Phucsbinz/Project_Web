# CAT — Nhật ký AI của nhánh feature/catalog-sales

Trạng thái: đã có prompt triển khai thực CAT-AI-001 ngày 15/10/2026; các ghi chú chưa có bên dưới thuộc lịch sử trước phiên này. Các prompt yêu cầu khởi tạo hồ sơ được lưu tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi tại đây mọi prompt thành viên gửi khi làm 18 UC P0 của catalog-sales, bao gồm code, sửa lỗi, hình kiến trúc/activity, diễn giải code/UI, setup và review. Sao chép [mẫu prompt](../../templates/PROMPT_ENTRY.md), dùng CAT-AI-001... Lưu response dài trong ai-responses; liên kết code/ảnh/commit và ghi nhóm đã chỉnh/kiểm thử gì. Không chuyển prompt gợi ý thành “đã dùng” khi chưa gửi.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu, giữ lịch sử cả kết quả AI sai và lần sửa.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.

## CAT-AI-001 — 15/10/2026 — triển khai catalog-sales

Người yêu cầu: chủ workspace; công cụ: Codex. Nhánh: `feature/catalog-sales`. Phạm vi: 18 UC theo BRANCH_CATALOG_SALES.md.

Prompt nguyên văn:

> thực hiện tạo danh mục , shop , sản phẩm , khuyến mãi , các yêu cầu trong file md và yêu cầu của nhánh này

Kết quả: Entity/Repository/DTO → service kiểm quyền/validation/transaction → Controller/JSP/JS → test, DDL proposal và hồ sơ. Chi tiết response/bàn giao tại [RUNBOOK](RUNBOOK.md), không tạo số liệu hay credentials provider giả.

Kiểm chứng: Maven verify/JDK21 đạt 35 test tại lượt xác minh 15/10/2026; có kiểm JSP trên Tomcat thật. Lần test đầu phát hiện fixture H2 DDL tự commit gây trùng user; đã chuyển DDL sang BeforeAll rồi chạy lại đạt. Lệnh wrapper trên máy lỗi null array; dùng Maven 3.9.16 cài sẵn, giữ pom/config. Sau đó bổ sung SQL search ở DB, kiểm giờ shop, schema smoke và media cleanup; xem RUNBOOK.

Nhóm sửa/reviewer/commit: chưa có xác nhận hoặc commit; không tự ghi danh tính thành viên. TiDB/Cloudinary thật/checkout-quota xuyên nhánh còn chờ owner tương ứng. Mọi assertion đạt chỉ áp dụng những ca test được nêu và môi trường test.

Kiểm chứng cuối CAT-AI-001: 37/37 tests đạt, WAR BUILD SUCCESS (16:10, 15/10/2026). Browser thật tạo danh mục thành công và quote hai ly 60.000đ; 9 ảnh tại assets/ui. Lỗi TIME/JDBC timezone và token CSRF cũ được phát hiện bằng browser, đã sửa và bổ sung hồi quy LocalTime + real HTTP cookie-CSRF. Chi tiết và fixture trong RUNBOOK. Không đánh đồng H2/test JWT với nghiệm thu Identity hoặc cloud/provider thật.
