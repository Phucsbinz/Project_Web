# Khắc phục bốn lỗi Full Project Audit — 28/10/2026

Phạm vi: `feature/order-payment`, working tree sau HEAD `269ea2c`. Căn cứ: [Full Project Audit](FULL_PROJECT_AUDIT_20261028.md) và yêu cầu chủ nhóm áp dụng systematic-debugging, TDD, verification-before-completion để sửa AUDIT-01..04. Không đổi layout, datasource, JWT hoặc provider; không thêm tính khoảng cách.

## Nguyên nhân gốc và cách sửa

### AUDIT-01 — GLOBAL promo mất sản phẩm áp dụng

Nguyên nhân: không chọn shop làm product lookup rỗng; UI biến việc không biểu diễn được selection thành `productIds=[]`, tức áp dụng toàn phạm vi.

Sửa trong `src/main/resources/static/assets/js/catalog-management.js`:

- Admin GLOBAL lấy sản phẩm qua các API quản trị theo từng shop đã được cấp quyền, không tạo API public hoặc nới quyền.
- Option của sản phẩm đã chọn nhưng không có trong lookup hiện tại vẫn được giữ bằng ID, không tự biến thành selection rỗng.
- Tạm khóa lưu và kiểm tra tại submit nếu lookup chưa thành công; dùng revision của lần tải để bỏ qua kết quả tải cũ.
- Vẫn cho phép Admin cố ý bỏ chọn toàn bộ để áp dụng toàn phạm vi theo contract hiện tại.

Regression browser đã kiểm sửa tên giữ `[1]`, cố ý đổi thành toàn phạm vi và lookup lỗi phải khóa lưu. Chưa có case browser riêng cho ID ngoài lookup hoặc đổi shop liên tiếp rất nhanh; reviewer đã đọc nhánh xử lý nhưng không coi đó là kiểm thử runtime của hai tình huống này.

### AUDIT-02 — QR trả tiền có dùng điểm không hủy được

Nguyên nhân: order coi LOCAL_QR đã thu tiền là hủy chờ hoàn, nhưng loyalty chỉ chấp nhận BANK_TRANSFER/VNPAY ở trạng thái đó. Số điểm bằng 0 đi tắt khỏi loyalty nên test hủy QR cũ không bắt được lỗi.

Sửa tối thiểu trong `src/main/java/com/utetra/identity/LoyaltyService.java`: thêm LOCAL_QR vào cùng điều kiện `CANCELLED/REFUND_PENDING`. Không bỏ kiểm giao dịch REDEEM gốc, loại hoàn điểm, trạng thái hoặc idempotency.

Hai integration tests mới kiểm solo và nhóm HOST dùng 2 điểm, trả QR rồi hủy trước xác nhận. Ca solo còn xác nhận Admin hoàn tiền sau hủy không cộng điểm lần thứ hai. Giữ nguyên transaction order/loyalty, quyền owner/operator và mô phỏng QR hiện có.

### AUDIT-03 — Stale form ghi đè cấu hình vừa thay đổi

Nguyên nhân: category/topping/area không mang version của bản đã mở từ API đọc đến request ghi. Service tải bản hiện hành rồi chép toàn bộ payload cũ lên; row lock/transaction đơn thuần không phát hiện hai form lưu nối tiếp.

Sửa xuyên suốt:

- Migration mới `src/main/resources/db/migration/mysql/V2026102802__catalog_configuration_versions.sql`: thêm `version BIGINT NOT NULL DEFAULT 0` cho categories/toppings/store_delivery_areas. Không sửa migration đã áp dụng hoặc xóa dữ liệu.
- Entity Category/Topping/StoreDeliveryArea có `@Version`; mặc định cột phù hợp cả dữ liệu cũ và fixture native SQL.
- CatalogViews trả version. CategoryInput/ToppingInput/AreaInput nhận expected version với validation số nguyên không âm.
- `CatalogGuard.expectedVersion` được gọi trước khi sửa entity hiện có: thiếu/null hoặc cũ trả 409. Frontend đã gửi `editing.version`, nay nhận được version thật từ API.
- Tạo mới vẫn có thể không gửi version; client cập nhật cũ không gửi version phải tải lại/nâng cấp form thay vì được phép ghi đè âm thầm.
- Không chỉ thêm annotation: kiểm expected version còn bảo vệ payload form cũ gửi sau khi transaction trước đã commit. Optimistic locking bảo vệ ghi cạnh tranh ở persistence layer, exception handler hiện có trả 409.

Bốn HTTP tests mới: stale topping/category/area và thiếu version khi cập nhật topping. Ba tests stale detached entity kiểm optimistic locking cho cả ba loại. Các ca detached chạy trong test transaction, **không phải phép thử hai transaction MySQL/TiDB độc lập chạy đồng thời**. Migration test chạy chuỗi SQL và kiểm ba cột version bắt buộc. Browser cũng xác nhận stale topping trả 409 và không bật lại dữ liệu đã tắt.

