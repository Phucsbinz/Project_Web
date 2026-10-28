# UTETra — Full Project Audit, 28/10/2026

> Cập nhật sau audit: bốn lỗi đã được sửa trong working tree nhánh order, có RED/GREEN và Maven 223 tests đạt. Xem [AUDIT_REMEDIATION_20261028.md](AUDIT_REMEDIATION_20261028.md). Báo cáo bên dưới giữ nguyên bằng chứng tại HEAD audit; không đồng nghĩa migration đã áp dụng lên cloud.

## Kết luận

**Chưa nên chốt toàn bộ project là hoàn chỉnh: có 4 lỗi mức Trung bình đã có bằng chứng tái hiện cần xử lý.** Chưa xác nhận lỗi mức Nghiêm trọng; không bổ sung finding Nhẹ nếu chưa có tác động rõ ràng.

Bộ kiểm thử hiện có vẫn đạt **214 tests, 0 failures, 0 errors, 0 skipped**. Shared UI checker đạt **một SiteMesh layout, 21 file dùng chung**. Điều này không mâu thuẫn với các lỗi bên dưới: các tình huống này chưa được bộ test hiện có bảo vệ đầy đủ.

Phạm vi là toàn bộ source tại `feature/order-payment`, HEAD `269ea2c8c6d434078ca96846527dcd3d47a6873e`, bao gồm các chức năng đã tích hợp, không chỉ commit Shipper. Bốn điểm lỗi đã có trong base `9d6b553` trước thay đổi Shipper; không quy tất cả lỗi cho commit Shipper vừa làm. Kết luận review Shipper trước đó chỉ áp dụng cho phạm vi đó.

Không sửa mã nguồn/cấu hình, không chạy migration hoặc ghi dữ liệu lên DB cloud, không commit/push trong đợt audit. Chỉ thêm báo cáo, minh chứng đã loại thông tin nhạy cảm và nhật ký prompt.

## Phương pháp và kiểm tra tự động

Áp dụng các skill local trong `D:\WEBCUOIKI\.agents\skills`:

- `requesting-code-review`: reviewer độc lập với context riêng; một người đọc security/roles/identity/support, một người đọc catalog/store/promotion và concurrency. Reviewer chỉ đọc, không tự gọi reviewer khác.
- `systematic-debugging`: đọc luồng dữ liệu, xác định giả thuyết, tạo ca tái hiện tối thiểu và ca đối chứng trước khi đề xuất sửa.
- `verification-before-completion`: lấy kết quả chạy mới, kiểm exit code và kết quả Surefire trước khi kết luận.
- `dispatching-parallel-agents`: chia hai phạm vi đọc code độc lập. Điều phối kiểm checkout/group/payment/transaction và thực hiện các probe.

Các lệnh được chạy mới trong lượt audit, sau khi chọn JDK 21 trên máy:

```powershell
.\mvnw.cmd test
py -3 tools/check_shared_ui.py
```

- Maven exit `0`, `BUILD SUCCESS`; Surefire 214/0/0/0.
- Shared UI exit `0`, một layout và đủ 21 file chung.
- `git diff --check` đạt tại thời điểm tổng hợp.
- Không chạy lại `package/verify` trong lượt audit nên không tuyên bố có bản WAR build mới từ lượt này.
- Các probe bổ sung chạy trên H2 cô lập: 3 ca đối chứng nghiệp vụ QR/điểm và 3 ca catalog qua API/browser Tomcat/SiteMesh. Đây là kiểm tra audit tạm thời, chưa được thêm vào bộ 214 tests.

Minh chứng: [verification.json](full-audit-20261028/verification.json), [Maven summary](full-audit-20261028/maven-test-summary.txt), [shared-ui.log](full-audit-20261028/shared-ui.log), [business-repro.json](full-audit-20261028/business-repro.json), [catalog-repro.json](full-audit-20261028/catalog-repro.json).

Log nguyên bản và chương trình tái hiện tạm thời nằm trong `.local/audit-20261028/` trên workspace này. Không đưa log đầy đủ có thể chứa thông tin phiên/cấu hình vào báo cáo.

