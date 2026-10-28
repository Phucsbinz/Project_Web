# Chạy và bàn giao feature/order-payment

> Mới nhất 21/10: order đã ghép catalog/identify, dùng auth, ledger và schema chung. Đọc [contract, lệnh Windows và kiểm chứng tích hợp](INTEGRATION_20261021.md) trước; các mô tả baseline ban đầu bên dưới là lịch sử.

Nhánh triển khai: `feature/order-payment`, baseline trước code `bb700d1`. Giữ Java 21, Maven Wrapper 3.9.16, Boot 3.5.13 WAR/JSP/JSTL/SiteMesh/Bootstrap, JPA và cấu hình MySQL theo main. Không đổi pom, datasource cloud, secret hoặc profile production để demo.

## Build và kiểm thử

```bash
bash mvnw -B -ntp clean verify
```

Trong cloud hiện tại:

```bash
JAVA_HOME=/workspace/onboarding-utetra/jdk21 \
MAVEN_USER_HOME=/workspace/.m2 \
MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
bash mvnw -s /workspace/onboarding-utetra/settings.xml -B -ntp \
  -Dmaven.repo.local=/workspace/.m2/repository clean verify
```

H2 chỉ là test isolated. Schema test tham chiếu ở `src/test/resources/order/reference-schema.sql`; fixture có tài khoản/phiên/role/address/shop/variant/topping/promo/commission/carrier/rates. Không seed tài khoản demo vào cloud. Test helper chỉ cho reset H2 in-memory utetra_* hoặc MySQL localhost utetra_order_* có `ORDER_MYSQL_TEST_ALLOW_RESET=true`; từ chối DB production.

## Dữ liệu cần ghép để chạy nghiệp vụ thật

V1 chỉ có roles/users/auth_sessions. V2 nhánh này tạo 19 bảng own; đọc [SCHEMA_PROPOSAL](SCHEMA_PROPOSAL.md) trước khi ghép version migration. Các module khác cần cung cấp:

- Catalog: stores (owner/status/accepting_orders/giờ/commission_policy_id), store_delivery_areas, categories/products/product_variants/toppings/product_toppings/store_toppings, promotions/promotion_products.
- Identity: user active + email_verified_at, role và auth_session hiện tại; user_addresses; store_memberships; loyalty_accounts/loyalty_transactions; audit_logs.
- Order: policy hoa hồng hoạt động gắn vào shop; carrier và carrier_area_rate hoạt động cho khu vực chọn.

`CommerceReferenceRepository` là adapter SQL theo DATABASE_DESIGN, không có HTTP CRUD catalog/identity. `OrderLoyaltyBridge` là adapter điểm trong transaction đơn (redeem/refund/earn/reversal/debt), cần review với owner identity và thay bằng contract chung khi owner cung cấp; không ghép hai implementation ledger cùng lúc. Không dựng catalog/auth giả trong production chỉ để chạy nhánh.

Các mutation order dùng READ_COMMITTED và khóa hàng. Contract ghép cần thống nhất thứ tự user/cart/store/catalog/carrier/rate/promo và account; MySQL REPEATABLE READ mặc định đã gây đọc snapshot cũ khi chờ khóa trong checkout đồng thời, test native đã bắt và sửa. Program quota dùng current locking read trước reserve. Catalog chỉnh sản phẩm/phí/khuyến mãi cần theo cùng lock contract để quote/checkout đối chiếu chính xác.

## Config của chức năng

`order.return-window=3h`: đúng mốc delivered_at + 3 giờ bị từ chối; hoàn cả đơn. COD bắt đầu unpaid, người có quyền phải xác nhận đã thu đủ tiền khi giao; refund COD chỉ được ghi sau khi thực sự trả tiền, có tham chiếu chứng cứ. App ghi nhận đối soát tiền mặt, không tự chuyển tiền ngân hàng.

`features.shipper.enabled=true` trong cấu hình chung của nhánh order từ 28/10/2026 theo yêu cầu hoàn thiện Shipper. Có thể override false tại môi trường không triển khai giao hàng. Quy trình cấp role, phân công, COD và thống kê: [SHIPPER_COMPLETION_20261028.md](SHIPPER_COMPLETION_20261028.md). Shipper đã được phân công chỉ lấy/giao sau CONFIRMED, không tự hủy/hoàn tiền.

VNPAY giữ disabled và chưa mở lựa chọn checkout theo ORD-AI-002. Khi người dùng cho làm tiếp cần merchant sandbox, secret trong cấu hình riêng, Return/IPN HTTPS và test signed callback/replay/amount. Không đưa hash secret vào prompt/log.

