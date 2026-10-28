# Sửa lỗi giới hạn — 28/10/2026

## Kết quả nghiệm thu cuối

**Đã sửa ba lỗi trong working tree, và giảm rủi ro do thứ tự khóa variant không thống nhất. Chưa xác minh deadlock trên MySQL/TiDB.**

- Baseline mới223 tests đạt; từng lỗi có RED đúng nguyên nhân trước production fix.
- Lượt cuối Maven Wrapper `verify` trên bản sao input nguyên trạng: **257 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS, WAR executable đóng gói thành công**. 316 input Maven so sánh với workspace không khác biệt. Không skip test.
- Workspace `verify` cũng257 tests đạt nhưng repackage bị chặn rename WAR khi ứng dụng cũ đang chạy. Không coi lệnh này là BUILD SUCCESS; đã giữ nguyên tiến trình và dùng thư mục build riêng.
- Native Edge/Tomcat/SiteMesh: **16 nhóm kiểm tra đạt**, gồm27 lượt render ở390/768/1440;0 JS/console errors,0 request failure bất thường;1 abort body logout do navigation, có200 và đối chứng revoke/Guest isolation.
- Shared UI đạt:1layout/21file; git diff --check đạt. Không dùng --compare main vì không xác nhận main mới nhất.
- Không sửa datasource/provider/JWT/CSRF/schema; không gọi DBcloud/provider thật; không commit/push/merge. Server H2 của công cụ đã dừng; app có sẵn không bị khởi động lại.

Minh chứng: [verification.json](scoped-fixes-20261028/verification.json), [native-ui.json](scoped-fixes-20261028/native-ui.json), [verify summary](scoped-fixes-20261028/isolated-final-verify-summary.txt), [baseline preservation](scoped-fixes-20261028/preservation.json), [review](scoped-fixes-20261028/review.txt).

Lệnh thực:

```powershell
.\mvnw.cmd test
# Các lượt RED/GREEN theo tên test được lưu trong .local/scoped-fixes-20261028/*.log
.\mvnw.cmd -B -ntp verify
# Bản sao build-input giữ nguyên src/pom/wrapper và proposal test; không thay file cấu hình:
.\mvnw.cmd -f .local/scoped-fixes-20261028/build-input/pom.xml -B -ntp verify
py -3 tools/check_scoped_fixes_ui.py
py -3 tools/check_shared_ui.py
git diff --check
```

WAR đã kiểm: `.local/scoped-fixes-20261028/build-input/target/utetra.war`; SHA256 được ghi trong verification.json. Chưa deploy/restart ứng dụng đang chạy. Log thô chỉ giữ .local, summary công bố đã loại HTTP request/token/OTP/password. Kết quả257 bao gồm test QR host rỗng thanh toán đến PAID bổ sung ở lượt cuối.

Ảnh không chứa QR/capability token: [form review390](scoped-fixes-20261028/reviews-390.png), [form review768](scoped-fixes-20261028/reviews-768.png), [form review1440](scoped-fixes-20261028/reviews-1440.png), [host không có phần trả390](scoped-fixes-20261028/individual-host-empty-payment-390.png).


## Phạm vi và bảo toàn workspace

Nhánh feature/order-payment; giữ nguyên HEAD, index và các thay đổi có sẵn. Baseline diff/status được lưu tại `.local/scoped-fixes-20261028/`. Không commit/push/merge/chuyển nhánh, không gọi provider hay ghi DB cloud. Không đổi schema/config/JWT.

## Trình tự thực hiện

1. Đọc contract, callers, baseline Maven mới.
2. Lỗi host rỗng: test RED → sửa quyền đọc host và điều hướng, giữ payer/membership/bill → test GREEN/regression.
3. Lỗi OTP: test RED riêng OTP/grant/cạnh tranh → thu hồi đúng PASSWORD_RESET trong transaction user lock → test GREEN/regression.
4. Lỗi purchases100: test RED paging/ownership/biên → pagination bounded và UI tải tiếp → test GREEN/regression/browser.
5. Rủi ro deadlock: trace khóa các caller, test thứ tự/transaction; sửa thứ tự khóa ID ổn định nếu không nhất quán; không ghi thành deadlock MySQL/TiDB đã tái hiện.
6. Maven Wrapper verify không skip, sharedUI, browser390/768/desktop, review diff, prompt log và bàn giao bằng chứng.

## Contract / điểm cần phân biệt

