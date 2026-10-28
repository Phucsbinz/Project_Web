# UTETra — Rà soát tất cả vai trò, 28/10/2026

## Kết luận và phạm vi

Chưa đủ cơ sở chốt project không còn lỗi. Bộ test hiện có đạt nhưng đã tái hiện **3 lỗi mức Trung bình** và xác định **1 rủi ro khóa dữ liệu chưa tái hiện trên DB đích**; các phát hiện và giới hạn dưới đây cần được xử lý trước khi chốt báo cáo/demo.

Audit trên `feature/order-payment`, HEAD `269ea2c8c6d434078ca96846527dcd3d47a6873e`, **bao gồm các bản sửa audit chưa commit trong working tree**. Không chỉ review commit Shipper. Đối chiếu 58 use case (55 bắt buộc, 3 Shipper thêm) và các phần mở rộng đơn nhóm/QR/CSKH. Danh mục: [USE_CASE_COVERAGE.md](all-role-audit-20261028/USE_CASE_COVERAGE.md).

Áp dụng `requesting-code-review` (reviewer độc lập identity/security và order/payment), `systematic-debugging` (đọc caller, tái hiện H2 trước kết luận), `verification-before-completion` (kết quả chạy mới). Đây là đợt kiểm tra và lập báo cáo; không sửa thêm production source, không commit/push, không ghi DB cloud hoặc gọi nhà cung cấp thật.

## Kiểm tra tự động đã chạy

- `.\mvnw.cmd test`: **223 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS, exit 0.
- `py -3 tools/check_shared_ui.py`: đạt **một SiteMesh layout, 21 file chung**, exit 0.
- Kiểm render Tomcat/JSP/SiteMesh bằng lifecycle `tools/check_all_ui.py`, scratch runner chỉ đổi thư mục xuất để giữ minh chứng cũ: **158 checks, 0 failures, 0 JS errors**. Guest 22, User 32, Vendor 28, Manager 32, Admin 34, Shipper 10; desktop 1440 và mobile 390.
- `git diff --check`: đạt lúc tổng hợp.
- Năm ca probe bổ sung (2 đơn nhóm, 2 vòng đời OTP/reset, 1 danh sách mua 101 dòng) chạy qua Spring service thật trên **H2 cô lập**, server bind localhost/port ngẫu nhiên; không gọi thanh toán/email thật.

Minh chứng: [verification.json](all-role-audit-20261028/verification.json), [Maven summary](all-role-audit-20261028/maven-test-summary.txt), [test suites](all-role-audit-20261028/test-suites.json), [UI results](all-role-audit-20261028/ui-verification.json), [group probe](all-role-audit-20261028/group-probe.json), [identity probe](all-role-audit-20261028/identity-probe.json). Scratch source/log/screenshot nằm trong `.local/all-role-audit-20261028/`; không công bố log thô hoặc credential.

**158 checks là render/navigation/responsive, không phải 158 luồng nghiệp vụ E2E.** Test H2 không thay thế kiểm tải/locking trên MySQL/TiDB; test provider dùng mock/fixture không chứng minh merchant, SMTP hay Cloudinary thật đã hoạt động.

## Phát hiện mới

### ALL-01 — Chủ nhóm không có món mất trang theo dõi/thanh toán sau khi đặt

**Trung bình — đã tái hiện runtime cả HOST và INDIVIDUAL.**

Vị trí:

- [GroupOrderService.java](../../src/main/java/com/utetra/order/service/GroupOrderService.java:70): chốt nhóm cho phép host không có món, miễn có đủ người khác có món và xác nhận.
- Cùng file dòng 124–133: `ordered()` chỉ gán `member.orderId` cho người có món; `group.orderId` vẫn được gán.
- Cùng file dòng 108: `view()` chỉ trả order/payment state nếu actor có member.orderId.
- Cùng file dòng 111: `paymentPage()` bắt buộc member.orderId, kể cả host.
- [LocalQrPaymentService.java](../../src/main/java/com/utetra/order/service/LocalQrPaymentService.java:34): trang QR đòi actor là payer của một dòng thanh toán.
- [checkout.jsp](../../src/main/webapp/WEB-INF/views/order/checkout.jsp:62): mọi LOCAL_QR đều chuyển người checkout tới trang QR cá nhân của đơn.

