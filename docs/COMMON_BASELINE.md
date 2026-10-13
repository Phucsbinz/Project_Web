# UTETra — Baseline và hợp đồng chung cho các nhánh

## 1. Nguồn cấu hình

Áp dụng cùng lúc [PROJECT_PLAN.md](../PROJECT_PLAN.md), [DATABASE_DESIGN.md](../DATABASE_DESIGN.md), [USE_CASES.md](../USE_CASES.md) và [docs/README.md](README.md). Nếu phát hiện mâu thuẫn, ghi đề xuất và thống nhất trên main trước khi code; AI không tự chọn thiết kế khác. Quy trình chia nhánh trong `BRANCH_WORKFLOW.md` thay cách phân nhánh `develop` trước đây; phạm vi nghiệp vụ và schema vẫn giữ.

| Thành phần | Baseline theo đặc tả |
| --- | --- |
| Java | 21 LTS |
| Backend | Spring Boot 3.5.x; khóa patch cụ thể trong pom khi dựng B0 |
| Security | Spring Security 6.x theo BOM của Boot; JWT + CSRF |
| View | JSP, Jakarta JSTL, SiteMesh 3.2.x tương thích Jakarta; JSP trong WEB-INF |
| UI | Bootstrap 5.3.x; các layout client/staff/admin/auth |
| Packaging | Maven, executable WAR, finalName=utetra, Tomcat/Jasper |
| Persistence | Spring Data JPA/Hibernate; Flyway; ddl-auto=validate; open-in-view=false |
| Database | sqlserver (demo chính), mysql, postgresql; mỗi lần chạy đúng một profile |
| Ngoài hệ thống | Cloudinary; SMTP; STOMP WebSocket /ws cùng origin |
| Thời gian/tiền | DB UTC; UI Asia/Ho_Chi_Minh; VND nguyên đồng, BigDecimal/DECIMAL(14,2) |

Phiên bản patch, dependency tọa độ và đường dẫn migration thực tế phải được xác nhận trên main và ghi commit. Các nhánh không dùng Boot 2.x, Thymeleaf, SPA hoặc cơ sở dữ liệu khác để thay stack đã yêu cầu.

## 2. Việc cần dựng trên main — B0

- [ ] Tạo Maven project com.utetra, Java 21, WAR và Maven Wrapper.
- [ ] Chốt pom với các starter, Jasper/JSTL, SiteMesh, JWT, Cloudinary, Flyway và JDBC driver; build WAR và mở trang mẫu thành công.
- [ ] Tạo các cấu hình application chung và ba profile DB, không chứa secret.
- [ ] Tạo view resolver, decorator, layout Bootstrap, trang lỗi, static assets và exception handling chung.
- [ ] Chốt package/config/DTO interface dùng chung bên dưới; không triển khai ba bản trùng nhau.
- [ ] Phân phối migration theo 26 bảng; chạy schema rỗng trên từng CSDL trước khi xác nhận tương thích.
- [ ] Tạo dữ liệu demo và cấu hình SMTP test/Cloudinary; cơ chế seed không bật ở môi trường thật.
- [ ] Cập nhật SETUP bằng lệnh thực đã chạy, kết quả và commit; đồng bộ main vào ba nhánh.

## 3. Chủ sở hữu dữ liệu và file

| Chủ sở hữu | Bảng/logic | Nhánh sử dụng |
| --- | --- | --- |
| TV1 | stores, store_delivery_areas, categories, products, product_variants, toppings, product_toppings, store_products, store_toppings; MediaService | TV2 dùng ảnh; TV3 đọc giá/tình trạng/phí |
| TV2 | roles, users, staff_store_assignments, auth_sessions, password_reset_tokens, user_addresses, loyalty_accounts, loyalty_transactions; audit_logs | TV1/TV3 dùng quyền; TV3 gọi nghiệp vụ điểm |
| TV3 | carts, cart_items, cart_item_toppings, orders, order_items, order_item_toppings, order_status_history, notifications | TV2 đọc thông tin đơn qua contract khi cần; không sửa đơn riêng |

Đủ 26 bảng, không tạo thêm bảng cùng ý nghĩa hoặc giữ một biến thể schema riêng. TV2 sở hữu ledger điểm nhưng TV3 sở hữu transaction đơn gọi dịch vụ ledger. TV2 sở hữu AuditService; TV1/TV3 gọi dịch vụ để ghi audit chung, không viết lại bảng log.

Thứ tự migration V1–V4 trong DATABASE_DESIGN.md có bảng thuộc nhiều thành viên. Nhóm trưởng tập hợp và commit migration chung trên main; không để ba người cùng tạo V1 khác nhau. Khi đã chạy migration, thay đổi qua version mới; không sửa migration cũ. Nhánh chức năng đề xuất thay đổi bằng mô tả bảng/cột/chỉ mục và cả ba dialect.

