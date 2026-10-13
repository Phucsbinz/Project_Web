# Hướng dẫn nhánh feature/identity-membership

**Người nhận:** TV2, điền họ tên trong docs/features/identity-membership/IMPLEMENTATION.md. **Phạm vi:** UC-AUTH-01–05, UC-USER-01–03, UC-ADMIN-01; toàn bộ lớp và hồ sơ của luồng tài khoản/thành viên.

Đọc PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md, docs/README.md, docs/COMMON_BASELINE.md và docs/SETUP.md. Hướng dẫn: [IMPLEMENTATION](docs/features/identity-membership/IMPLEMENTATION.md). Prompt thật: [AI_LOG](docs/features/identity-membership/AI_LOG.md).

Cấu hình theo main: Java 21/Boot 3.5.x/WAR/JSP/SiteMesh/JPA, Security 6.x/JWT cookie + CSRF, profile/migration chung. JWT/mail dùng biến môi trường; không sinh cấu hình thứ hai. Owner 9 bảng identity/loyalty/audit, cung cấp Authorization/Address/Loyalty/Audit contract. Dùng MediaService TV1; LoyaltyService cùng transaction đơn TV3.

Làm trọn từng UC, ghi prompt và minh chứng kiến trúc/activity/code/UI/setup/test theo template. Không bỏ điểm/hạng/reset/assignment hoặc chức năng khác; chỉ phần tính khoảng cách đã loại. Bắt đầu code sau đồng bộ B0; phối hợp quyền socket với TV3, tích hợp theo docs/BRANCH_WORKFLOW.md.