## Các lỗi cần sửa

### AUDIT-01 — Sửa tên khuyến mãi GLOBAL làm mất tập sản phẩm áp dụng

**Mức độ: Trung bình. Ưu tiên sửa đầu tiên do có thể mở rộng phạm vi giảm giá ngoài ý muốn.**

Vị trí chính:

- `src/main/resources/static/assets/js/catalog-management.js:178` và `:204`: không chọn shop thì `productList=[]`.
- Cùng file `:50`: select sản phẩm lấy options từ `productList`; `:185`: submit lấy tất cả giá trị đang được chọn.
- `src/main/java/com/utetra/service/PromotionService.java:56`: xóa các liên kết cũ trước khi ghi bộ sản phẩm từ request.
- `src/main/java/com/utetra/order/service/OrderPromotionService.java:84`: chỉ giới hạn sản phẩm khi tập liên kết không rỗng; rỗng được hiểu là toàn phạm vi.

Tái hiện đã chạy:

1. Admin tạo GLOBAL promotion qua API với `productIds=[1]`.
2. Mở `/admin/promotions`, chọn toàn hệ thống, mở chỉnh sửa chương trình.
3. Form có **0 option sản phẩm**, không thể thể hiện sản phẩm đang áp dụng.
4. Chỉ sửa tên rồi lưu. Server trả `200`; đọc lại được `productIds=[]`.

Nguyên nhân: trạng thái “lookup không có dữ liệu để hiển thị” bị chuyển thành ý định “áp dụng tất cả sản phẩm”. Backend xử lý đúng request rỗng theo contract hiện tại, nhưng UI đã làm mất ý định của Admin. Sau khi links bị xóa, luồng tính giảm giá hiểu là toàn bộ sản phẩm đủ điều kiện thay vì tập đã chọn. Việc xóa links được xác nhận runtime; ý nghĩa áp dụng toàn phạm vi được đối chiếu tại code tính giá.

Đề xuất sửa:

1. Cung cấp product lookup cho phạm vi GLOBAL của Admin, có thể tìm kiếm/phân trang theo chi nhánh.
2. Giữ bộ `selectedProductIds` độc lập với các option đang tải; ID đã chọn phải được giữ khi đổi trang lookup hoặc tải dữ liệu lỗi.
3. Nếu chưa biểu diễn được đầy đủ selection hiện tại thì chặn lưu và hiển thị lỗi; không gửi `[]` như một giá trị mặc định.
4. Làm rõ lựa chọn “Tất cả sản phẩm” và “Các sản phẩm được chọn”. Nếu đổi DTO để biểu diễn ý định này, cập nhật contract thống nhất; không phá cách dùng toàn phạm vi đang hợp lệ.

Test cần bổ sung: GLOBAL promo chỉ áp dụng P1; mở form sửa tên, lưu; assert links vẫn chỉ P1 và P2 không được giảm. Kiểm lookup lỗi, lookup phân trang, đổi phạm vi khi tạo mới và hành động cố ý chọn toàn bộ.

### AUDIT-02 — Không hủy được đơn LOCAL_QR đã thanh toán nếu có dùng điểm

**Mức độ: Trung bình. Lỗi tích hợp policy giữa order và loyalty; không phải lỗi vượt quyền.**

Vị trí chính:

- `src/main/java/com/utetra/identity/LoyaltyService.java:57`–`:59`: nhánh hủy chờ hoàn chỉ chấp nhận `BANK_TRANSFER`/`VNPAY`, bỏ sót `LOCAL_QR`.
- `src/main/java/com/utetra/order/service/OrderWorkflowService.java:370`, `:376`, `:380`: đơn QR đã thu tiền chuyển tạm sang `CANCELLED/REFUND_PENDING`, sau đó gọi hoàn điểm trong cùng transaction.
- `src/main/java/com/utetra/order/port/OrderLoyaltyBridge.java:14`: không gọi loyalty nếu số điểm bằng 0, giải thích vì sao các ca hủy QR không dùng điểm vẫn qua.
- `src/main/java/com/utetra/order/service/CheckoutService.java:166`: chỉ cấm điểm ở chế độ nhóm INDIVIDUAL; solo/HOST vẫn được dùng điểm.