Tái hiện: host tạo nhóm, không chọn món; hai thành viên khác thêm món/xác nhận; host chốt, chọn chi nhánh và tạo đơn LOCAL_QR thành công. HOST và INDIVIDUAL đều không có `orderId` trong group view, gọi trang payment nhóm bị từ chối. HOST mở QR trực tiếp được; INDIVIDUAL bị từ chối vì host không có payer share — đây cũng là đích redirect mặc định ngay sau đặt.

Nguyên nhân: “chủ nhóm điều phối” bị đồng nhất với “thành viên có phần món phải thanh toán” trong truy vấn quyền hiển thị, trong khi luồng chốt nhóm chủ động cho phép host không có món. Contract `docs/features/order-payment/GROUP_JOIN_FIRST_COD_20261027.md` cho phép thành viên không có món không chặn đặt đơn.

Đề xuất:

1. Quyền host xem theo dõi dựa trên `g.hostUserId` và `g.orderId`; thành viên khác vẫn cần membership/phần đơn hợp lệ. Không mở quyền cho khách ngoài nhóm.
2. HOST: host được xem phần thanh toán tổng dù không có món. INDIVIDUAL: host không có món chỉ xem tracking/tổng quan, không tạo khoản thu giả hoặc chia thêm phí cho host.
3. Checkout trả đích điều hướng hoặc trạng thái payer rõ ràng; UI đưa non-payer về theo dõi nhóm thay vì QR cá nhân.
4. Thêm test HOST/INDIVIDUAL/COD với host rỗng, hai thành viên có món; kiểm tổng tiền, payer count, tracking sau refresh, redirect và outsider bị từ chối.

### ALL-02 — Đổi mật khẩu trong tài khoản chưa thu hồi OTP/grant đặt lại mật khẩu cũ

**Trung bình — đã tái hiện runtime H2, cả grant đã verify và OTP chưa verify.**

- [AccountService.java](../../src/main/java/com/utetra/identity/AccountService.java:56): đổi BCrypt rồi `auth.revokeAll`, nhưng chỉ thu hồi auth_sessions.
- [AuthService.java](../../src/main/java/com/utetra/identity/AuthService.java:70): reset kiểm challenge/grant còn hạn và chưa revoke; không đối chiếu mật khẩu đã đổi sau lúc cấp challenge.
- Cùng file dòng 78: reset qua OTP có thu hồi các challenge, còn đổi mật khẩu thông thường chưa dùng chính sách này.

Tái hiện đã chạy: cấp một PASSWORD_RESET grant hợp lệ; chủ tài khoản đổi mật khẩu bằng mật khẩu hiện tại; phiên đăng nhập đã bị thu hồi nhưng vẫn còn 1 reset challenge sống. Grant cũ được chấp nhận và đăng nhập được bằng mật khẩu vừa đặt lại. Ca thứ hai cấp OTP chưa verify, đổi mật khẩu rồi mới verify OTP cũ: vẫn lấy grant và reset thành công. Email/OTP dùng delivery stub, không gửi thật. Người chỉ biết email không tự khai thác được: cần sở hữu OTP/grant hợp lệ trước đó. Đây là lỗ hổng vòng đời chứng thực sau thay đổi mật khẩu, không phải mật khẩu đang lưu bản rõ.

Đề xuất thu hồi toàn bộ PASSWORD_RESET challenge/grant của user trong cùng transaction đổi mật khẩu, giữ thứ tự khóa user rồi challenge nhất quán với OTP/reset. Thêm test grant đã verify và code chưa verify đều mất hiệu lực sau đổi mật khẩu; reset mới vẫn dùng đúng một lần; phiên cũ bị revoke.

### ALL-03 — Màn hình viết đánh giá/bình luận chỉ chọn được 100 dòng mua gần nhất

**Trung bình — đã tái hiện runtime H2, đối chiếu UI không có phân trang.**

- [CommunityService.java](../../src/main/java/com/utetra/identity/CommunityService.java:23): `purchases()` cố định `limit 100`, không nhận page/filter.
- [identity.js](../../src/main/resources/static/assets/js/identity.js:324): tải `/reviews/purchases` một lần để dựng lựa chọn; không tải trang tiếp theo.

