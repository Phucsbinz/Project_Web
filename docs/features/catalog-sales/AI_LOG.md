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

## CAT-AI-002 — 24/10/2026 — kiểm tra tính đầy đủ

Prompt nguyên văn: “kiểm tra xem đầy đủ tính năng theo yêu cầu chưa”.

Nhánh `feature/catalog-sales`, commit `fa3efab`. Đã đối chiếu 18 UC với code và chạy lại Maven verify bằng JDK21: 37/37 tests đạt, WAR BUILD SUCCESS; kiểm shared UI đạt. Chưa chạy cloud/provider thật. Có code cho đủ UC nhưng còn migration triển khai, phụ thuộc Identity/Order, lỗi chọn mục quản trị theo URL và hồ sơ chưa hoàn chỉnh. Xem [báo cáo kiểm tra](AUDIT_20261024.md). Không sửa code nghiệp vụ, không commit/push.

## CAT-AI-003 — 24/10/2026 — bổ sung trong phạm vi catalog

Prompt nguyên văn: “update các tính năng còn thiếu của nhánh này”.

Ràng buộc bổ sung nguyên văn: “chỉ thực hiện nhiệm vụ nhánh này không làm phần nhiệm vụ của nhánh khác như order và indentity”.

Đã hoàn tác bước fast-forward baseline tích hợp vì rộng hơn phạm vi được người dùng xác nhận. Nền catalog vẫn `fa3efab`; giữ các thay đổi audit trước đó. Chỉ bổ sung hồ sơ shop, đọc policy có sẵn cho reviewer, điều hướng quản trị có giữ shop, phân trang thực đơn shop và giữ bộ lọc top. Không sửa code/schema của Order/Identity, không đưa dependency vào nhánh. DDL catalog được tách thành artifact bàn giao, không tự chạy cloud. Kết quả và giới hạn tại [UPDATE_20261024](UPDATE_20261024.md). Chưa commit/push.

Kiểm chứng cuối: Java21/Maven verify đạt 42/42 test, WAR build thành công; shared UI so origin/main đạt, hai script catalog qua Node syntax check. Edge/H2 đạt 7 checks, có ảnh desktop/tablet/mobile và lịch sử hồ sơ. Kiểm ảnh phát hiện nhãn JSP fragment lỗi encoding, đã thêm UTF-8 và test hồi quy render, chạy lại Maven + browser đều đạt. Không gọi provider/database thật. Chi tiết lỗi công cụ, test-issued JWT và ranh giới các module ghi trong UPDATE; không tính fixture H2 là triển khai Order/Identity.

## CAT-AI-004 — 24/10/2026 — bàn giao lên feature/catalog-sales

Prompt nguyên văn: “git add và git push code lên nhánh này”.

Phạm vi bàn giao: code/test/tài liệu/ảnh kiểm chứng catalog của CAT-AI-002 và CAT-AI-003 trên `feature/catalog-sales`. Kết quả kiểm chứng trước bàn giao: 42/42 tests bằng Java21 và 7 checks Edge/H2 đạt; không thay code Order/Identity hoặc bundle Shared UI. Commit/push thực hiện theo yêu cầu này; kết quả remote cần xác nhận sau khi lệnh hoàn tất.