## 4. Hợp đồng dịch vụ cần chốt trước lập trình

Các tên sau là contract thiết kế cần hiện thực và khóa trên main; chưa phải class đã tồn tại. Mỗi contract phải có signature Java, DTO, lỗi nghiệp vụ, cách khóa và kiểm thử contract trong B0.

| Contract | Owner | Hành vi và ranh giới |
| --- | --- | --- |
| CatalogService | TV1 | Đọc món/variant/topping hợp lệ, cung cấp giá BigDecimal và version để giỏ/checkout kiểm tra lại |
| StoreService | TV1 | Chi nhánh khách tự chọn, kiểm tra nhận đơn, khu vực giao thuộc chi nhánh, phí giao theo cấu hình khu vực |
| MediaService | TV1 | UploadResult gồm secureUrl/publicId; xóa chỉ tài sản server đã xác minh; dọn ảnh sau commit |
| CurrentUser/AuthorizationService | TV2 | Lấy user từ principal, kiểm tra vai trò, sở hữu và phân công; không tin userId client |
| AddressService | TV2 | Đọc địa chỉ của đúng Customer; trả DTO để tạo snapshot, không trả Entity chưa nạp |
| LoyaltyService | TV2 | Redeem/refund/earn idempotent theo orderId + type, khóa account, tham gia transaction của đơn; không REQUIRES_NEW |
| AuditService | TV2 | Append audit lọc secret trong cùng transaction của nghiệp vụ gọi |
| OrderService | TV3 | Checkout, chuyển/hủy/hoàn tất/thu COD theo state machine, snapshot và version |
| NotificationService/OrderEventPublisher | TV3 | Lưu từng người nhận trong transaction; phát sự kiện sau commit; đọc lại DB khi offline |

DTO chung dự kiến: CartItemRequest, CheckoutRequest, OrderStatusRequest, OrderEvent, MediaUploadResult. Khách chỉ gửi lựa chọn và thông tin nhận; tiền, vai trò, trạng thái thanh toán và điểm kiếm do server quyết định. TV3 có thể dùng mock contract khi TV1/TV2 chưa xong nhưng chỉ trong test; không merge luồng production dựa vào giá/quyền giả.

## 5. Quy ước giao diện, route và bảo mật

- Package và cây JSP theo PROJECT_PLAN.md; Controller nhận DTO; JSP EL/JSTL không có SQL/scriptlet; render dữ liệu người dùng có escape.
- Public: /, /menu, /products/{slug}, /stores. Auth: /auth/**. Khách: /account/**, /cart/**, /checkout, /orders/**. Staff: /staff/**. Admin: /admin/**. Thông báo: /notifications/**.
- GET chỉ đọc; POST thay đổi cần CSRF. JSON hỗ trợ view dưới /api/** có cùng kiểm tra quyền; không đổi sang API riêng làm lệch route.
- Cookie access/refresh HttpOnly, Secure khi HTTPS, SameSite=Lax; access 15 phút, refresh tối đa 7 ngày; session DB để thu hồi. Không disable Security toàn cục để demo.
- Socket /ws: Customer dùng /user/queue/orders và /user/queue/notifications; Staff /topic/stores/{storeId}/orders; Admin /topic/admin/orders. Kiểm tra SUBSCRIBE, không phát dữ liệu riêng tư lên topic.
- Chỉ main điều phối sửa SecurityConfig/WebSocketConfig/SiteMeshConfig; chức năng thông báo và giao diện vẫn do owner module làm và đề xuất cấu hình cho main.

## 6. Quy tắc phải giữ khi tích hợp

Chi nhánh do khách chọn; không tính khoảng cách. Khu vực/phí giao và các chức năng khác vẫn còn. Đơn P0 COD, trạng thái PENDING → PREPARING → DELIVERING → COMPLETED; hủy theo cạnh cho phép. COMPLETED đi cùng PAID; tiền/điểm theo BR-10–17. Địa chỉ/giá/topping lưu snapshot; idempotency checkout_key/request_hash; @Version chống màn hình cũ.

Điểm 1.000 đồng/điểm, giảm tối đa 20% subtotal; tích floor((subtotal-discount)/10.000) khi hoàn tất, hoàn điểm khi hủy đúng một lần. Không cộng doanh thu từ đơn hủy/chưa trả. Schema enum, cột nullable, unique và thời gian dùng đúng DATABASE_DESIGN.md.

## 7. Nhật ký chốt contract

| Ngày | Contract/version | Signature/DTO thật | Chủ triển khai | Nhánh dùng | Commit main | Kiểm chứng |
| --- | --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền] | [điền] |