- GROUP_JOIN_FIRST_COD_20261027 mục thao tác5 cho phép người chưa có món không chặn; ít nhất2 người có món. GROUP_SPLIT_QR_POPUP cũ yêu cầu trang payment có member.orderId; đây là điều kiện gây mâu thuẫn cho host không có món, không dùng nó để loại host. LOCAL_QR_WALLET nói chỉ người có phần tiền có payer row; giữ nguyên quy tắc này. GROUP_CHECKOUT_REFINEMENT_20261028 mới hơn: COD ép HOST; QR giữ chế độ. Không phục hồi COD chia phần cũ.
- GROUP_MEMBER_TRACKING_20261028 chặn thành viên thường không có snapshot. Host là order owner vẫn xem đơn theo guard hiện hành, không cần nới quyền thành viên khác.
- UC-USER-01/AUTH-06: verify mật khẩu hiện tại, BCrypt, revoke sessions; bổ sung revoke đúng reset OTP/grant cũ theo yêu cầu mới, không đổi OTP REGISTER/tài khoản khác/JWT policy.
- UC-USER-05/06 + CommerceIdentityAdapter: đã giao/trả tiền, ownership orderItem; review unique và comment50–2000/media validation giữ nguyên. Không có contract giới hạn khả năng review ở100 purchase gần nhất.
- DATABASE_DESIGN phần transaction: current row locks, atomic rollback, lock order ổn định; WebSocket chỉ after commit.

Kết quả và bằng chứng cập nhật sau từng bước, không dùng báo cáo cũ làm chứng minh lần chạy này.

## Điều tra và bản sửa theo từng lỗi

### 1. Host không có món

- Tái hiện trước sửa: host không chọn món, hai người khác có món; chốt → checkout thành công → group view thiếu orderId, paymentPage từ chối, INDIVIDUAL bị redirect nhầm QR cá nhân không có payer.
- Mong đợi: host tiếp tục điều phối/theo dõi; HOST trả tổng kể cả không có món, INDIVIDUAL không có share thì không được tạo khoản thu. Thành viên thường không có snapshot và outsider vẫn bị chặn.
- Nguyên nhân: `GroupOrderService.view/paymentPage` dùng member.orderId như điều kiện cho mọi actor, bỏ sót vai trò điều phối của host. `checkout.jsp` chuyển mọi QR về trang cá nhân. `OrderWorkflowService.readable` đã nhận owner nên không cần nới.
- Sửa: GroupOrderService.java:109/112; order/checkout.jsp:62 đi qua `/group-orders/{token}/payment` cho QR nhóm. GroupOrderController sẵn có tiếp tục redirect người có nghĩa vụ trả tới QR; non-payer xem thông tin. group.jsp dùng orderId có quyền để hiện tracking và link mở lại payment của host rỗng; group-payment.jsp dùng thông điệp không có khoản trả riêng.
- Caller/role: GroupOrderController GET group/data/payment, checkout JSP, group JSP, User/Vendor; LocalQrController vẫn giữ payer guard, OrderWorkflowService vẫn giữ owner/member/operator/assignment. Không tạo API hoặc schema mới.
- Giữ nguyên: ít nhất2 người có món, số participant/share/payer thực, tiền/rounding, ORD state, outsider/empty-member guards, COD ép HOST theo contract mới, QR INDIVIDUAL, solo checkout và idempotency.
- RED: 15 ca mới, 7 failures đúng việc thiếu orderId ở host rỗng; 0 errors/skipped. GREEN: 107 commerce tests đạt trước chuyển lỗi2. 14 tổ hợp host có/không món × phương thức/chế độ, thêm nhóm hoàn toàn rỗng; kiểm tổng2 người95000,3 người129000 trong fixture40k/ly +15k giao, ưu đãi5% khi3 người. Có legacy BANK_TRANSFER API, HOST VNPAY bằng fixture; không bật provider production.
- Bổ sung nghiệm thu cuối: các ca QR gọi thanh toán giả lập đúng payer đến PAID, kiểm group/đơn/bill/tổng ledger. Không suy ra VNPAY/Bank thật đã thu tiền.

### 2. OTP/grant reset cũ sống sau đổi mật khẩu

