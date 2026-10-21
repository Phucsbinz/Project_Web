# Đề xuất tích hợp identity — 17/10/2026

Nhánh triển khai: `feature/identity-membership`, đã merge `origin/main` trước khi viết code.

Đề xuất bổ sung migration identity có version timestamp riêng, không sửa V1. Các bảng phụ thuộc `stores`, `products`, `orders`, `order_items` do catalog/order sở hữu; adapter đọc đúng schema đã thiết kế, không tạo entity hoặc dữ liệu giả cho module khác. DDL FK liên module được tách để main review sau khi các bảng cha tồn tại.

Thay đổi SecurityConfig chỉ bổ sung route cho controller identity; giữ JWT cookie, CSRF và vai trò hiện tại. MediaService ảnh hiện hành được tái sử dụng; hợp đồng video cần mở rộng khi ghép catalog. Không đổi pom hoặc cấu hình datasource/provider.

Đính chính sau QA: SecurityConfig còn materialize CSRF trước JSP/SiteMesh và dùng NullAuthenticatedSessionStrategy riêng cho CSRF trong JWT stateless. Cookie xoay rõ ràng tại login/logout/reset/password, không xoay ở mọi request đã xác thực; test Tomcat và trình duyệt bắt được lỗi 403 của cấu hình mặc định. Đã thêm IdentityStoreAccessPolicy và AuditService làm contract dùng chung; native projections dùng JPA/Hibernate, metadata Entity validate 12 bảng mới.

Main/integration cần review migration, DTO, quyền shop và signature service trước khi merge; việc triển khai trên feature không có nghĩa thay đổi đã được main phê duyệt. Không tự commit/push.

## Bổ sung validation 20/10/2026

Đề xuất review migration additive `V2026102001__identity_validation_guards.sql`, các CHECK purpose/attempt/expiry/status/ledger/media/required address. DTO profile/manage/edit dùng version bắt buộc, integer không coercion; assignment phải có active boolean rõ ràng. Validation ở service boundary và client được thống nhất (codepoint text, UTF-8 password, phone); provider contract video cần metadata codec/duration hợp lệ. Không sửa cấu hình DB/pom/checksum migration trước. Đã kiểm trên H2 và demo; chưa tuyên bố CHECK/TLS/provider hoạt động trên TiDB thật. Xem VALIDATION_REVIEW_20261020.md.


## Tích hợp thực tế 20/10/2026 — IDN-AI-003

Đã merge catalog-sales/order-payment vào identity, thống nhất UI/quyền/ledger và chạy WAR với TiDB thật. Các giới hạn H2/demo/integration của các phần lịch sử phía trên được thay bằng trạng thái hiện tại tại [INTEGRATION_20261020](INTEGRATION_20261020.md). Không push.
