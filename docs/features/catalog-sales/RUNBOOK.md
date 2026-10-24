# Catalog-sales — triển khai và bàn giao ngày 15/10/2026

> Bổ sung ngày 24/10/2026: xem [UPDATE_20261024](UPDATE_20261024.md) về điều hướng quản trị, hồ sơ shop, chọn policy có sẵn, phân trang shop và artifact schema chỉ gồm bảng catalog. Các lệnh/phiên bản/kiểm chứng bên dưới là lịch sử ngày 15/10, không thay kết quả chạy mới.

Đã bổ sung mã nguồn cho 18 UC của nhánh `feature/catalog-sales`. Kiểm chứng hiện tại trên H2 isolated và Tomcat thật; chưa nghiệm thu TiDB, Cloudinary thật hoặc luồng checkout xuyên nhánh. Không thay đổi V1, pom, properties, khóa JWT hoặc cấu hình provider. Không commit/push trong phiên này.

## Phạm vi có mã nguồn

| UC | Route giao diện | Xử lý chính |
| --- | --- | --- |
| CAT-01 | `/` khi Guest | Món bán >10 ly, giảm dần; chỉ đơn DELIVERED + PAID |
| CAT-02 | `/menu` | Tên/danh mục/shop/khoảng giá, sort allowlist, phân trang SQL |
| CAT-03 | `/` khi đăng nhập; `/menu?top=true&sort=...` | Bốn nhóm mới/bán chạy/đánh giá/yêu thích; tối đa 20, phân trang ổn định |
| CAT-04 | `/menu?categoryId=...` | Danh mục và shop hoạt động; trạng thái rỗng |
| CAT-05 | `/products/{id}` | Size, đường/đá, topping sẵn bán tại shop, giá server, số lượng 1–20 |
| CAT-06 | `/stores`, `/stores/{id}` | Chọn shop thủ công, thông tin/giờ/khu vực/phí giao nếu Order đã cung cấp bảng carrier |
| VND-01 | `/account/shop-registration` | User/Vendor gửi hồ sơ PENDING; duyệt mới cấp Vendor |
| VND-02 | `/vendor/shop` | Sửa thông tin/giờ/nhận đơn; ảnh và banner qua MediaService |
| VND-03 | `/vendor/products` | CRUD mềm món/size/topping; giữ ID variant và lịch sử |
| VND-05 | `/vendor/promotions` | Khuyến mãi SHOP, mã, giới hạn, thời hạn, sản phẩm áp dụng |
| MGR-02 | `/manager/products` | Quản lý món trong shop được giao |
| MGR-03 | `/manager/categories`, `/manager/toppings` | Cần quyền quản trị catalog được Admin cấp rõ ràng |
| MGR-05 | `/manager/promotions` | Chương trình giảm món/phí giao trong shop được giao |
| MGR-07 | `/manager/stores` | Thông tin/trạng thái/duyệt shop trong phạm vi; cấm tự duyệt |
| ADM-02 | `/admin/products` | Mọi shop; ngừng bán giữ lịch sử |
| ADM-03 | `/admin/categories`, `/admin/toppings` | Tạo/sửa/ngừng hoạt động dữ liệu chung |
| ADM-05 | `/admin/promotions` | SHOP hoặc GLOBAL; phân biệt PRODUCT_PERCENT/SHIPPING_PERCENT |
| ADM-07 | `/admin/stores` | Duyệt/từ chối/khóa/mở, phân công Manager và quyền catalog |

Các trang quản trị dùng chung `catalog/manage.jsp` và `catalog-management.js`; chọn chức năng và shop ở đầu trang. Các API có tiền tố `/api/vendor/catalog`, `/api/manager/catalog`, `/api/admin/catalog`. Security hiện có kiểm role ở route; service đọc lại role/active/verified từ DB và kiểm owner/membership. Không chỉ ẩn nút để phân quyền.

## Mã nguồn và transaction

