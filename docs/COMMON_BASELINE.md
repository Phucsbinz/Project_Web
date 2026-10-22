# UTETra — Baseline main và hợp đồng các nhánh

> Chuẩn UI 22/10/2026: một SiteMesh decorator layouts/app.jsp cho mọi nhánh và role. Header/footer/sidebar/assets tập trung; hướng dẫn agent và đồng bộ tại [SHARED_UI](SHARED_UI.md). Quy định này thay cấu trúc sáu layout trước đó.

> Nguồn ưu tiên: QUY_DINH_VA_CAU_HINH_CHUNG.md, PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md. Bản này thay baseline 26 bảng/3 role/COD-only/.env; đọc cấu hình thật trong src/main/resources/application*.properties.

## 1. Stack khóa chung

Java 21; Spring Boot 3.5.13; Maven 3.9.16 Wrapper; WAR finalName utetra; JSP/Jakarta JSTL API+implementation; SiteMesh 3.2.2 filter đăng ký một lần; Bootstrap 5.3.3 + Start Bootstrap Shop Homepage; Spring Data JPA; MySQL Connector theo BOM; Flyway core/mysql; Nimbus 10.10; Cloudinary HTTP5 2.4.0; Mail; Spring WebSocket/STOMP và Security messaging. SQL Server/PostgreSQL không dùng cho bản hiện tại.

Database TiDB Cloud/MySQL protocol riêng utetra_db, TLS VERIFY_IDENTITY. Hibernate validate, open-in-view=false, UTC; UTF8/utf8mb4, BigDecimal VND. Theo yêu cầu nhóm private: cấu hình trực tiếp properties, không .env, không ignore cấu hình. Không sao chép credential vào UI/log/ảnh/prompt.

## 2. Code baseline hiện có

UtetraApplication/WAR, SiteMeshConfig và 6 layout client/auth/vendor/manager/admin/shipper, HomeController/JSTL trang kiểm chứng, SystemController → SystemStatusService, Role/User/AuthSession và hai JPA repository; SecurityConfig/AuthenticationConfig/BCrypt, JwtService/JwtAuthenticationFilter/SessionIdentityService; WebSocketConfig/WebSocketSecurityConfig; MediaService và adapter Cloudinary có enabled flag. V1 migration chỉ identity + seed 5 role, không tài khoản demo.

Không xem B0 đã triển khai OTP/login screens/giỏ/checkout/callback/review/...; các tính năng đó nằm trên feature. Default StoreAccessPolicy deny-all cho non-Admin đến khi catalog/identity hiện thực scope; không bỏ kiểm tra quyền để demo. Credentials/provider thiếu => enabled=false, không fake upload/email/payment.

## 3. Owner và contract

| Contract | Owner | Hợp đồng |
| --- | --- | --- |
| CatalogService/StoreService | TV1 | Món/variant/topping/product thuộc store; store approved/open; area/quote/version, không distance |
| MediaService | TV1 | secureUrl/publicId/resourceType, ảnh thật và cleanup theo transaction; mở rộng video cho comments qua review contract |
| PromotionService | TV1 | Program/code/scope/quota/eligibility/funding; quote tách product/shipping, TV3 reserve/consume/release cùng transaction |
| CurrentUser/Authorization/StoreAccessPolicy | TV2 phối hợp TV1 | Role current, owner store/membership Manager, Vendor có User; DB kiểm từng request và socket |
| OTP/Auth/Address services | TV2 | email verified, session rotation/revoke, OTP purpose/HMAC/limits, owner address snapshot |
| Loyalty/AuditService | TV2 | Redeem/refund/earn/reversal/debt, cùng transaction đơn, unique order/type; audit lọc secret |
| Order/Payment/RefundService | TV3 | Idempotency/price snapshot/state/COD/IPN/reconcile/refund và ledger; không tin client amount |
| Carrier/Commission/Report/Notification | TV3 | Scope shop/role, rate/commission snapshot, net/refund report, after commit events |

Owner từng bảng/UC trong DATABASE_DESIGN và USE_CASES. Lock order và account/quota/config theo thứ tự đã thống nhất; không viết service điểm hoặc giá thứ hai trên nhánh mua. DTO dùng chung: CartItemRequest, CheckoutRequest, CheckoutQuote, OrderStatusRequest, OrderEvent, MediaUploadResult, PaymentResult, RefundRequest; signature thực phải chốt trên main trước triển khai phụ thuộc.

## 4. Routes/layout/Security

Public /menu/products/stores; /auth/**; /account/profile và password cho người đăng nhập; mua /cart/checkout/orders/community/loyalty cho User+Vendor; /vendor owner; /manager scope; /admin global; /shipper thêm. Cookie JWT và CSRF cho form/AJAX; API không trả Entity. Auth profile chưa hiện thực không tự đánh dấu hoàn tất. Tiền/callback phải đi qua Service, callback chỉ mở khi có handler verifying signature và review CSRF exception.

Socket /ws same origin, handshake cookie JWT, CSRF CONNECT; /user/queue/orders|notifications own, /topic/stores/{id}/orders theo store scope, /topic/admin/orders Admin. Không client SEND đổi đơn; kiểm session/token mỗi message; defaultfailclosed scope. Khi hết hạn/khóa/đổi quyền cần đóng kết nối, không chỉ kiểm CONNECT. Reconnect/polling đọc trạng thái từ DB; không PII lên topic.

## 5. Thay đổi trên main

Chỉ main chốt pom/config/layout/contracts/migration. Feature đề xuất đổi kèm lý do/UC/bảng/file/test rồi review; sau merge mọi nhánh lấy main. Không dùng update/create để chữa lỗi schema, không đổi project_idv, không mang Boot4/Java26/Thymeleaf/MongoDB/Socket.IO từ ví dụ vào Boot3/JSP.

| Ngày | Main commit | Cấu hình/contract đổi | Nhánh affected | Reviewer | Đồng bộ |
| --- | --- | --- | --- | --- | --- |
| 13/10/2026 | [điền commit sau chốt] | Baseline MySQL/TiDB, teacher roles/OTP/VNPAY/return/promo | Cả ba | [điền] | [điền] |

## Bổ sung nền chung 14/10/2026

SiteMeshConfig đặt decorator prefix rỗng trước mapping servlet path đầy đủ; tránh mặc định /WEB-INF/decorators/ ghép trùng đường dẫn. BaselineViewTest kiểm HTTP thật qua embedded Tomcat/JSP/SiteMesh; test security MockMvc riêng vẫn giữ nguyên. Ba BRANCH_*.md là file chung để scope và cấu hình cập nhật cùng main. 58 hồ sơ UC là đặc tả, chưa phải nghiệp vụ chạy; migration mới và contract cụ thể phải review trên main.
