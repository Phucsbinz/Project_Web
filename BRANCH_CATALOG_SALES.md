# Hướng dẫn nhánh feature/catalog-sales — Danh mục, shop và khuyến mãi

Owner: TV1; tên cá nhân và reviewer do nhóm phân công. 18 UC bắt buộc, 0 UC thêm. Danh mục này thay hướng dẫn 28 UC trước đây.

Đọc [quy định](docs/QUY_DINH_VA_CAU_HINH_CHUNG.md), [PROJECT_PLAN](PROJECT_PLAN.md), [DATABASE_DESIGN](DATABASE_DESIGN.md), [USE_CASES](USE_CASES.md), [COMMON_BASELINE](docs/COMMON_BASELINE.md), [SETUP](docs/SETUP.md), [IMPLEMENTATION](docs/features/catalog-sales/IMPLEMENTATION.md). Ghi prompt thực tại [AI_LOG](docs/features/catalog-sales/AI_LOG.md).

## Phạm vi trọn luồng

| UC | Chức năng | Actor | Ưu tiên |
| --- | --- | --- | --- |
| UC-CAT-01 | Trang chủ Guest | Guest | P0 bắt buộc |
| UC-CAT-02 | Tìm kiếm và lọc | Mọi người | P0 bắt buộc |
| UC-CAT-03 | Trang chủ User và top 20 | User, Vendor | P0 bắt buộc |
| UC-CAT-04 | Sản phẩm theo danh mục | Mọi người | P0 bắt buộc |
| UC-CAT-05 | Chi tiết và tùy biến trà sữa | Mọi người | P0 bắt buộc |
| UC-CAT-06 | Xem/chọn chi nhánh và trang shop | Mọi người | P0 bắt buộc |
| UC-VND-01 | Đăng ký shop | User, Vendor | P0 bắt buộc |
| UC-VND-02 | Quản lý trang chủ shop | Vendor | P0 bắt buộc |
| UC-VND-03 | Quản lý sản phẩm của mình | Vendor | P0 bắt buộc |
| UC-VND-05 | Tạo khuyến mãi shop | Vendor | P0 bắt buộc |
| UC-MGR-02 | Quản lý sản phẩm shop | Manager | P0 bắt buộc |
| UC-MGR-03 | Quản lý danh mục/topping | Manager | P0 bắt buộc |
| UC-MGR-05 | Quản lý khuyến mãi | Manager | P0 bắt buộc |
| UC-MGR-07 | Quản lý cửa hàng | Manager | P0 bắt buộc |
| UC-ADM-02 | Quản lý sản phẩm các shop | Admin | P0 bắt buộc |
| UC-ADM-03 | Quản lý danh mục/topping | Admin | P0 bắt buộc |
| UC-ADM-05 | Quản lý chương trình khuyến mãi | Admin | P0 bắt buộc |
| UC-ADM-07 | Duyệt/quản lý shop | Admin | P0 bắt buộc |

Mỗi UC làm Entity/Repo/DTO → Service → Controller → JSP/JS → test → hồ sơ. Xem đặc tả từng UC trong docs/features/catalog-sales/use-cases. Hồ sơ đã có luồng/đầu vào/lỗi/test dự kiến; code, hình, test và commit thực còn cần bổ sung sau triển khai.

## Nền chung và phụ thuộc

Java 21/JDK đầy đủ, Maven Wrapper 3.9.16, Boot 3.5.13/WAR/JSP/Jakarta JSTL/SiteMesh 3.2.2/Bootstrap 5.3.3, JPA/Flyway/MySQL TiDB `utetra_db`, TLS VERIFY_IDENTITY, Security 6/Nimbus JWT cookie+CSRF, STOMP và MediaService theo main. Cấu hình trực tiếp `application*.properties`, không .env, không dùng DB `project_idv`. SMTP/Cloudinary/VNPAY chưa có credential thật vẫn disabled. H2 chỉ test isolated; không thay datasource thật bằng H2 để nghiệm thu cloud.

Main quản lý pom/version/config/layout/security/contracts/migrations và ba hướng dẫn BRANCH_*.md. Đề xuất thay đổi chung về main, sau review các feature merge main; không sửa cấu hình riêng để chữa lỗi. Catalog cung cấp store/giá/promotion/media; Identity cung cấp quyền/address/OTP/loyalty/audit; Order cung cấp purchase eligibility/payment/refund/notification và phối hợp transaction điểm/quota. Tham khảo bảng contract và owner schema, không tạo bản sao service chung.

Vendor có quyền User; Manager kiểm membership/phạm vi; Admin global; Shipper chỉ assignment khi bật. Khách chọn shop thủ công, chỉ bỏ tính khoảng cách. Giữ COD+VNPAY, OTP email, review/comment/media, carrier/commission/return-refund theo danh mục.

## Đồng bộ và bàn giao

```bash
git fetch origin
git switch feature/catalog-sales
git merge origin/main
bash mvnw -B -ntp verify
```

Lệnh chạy cần JAVA_HOME trỏ JDK21. Với cloud dùng proxy/cache/truststore theo SETUP. Không reset/force-push để đồng bộ; giữ commit riêng và giải quyết xung đột cùng chủ file. Integration merge catalog → identity → order rồi kiểm xuyên suốt. Chỉ push/PR khi chủ repository yêu cầu.

Bàn giao activity/sequence, hướng dẫn code, screenshot code/UI thật, dữ liệu demo, kết quả test/prompt/commit/reviewer theo template. Sơ đồ dự kiến và UC chưa code phải ghi đúng trạng thái; không giả báo cáo DOCX hoặc provider đã chạy.