- `entity/catalog`, `repository/catalog`: 13 entity và repository; FK scalar để không tạo entity bản sao của module khác. Không trả entity qua API. `CatalogQueryRepository` thực hiện lọc, tổng số, sort và LIMIT/OFFSET bằng SQL tham số hóa.
- `CatalogService`: danh mục/topping, sản phẩm/size/liên kết và báo giá. Giá dùng BigDecimal, VND nguyên đồng. Đường/đá chỉ 0/30/50/70/100; topping phải thuộc món, đang hoạt động và sẵn bán tại shop. Shop phải ACTIVE, nhận đơn và trong giờ phục vụ Asia/Ho_Chi_Minh.
- `StoreService`: hồ sơ, duyệt, trạng thái, khu vực và phân công. Duyệt kiểm version và chính sách hoa hồng active; đổi role sang Vendor và thu hồi tất cả phiên cũ cùng transaction. Chủ shop phải đăng nhập lại bằng luồng Identity sau duyệt.
- `CatalogGuard` và `CatalogStoreAccessPolicy`: Admin global, Vendor owner, Manager membership active. `member_role=CATALOG_ADMIN` là đề xuất quyền quản trị danh mục chung được cấp rõ ràng; `MANAGER` thông thường không được sửa dữ liệu dùng chung. Cả hai được vận hành đúng shop của membership. Chỉ Admin có API phân công.
- `PromotionService`: loại/scope/funding/code/time/quota/product eligibility; quote làm tròn xuống và chặn mức giảm vượt eligible/max. Vendor/Manager không tạo GLOBAL hoặc chuyển scope khi sửa. Không sửa snapshot đơn cũ.
- `CatalogMediaService`: kiểm quyền trước upload, gọi provider ngoài transaction DB, kiểm quyền/version lại khi gắn ảnh; rollback dọn ảnh mới, commit xong mới dọn ảnh cũ. Không nhận public_id hoặc URL do client tự khai. Cleanup provider thất bại được log và cần xử lý lại; chưa có hàng đợi retry bền vững.
- `audit_logs`: chỉ ghi actor/action/entity/ID và thông điệp cố định; không lưu raw request, token hoặc credentials. Mapping tối thiểu chờ phối hợp owner Identity.

Product/store/promotion kiểm version, trả 409 khi dữ liệu cũ. Khi sửa size/topping của product, parent được cập nhật để version tăng ngay cả khi tên/giá gốc không đổi. Category/topping theo schema gốc không có version. Ngừng hoạt động giữ nguyên bản ghi và ID đã được tham chiếu.

```mermaid
sequenceDiagram
    actor Actor as Vendor / Manager / Admin
    participant UI as JSP + JS
    participant C as CatalogManagementController
    participant S as CatalogService / StoreService / PromotionService
    participant G as CatalogGuard
    participant DB as Repository + DB
    Actor->>UI: Chọn shop, nhập dữ liệu
    UI->>C: JSON + JWT cookie + CSRF
    C->>S: DTO đã validate + principal
    S->>G: Role hiện tại, owner / membership
    G->>DB: Kiểm active / verified / scope
    alt Không có quyền hoặc input/version sai
        S-->>C: 400 / 401 / 403 / 404 / 409
        C-->>UI: Thông báo; transaction rollback
    else Hợp lệ
        S->>DB: Ghi dữ liệu + audit trong transaction
        S-->>C: DTO
        C-->>UI: Kết quả; tải lại dữ liệu và version
    end
```

## Schema cần main duyệt

[proposals/catalog-sales.sql](proposals/catalog-sales.sql) là đề xuất để nhóm trưởng đặt version Flyway duy nhất; không nằm trong thư mục auto migration. V1 được giữ nguyên. Script gồm các bảng catalog, cùng định nghĩa prerequisite commission_policies/store_memberships/audit_logs để owner Order/Identity đối chiếu và tách ghép, tránh tạo trùng.

Điểm cần review: `stores.commission_policy_id` nullable khi PENDING_APPROVAL, bắt buộc khi ACTIVE; hồ sơ chưa duyệt không tự được gán tỷ lệ hoa hồng. Description giới hạn 4.000 ký tự trong DTO/entity/DDL. Quyền `CATALOG_ADMIN` cần owner Identity thống nhất. Giữ utf8mb4, DATETIME(6), FK/UQ/CHECK, index, không cascade xóa lịch sử.

**WAR trên profile mysql hiện chưa thể khởi động chỉ với V1 cũ**, vì JPA validate yêu cầu bảng mới. Khi tích hợp: main review/đặt version migration, đối chiếu những bảng owner khác đã có, chạy trên database `utetra_db`, kiểm Flyway/JPA validate/TLS rồi mới nghiệm thu. Không đổi datasource hoặc dùng ddl-auto=update/create để chữa thiếu bảng. Kiểm SQL đề xuất trên H2 chỉ xác minh cấu trúc quan hệ, không chứng minh dialect TiDB.

