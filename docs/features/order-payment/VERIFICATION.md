# Kiểm chứng order-payment — 14/10/2026

Kết quả dưới đây dùng dữ liệu test riêng, không phải giao dịch ngân hàng/Cloudinary hay dữ liệu TiDB production. Scope đã code: 20 UC bắt buộc + 3 Shipper có flag; UC-ORD-06 hoãn theo người dùng. [IMPLEMENTATION](IMPLEMENTATION.md) ánh xạ từng UC → symbol/test; [SETUP](SETUP.md) có cách chạy lại.

## Maven / WAR

`clean verify` với Java 21 / Maven Wrapper / proxy & truststore cloud: **51 tests, 0 failures, 0 errors, BUILD SUCCESS**.

| Bộ test | Số case | Nội dung |
| --- | ---: | --- |
| Baseline kế thừa | 16 | JWT, phiên hiện hành, security, JSP/layout |
| OrderCommerceIntegrationTest | 24 | Giỏ/merge/quote/idempotency/quota cạnh tranh, COD, cutoff 3 giờ, refund/reversal/debt, scope, policy/fees, notifications, JSP/CSRF |
| OrderSocketGuardTest | 3 | Thu hồi phiên/scope, lỗi lookup fail closed |
| ReturnEvidenceServiceTest | 5 | Lý do/quyền trước upload, disabled provider, asset server, rollback cleanup, provider failure (mock adapter) |
| OrderEventDeliveryTest | 3 | Transaction bắt buộc, rollback không phát socket, broker lỗi sau commit không biến purchase thành thất bại |

Artifact `target/utetra.war` build thành công; helper/fixture/token test không nằm trong production WAR. Không đổi pom hoặc phiên bản thư viện để làm test pass.

## MySQL thật riêng

MySQL 8.4 container riêng, localhost:13306, database utetra_order_native_test; JDBC TLS VERIFY_CA. **24/24 OrderCommerceIntegrationTest đạt trên MySQL**, gồm hai checkout cùng UUID và hai user tranh slot promotion cuối. Test reset chỉ cho DB localhost utetra_order_* có opt-in, không reset utetra_db.

DB mới utetra_order_final_smoke: Flyway áp dụng V1 và V2, chạy lại **0 migration**, Hibernate **ddl-auto=validate đạt**, session TLS cipher có giá trị. Marker: MYSQL_FLYWAY_TWO_MIGRATIONS_REPLAY_AND_JPA_VALIDATE_PASSED_WITH_TLS. Chưa chạy migration/kiểm chứng native TiDB thật.

## Trình duyệt và ảnh

Playwright 1.62.0 + Chromium headless, ứng dụng Spring/Tomcat/JSP chạy profile test. Script: [order-smoke.py](../../../src/test/browser/order-smoke.py); fixture: OrderSmokeHarness test-only. Đồng hồ cố định 14/10/2026 03:00 UTC, shop/role/address/giỏ và tiền COD là dữ liệu test.

Luồng kiểm: cart → quote 95.000đ → tạo COD → WebSocket STOMP queue của user → phân công Shipper → Vendor confirm → Shipper picked up/delivering/delivered và thu COD → buyer request return → Vendor approve/waive → ghi chứng cứ hoàn tiền mặt → REFUNDED. Kiểm không có JavaScript error và không tràn ngang ở 390px mobile, 768px tablet; desktop 1365px. Mở trang tools/report/notifications theo role và chụp ảnh thật.

- [Giỏ](assets/UC-ORD-03/ui-cart.png), [quote](assets/UC-ORD-03/ui-checkout-quote.png), [đơn mới](assets/UC-ORD-03/ui-order-created.png).
- [Yêu cầu trả](assets/UC-ORD-09/ui-return-request.png), [hoàn tiền](assets/UC-ORD-09/ui-refunded.png), [mobile](assets/UC-ORD-09/ui-refunded-mobile.png).
- [Shipper được phân công](assets/UC-SHP-01/ui-assigned-orders.png), [Shipper giao COD](assets/UC-SHP-02/ui-delivered-cod.png), [thống kê](assets/UC-SHP-03/ui-page.png).
- [Manager biểu phí](assets/UC-MGR-06/ui-page.png), [Manager báo cáo](assets/UC-MGR-09/ui-page.png), [Admin vận chuyển](assets/UC-ADM-06/ui-page.png), [Admin báo cáo](assets/UC-ADM-09/ui-page.png).
- [Vendor đơn](assets/UC-VND-04/ui-page.png), [Vendor doanh thu](assets/UC-VND-06/ui-page.png), [thông báo](assets/UC-ORD-10/ui-page.png).

## Phần còn cần ghép hoặc kiểm chứng

- Catalog/identity business code và schema ngoài module: adapter đã test theo DATABASE_DESIGN, nhưng integration với implementation thật của owner chưa chạy. Không sửa/push nhánh owner để giả hoàn thành toàn project.
- VNPAY merchant/Return/IPN và bank refund: hoãn theo ORD-AI-002; checkout hiện COD, không fake PAID từ redirect.
- Cloudinary thật: chưa có cấu hình; reason-only dùng được. Test upload/cleanup hiện mock MediaService; nếu cleanup thật lỗi cần đối soát asset thủ công, chưa có durable retry worker.
- Shipper flag production vẫn false theo baseline; bật rõ khi nhóm dùng phần thêm. Assignment hiện tại unique theo order; lịch sử đổi người nằm trong DeliveryEvent, báo cáo assignment theo bản hiện lưu.
- FK ngoài module và version migration cần review trước deploy integration; login/logout identity phải xoay CSRF ở ranh giới xác thực thực.
- Reviewer cá nhân, DOCX, ảnh source có callout và sơ đồ activity xuất riêng theo từng UC cần nhóm hoàn thiện; sơ đồ module SVG không thay toàn bộ hồ sơ nộp.