Tái hiện đã chạy trên cùng fixture, trước khi cửa hàng xác nhận đơn:

- QR đã thanh toán, **0 điểm**: hủy thành công, đơn `CANCELLED/REFUND_PENDING`, số dư điểm 100.
- QR đã thanh toán, **2 điểm**: hủy thất bại với “Chưa đủ điều kiện hoàn điểm.”; rollback giữ đơn `NEW/PAID`, số dư còn 98.
- QR **chưa thanh toán**, 2 điểm: hủy thành công, đơn `CANCELLED/CANCELLED`, số dư phục hồi 100.

Ảnh hưởng: khách hoặc operator không thể thực hiện việc hủy được phép theo trạng thái đơn; stock/voucher và điểm cũng chưa được giải phóng vì toàn bộ transaction rollback. Không có bằng chứng tiền/điểm bị ghi dở dang trong probe; transaction rollback hoạt động đúng nhưng policy từ chối sai.

Đề xuất sửa:

1. Đồng bộ khái niệm “hủy đơn trả trước đang chờ hoàn” giữa order và loyalty, bao gồm `LOCAL_QR` ở đúng trạng thái `CANCELLED/REFUND_PENDING`.
2. Giữ giới hạn `type=REFUND`, đối chiếu giao dịch REDEEM gốc và tính idempotent; không nới điều kiện hoàn điểm cho mọi phương thức/trạng thái.
3. Nếu gom policy vào predicate/contract chung thì dùng lại ở cả hai module để tránh danh sách provider bị lệch.

Test cần bổ sung: solo QR và nhóm HOST dùng điểm, thanh toán rồi hủy trước CONFIRMED; điểm trả đúng một lần, stock/voucher được giải phóng và tiền chuyển chờ hoàn. Sau Admin xác nhận hoàn, không trả điểm lần thứ hai. Giữ các ca đối chứng chưa thanh toán/0 điểm và các provider khác.

### AUDIT-03 — Form cũ có thể bật lại topping/danh mục/khu vực vừa bị tắt

**Mức độ: Trung bình. Đây là ghi đè payload cũ, không chỉ là nhận xét thiếu annotation.**

Vị trí chính:

- `src/main/java/com/utetra/service/CatalogService.java:44`: ghi toàn bộ thuộc tính danh mục từ request.
- Cùng file `:49`: ghi tên/giá/active của topping từ request.
- `src/main/java/com/utetra/service/StoreService.java:98`: cùng mẫu ghi đè khu vực giao.
- `src/main/java/com/utetra/dto/CatalogRequests.java:16`, `:19` và `AreaInput`: không nhận expected version tương ứng.
- `src/main/java/com/utetra/dto/CatalogViews.java:9`–`:11`: view tương ứng không mang version. Các entity Category/Topping/StoreDeliveryArea chưa có `@Version`.

Tái hiện runtime với topping:

1. Lấy snapshot đang `active=true` để đại diện form B còn mở.
2. Request A tắt topping: trả `200`, `active=false`.
3. Request B sửa giá từ snapshot cũ, vẫn gửi `active=true`: tiếp tục trả `200`, topping bị bật lại.

Không cần hai transaction chạy đồng thời; hai tab hoặc hai người dùng lưu nối tiếp đã đủ. Trường hợp topping đã xác nhận qua API; trường hợp danh mục/khu vực là cùng mẫu code đã đối chiếu, chưa chạy thêm probe riêng cho hai entity đó.

Đề xuất sửa:

