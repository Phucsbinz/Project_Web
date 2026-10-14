# UTETra — Kết quả kiểm chứng nền cấu hình

Ngày 13/10/2026, JDK21.0.12.1, Boot3.5.13, MySQL-compatible TiDB8.5.3 serverless. Đã tạo database riêng utetra_db qua TLS VERIFY_IDENTITY; không sửa project_idv. Flyway áp dụng V1 identity và Hibernate validate thành công; 3 bảng nghiệp vụ + flyway_schema_history, seed5roles.

## 1. Kết quả đã có

| Kiểm tra | Kết quả và phạm vi |
| --- | --- |
| Maven package/verify | WAR được build; 13 test ban đầu qua, xem kết quả cuối sau chỉnh dispatcher |
| JWT tests | 5: identity signature, expired, tampered role payload, wrong issuer/audience, weak key |
| HTTP security/CSRF tests | 6: public JSON, admin unauth401, User403, Admin200, forged401, missing CSRF403 |
| Session tests | 2: missing/revoked và expired token không query DB |
| TiDB TLS/Flyway/JPA | Kết nối thật, V1 applied, EntityManager validate và startup thành công |
| WebSocket | Simple broker started; chưa test authorized business event/polling/thu hồi chủ động |
| JSP/SiteMesh HTTP | Lần đầu500 do internal decorator forward bị firewall; đã chỉnh dispatcher, kết quả HTTP cuối cần ghi sau kiểm lại |
| Maven Wrapper | Đã tạo only-script3.3.4 dùng Maven3.9.16; kiểm wrapper version cuối cần ghi |

## 2. Các phần chưa kiểm chứng/chưa triển khai

Chưa có credential SMTP/Cloudinary/VNPAY nên disabled, chưa gửi OTP/upload cloud/pay/refund thật. Form login là placeholder ghi rõ, chưa user demo. StoreAccessPolicy non-Admin fail-closed đến khi có ownership/membership. Chưa test responsive trực quan/screenshot đầy đủ. 43 bảng P0 +2 optional và55UC+3optional là kế hoạch feature, không phải mọi bảng/tính năng đã có.

H2 test isolated không chứng minh dialect MySQL, nhưng MySQL/TiDB baseline đã kiểm qua WAR riêng. Lỗi và cách sửa cần giữ trong nhật ký AI/config; không thay strict firewall toàn cục để chữa SiteMesh. Credential thật không được sao chép vào kết quả/ảnh/log.

## Kiểm chứng bổ sung trên cloud — 14/10/2026

Commit nền trước bổ sung: 62a5ded. Cloud tại /workspace/Project_Web; JDK21 đầy đủ kiểm checksum Oracle, Maven Wrapper3.9.16 dùng proxy settings và truststore hệ thống. Build/test sau sửa prefix SiteMesh: `verify` BUILD SUCCESS, WAR target/utetra.war.

| Check | Kết quả lượt hiện tại |
| --- | --- |
| BaselineSecurityTest | 6 passed, 0 failure/error/skipped |
| JwtServiceTest | 5 passed, 0 failure/error/skipped |
| SessionIdentityServiceTest | 2 passed, 0 failure/error/skipped |
| BaselineViewTest — embedded Tomcat thật với H2 | 3 passed; home/login200, JSP/SiteMesh rendered, API JSON và CSS không decorate |
| Tổng Surefire | 16 executed, 16 passed, 0 failure/error/skipped |
| Build WAR | Passed |
| Danh mục/hồ sơ/owner UC | 58 mã unique, 55 P0+3 P1; đủ hướng dẫn/module/đặc tả/link |
| TiDB mysql profile | Không chạy được: native DNS lỗi; HTTPS CONNECT probe4000 nhận403. Chưa xác minh Flyway/JPA/TLS trong cloud |
| SMTP/Cloudinary/VNPAY | Disabled; chưa kiểm provider thật |
| Nghiệp vụ UC | Chưa triển khai/chưa chạy; bảng test trong từng hồ sơ là tiêu chí dự kiến |

SiteMesh lỗi 500 đã tái hiện ở home/login: default prefix /WEB-INF/decorators/ ghép với /WEB-INF/views/layouts/*.jsp. Sửa `setDecoratorPrefix("")`, không hạ strict firewall. Test HTTP Tomcat thật bắt được lỗi này; không dùng MockMvc-only làm bằng chứng JSP render.

Lượt test mới đầu tiên có một assertion CSS quá chặt với khoảng trắng trong header Bootstrap; chỉnh assertion kiểm nội dung CSS/navbar và không HTML, rồi rerun đạt16/16. Không tắt test hoặc bỏ assertion hành vi.

Các ghi nhận TiDB thành công trong phần trước là kết quả máy Windows của phiên cũ. H2 ở lượt này không thay bằng chứng SQL dialect/TLS. Chưa có screenshot responsive hoặc test socket business event; cần kiểm khi feature hiện thực.
