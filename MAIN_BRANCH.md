# Nhánh main — Nền tảng chung UTETra

Main giữ cấu hình/baseline dùng chung và bản tích hợp đã qua kiểm chứng. Khởi tạo hiện tại là tài liệu, chưa có ứng dụng Spring Boot chạy được.

Đọc theo thứ tự: PROJECT_PLAN.md → DATABASE_DESIGN.md → USE_CASES.md → docs/README.md → docs/COMMON_BASELINE.md → docs/SETUP.md → docs/BRANCH_WORKFLOW.md.

Người phụ trách B0: nhóm trưởng, các thành viên review. Dựng Java 21/Boot 3.5.x/WAR/JSP/JSTL/SiteMesh/Bootstrap/JPA, ba profile DB, config mẫu không secret, layout/route, contract và migration theo 26 bảng. Khóa phiên bản cụ thể trong pom; không để từng nhánh tự khởi tạo project riêng.

Nhánh chức năng nhận cả dữ liệu/logic/controller/JSP/test/hồ sơ của use case: feature/catalog-sales (TV1), feature/identity-membership (TV2), feature/order-payment (TV3). integration/utetra ghép chúng trước khi đưa bản ổn định về main.

## Hồ sơ B0 cần nhóm ghi

| Công việc | File/commit thực | Cấu hình/chọn version | Lệnh/kết quả | Hình minh chứng | Prompt/reviewer |
| --- | --- | --- | --- | --- | --- |
| Maven/WAR/dependencies | [điền] | [điền] | [chưa chạy] | [điền] | [điền] |
| JSP/JSTL/SiteMesh/layout | [điền] | [điền] | [chưa chạy] | [điền] | [điền] |
| Profile DB/migration/seed | [điền] | [điền] | [chưa chạy] | [điền] | [điền] |
| Contract/security/socket config | [điền] | [điền] | [chưa chạy] | [điền] | [điền] |
| Setup và smoke test chung | [điền] | [điền] | [chưa chạy] | [điền] | [điền] |

Giữ nhật ký yêu cầu tại docs/AI_PROMPT_HISTORY.md và ghi prompt B0 thực dùng vào file mới docs/BASELINE_AI_LOG.md theo mẫu prompt. Chỉ cập nhật docs/SETUP thành “đã chạy” khi có log/ảnh thực. Phần khoảng cách đã loại khỏi phạm vi; các chức năng khác giữ theo đặc tả.