1. Thêm cột `version NOT NULL DEFAULT 0` bằng migration mới, thêm `@Version` cho các entity liên quan.
2. Đưa version vào DTO đọc/ghi; frontend giữ version của bản người dùng đã mở, không tự lấy version mới trước khi lưu payload cũ.
3. Service so sánh expected version rồi cập nhật; stale request trả `409` theo cơ chế hiện có. Giữ kiểm tra quyền catalog/store.
4. Ánh xạ optimistic locking conflict thành `409`, yêu cầu người dùng tải lại thay vì tự ghi đè.

**Chỉ thêm `@Version` chưa đủ**: nếu B gửi sau khi A đã commit, service sẽ tải version mới và vẫn chép payload cũ lên. Cần version từ chính snapshot của form B hoặc mutation có điều kiện tương đương.

Test cần bổ sung: mở snapshot A/B; A đổi active; B đổi giá/tên với version cũ phải nhận 409, trạng thái A giữ nguyên. Thêm ca hai transaction thật chạy đồng thời cho cả topping/category/area.

### AUDIT-04 — Mã món chung ghi tùy chọn nhưng bị HTML validation bắt nhập

**Mức độ: Trung bình. Chặn một luồng tạo/sửa sản phẩm được contract cho phép.**

Vị trí chính:

- `src/main/resources/static/assets/js/catalog-management.js:26`: nhãn `catalogKey` ghi “có thể để trống”.
- Cùng file `:60`: công thức đặt `required` chỉ miễn trường `code`, không miễn `catalogKey`.
- Contract cho phép mã rỗng: `docs/features/order-payment/BRANCH_SELECTION_20261026.md:17`.

Tái hiện trên form quản trị được Tomcat/SiteMesh render và JavaScript khởi tạo:

- `#manage-catalogKey.required === true`.
- Khi để trống, `validity.valueMissing === true` và `checkValidity() === false`.

Đây là lỗi trước khi submit tới API. Nó ảnh hưởng tạo món riêng không có mã chung, sửa món đang có mã NULL và xóa mã chung để tách món; không phải lỗi server từ chối một request hợp lệ đã gửi đi.

Đề xuất sửa: coi `catalogKey` là optional trong hàm dựng field; giữ validation cú pháp/độ dài/unique khi có giá trị. Giữ contract backend: rỗng có thể đổi về NULL, client cũ không gửi trường không vô tình xóa mã.

Test cần bổ sung: browser tạo/sửa món với mã rỗng, bỏ mã cũ, và API kiểm mã sai/trùng. Không bỏ validation toàn bộ form để né lỗi một trường.

## Security và role: kết quả rà soát

**Chưa xác nhận lỗ hổng IDOR hoặc endpoint vượt quyền trong các route/service đã rà.** Đây là kết luận có giới hạn, không phải chứng nhận tuyệt đối an toàn.

- `SecurityConfig.java:45`–`:49` phân vùng Admin/Manager/Vendor/Shipper và các trang profile/password dùng chung. Quyền menu không được xem là authorization.
- Guest: chỉ các chức năng public và giỏ guest; không có quyền gọi mutation tài khoản/đơn/CSKH cần đăng nhập.
- USER: profile/địa chỉ lấy từ principal; item giỏ kiểm đúng cart của người gọi; đơn kiểm owner hoặc participant snapshot của nhóm. Participant theo dõi không được trở thành chủ đơn để hủy/trả/hoàn.
- VENDOR: có chức năng mua của User; vận hành sản phẩm/cửa hàng/đơn theo owner cửa hàng tại service, không chỉ theo tiền tố URL.
- MANAGER: nghiệp vụ theo membership chi nhánh còn hiệu lực; quyền danh mục chung có điều kiện CATALOG_ADMIN; không tự suy diễn mọi Manager đều quản lý catalog toàn hệ thống.
- ADMIN: quyền quản trị toàn hệ thống; bảo vệ Admin hoạt động cuối cùng; xác nhận ngân hàng/hoàn trả trước chỉ ở phạm vi Admin theo contract.
- SHIPPER: đọc/thao tác theo assignment, chỉ bước giao được phép, mất quyền khi đổi/hủy phân công; hoàn thành chỉ đọc. Không có quyền tự refund hoặc quản trị tài khoản.
- Support: SQL ràng buộc customer/channel/branch và membership; không tạo chat trực tiếp giữa staff. Private queue không thay kiểm quyền đọc message qua service.
- JWT: còn kiểm trạng thái DB session, tài khoản active/verified, role hiện tại, access-token ID/fingerprint/expiry/revocation; không chỉ kiểm chữ ký token.
- Mật khẩu/OTP: BCrypt và Spring Security hiện diện; reset grant/refresh có khóa, điều kiện sử dụng và thu hồi phiên. Không dùng raw password làm dữ liệu đăng nhập phụ.
- WebSocket: matcher `authenticated()` còn được bổ sung bởi `WebSocketConfig.allowed()` và outbound guard; không kết luận hở `/topic/admin/orders` chỉ từ một dòng matcher.
- Payment: VNPAY IPN kiểm chữ ký/số tiền/idempotency; return trình duyệt không tự đánh dấu đã trả tiền. LocalQR là mô phỏng nội bộ theo contract, không phải bằng chứng ngân hàng.

