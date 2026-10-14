# Hướng dẫn nhánh feature/identity-membership — Xác thực, tài khoản và tương tác

Owner: TV2; tên cá nhân và reviewer do nhóm phân công. 16 UC bắt buộc, 0 UC thêm. Danh mục này thay hướng dẫn 28 UC trước đây.

Đọc [quy định](docs/QUY_DINH_VA_CAU_HINH_CHUNG.md), [PROJECT_PLAN](PROJECT_PLAN.md), [DATABASE_DESIGN](DATABASE_DESIGN.md), [USE_CASES](USE_CASES.md), [COMMON_BASELINE](docs/COMMON_BASELINE.md), [SETUP](docs/SETUP.md), [IMPLEMENTATION](docs/features/identity-membership/IMPLEMENTATION.md). Ghi prompt thực tại [AI_LOG](docs/features/identity-membership/AI_LOG.md).

## Phạm vi trọn luồng

| UC | Chức năng | Actor | Ưu tiên |
| --- | --- | --- | --- |
| UC-AUTH-01 | Đăng ký tài khoản | Guest | P0 bắt buộc |
| UC-AUTH-02 | Kích hoạt bằng OTP | Guest | P0 bắt buộc |
| UC-AUTH-03 | Đăng nhập | Guest | P0 bắt buộc |
| UC-AUTH-04 | Đăng xuất | Người đăng nhập | P0 bắt buộc |
| UC-AUTH-05 | Quên mật khẩu gửi OTP | Guest | P0 bắt buộc |
| UC-AUTH-06 | Đặt lại mật khẩu bằng OTP | Guest | P0 bắt buộc |
| UC-AUTH-07 | Làm mới phiên | Người đăng nhập | P0 bắt buộc |
| UC-USER-01 | Profile, avatar và đổi mật khẩu | Người đăng nhập | P0 bắt buộc |
| UC-USER-02 | Nhiều địa chỉ nhận hàng | User, Vendor | P0 bắt buộc |
| UC-USER-03 | Thích/bỏ thích sản phẩm | User, Vendor | P0 bắt buộc |
| UC-USER-04 | Sản phẩm đã xem | User, Vendor | P0 bắt buộc |
| UC-USER-05 | Đánh giá sản phẩm đã mua | User, Vendor | P0 bắt buộc |
| UC-USER-06 | Bình luận text, ảnh/video | User, Vendor | P0 bắt buộc |
| UC-USER-07 | Điểm và hạng thành viên | User, Vendor | P0 bắt buộc |
| UC-MGR-01 | Tìm/quản lý user | Manager | P0 bắt buộc |
| UC-ADM-01 | Tìm/quản lý user và phân quyền | Admin | P0 bắt buộc |

Mỗi UC làm Entity/Repo/DTO → Service → Controller → JSP/JS → test → hồ sơ. Xem đặc tả từng UC trong docs/features/identity-membership/use-cases. Hồ sơ đã có luồng/đầu vào/lỗi/test dự kiến; code, hình, test và commit thực còn cần bổ sung sau triển khai.

## Nền chung và phụ thuộc

Java 21/JDK đầy đủ, Maven Wrapper 3.9.16, Boot 3.5.13/WAR/JSP/Jakarta JSTL/SiteMesh 3.2.2/Bootstrap 5.3.3, JPA/Flyway/MySQL TiDB `utetra_db`, TLS VERIFY_IDENTITY, Security 6/Nimbus JWT cookie+CSRF, STOMP và MediaService theo main. Cấu hình trực tiếp `application*.properties`, không .env, không dùng DB `project_idv`. SMTP/Cloudinary/VNPAY chưa có credential thật vẫn disabled. H2 chỉ test isolated; không thay datasource thật bằng H2 để nghiệm thu cloud.

Main quản lý pom/version/config/layout/security/contracts/migrations và ba hướng dẫn BRANCH_*.md. Đề xuất thay đổi chung về main, sau review các feature merge main; không sửa cấu hình riêng để chữa lỗi. Catalog cung cấp store/giá/promotion/media; Identity cung cấp quyền/address/OTP/loyalty/audit; Order cung cấp purchase eligibility/payment/refund/notification và phối hợp transaction điểm/quota. Tham khảo bảng contract và owner schema, không tạo bản sao service chung.

Vendor có quyền User; Manager kiểm membership/phạm vi; Admin global; Shipper chỉ assignment khi bật. Khách chọn shop thủ công, chỉ bỏ tính khoảng cách. Giữ COD+VNPAY, OTP email, review/comment/media, carrier/commission/return-refund theo danh mục.

## Đồng bộ và bàn giao

```bash
git fetch origin
git switch feature/identity-membership
git merge origin/main
bash mvnw -B -ntp verify
```

Lệnh chạy cần JAVA_HOME trỏ JDK21. Với cloud dùng proxy/cache/truststore theo SETUP. Không reset/force-push để đồng bộ; giữ commit riêng và giải quyết xung đột cùng chủ file. Integration merge catalog → identity → order rồi kiểm xuyên suốt. Chỉ push/PR khi chủ repository yêu cầu.

Bàn giao activity/sequence, hướng dẫn code, screenshot code/UI thật, dữ liệu demo, kết quả test/prompt/commit/reviewer theo template. Sơ đồ dự kiến và UC chưa code phải ghi đúng trạng thái; không giả báo cáo DOCX hoặc provider đã chạy.
