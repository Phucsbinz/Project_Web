# Main — Nền cấu hình UTETra

Đọc docs/QUY_DINH_VA_CAU_HINH_CHUNG.md trước, rồi PROJECT_PLAN, DATABASE_DESIGN, USE_CASES, docs/README, COMMON_BASELINE, SETUP và BASELINE_VERIFICATION.

Main đã có mã nền Java21/Boot3.5.13/WAR/JSP/JSTL/SiteMesh/Bootstrap/JPA/MySQL/TiDB/JWT/Security/STOMP/Cloudinary adapter, Maven Wrapper và V1 identity. Main giữ cấu hình trực tiếp properties được Git theo dõi; database riêng utetra_db, không sửa project_idv. Provider chưa có credential tắt và ghi rõ. Nền kiểm chứng không phải các chức năng thương mại đã xong.

Ba nhánh catalog-sales/identity-membership/order-payment làm UC trọn luồng theo actor/owner bảng USE_CASES. Vendor quyền User, Manager shop-scope, Admin toàn app, Shipper phần thêm. Không giới hạn lại COD-only/3role/link-reset hoặc tự bỏ tính năng thầy. Integration ghép và review; các nhánh lấy main mới trước làm.

Prompt khởi tạo/quy định/cấu hình của người dùng được ghi AI_PROMPT_HISTORY; công việc baseline/test/sửa lỗi ở BASELINE_AI_LOG. Nhóm điền tên/reviewer/PR/commit/tiến độ thật, không bịa kết quả.