Fixture tạo 101 order_items đủ điều kiện của cùng user: service trả đúng 100, không có dòng cũ nhất. Gọi service viết review và comment cho dòng cũ nhất vẫn thành công. Kết hợp với UI chỉ tải một lần, User/Vendor không chọn được dòng cũ để viết đánh giá/bình luận qua giao diện. Giới hạn trang đầu là hợp lý cho hiệu năng, nhưng thiếu đường truy cập phần còn lại làm mất chức năng.

Đề xuất API phân trang/tìm theo mã đơn hoặc sản phẩm; UI có tải tiếp/tìm kiếm hoặc link viết đánh giá từ chi tiết đơn truyền orderItemId. Giữ kiểm quyền orderItem thuộc actor, trạng thái DELIVERED/PAID, chống trùng đánh giá; test ít nhất 101 dòng và truy cập được dòng đầu tiên.

### RISK-01 — Thứ tự khóa biến thể giữa checkout và hoàn tồn không thống nhất

**Rủi ro Trung bình từ static review; chưa tái hiện deadlock bằng hai service transaction trên MySQL/TiDB. Không xếp là lỗi runtime đã xác nhận.**

- [CheckoutService.java](../../src/main/java/com/utetra/order/service/CheckoutService.java:114): duyệt selectedItems theo cart_item.id rồi gọi saleVariant có khóa.
- [CommerceReferenceRepository.java](../../src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java:110): `saleVariant(...,true)` khóa hàng product/variant.
- [OrderInventoryService.java](../../src/main/java/com/utetra/order/service/OrderInventoryService.java:45): release duyệt order_item.id rồi UPDATE biến thể theo thứ tự này.
- [OrderWorkflowService.java](../../src/main/java/com/utetra/order/service/OrderWorkflowService.java:360): cancel khóa order; scope check chỉ đọc store, không lấy khóa store tương đương checkout.

Có thể tạo vòng chờ khi một transaction hủy đơn có biến thể B→A, trong khi checkout khác khóa A→B cùng shop. `reserve()` có TreeMap nhưng khóa đã được lấy trước đó trong calculate nên chưa đủ chứng minh toàn luồng có thứ tự thống nhất. Transaction rollback bảo vệ không ghi nửa chừng; không đồng nghĩa tránh được request thất bại do deadlock. OrderErrorHandler.java:41 đã xử lý PessimisticLockingFailureException, thường trả HTTP409; không có bằng chứng mất tồn/thu tiền trùng do rủi ro này.

Đề xuất thử barrier concurrency trên MySQL/TiDB cô lập, cả checkout-vs-cancel và cancel-vs-cancel; thống nhất thứ tự khóa product/variant tăng dần tại mọi luồng, xét thêm topping/promo/store/user để không tạo vòng chờ mới. Có thể retry bounded cho deadlock transaction sau khi bảo đảm idempotency; không retry riêng một câu SQL trong transaction đã lỗi. Chưa sửa chỉ dựa trên giả thuyết.

## Rà soát quyền và bảo mật

- Guest: trang public/menu/filter/shop/detail; đường ghi dữ liệu tài khoản/đơn yêu cầu chứng thực. Guest cart không được chuyển thành quyền sở hữu đơn khác.
- User/Vendor: profile/address/favorite/view/review/order/cart dùng principal/actor server, không tin userId client; review/comment có kiểm đã mua; account mutation có validation/transaction.
- Vendor: scope owner của store. Có quyền mua hàng riêng không đồng nghĩa được sửa shop/đơn của Vendor khác.
- Manager: membership active theo store; quyền catalog dùng delegation; quản lý user không cho tự nâng Admin hoặc đọc user ngoài phạm vi.
- Admin: quyền toàn hệ thống vẫn giữ quy tắc trạng thái, version, validation, bảo vệ Admin cuối cùng; không có nghĩa tùy ý đánh dấu PAID/REFUNDED không đối soát.
- Shipper: assignment còn hiệu lực, state transition hợp lệ, ghi nhận COD đúng quyền; không tự hoàn tiền hoặc đọc đơn ngoài assignment.
- JWT/session: Spring Security và service guards phối hợp; JWT được kiểm cùng trạng thái phiên/token DB, có revoke/refresh. Cookie/session không tự thay thế kiểm quyền tại service.
- CSKH: đọc guard conversation/message/socket, persistence và SupportIntegrationTest. Khách chat với quản lý hoặc Admin; không mở hội thoại quản lý-Admin hay quản lý với nhau theo vai trò staff. Scope recipient được kiểm phía server.
- Bộ test security/identity/order/support hiện đạt; đợt này chưa xác nhận IDOR hoặc endpoint vượt quyền mới. Đây không phải cam kết đã pentest mọi tổ hợp URL/body/socket frame.

