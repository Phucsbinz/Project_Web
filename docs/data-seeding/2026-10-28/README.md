# UTETra — Bổ sung danh mục demo ngày28/10/2026

## Kết quả và phạm vi

Đã bổ sung **31 dòng danh mục** vào `utetra_db` trên TiDB Cloud/MySQL-compatible, sau khi người dùng xác nhận đây là DB phát triển/demo của project cuối kỳ, không bán thật. Không đổi datasource/JWT/provider/schema/business code, không chạy migration, không commit/push/merge. Git vẫn ở feature/order-payment, HEAD269ea2c8c6d434078ca96846527dcd3d47a6873e; mọi thay đổi có sẵn được giữ nguyên.

- `products`:90→96, thêm6 dòng (2 món ×3 shop hiện có).
- `product_variants`:270→279, thêm9 dòng. Trà sữa xoài chỉ M/L; khoai môn đá xay chỉ M theo nguồn.
- `toppings`:3→4, thêm sương sáo giá demo10.000đ.
- `store_toppings`:9→12, thêm3 liên kết khả dụng của topping ở ba shop demo.
- `product_toppings`:270→282, thêm12 liên kết chọn lọc; không gán mọi topping cho mọi món.
- `categories`:giữ6 nhóm, không thêm nhóm/món gần trùng để tăng số lượng. Cấu trúc30 món canonical hiện có đã phủ đủ nhóm; thêm2 món phân biệt rõ. Menu toàn chuỗi gộp cùng catalog_key nên thấy2 món mới, không phải6 tên khác nhau. Trang chủ Guest lọc số bán>10 sẽ chưa đưa món mới0lượt bán vào danh sách; không tạo số bán giả để ép hiển thị.

**Sáu sản phẩm mới chưa mở bán** (`is_available=false`), tồn theo size đểNULL là chưa xác nhận. Không tự tạo số lượng hoặc bật mua không kiểm tồn. Giá/demo và mô tả được nêu rõ trong description; không gắn tên thương hiệu khảo sát vào sản phẩm UTETra. Topping mới được dùng với một số món cũ còn tồn; luồng chốt tồn/mở bán mới thuộc người phụ trách.

## Hồ sơ và nguồn

- [Phân loại đủ51 bảng, bao gồm13 bảng trống](TABLE_CLASSIFICATION.md): COUNT(*) thực, ý nghĩa/nguồn/nhóm A-B-C/phụ thuộc và source tham chiếu.
- [Nguồn khảo sát chính thức](SOURCES.md): Gong Cha, ToCoToCo, Chapa Tea; ghi thêm các giới hạn khi mở Phúc Long và KOI Thé. Nguồn, ngày truy cập, thông tin quan sát và biên soạn demo tách riêng.
- [Manifest31 dòng](manifest.json): toàn bộ tên/giá/size/liên kết/store scope, thứ tự insert, natural keys, prerequisites và giả định. [Preview trước khi ghi](preview.json) có31 dự kiến,0conflict/0dependency skip.
- [Ledger đã commit](applied-ledger.json): **ID chính xác cùng snapshot từng dòng**; là căn cứ hoàn tác, không sửa thủ công. Không có credentials/token/thông tin cá nhân.
- [Trước](inventory-before.json) / [sau](inventory-after.json), [kiểm dữ liệu](data-checks.json), [preview chạy lại](idempotency-preview.json), [preview hoàn tác](rollback-preview.json).

Ba trang dùng cho giá seed thực sự được mở và đọc thành công:

