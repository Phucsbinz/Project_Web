# Hướng dẫn nhánh integration/utetra — Ghép chức năng

**Người phụ trách:** nhóm trưởng và reviewer của ba module. Đọc bốn đặc tả/chỉ mục PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md, docs/README.md cùng docs/COMMON_BASELINE.md, docs/SETUP.md và docs/BRANCH_WORKFLOW.md.

Cấu hình phải đúng baseline main: Java 21, Boot 3.5.x đã khóa patch, WAR/JSP/JSTL/SiteMesh/Bootstrap/JPA, profile DB, schema 26 bảng, JWT/CSRF, Cloudinary/SMTP và WebSocket contract. Không tạo profile/phiên bản khác để chữa lỗi ghép.

Lấy main/B0 mới nhất, merge lần lượt catalog-sales → identity-membership → order-payment. Kiểm contract catalog/store/media, quyền/address/loyalty/audit và transaction đơn; phối hợp owner khi xung đột. Không thay mất chức năng của người khác khi resolve.

## Checklist ghép và hồ sơ thực

| Bước | Nhánh/commit đã ghép | Kiểm tra | Kết quả/hình/log | Owner/reviewer |
| --- | --- | --- | --- | --- |
| Baseline main | [điền] | WAR/JSP/config/migration | [chưa chạy] | [điền] |
| Catalog | [điền] | CRUD/menu/chi nhánh/Cloudinary | [chưa chạy] | [điền] |
| Identity | [điền] | Auth/quyền/địa chỉ/điểm/audit | [chưa chạy] | [điền] |
| Order | [điền] | Cart/checkout/COD/state/socket/report | [chưa chạy] | [điền] |
| Toàn luồng | [điền] | AT-01–16, ba profile DB | [chưa chạy] | [điền] |
| Hồ sơ nộp | [điền] | Sơ đồ/code/UI/setup/prompt/Word | [chưa đủ minh chứng] | [điền] |

Ghi prompt phối hợp/sửa lỗi thật vào docs/BASELINE_AI_LOG.md bằng ID INT-AI-001... Nếu sửa code module nào, ghi hồ sơ UC module đó. Hình/prompt/setup tổng hợp theo docs/REPORT_ASSEMBLY.md. Chỉ đưa về main khi chức năng và hồ sơ đã được review; báo cáo nêu rõ phần chưa làm.