## Transaction, dữ liệu và xung đột

- Quote là preview; server tính lại checkout, so quote hash và idempotency trước ghi. Không tin tổng tiền từ browser.
- Checkout ghi order/snapshot/reservation/discount/points cùng transaction. Stock kiểm/trừ có khóa và UPDATE có điều kiện; quota được giữ ở bước reserve, không dựa vào WebSocket.
- WebSocket thông báo sau commit giúp UI cập nhật; khả năng chống bán vượt tồn/dùng trùng voucher nằm ở transaction, khóa và điều kiện DB.
- Inventory helper yêu cầu `Propagation.MANDATORY`; promotion/loyalty helper được gọi trong transaction service cha. Không kết luận thiếu transaction chỉ vì helper không tự gắn annotation.
- Nhiều mutable JPA aggregates có @Version; JDBC identity/session/OTP còn dùng row lock và version increment/expected-version tùy luồng. Không coi số annotation là bằng chứng mọi write đã chống xung đột đầy đủ. Append-only event/ledger/link không cần máy móc gắn @Version lên mọi bảng.
- Các bản sửa trước đã thêm version cho category/topping/area, chống payload cũ; migration V2026102802 mới được test H2, **chưa áp dụng lên DB cloud trong đợt này**.
- Tests hiện có bảo vệ concurrent refresh/default address, stock/quota, trạng thái đơn, snapshot và tính idempotent trong phạm vi fixture; không suy ra mọi interleaving trên TiDB đã đạt. Xem RISK-01.
- Bốn phát hiện cũ đã được sửa trong working tree và được bộ 223 tests kiểm lại; xem [AUDIT_REMEDIATION_20261028.md](AUDIT_REMEDIATION_20261028.md). Bốn lỗi mới/rủi ro ở báo cáo này không bị lẫn với danh sách lịch sử đó.

## Phần vẫn cần kiểm thật hoặc người phụ trách hoàn tất

- Merchant VNPAY/MOMO: có VNPAY adapter và tests, nhưng cấu hình thực còn tắt; QR chuyển khoản thủ công/LOCAL_QR không tự chứng minh đã đáp ứng tích hợp cổng theo yêu cầu thầy. Cần xác nhận tiêu chí và thử sandbox thật khi có tài khoản.
- SMTP: thử nhận OTP đăng ký/quên mật khẩu thật trên máy báo cáo; tests hiện không thay thế email delivery thật.
- Cloudinary: thử upload/xóa ảnh và upload media phù hợp, kể cả lỗi provider/rollback; không gọi provider thật trong audit này.
- DB cloud: áp dụng migration đúng quy trình, kiểm schema/data/foreign keys và concurrency trên DB đích; không dùng test reset fixture lên cloud.
- Báo cáo: khảo sát thực, tên/MSSV/phân công, commit cá nhân, activity/source/UI có diễn giải, DOCX và diễn tập vẫn cần nhóm xác nhận. Có code không đồng nghĩa các đầu mục báo cáo đã hoàn tất.

Không phát hiện mới đã xác nhận mức Nghiêm trọng trong phạm vi này. Ưu tiên ALL-02 (vòng đời bảo mật), ALL-01 (checkout nhóm), ALL-03 (đánh giá cũ), sau đó kiểm thực nghiệm RISK-01; chưa nên ghi “đáp ứng hoàn toàn/không còn lỗi”.


## Cập nhật sau sửa lỗi cùng ngày

Ba finding ALL-01/02/03 đã sửa trong working tree,257 tests và UI nghiệm thu đạt. RISK-01 đã thống nhất thứ tự khóa variant; chưa xác minh MySQL/TiDB. Xem [SCOPED_FIXES_20261028.md](SCOPED_FIXES_20261028.md) và giữ nguyên bằng chứng audit lịch sử bên trên. Chưa commit/push/merge.
