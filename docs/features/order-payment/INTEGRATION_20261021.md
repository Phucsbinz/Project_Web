# Đồng bộ order với catalog và identity — 21/10/2026

## Nguồn code và kiến trúc

Thực hiện trên feature/order-payment. Đã đọc catalog 812e26e và identity 78facd2 trên nhánh identify; identify đã chứa catalog và baseline order daf5979. Lưu sửa ảnh trả hàng thành 5882d52 rồi merge identify bằng 28e8c18, giữ cả lịch sử prompt khi giải quyết xung đột tài liệu. Không cập nhật remote trong lượt này.

Ứng dụng vẫn là Spring MVC 3 tầng, JSP/JSTL + SiteMesh render trang. JavaScript gọi endpoint JSON trong cùng ứng dụng cho giỏ/checkout/vận hành; không tạo frontend/backend thứ hai.

## Contract dùng chung

| Phần | Contract đã ghép |
| --- | --- |
| Catalog | CommerceReferenceRepository đọc stores/products/variants/toppings/categories; danh mục tắt không được mua qua giỏ cũ. Giá và quota được kiểm lại khi checkout, snapshot giữ trên đơn |
| Auth | Login thật phát JWT cookie dùng được với order; CSRF chung và xoay ở login/logout. Accept JSON nhận 401 khi hết phiên, HTML chuyển login; JavaScript không coi HTML login là thao tác thành công |
| Scope | Vendor owner, membership MANAGER/CATALOG_ADMIN đang hoạt động, Admin; order và thông báo dùng cùng phạm vi với policy identity/catalog, không cấp shop khác |
| Địa chỉ | Identity sở hữu user_addresses; checkout kiểm owner và snapshot vào đơn |
| Điểm | OrderLoyaltyBridge chỉ gọi LoyaltyService trong transaction bắt buộc, flush snapshot trước ledger; không giữ ledger SQL độc lập của order |
| Mua hàng | DELIVERED + PAID được identity nhận diện tại reviews/purchases; không dựng chứng cứ mua giả |
| Media | MediaService chung; giữ kiểm JPEG/PNG thật, decode/5 MB/6000 px, phản hồi xóa và cleanup nếu tạo yêu cầu trả thất bại |
| JSON | DTO order dùng StrictNumbers của identity để chặn ID/số lượng dạng chuỗi hoặc thập phân; client đọc được cả error và message |
| Khóa | Khóa riêng users rồi đọc role, tránh khóa roles dùng chung cho nhiều buyer; giữ checkout READ_COMMITTED, idempotency và quota trong transaction |

## Kiểm chứng thực tế

- JDK 21.0.12.1 + Maven Wrapper: **clean verify thành công, 130 tests, 0 failure/error/skipped**, WAR đóng gói/repackage thành công; đối chiếu Surefire XML.
- Thêm 5 test contract trong OrderCommerceIntegrationTest: HTTP login BCrypt thật → giỏ khách → merge → COD → ledger → món đã mua → trả/hoàn COD; JSON 401/HTML 302 khi revoke; JSON số sai định dạng; danh mục tắt; membership CATALOG_ADMIN giữ scope.
- Flow ledger kiểm balance 100 → 98 sau redeem → 105 sau giao → 100 sau hoàn; đúng 4 loại REDEEM/EARN/REFUND/REVERSAL. Đây là dữ liệu test isolated, không phải giao dịch khách thật.
- orders.js/catalog.js/catalog-management.js qua node --check; helper Python qua py_compile.
- WAR profile mysql khởi động tại localhost:8083, kết nối **utetra_db**, Hibernate validate đạt; Flyway validate đủ **6 migration**, báo schema up to date và **không chạy migration mới**.
- Read-only HTTP trên WAR thật: /api/system/health, /, /menu, /stores, /cart, /auth/login, /assets/js/orders.js đều 200. Health là JSON, trang là HTML; không seed hoặc tạo user/order vào database chung. Đã dừng đúng PID WAR sau kiểm chứng.
- Helper MySqlMigrationSmoke được cập nhật cho toàn bộ migration và entity của ba module, chỉ nhận DB disposable localhost/utetra_order_*. Đã compile và chạy lại IntegratedMigrationTest; chưa chạy helper trên MySQL disposable mới.

Lỗi thực khi chạy WAR: build cũ còn class policy order đã bị xóa sau merge, gây hai bean primary. Clean build loại class dư và WAR chạy lại thành công. Script verify mặc định dùng clean verify; không chữa lỗi bằng cách giữ hai policy cùng hoạt động.

## Chạy trên máy thành viên

Đặt JAVA_HOME hoặc UTETRA_JAVA_HOME tới JDK 21 của máy; cũng hỗ trợ .tools/jdk21. Helper không còn bắt buộc đường dẫn riêng của người viết identity, và dùng Maven Wrapper trong Git. Python chỉ là công cụ hỗ trợ.

```powershell
# Thay ví dụ bằng đúng thư mục JDK 21 trên máy.
$env:JAVA_HOME = 'C:\Java\jdk-21'
py -3 tools/run_verify.py
py -3 tools/start_web.py 8080
# http://localhost:8080
py -3 tools/stop_web.py
```

Có thể dùng mvnw.cmd -B -ntp clean verify và java -jar target/utetra.war trực tiếp với Java 21. Log helper nằm trong .local; target, log, Python bytecode và JDK tải về không được đưa vào Git.

## Giới hạn cần bàn giao

- VNPAY vẫn disabled theo quyết định nhóm; Shipper production false, test bật riêng.
- Máy này chưa có .local/mail.properties. Gửi OTP thật cần cấu hình SMTP theo [MAIL_SETUP](../identity-membership/MAIL_SETUP.md); code/template mail đã ghép, không chép App Password vào tài liệu.
- Chưa tạo đơn/refund thật trên TiDB; đã kiểm nghiệp vụ xuyên module trong H2 và khởi động/read-only trên TiDB.
- Upload/delete Cloudinary thật đã kiểm ngày 14/10, chưa chạy lại provider trong lượt 21/10. Cleanup lỗi vẫn cần đối soát thủ công, chưa có durable retry worker.
- Chưa chạy visual QA hoặc chụp bộ ảnh UI mới ngày 21/10. Ảnh cũ thuộc các phiên trước; DOCX/reviewer vẫn do nhóm hoàn thiện.