- Tái hiện riêng: cấp OTP reset trước đổi password rồi verify; hoặc verify OTP lấy grant trước đổi password rồi reset. Cả hai vốn được chấp nhận dù session đã bị revoke.
- Nguyên nhân: AccountService.password chỉ cập nhật hash và auth_sessions, không revoke otp_challenges. AuthService.reset/OtpService.verify đúng khi kiểm challenge chưa bị revoke nên vẫn nhận bằng chứng cũ.
- Sửa: AccountService.java:58 thêm UPDATE có điều kiện user_id actor, purpose PASSWORD_RESET, revoked_at null; xóa reset_grant_hash và đánh dấu revoke. Dùng cùng transaction sau kiểm current password và khóa user, trước commit. Không thay password/session/JWT policy, purpose REGISTER hoặc user khác.
- Caller/role: AccountController đổi mật khẩu của mọi tài khoản đăng nhập qua service chung; AuthController OTP/forgot/reset; verify và reset đều lấy user lock trước challenge nên serialize với password(). Không thêm khóa ngược thứ tự.
- Giữ nguyên: đổi sai password rollback, BCrypt, limits/expiry/one-use, OTP mục đích khác/tài khoản khác, fresh reset sau đổi vẫn dùng được, phiên cũ bị thu hồi như trước.
- RED: 7 ca, 5 failures đúng bằng chứng reset cũ còn được chấp nhận; 0 errors/skipped. GREEN: toàn bộ43 identity tests và1 logout commerce HTTP, tổng44 đạt trước chuyển lỗi3.
- Cạnh tranh: outer transaction thực đổi password và giữ user lock; thread verify/reset đến sau phải chờ (Future timeout200ms có gate), commit rồi bị từ chối, login bằng password mới thành công. Có timeout5/10s để không treo test; không ghi OTP/grant/password vào hồ sơ.

### 3. Dòng mua cũ hơn100

- Tái hiện: 101 dòng đủ điều kiện của một user; query chỉ trả100, UI không có đường đi tiếp. ID cũ vẫn đủ quyền review/comment theo service.
- Nguyên nhân: CommunityService.purchases limit100 cố định; CommunityController không nhận page/size; identity.js tải một lần.
- Sửa: CommunityService.java:25 nhận page/size qua IdentityValidation.offset hiện hành, query bound LIMIT/OFFSET và ORDER BY oi.id DESC (ID duy nhất). CommunityController cùng URL, thêm tham số tùy chọn, giữ default0/100 và dạng JSON list để không phá caller cũ. Overload service cũ giữ tương thích. API không đổi DTO/schema, validation 1≤size≤100, page≤10000 theo policy chung.
- UI: identity.js:331 và identity/page.jsp:58 dùng20 dòng/trang, previous/next, trạng thái loading/empty/error/retry; xóa selection/productId cũ khi đổi trang. Không cộng dồn lịch sử vào select/bộ nhớ. Với trang đủ20, có thể bấm sang một trang rỗng cuối để biết đã hết; có nút quay lại, không bỏ/lặp dòng. Đây là phân trang kiểu hiện có, không tăng100 thành giới hạn lớn hơn.
- Caller/role: GET /reviews/purchases dùng ở cả /reviews và /comments, User/Vendor. POST review/comment vẫn qua CommerceIdentityAdapter.purchase và kiểm order owner, DELIVERED/PAID; không tin ID UI. Controller công khai nội dung sản phẩm, favorites/views và phân trang danh sách bài viết không thay đổi.
- Giữ nguyên: review unique order_item, comment50–2000 sau trim, media validation/ownership/rollback; không lọc mất dòng đã review vì vẫn có thể dùng để comment và server quyết định chống trùng.
- RED: 5 ca, 3 failures (100 thay vì20 và query phân trang sai chưa bị chặn), 0 errors/skipped. GREEN:48 identity +2view=50 đạt trước xử lý rủi ro.
- Ca mới:0/1/100/101 dòng, trang cuối/rỗng; tập ID mỗi trang khớp fixture theo thứ tự ổn định, không trùng; dòng cũ nhất101 gửi review/comment được; duplicate review/49 ký tự bị chặn; đơn người khác/chưa giao/chưa trả bị chặn; tham số sai400.

## Rủi ro khóa: bằng chứng, thứ tự và giới hạn

Đã xác nhận **thứ tự khóa không nhất quán**, không xác nhận một deadlock MySQL/TiDB đã xảy ra. Các mutation ghi tồn trực tiếp chỉ ở inventory.reserve và release; releaseBeforePreparation chỉ được gọi bởi cancel. Các luồng khác có đọc khóa variant cũng được truy vết để không tạo lại thứ tự ngược.

