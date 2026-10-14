# Hướng dẫn nhánh feature/order-payment — Giỏ, đơn, thanh toán và vận hành

Owner: TV3; tên cá nhân và reviewer do nhóm phân công. 21 UC bắt buộc, 3 UC thêm. Danh mục này thay hướng dẫn 28 UC trước đây.

Đọc [quy định](docs/QUY_DINH_VA_CAU_HINH_CHUNG.md), [PROJECT_PLAN](PROJECT_PLAN.md), [DATABASE_DESIGN](DATABASE_DESIGN.md), [USE_CASES](USE_CASES.md), [COMMON_BASELINE](docs/COMMON_BASELINE.md), [SETUP](docs/SETUP.md), [IMPLEMENTATION](docs/features/order-payment/IMPLEMENTATION.md). Ghi prompt thực tại [AI_LOG](docs/features/order-payment/AI_LOG.md).

## Phạm vi trọn luồng

| UC | Chức năng | Actor | Ưu tiên |
| --- | --- | --- | --- |
| UC-ORD-01 | Thêm món vào giỏ | Guest, User, Vendor | P0 bắt buộc |
| UC-ORD-02 | Sửa/xóa/hợp nhất giỏ | Guest, User, Vendor | P0 bắt buộc |
| UC-ORD-03 | Checkout | User, Vendor | P0 bắt buộc |
| UC-ORD-04 | Chọn mã giảm giá | User, Vendor | P0 bắt buộc |
| UC-ORD-05 | Thanh toán COD | User, Vendor | P0 bắt buộc |
| UC-ORD-06 | Thanh toán VNPAY | User, Vendor | P0 bắt buộc |
| UC-ORD-07 | Lịch sử và theo dõi đơn | User, Vendor | P0 bắt buộc |
| UC-ORD-08 | Hủy đơn | User, Vendor | P0 bắt buộc |
| UC-ORD-09 | Yêu cầu trả hàng–hoàn tiền | User, Vendor | P0 bắt buộc |
| UC-ORD-10 | Realtime và hộp thông báo | Người đăng nhập | P0 bắt buộc |
| UC-VND-04 | Quản lý đơn shop | Vendor | P0 bắt buộc |
| UC-VND-06 | Doanh thu shop | Vendor | P0 bắt buộc |
| UC-VND-07 | Xử lý yêu cầu trả hàng | Vendor | P0 bắt buộc |
| UC-MGR-04 | Chiết khấu app cho shop | Manager | P0 bắt buộc |
| UC-MGR-06 | Nhà vận chuyển và phí | Manager | P0 bắt buộc |
| UC-MGR-08 | Đơn, trả hàng và hoàn tiền | Manager | P0 bắt buộc |
| UC-MGR-09 | Báo cáo cửa hàng | Manager | P0 bắt buộc |
| UC-ADM-04 | Chiết khấu app cho shop | Admin | P0 bắt buộc |
| UC-ADM-06 | Nhà vận chuyển/phí | Admin | P0 bắt buộc |
| UC-ADM-08 | Đơn/giao dịch/trả–hoàn | Admin | P0 bắt buộc |
| UC-ADM-09 | Báo cáo toàn hệ thống | Admin | P0 bắt buộc |
| UC-SHP-01 | Đơn giao được phân công | Shipper | P1 thêm |
| UC-SHP-02 | Cập nhật giao hàng | Shipper | P1 thêm |
| UC-SHP-03 | Thống kê giao hàng | Shipper | P1 thêm |

Mỗi UC làm Entity/Repo/DTO → Service → Controller → JSP/JS → test → hồ sơ. Xem đặc tả từng UC trong docs/features/order-payment/use-cases. Hồ sơ đã có luồng/đầu vào/lỗi/test dự kiến; code, hình, test và commit thực còn cần bổ sung sau triển khai.

## Nền chung và phụ thuộc

Java 21/JDK đầy đủ, Maven Wrapper 3.9.16, Boot 3.5.13/WAR/JSP/Jakarta JSTL/SiteMesh 3.2.2/Bootstrap 5.3.3, JPA/Flyway/MySQL TiDB `utetra_db`, TLS VERIFY_IDENTITY, Security 6/Nimbus JWT cookie+CSRF, STOMP và MediaService theo main. Cấu hình trực tiếp `application*.properties`, không .env, không dùng DB `project_idv`. SMTP/Cloudinary/VNPAY chưa có credential thật vẫn disabled. H2 chỉ test isolated; không thay datasource thật bằng H2 để nghiệm thu cloud.

Main quản lý pom/version/config/layout/security/contracts/migrations và ba hướng dẫn BRANCH_*.md. Đề xuất thay đổi chung về main, sau review các feature merge main; không sửa cấu hình riêng để chữa lỗi. Catalog cung cấp store/giá/promotion/media; Identity cung cấp quyền/address/OTP/loyalty/audit; Order cung cấp purchase eligibility/payment/refund/notification và phối hợp transaction điểm/quota. Tham khảo bảng contract và owner schema, không tạo bản sao service chung.

Vendor có quyền User; Manager kiểm membership/phạm vi; Admin global; Shipper chỉ assignment khi bật. Khách chọn shop thủ công, chỉ bỏ tính khoảng cách. Giữ COD+VNPAY, OTP email, review/comment/media, carrier/commission/return-refund theo danh mục.

## Đồng bộ và bàn giao

```bash
git fetch origin
git switch feature/order-payment
git merge origin/main
bash mvnw -B -ntp verify
```

Lệnh chạy cần JAVA_HOME trỏ JDK21. Với cloud dùng proxy/cache/truststore theo SETUP. Không reset/force-push để đồng bộ; giữ commit riêng và giải quyết xung đột cùng chủ file. Integration merge catalog → identity → order rồi kiểm xuyên suốt. Chỉ push/PR khi chủ repository yêu cầu.

Bàn giao activity/sequence, hướng dẫn code, screenshot code/UI thật, dữ liệu demo, kết quả test/prompt/commit/reviewer theo template. Sơ đồ dự kiến và UC chưa code phải ghi đúng trạng thái; không giả báo cáo DOCX hoặc provider đã chạy.