**Migration chưa chạy trên DB cloud trong lượt này.** Khi ghép/triển khai phải đồng bộ migration, entity, DTO/view, service và JS; để Flyway chạy theo quy trình setup của project. Không chỉ lấy một phần backend hoặc một file JS.

### AUDIT-04 — catalogKey tùy chọn nhưng bị required

Nguyên nhân: hàm dựng field chỉ miễn `required` cho code khuyến mãi, không miễn catalogKey, trái nhãn và DTO.

Sửa một điều kiện `required` trong catalog-management.js cho đúng trường catalogKey. Không bỏ validation của các trường khác; backend vẫn phân biệt không gửi trường (giữ mã hiện có) với chuỗi rỗng (xóa về NULL), kiểm cú pháp/unique khi có mã.

Browser regression kiểm mã rỗng hợp lệ, lưu sửa sản phẩm hiện có về NULL và tạo món riêng có mã NULL qua form thật.

## RED → GREEN thực tế

- Loyalty RED: hai ca mới thất bại do “Chưa đủ điều kiện hoàn điểm.”; GREEN: cả hai đạt sau khi sửa predicate.
- Catalog stale-form RED: bốn ca trả 200 thay vì 409; GREEN: cả bốn đạt.
- Optimistic persistence RED: ba ca stale merge không ném lỗi; GREEN: cả ba phát hiện OptimisticLockException.
- Browser RED: `Existing GLOBAL selection lost: []` và `Optional empty catalogKey is rejected by HTML validation`.
- Browser GREEN: các case global/key/version đều đạt, không có page error.

Trong quá trình kiểm UI đã phát hiện target/classes còn JavaScript cũ qua SHA256 khác source; chạy Maven resources:resources để đồng bộ rồi kiểm lại. Test chờ editor reset sau async save để không bấm vào form đang reload. Không bỏ assertion nghiệp vụ hoặc sửa backend cho test giả qua.

Minh chứng: [audit-fixes-20261028](audit-fixes-20261028/verification.json), các file `*-red-summary.txt`, `*-green-summary.txt`, `catalog-ui-red.json` và `catalog-ui-green.json` trong cùng thư mục. Log đầy đủ nằm `.local/remediation-20261028/`; không xuất khóa/token/password vào tài liệu.

## Kiểm chứng toàn bộ và review

- Đã chạy mới `.\mvnw.cmd test`: **223 tests, 0 failures, 0 errors, 0 skipped**, exit 0, BUILD SUCCESS.
- `py -3 tools/check_shared_ui.py`: một layout SiteMesh, 21 file chung, exit 0.
- `node --check src/main/resources/static/assets/js/catalog-management.js` và `git diff --check`: đạt.
- `py -3 tools/check_all_ui.py --catalog-regressions`: ba case browser qua Tomcat/SiteMesh/H2 đạt, bao gồm nhiều bước lưu/API trong mỗi case.
- Reviewer độc lập đọc toàn bộ diff backend/UI/migration/tests: không phát hiện Critical/Important. Reviewer không chạy lại Maven; kết quả Maven là lượt implementer chạy và đọc trực tiếp.

Lượt native UI cuối: **17/17 kiểm tra quản trị** và **18/18 kịch bản shopping** đạt trên H2/Tomcat/SiteMesh. Minh chứng JSON và verification.json trong thư mục audit-fixes-20261028; các server do tool tạo đã tự dừng. Hash JavaScript target/classes trùng source tại lượt cuối.

## Giới hạn và trạng thái bàn giao

- Không thực hiện thu/giao/hoàn tiền thật, gửi email hoặc upload provider thật.
- Không xác nhận migration/lock behavior trên DB cloud từ kết quả H2.
- Chưa có test browser riêng cho mọi tổ hợp lookup; thiếu/null version category/area dùng cùng helper nhưng ca HTTP thiếu version trực tiếp hiện là topping.
- Không mở rộng sửa các bảng/luồng không có finding trong đợt audit.
- Thay đổi chưa commit/push. Giữ nguyên ảnh nguồn QR bị xóa, thư mục .agents, skills-lock và tài liệu local có trước.

## Chạy lại

```powershell
.\mvnw.cmd test
py -3 tools/check_shared_ui.py
py -3 tools/check_all_ui.py --catalog-regressions
py -3 tools/check_all_ui.py --admin
py -3 tools/check_all_ui.py --shopping
```

Các tool UI mở H2 ở 127.0.0.1:8882 và tự dừng process chúng tạo. Không dùng fixture để reset DB cloud.