| Luồng | Thứ tự khóa thực tế trước sửa | Điều chỉnh / trạng thái sau sửa |
| --- | --- | --- |
| Checkout | user → cart → store → carrier → carrier_area_rate → commission policy → từng product/variant theo cart_item.id, xen topping → promotions theo ID tăng → group nếu có → inventory reserve tăng variantId → order/items → điểm/quota/payment/events | Khóa từng sale variant theo ID tăng trước vòng tính line/topping; giữ nguyên thứ tự line/snapshot. Reserve giữ tăng ID. Các khóa khác không bị đổi ngoài việc lấy đủ variant trước topping |
| Cancel | order → scope read store không khóa → loyalty nếu dùng điểm (order/user/account) → UPDATE variant theo order_item.id → coupon redemptions → assignment/payment/refund/group/history/event | UPDATE variant theo variantId tăng, itemId phụ; flags stockReserved và transaction/version/idempotency giữ nguyên |
| Select branch | user → cart → target store → mapping không khóa → sale variant/stock theo TreeMap ID tăng → thay dòng giỏ | Không đổi; các lần addLocked sau đó chỉ lấy lại khóa đã giữ |
| Guest merge kiểu legacy | user → cart → addLocked từng dòng đầu vào, mỗi lần store → sale variant → topping/cart writes | Lấy sale variant theo ID tăng trước replay các dòng đầu vào; không đổi display order, mergeKey hoặc stock quantity |
| Add một món | user → store → một sale variant → cart → topping/link | Chỉ một variant, không có cặp khóa nhiều variant đảo thứ tự; không đổi |
| Draft/group draft | user/cart, chỉ đọc catalog không FOR UPDATE variant | Không đổi; chưa giữ/trừ stock ở bước draft |
| Catalog sửa tồn | JPA product/variants, optimistic stockVersion và flush | Đọc để đánh giá phạm vi; không refactor catalog. Không dùng kết quả này để khẳng định toàn bộ lock graph catalog/payment/loyalty không thể deadlock |

Cycle có thể có trước sửa: cancel của B khóaV3 rồi đợiV1; checkout A khóaV1 rồi đợiV3. Store lock checkout không chặn cancel vì cancel scope chỉ đọc store. `reserve(TreeMap)` không đủ vì calculate đã lấy variant locks trước reserve. Hai cancel có dòng đảo thứ tự cũng cùng nguy cơ ở phần stock. Các khóa order/user/group/loyalty có thể có rủi ro riêng; đợt này không tuyên bố giải quyết mọi vòng chờ.

Sửa nhỏ tại CheckoutService.java:114, OrderInventoryService.java:45, CartService.java:328. Không dùng synchronized/retry, không đổi isolation, không nuốt exception. Mỗi ID được gửi trong **một câu point SELECT FOR UPDATE hoặc UPDATE có WHERE id=?**, tuần tự trên JDBC transaction. Không dựa vào ORDER BY của một query IN nhiều dòng để suy đoán thứ tự DB lock. SaleVariant còn JOIN product, có thể khóa hàng product/index theo engine; thứ tự riêng của engine vẫn cần kiểm thực.

Test InventoryLockOrderIntegrationTest dùng subclass repository test-only gọi **super với JdbcTemplate thật**, ghi query và ID bind khi SQL đã thực thi, không mock kết quả DB. RED hợp lệ:3 failures nhận[3,1] thay vì[1,3] ở checkout/cancel/merge. Lần dựng harness ban đầu3 errors do đọc field trên CGLIB proxy, đã sửa harness bằng method và chạy lại RED đúng nguyên nhân **trước sửa production**; không tính lần error là bằng chứng TDD.

GREEN:6 ca khóa (3 thứ tự SQL +3 lần cạnh tranh) và108 commerce=114 đạt. Gate giữ khóa releaseV1 thực, thread checkout giỏV1→V3 bắt đầu chờV1, release tiếpV3 rồi commit; cả hai hoàn tất trong timeout, tồn cuối9/9 từ10/10, đơn cũ hết stockReserved, chỉ1 đơn mới dù replay checkout. Hai biến thể thuộc hai sản phẩm khác nhau cùng shop; snapshot vẫn giữ dòng3→1. Có kiểm hoàn đúng một lần, không oversell/cancel/redeem/quota theo bộ commerce hiện có.

**Không có môi trường MySQL/TiDB cô lập được cho phép cho lượt này, nên không chạy thử cạnh tranh trên engine đó. H2 không chứng minh loại trừ deadlock MySQL/TiDB.** Không chạy reset/migration lên DB thật; không tạo migration mới.