1. [Gong Cha trà sữa xoài](https://gongcha.com.vn/en/san-pham/tra-sua-xoai-tran-chau-den/):M51.000/L59.000.
2. [Gong Cha khoai môn đá xay](https://gongcha.com.vn/en/san-pham/khoai-mon-da-xay/):M63.000, không suy ra L/S.
3. [Gong Cha sương sáo](https://gongcha.com.vn/en/san-pham/suong-sao/):10.000; gợi ý phối với trà sữa caramel/chocolate/khoai môn/oolong.

ToCoToCo/Chapa dùng để đối chiếu nhóm món và độ chi tiết công bố; **không lấy giá khuyến mãi hoặc giá chưa rõ size để seed**. Liên kết trân châu đen tùy chọn cho món xoài là công thức demo UTETra biên soạn, không phải khẳng định chính sách topping của nguồn. Không tải/dùng logo/ảnh thương hiệu; ảnh sản phẩm mớiNULL dùng placeholder lá sẵn có.

## Những bảng chủ động giữ trống

Cả13 bảng trống trước seed vẫn trống: cart_items, cart_item_toppings, comment_media, delivery_assignments, delivery_events, payment_callback_events, promotion_products, refund_transactions, return_requests, review_media, store_registration_requests, support_conversations, support_messages.

12 bảng là dữ liệu thao tác/sự kiện/media, không được giả lập để làm đầy. `promotion_products` thuộc quyết định khuyến mãi: rỗng có thể nghĩa áp dụng toàn phạm vi; tự thêm sẽ đổi eligibility. Không seed order_items/cart_items dưới cách hiểu nhầm “item danh mục”. Các bảng shop/owner/membership/roles, phí/hoa hồng/voucher được giữ nguyên; chỉ sử dụng scope hiện có.

Số dòng một số bảng B đã tăng giữa hai snapshot: phiên, thông báo, đơn/dòng đơn/payment/status và lượt xem. Đây là DB demo dùng chung đang hoạt động; chưa xác định nguồn hoạt động khác đó. Không quy toàn bộ chênh lệch DB cho seed. Utility chỉ có whitelist5 bảng INSERT, không Spring startup/provider hay lệnh tạo giao dịch; DB có0 trigger. **Không xóa/chỉnh các dòng phát sinh ngoài manifest** để làm số liệu khớp. Chi tiết delta nằm verification.json.

## Cách script bảo toàn dữ liệu

- Tool riêng [catalog_data_seed.py](../../../tools/catalog_data_seed.py), [CatalogSeed.java](../../../tools/data/CatalogSeed.java); không dùng seed_demo.py cũ vì nó tạo cả tài khoản, đơn và đánh giá.
- Đọc cấu hình hiện có trong bộ nhớ, kiểm đúng schema utetra_db; không in connection string, username/password. TiDB không hỗ trợ thao tác setReadOnly của probe ban đầu; bỏ riêng thiết lập đó ở công cụ khảo sát, chỉ giữ SELECT và rollback, không đổi cấu hình DB/app.
- Manifest có whitelist cột/bảng/natural key, kiểm size/giá nguyên đồng/name/slug, scope store và category đã duyệt. Mỗi product có size active hợp lệ; số tồn mới bắt buộcNULL và product bắt buộc unavailable. Không thực thi SQL tùy ý từ manifest.
- Apply trong một transaction READ_COMMITTED, khóa prerequisites categories/stores/products/toppings theo ID tăng, kiểm lại contract rồi insert theo FK. Không tắt FK/UQ/validation, không upsert, không truncate/delete/reset sequence.
- Natural key: topping(name), product(slug) cộng kiểm tương tự theo store/catalog_key hoặc store/category/name; variant(product,size); hai bảng link theo cặp FK. Nếu dòng cũ khác nội dung/không rõ trùng thì ghi conflict và giữ nguyên, không update giá/trạng thái. Dependency bị conflict được bỏ qua, không tạo dòng con sai.
- Ledger ghi snapshot+ID trước commit bằng CREATE_NEW/FileChannel.force; trạng thái committed hoàn thiện bằng file tạm cùng thư mục rồi atomic replace. Không ghi đè ledger invocation khác. Nếu lỗi sau DB commit mà ledger còn prepared, dùng reconcile chỉ đọc để xác minh từng snapshot trước khi lập ledger phục hồi; có chỉnh sửa/thiếu dòng thì dừng để xem thủ công.
- Preview dùng ID âm làm ký hiệu tạm, không ghi chúng vào DB; ledger committed mới chứa ID thật. Auto-increment của TiDB cấp ID30001 trở lên là hành vi allocator; không reset/chỉnh sequence để ép ID liền nhau.

## Hướng dẫn chạy lại

Từ `D:\WEBCUOIKI`, dùng JDK21 qua JAVA_HOME hoặc UTETRA_JAVA_HOME. Maven test/Surefire classpath phải có sẵn; không thêm dependency. Mỗi lần chọn **tên output mới**; utility từ chối ghi đè ledger cũ.

```powershell
# Kiểm logic import trên H2 riêng, không kết nối cloud, không tạo account/order/interaction:
py -3 tools/catalog_data_seed.py --self-test

# Chỉ xem trước; hiện tại dự kiến0 dòng mới:
py -3 tools/catalog_data_seed.py --mode preview --output .local/catalog-preview-next.json

# Chỉ dùng trên DBdemo đã được xác nhận, sau khi đọc preview:
py -3 tools/catalog_data_seed.py --mode apply --confirm-demo --output .local/catalog-apply-next.json

# Kiểm dữ liệu chỉ đọc:
py -3 tools/catalog_data_check.py
py -3 tools/database_inventory.py --output .local/catalog-inventory-next.json

# Đọc HTTP/JSP menu, tìm/lọc/phân trang/size/placeholder; không giả nhận native browser:
py -3 docs/data-seeding/2026-10-28/verify_http.py --pages-only
```

Tool mặc địnhpreview, không có --confirm-demo thì CLI từ chối apply/rollback. Manifest giữ nguyên thì lần tiếp theo không thêm trùng. Không chạy seed_demo.py hoặc các công cụ showcase giao dịch để nghiệm thu danh mục.

## Hoàn tác có điều kiện — CHƯA thực hiện trên TiDB

```powershell
py -3 tools/catalog_data_seed.py --mode rollback-preview --ledger docs/data-seeding/2026-10-28/applied-ledger.json --output .local/catalog-rollback-preview-next.json
# Chỉ sau khi xem preview và xác nhận dữ liệu chưa được sử dụng:
py -3 tools/catalog_data_seed.py --mode rollback --confirm-demo --ledger docs/data-seeding/2026-10-28/applied-ledger.json --output .local/catalog-rollback-next.json
```

Rollback chỉ nhận committed ledger chuẩn, đúng5 bảng/ID tạo mới. Nó khóa và so **toàn bộ snapshot**, đọc incomingFK thực để phát hiện phụ thuộc ngoài ledger, kiểm tất cả trước bất kỳ DELETE nào; có giá/tồn/version/ảnh sửa hoặc dòng con mới thì chặn toàn bộ. Xóa theo thứ tự ngược insert chỉ31 dòng đã tạo, không xóa parents cũ. Chạy rollback-preview tại lúc kiểm cho thấy31 có thể hoàn tác,0blocked, **deleted0**; kết quả có thể thay đổi khi nhóm bắt đầu sử dụng.

Nếu lỗi ghi file sau commit để lại prepared ledger:

```powershell
py -3 tools/catalog_data_seed.py --mode reconcile --ledger .local/ledger-prepared.json --output .local/ledger-reconciled.json
```

Reconcile chỉ đọc; yêu cầu toàn bộ ID/snapshot còn khớp, không tự xóa/ghi DB. Nếu thiếu/sửa dòng, không đoán commit thành công hay tự apply lại, cần kiểm tay. Không chỉnh status trong JSON để vượt guard.

## Kiểm chứng thực tế và giới hạn

- Test tool có RED trước implementation (preview phải31 nhưng trả0). Self-test sau sửa đạt preview/apply31/idempotency0, unknown-stock/unavailable, giữ giá chỉnh sửa, whitelist cấm bảng giao dịch, ledger JSON roundtrip/recovery, collision và atomic write, rollback riêng dòng mới, edited-row/no-partial-delete và externalFK guard. Fixture H2 chỉ có public catalog và stubs tham chiếu; không account/OTP/order/chat/sales.
- Lúc thêm test rollback, đã bắt được khác biệt metadata uppercase của H2; sửa công cụ nhận tên table theo metadata, test đã xanh trước applyTiDB. Review độc lập phát hiện ledger durability/collision/preflight race, đã sửa và review lại không còn blocker trước apply.
- `.\mvnw.cmd test` chạy mới: **257 tests,0failure/0error/0skip, BUILD SUCCESS**. Tool Java được javac/JDK21 biên dịch lại và self-test; business source/pom không đổi.
- TiDBapply:31 created,0conflicts, transaction committed. Preview chạy lại0created/0conflicts/0dependency skip.9 kiểm tra FK/unique/giá/stock/availability trả0vi phạm. Snapshotstrings/tiếng Việt khớp manifest qua idempotency comparison.
- HTTP/JSP đã đọc thành công menu/tìm kiếm/lọc/phân trang,6 trang món mới với size đúng và placeholder. **Không có browser UI khả dụng** (CUA inventory rỗng); chưa kiểm render/console/mobile bằng browser trong lượt này.
- Thử quote hai lần trên localhost bị timeout, vẫn lưu kết quả thất bại trong http-quote-blocked.json. Runtime hiện báo `NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy`; chưa sửa/restart app, không coi quote đã đạt. `verify_http.py` không có --pages-only sẽ thử public quote, không thêm giỏ/đơn/thanh toán.
- Schema cloud thiếu version ở categories/toppings/store_delivery_areas so với sourceV2026102802. Cần người phụ trách migration đồng bộ source/schema trước khi chạy WAR mới; nhiệm vụ này không cho phép tự đổi schema. Không quy thiếu cột thành thiếu dữ liệu để tự sửa.
- Chưa mở bán món mới khi thiếu quyết định tồn kho; không khẳng định đã đầy đủ toàn bộ DB chỉ vì dữ liệu danh mục tăng.

## Việc nhóm cần chốt

1. Tồn kho theo size cho6 món mới, rồi mở bán qua luồng quản trị; không dùng NULL thành cách bỏ kiểm tồn.
2. Đồng bộ migrationV2026102802 và runtime WAR/thư viện; chạy lại quote/browser khi môi trường khớp. Không đổi cấu hình nghiệp vụ để che lỗi.
3. Giữ phí/hoa hồng/carrier/voucher hiện có đến khi owner quyết định; nếu cần đổi, làm nhiệm vụ riêng có phạm vi/contract.
4. Kiểm render mobile/console trong browser có kết nối, chụp minh chứng báo cáo. Các nguồn giá demo không phải xác nhận giá bán thật.
