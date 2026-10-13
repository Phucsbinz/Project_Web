# Hướng dẫn nhánh feature/catalog-sales

**Người nhận:** TV1, điền họ tên trong docs/features/catalog-sales/IMPLEMENTATION.md. **Phạm vi:** UC-CAT-01–09, trọn luồng dữ liệu → nghiệp vụ → controller → JSP → kiểm thử/hồ sơ.

Đọc PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md, docs/README.md, docs/COMMON_BASELINE.md và docs/SETUP.md trước làm. Hướng dẫn chi tiết: [IMPLEMENTATION](docs/features/catalog-sales/IMPLEMENTATION.md). Ghi prompt thực tại [AI_LOG](docs/features/catalog-sales/AI_LOG.md).

Cấu hình: Java 21, Boot 3.5.x theo patch main, WAR/Jakarta JSP/JSTL/SiteMesh/Bootstrap/JPA; dùng profile DB và migration main. Cloudinary từ biến môi trường; không đổi pom/profile/security tùy ý. Owner 9 bảng catalog/store như hồ sơ, cung cấp CatalogService/StoreService/MediaService cho các nhánh khác. Dùng quyền/audit TV2.

Thực hiện từng UC từ đầu đến cuối theo bảng công việc; tạo hồ sơ UC từ docs/templates/USE_CASE_NOTE.md, lưu kiến trúc/activity/code/UI/setup/prompt thật. Khách tự chọn chi nhánh; giữ khu vực/phí giao và các chức năng khác. Lấy baseline main đã hoàn thành B0 trước khi lập trình; ghép qua integration/utetra theo BRANCH_WORKFLOW.