## Kiểm tra thêm theo phản hồi logout

Prompt tiếp nối của người dùng được lưu nguyên văn cùng prompt chính. Không thay AuthController/cookie/session/shared logout handler trong lượt này.

- HTTP test logoutUsesOnlyGuestCartAndNeverExposesTheAccountCart: account cart1 ly, Guest trước login3 ly; login chỉ thấy account1; logout thấy Guest3; account DB vẫn1 và /orders/data401. Login/merge rồi logout thì Guest rỗng, account DB4. Không mất giỏ tài khoản và không lộ nó sang Guest.
- Đây là hành vi giỏ Guest độc lập lưu trong session, không phải chứng cứ logout thất bại. Nội dung trên máy đang chạy của người dùng chưa được quan sát trực tiếp; không khẳng định mọi trường hợp người dùng thấy đều đã tái hiện.
- Native browser kiểm nút logout thật, 200, chuyển login, profile API401, giỏ Guest3 vẫn riêng. Handler chung chuyển trang ngay sau fetch nhận headers; Chromium có thể ghi ERR_ABORTED cho body logout do navigation. Chỉ phân loại là abort điều hướng dự kiến khi đối chứng200/đích login/revoke/Guest isolation đều đạt; mọi request failure khác vẫn fail công cụ.

## Bảo toàn phạm vi / review độc lập

- Không đổi schema/config/dependency/provider/CSRF/JWT hoặc layout/theme chung. Chỉ thêm tham số phân trang tùy chọn cần thiết, giữ body/default cũ.
- Baseline diff 16 file ngoài các file được mở rộng giữ nguyên byte-level patch; phần test QR+points có sẵn trong OrderCommerceIntegrationTest vẫn giữ nguyên. Image deletion, .agents và skills-lock có trước không bị đụng. Những file tools/run_full_e2e_all_roles.py và docs/test_reports xuất hiện thêm trong lúc chạy không thuộc lượt này, giữ nguyên.
- Reviewer scoped_fixes_final_review chỉ đọc, không sửa, không gọi cloud/chạy Maven; không thấy regression Critical/Important/Minor trong source/test phạm vi sửa. Họ không xác nhận browser hoặc MySQL/TiDB; root kiểm thực riêng.
- Không commit/push/merge; nhánh và HEAD không đổi. Ảnh UI được xem trực tiếp: reviews390 và payment host rỗng INDIVIDUAL390, không tràn ngang; script kiểm thêm768/1440.

## Trở ngại kiểm thử và cách xử lý

- Java21 được chọn từ JDK cài sẵn; không nâng dependency.
- Verify tại workspace:257 tests đạt0 failures/errors/skips, nhưng **BUILD FAILURE ở repackage**, không phải test: không rename target/utetra.war sang .original. Có tiến trình Java đang chạy từ target WAR; không tự dừng/ghi đè tiến trình đó.
- Tạo bản sao nguyên trạng input Maven trong `.local/scoped-fixes-20261028/build-input` (src/pom/wrapper và SQL proposal mà test đọc); không sửa pom/config. Chạy nguyên Maven Wrapper verify với `-f` tới bản sao, không skip test. So sánh316 input file với workspace; không khác biệt tại lúc kiểm. Artifact ở thư mục build riêng, không thay thế app đang chạy.
- UI harness ban đầu bị trùng fixture controller do dùng chung thư mục class tạm, sau đó dùng thư mục riêng. Script trước đã thao tác nhầm native select vốn được thay bằng combobox chung; chỉnh script dùng input data-choice thật. Logout test mở menu account trước, dùng account host để không bị popup nhắc thanh toán của thành viên chặn thao tác độc lập. Các lần harness lỗi được giữ ở .local, không đổi source ứng dụng để làm test xanh.
- Lượt native có16 check đạt nhưng báo1 ERR_ABORTED logout: giữ kết quả thô và bổ sung phân loại hẹp có đối chứng, không xóa request failure bằng regex rộng.

## Prompt và hồ sơ

Prompt nguyên văn và metadata theo docs/templates/PROMPT_ENTRY.md: [UTETRA-FIX-20261028.md](prompts/UTETRA-FIX-20261028.md). Nhật ký tổng và hai module được append đường dẫn/kết quả; không ghi khóa, token, OTP, grant hay password. Báo cáo này là kết quả xử lý các finding trong ALL_ROLE_AUDIT_20261028.md, không xóa lịch sử audit cũ.