## Contract cho Identity và Order

| Contract | Có sẵn | Trách nhiệm module còn lại |
| --- | --- | --- |
| `CatalogService.quote(productId, DrinkInput)` | Giá server từ variant/topping thực, product/store version | Order gọi lại trong transaction checkout và đối chiếu giá/options; không tin amount từ browser |
| `StoreService.publicStore`, `deliveryRates` | Shop/area; bảng phí đọc từ carriers/carrier_area_rates nếu có | Order tạo carrier/rate/commission; checkout kiểm khu vực/shop và snapshot phí |
| `PromotionService.quote(promotionId,storeId,Map<productId,lineAmount>,shipping)` | Eligibility và số giảm; dữ liệu số tiền chỉ từ service Order đã định giá | Order khóa promo/quota, kiểm usage/per-user, reserve/use/release redemption cùng transaction đơn; tối đa 1 promo hàng + 1 phí giao |
| `StoreAccessPolicy.canOperate` | Ownership/membership từ DB | Identity quản lý session/role hiện tại; WebSocket kiểm session trước kiểm scope |
| `MediaService` | Adapter baseline; shop/product upload có transaction cleanup | Bật Cloudinary chỉ khi có credential thật; không báo provider thành công khi disabled |
| Top/bán chạy/đánh giá/yêu thích | Đọc dữ liệu owner qua schema thống nhất | Chưa có bảng Order/Identity thì số liệu = 0; không seed doanh số giả |

Giới hạn trên là dependency xuyên nhánh. Kiểm thử H2 dùng bảng owner tối thiểu chỉ trong test; không phải triển khai Order/Identity thay các thành viên đó. Chưa có màn hình login thật/OTP trên baseline này; bảo vệ route vẫn giữ nguyên, không thêm đăng nhập giả hoặc tài khoản Admin mặc định.

## Chạy kiểm tra và thao tác demo

```powershell
# JAVA_HOME trỏ JDK21 của máy; không thay pom xuống Java khác.
$env:Path=$env:JAVA_HOME+'/bin;'+$env:Path
.\mvnw.cmd -B -ntp verify
```

Trong môi trường phiên này wrapper báo `Cannot index into a null array`, nên dùng Maven 3.9.16 cài sẵn, cùng pom và JDK21. JDK Oracle 21.0.12.1 được tải vào target và xác minh SHA-256; target không được Git theo dõi. Maven cache dùng đường dẫn của máy, không thay config repository:

```powershell
$env:JAVA_HOME=(Resolve-Path 'target/jdk21/jdk-21.0.12.1').Path
$env:Path=$env:JAVA_HOME+'/bin;'+$env:Path
mvn -B -ntp '-Dmaven.repo.local=C:/Users/nchie/.m2/repository' verify
```

Sau khi migration main đã được duyệt: User đăng nhập → gửi hồ sơ shop → Admin chọn shop, duyệt và nhập ID policy hoa hồng đang hoạt động → chủ shop đăng nhập lại → Vendor cập nhật giờ/nhận đơn → Admin hoặc Manager được cấp catalog tạo danh mục/topping → Vendor cấu hình topping sẵn bán, tạo món và size → tạo khuyến mãi → Guest kiểm `/menu`, `/stores`, `/products/{id}`. Dữ liệu thử nghiệm trong các test dùng email `.test`, không phải tài khoản cloud.

## Kiểm chứng thực

Ngày 15/10/2026: `mvn ... verify` bằng JDK21: **37 tests, 0 failure, 0 error, WAR BUILD SUCCESS**. Log tạm tại `target/catalog-final-verify.log`, báo cáo XML tại `target/surefire-reports`; có thể tái tạo bằng lệnh trên.

| Bộ kiểm tra | Số test | Bằng chứng |
| --- | ---: | --- |
| BaselineSecurityTest | 6 | Role/CSRF/auth |
| BaselineViewTest | 3 | JSP/SiteMesh/API/assets thật |
| JwtServiceTest | 5 | Verify/claims/token |
| SessionIdentityServiceTest | 2 | Phiên/quyền hiện tại |
| CatalogIntegrationTest | 14 | Duyệt, scope, giá, option, soft disable, version, giờ phục vụ, khuyến mãi, top20, API |
| CatalogViewTest | 3 | Tomcat thật: menu/detail/shop, home bốn nhóm, đăng ký và ba layout quản trị; escape XSS; tạo danh mục bằng cookie CSRF thật |
| CatalogSchemaProposalTest | 1 | V1 + proposal trên H2 isolated, FK/check; không xác nhận TiDB |
| CatalogMediaServiceTest | 3 | Provider lỗi, cleanup rollback, xóa ảnh cũ sau commit; mock provider |