Phạm vi reviewer đã đọc: inventory 17 controller classes và service/guard liên quan cho public/catalog/identity/account/community/cart/checkout/group/order/operations/payments/support. Inventory tĩnh gồm 172 file Java và 50 JPA entity classes; không đồng nhất số entity với số bảng vật lý đang có trên cloud.

## Transaction và chống xung đột dữ liệu

**Có áp dụng transaction/locks/version ở các luồng chính, nhưng chưa đồng đều ở các form cấu hình — xem AUDIT-03.** Không yêu cầu đặt `@Version` lên mọi bảng để đạt tiêu chí này.

- Checkout: `CheckoutService.java:263` dùng READ_COMMITTED; khóa user/cart và dữ liệu cần chốt, tính lại quote và kiểm hash tại `:281`, reserve stock tại `:285`; ghi order/items/payment/điểm/voucher trong transaction.
- Kho: `OrderInventoryService.java:22` khóa variant, update có điều kiện `stock_quantity>=quantity` và tăng version. NULL là tồn kho không giới hạn theo contract, không tự coi là dữ liệu thiếu.
- Voucher: `OrderPromotionService.java:52`/`:65` dùng locking read cho quota khi checkout chốt. Preview không giữ quota là chủ đích; đặt đơn mới reserve.
- Product/Variant/Store/Promotion: có `@Version` và các kiểm version phù hợp; cập nhật stock qua SQL cũng tăng version để không bỏ qua optimistic locking của catalog.
- Order/payment/assignment: có row lock/version và state machine, không chỉ dựa trạng thái frontend. Phần trả hàng/hoàn tiền kiểm điều kiện, quyền và retry trong transaction.
- Loyalty: `MANDATORY`, khóa user/account, ledger theo giao dịch gốc và xử lý debt; AUDIT-02 là lỗi policy chấp nhận provider, không phải thiếu transaction.
- Identity: register/login/refresh/reset/OTP và cập nhật tài khoản có transaction; native SQL dùng khóa/điều kiện/tăng version. Helper như revokeAll phải đọc trong call graph, không đánh dấu thiếu transaction chỉ vì helper không gắn annotation riêng.
- Upload: provider call nằm ngoài transaction DB; quyền/điều kiện được kiểm trước và ghi DB được kiểm lại trong transaction, có cleanup khi ghi thất bại. Đây là thiết kế có chủ đích, không phải mọi method upload đều phải giữ SQL transaction qua mạng.
- Nhật ký/sự kiện/media-link và các mutation phụ thuộc caller transaction không bắt buộc mỗi dòng phải có `@Version`; đánh giá theo invariant, unique key, khóa và call graph.

Inventory tĩnh có **22/50 entity classes chứa `@Version`**. Con số này dùng để định hướng đọc code, không phải tỷ lệ “đạt chuẩn”. Xem [source-inventory.json](full-audit-20261028/source-inventory.json). Không có kết luận rằng chỉ cần thêm annotation cho 28 entity còn lại là đúng thiết kế.