Cloudinary đã bật bằng tài khoản chung trong `src/main/resources/application.properties` và đã kiểm chứng upload/xóa ảnh PNG thật ngày 14/10/2026. Không sao chép khóa vào báo cáo/prompt log. Yêu cầu trả có lý do không cần ảnh; ảnh JPEG/PNG tùy chọn qua MediaService chung, tối đa 5 MB và kích thước 6000 px. Upload ngoài SQL, kiểm owner/hạn trước gọi provider và kiểm lại trong transaction; rollback xóa asset. Adapter kiểm kết quả xóa `ok` hoặc `not found`; lỗi xóa có log asset ID để đối soát thủ công, chưa có worker retry bền vững. Smoke provider xác minh adapter, chưa thay thế kiểm thử trả hàng với database và catalog/identity thật.

Security của nhánh dùng NullSecurityContextRepository: mỗi request xác thực JWT, không khôi phục authentication từ JSESSIONID. Bỏ SessionManagementFilter vì filter xem JWT mỗi request như login mới và xoay CSRF liên tục; giữ DisableEncodeUrlFilter, CookieCsrfTokenRepository và XOR CSRF. Filter order 0 materialize cookie trước SiteMesh order 1. Guest vẫn dùng HTTP session cho giỏ. Owner identity phải xoay CSRF tại login/logout thực; login/OTP không được triển khai trong nhánh này.

## Chạy website sau tích hợp

Theo IDN-AI-003, runnable demo/harness và script đăng nhập test đã gỡ. Chạy WAR mysql bằng `python tools/start_web.py`; không seed user/order vào DB thật. Xem [báo cáo tích hợp](../identity-membership/INTEGRATION_20261020.md). Test fixtures trong JUnit vẫn chỉ dùng database isolated.

## MySQL native riêng

Dùng DB **localhost** tên utetra_order_* riêng, không dùng utetra_db hay DB project khác. Bind datasource URL/user/password trong process test (không commit secret test), driver MySQL, dialect MySQL, ddl-auto=validate, flyway.enabled=true, locations=classpath:db/migration/mysql; thêm ORDER_MYSQL_TEST_ALLOW_RESET=true. Chạy `-Dtest=OrderCommerceIntegrationTest test`. Fixture xóa dữ liệu của DB test này giữa case; không chạy chung với dữ liệu cần giữ.

`MySqlMigrationSmoke` nhận ORDER_MYSQL_TEST_URL và ORDER_MYSQL_TEST_PASSWORD của DB MySQL disposable mới, chỉ cho localhost/utetra_order_*, chạy toàn bộ migration hiện có, replay 0 và JPA validate ba module; kiểm TLS cipher. Kết quả MySQL 8.4 cổng localhost 13306 của phiên đầu chỉ gồm V1+V2, chưa chạy lại helper disposable sau merge. Production TiDB vẫn VERIFY_IDENTITY.

## Routes và thao tác demo

- User/Vendor: giỏ → checkout quote → xác nhận COD → /orders/{id}; NEW được hủy có lý do; sau giao yêu cầu trả trước 3 giờ.
- Vendor: /vendor/orders; Manager: /manager/orders; Admin: /admin/orders. Confirm → picked up → delivering → delivered và thu tiền; review trả, nhận hàng hoặc waive, chứng cứ hoàn COD, xác nhận hoàn cả đơn.
- Manager/Admin: /manager/order-tools hoặc /admin/order-tools; carrier global chỉ Admin, phí khu vực Manager scope, hoa hồng shop hoặc Admin global.
- /vendor/revenue, /manager/reports, /admin/reports và CSV; /notifications cho tài khoản hiện tại; /shipper/orders và /shipper/reports khi bật flag.

Xem trạng thái từng UC ở [IMPLEMENTATION](IMPLEMENTATION.md). Cần review integration với owner catalog/identity, schema/FK ngoài module và kiểm TiDB/provider thật trước nghiệm thu toàn project. Không merge/push main hay nhánh thành viên khác trong lượt này.


## Bổ sung sau audit — 28/10/2026

Migration mới `V2026102802__catalog_configuration_versions.sql` thêm `version BIGINT NOT NULL DEFAULT 0` cho `categories`, `toppings`, `store_delivery_areas`; không sửa migration cũ hay xóa dữ liệu. Source entity dùng @Version. API đọc trả version, API cập nhật category/topping/area phải gửi version của bản form đã mở; thiếu hoặc cũ trả409 và cần tải lại. Tạo mới vẫn cho phép không gửi version. Phải đồng bộ migration + backend DTO/view/service + JS quản trị cùng nhau khi ghép nhánh. Lượt sửa này chỉ kiểm H2, không chạy migration lên DB cloud.

Không thay datasource/JWT/provider/layout hay tính khoảng cách. GLOBAL promo giữ sản phẩm đã chọn; lookup không sẵn sàng thì chặn lưu. `catalogKey` cho phép bỏ trống; backend vẫn kiểm cú pháp khi có giá trị. LOCAL_QR hủy đã trả tiền có dùng điểm theo cùng policy hoàn điểm như đơn trả trước khác. Kết quả RED/GREEN và giới hạn kiểm chứng xem `docs/reviews/AUDIT_REMEDIATION_20261028.md` (đường dẫn từ gốc project).