Node syntax check đạt cho `catalog.js` và `catalog-management.js`. Chưa có bằng chứng Cloudinary upload thật, TiDB migrations/JPA validate hoặc cạnh tranh checkout/quota đa request. Review nhóm, commit, screenshot và báo cáo DOCX phải dựa trên bằng chứng thực khi bàn giao, không coi đặc tả cũ hoặc test mock là nghiệm thu cloud.

## Kiểm tra trình duyệt và ảnh thật

Đã chạy Edge headless trên demo H2 tại localhost, viewport desktop 1440px và mobile 390px, ngày 15/10/2026. Đã tạo danh mục “Đồ uống mới” bằng form quản trị; báo giá 2 ly size S không topping = 60.000đ; không có exception JavaScript chưa xử lý trong luồng capture. Dữ liệu/phiên Admin là fixture test, không phải tài khoản đăng nhập thật hoặc dữ liệu TiDB. Chưa chụp riêng từng màn hình lỗi và mọi role; test Tomcat đã render cả ba layout quản trị.

| Ảnh | Nội dung |
| --- | --- |
| [Menu desktop](assets/ui/menu-desktop.png) | Bộ lọc và thẻ sản phẩm |
| [Menu mobile](assets/ui/menu-mobile.png) | Bố cục một cột trên 390px |
| [Chi tiết mobile](assets/ui/product-mobile.png) | Size, đường, đá, topping |
| [Báo giá mobile](assets/ui/product-quote-mobile.png) | Hai ly size S, kết quả 60.000đ từ server |
| [Trang shop](assets/ui/shop-desktop.png) | Thông tin, giờ, khu vực, khuyến mãi, thực đơn |
| [Quản trị danh mục](assets/ui/admin-categories-desktop.png) | Dữ liệu vừa tạo qua form, thông báo thành công |
| [Quản trị sản phẩm](assets/ui/admin-products-desktop.png) | Món, size, topping, ngừng bán |
| [Quản trị khuyến mãi](assets/ui/admin-promotions-desktop.png) | Điều kiện/quota/thời gian/sản phẩm |
| [Quản trị shop](assets/ui/admin-shops-desktop.png) | Hồ sơ và cấu hình shop |

Hai lỗi phát hiện khi kiểm trình duyệt đã sửa: `LocalTime` qua cấu hình JDBC UTC có thể bị lệch khi đọc TIME từ SQL; mapping trực tiếp `SqlTypes.LOCAL_TIME` giữ giờ cửa hàng, có test hồi quy. Token CSRF trong HTML có thể cũ so với cookie; JS lấy masked token hiện tại từ `/products/csrf` trước mỗi request ghi, vẫn giữ CSRF; test HTTP cookie thực xác minh tạo danh mục thành công.

Fixture tái tạo giao diện tại [demo/catalog-demo-h2.sql](demo/catalog-demo-h2.sql), **chỉ cho H2 disposable**. Không có password dùng để đăng nhập, không tạo account cloud. Lệnh đã dùng, sau khi verify và trỏ JAVA_HOME tới JDK21:

```powershell
mvn -B -ntp '-Dmaven.repo.local=C:/Users/nchie/.m2/repository' spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=test' '-Dspring-boot.run.arguments=--spring.config.additional-location=file:./src/test/resources/ --server.port=8087 --server.address=127.0.0.1 --spring.jpa.defer-datasource-initialization=true --spring.sql.init.mode=always --spring.sql.init.data-locations=file:docs/features/catalog-sales/demo/catalog-demo-h2.sql'
```

Mở `http://127.0.0.1:8087/menu` để xem dữ liệu demo; các trang quản trị vẫn cần JWT/session hợp lệ. Capture dùng test-issued JWT cho session fixture, không cung cấp đường vòng đăng nhập trong ứng dụng. Bỏ qua `additional-location` sẽ không nạp test properties trong lần chạy plugin trên máy này; đã gặp vòng phụ thuộc Flyway/JPA, đã sửa lệnh chạy, không sửa properties thật. Dịch vụ demo và browser chỉ dùng tạm để kiểm tra.