## Các nghi vấn đã kiểm và không ghi thành lỗi

- Snapshot promotion_products cũ do REPEATABLE READ: đã loại vì transaction chốt checkout chỉ định READ_COMMITTED, không áp dụng suy luận RR một cách máy móc.
- Race catalog_key chỉ dựa app precheck: schema đã có unique `(store_id,catalog_key)`, nên không báo lỗi thiếu unique.
- StoreTopping/membership thao tác đặt trạng thái không có version: chưa có trace tác động tương đương form nhiều trường để kết luận lỗi độc lập.
- Group invite token cho người giữ link xem thông tin nhóm trước khi tham gia: phù hợp capability của luồng mời, không tự gắn nhãn IDOR.
- Báo cáo Shipper dựa assignment hiện tại và COD gross: đã được ghi rõ trong contract/UI, không coi là sai nếu không có yêu cầu lịch sử phân công đầy đủ hoặc số dư sau hoàn.
- Operator vẫn cập nhật giao hàng: là quyền hiện có, không mâu thuẫn giới hạn của riêng Shipper.
- Giờ mở cửa không qua đêm, preview không reserve voucher, không tính khoảng cách: phù hợp contract/yêu cầu hiện tại.

## Giới hạn và rủi ro cần kiểm ở môi trường báo cáo

- Các probe dùng H2; chưa xác minh lock/deadlock/isolation, CHECK/FK enforcement và dữ liệu orphan trên MySQL/TiDB thật trong lượt này.
- Không gửi OTP thật, upload/xóa Cloudinary thật, nhận IPN merchant thật, nhận chuyển khoản hoặc thu COD thật. Không coi các integration test dùng fixture là xác nhận provider thực tế.
- Chưa chạy ma trận tấn công động cho mọi endpoint/role; đã đọc route/service và chạy suite hiện có, không tuyên bố “không thể có IDOR”.
- Cần kiểm HTTPS/reverse proxy và STOMP thực tế: cookie policy, CSRF CONNECT, giả destination người khác, membership bị thu hồi khi socket còn mở và session hết hạn. Rà tĩnh cho thấy có cơ chế bảo vệ; cấu hình triển khai vẫn cần kiểm.
- Rate limiter trong bộ nhớ chỉ có hiệu lực theo process; nếu triển khai nhiều instance cần kiểm/cấu hình cơ chế dùng chung. Không kết luận đây là bug của cấu hình chạy một instance hiện tại.
- Không thực hiện dependency CVE scan, pentest Internet, load test, audit DB 3NF, backup/restore hoặc triển khai trong phạm vi các lệnh được yêu cầu.
- Cấu hình provider/private-repo được giữ theo quyết định của chủ project; báo cáo không sao chép khóa/token/password. Lượt này không thay cách quản lý credentials hoặc xác nhận phạm vi người có quyền đọc repo/cloud.

## Thứ tự xử lý đề xuất

1. AUDIT-01: giữ đúng phạm vi sản phẩm của khuyến mãi GLOBAL khi chỉnh sửa.
2. AUDIT-02: thống nhất policy hoàn điểm khi hủy LOCAL_QR đã trả tiền.
3. AUDIT-03: bổ sung expected-version xuyên DB/entity/DTO/UI cho form cấu hình có nguy cơ stale overwrite.
4. AUDIT-04: sửa required của catalogKey và thêm browser regression test.
5. Chạy lại suite đầy đủ, checker UI và từng ca tái hiện sau sửa; chỉ ghi “đã sửa” khi ca cũ không còn tái hiện. Tiếp theo kiểm môi trường MySQL/TiDB báo cáo theo giới hạn ở trên.

Hiện các lỗi vẫn giữ nguyên để chủ project xem xét báo cáo; không có tuyên bố đã khắc phục.

## Prompt gốc

Prompt nguyên văn và lời nhắc về đường dẫn skill đã được ghi tại `docs/AI_PROMPT_HISTORY.md`, mục Full Project Audit 28/10/2026.
