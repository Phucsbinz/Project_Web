# Nhật ký yêu cầu UI dùng chung

## UI-AI-001 — 22/10/2026

Prompt nguyên văn:

```text
hình như tôi chưa cấu hình sẵn 1 layout đồng bộ cho các nhánh để cách nhánh làm chung nên giờ mỗi người 1 kiểu frontend
có gì bạn chuẩn hóa 1 layout dùng chung tất cả các nhánh nha duy nhất 1 layout thống nhất và ghi md cho agent của thành viên khác biết sử dụng và chỉnh sửa cấu hình thống nhất ấy
```

Khảo sát: main/catalog dùng giao diện cũ; identify/order đã có giao diện mới; sáu JSP layout theo vai trò của bản mới sao chép cùng nội dung. Chọn bộ UI mới đang dùng trên identify/order, tập trung tại app.jsp, một mapping SiteMesh, shared assets và NavigationAdvice. Tách bundle khỏi nghiệp vụ order để đồng bộ vào cả sáu nhánh. Kết quả kiểm chứng/triển khai được ghi tại docs/SHARED_UI.md; không ghi secret.

## 26/10/2026 — Giao diện theo ảnh tham chiếu của Thanh

Yêu cầu: giữ chức năng cũ, áp dụng thiết kế trang chủ/menu/chi tiết và quản lý theo ảnh kem–xanh lá; sử dụng 30 ảnh UTETra đã tạo. Thực hiện trong layout/theme chung và JSP module, không đổi provider/datasource/security để sửa UI. Ảnh thumbnail thêm logo lá ở ba vòng tròn xanh; ảnh menu riêng cho 30 món. Thêm giỏ tóm tắt dùng /cart/data, thẻ KPI/bảng trạng thái/biểu đồ so sánh số liệu thật, giữ bảng cũ và luồng checkout/chi tiết. Kiểm ảnh và kết quả trình duyệt ở docs/demo. Chưa push theo yêu cầu Thanh.

## 26/10/2026 — Chọn món trước, chi nhánh sau

Thanh yêu cầu chọn chi nhánh tại bước lên đơn, bỏ các chi nhánh khỏi menu và tiếp tục áp dụng thiết kế popup đồ uống. Menu hiện gộp theo mã món chung; popup dùng lại trang chi tiết trong tài liệu cùng origin và template chung, không tạo layout/framework thứ hai. Giữ trang cửa hàng để tham khảo và sidebar quản lý. Backend migration/giỏ/checkout được ghi riêng trong BRANCH_SELECTION_20261026.md; chưa push GitHub.

## UI-REFERENCE-20261026 — Màu và phông theo ảnh

Prompt nguyên văn:

```text
[Image #1] bạn thiết kế giao diện theo màu và phông như này được không vẫn giữ đầy đủ chức năng
```

Ảnh người dùng: 2aOboR8CXHLLqRujaayM4hUEoJJikhocP0fkZwvY.jpg. Áp dụng trên shared CSS và app.jsp, giữ một layout. Dùng Be Vietnam Pro local (SIL OFL), nền kem/xanh trà và serif cho logo; giữ nguyên chức năng, endpoint và phân quyền. Không dùng ImageGen cho việc CSS/font. Kết quả kiểm thử: UI_REFERENCE_REFINEMENT_20261026.md. Chưa commit/push hoặc đồng bộ nhánh khác.

Kết quả UI-REFERENCE-20261026: Maven package chạy 10 render tests đạt, browser vòng đầu 21 route/viewport đạt. Review ảnh phát hiện tương phản wordmark footer và ghi chú sidebar cần chỉnh; đã sửa CSS, package lại với skipTests và kiểm browser cuối riêng. Không cộng test cũ 167 vào lượt này, không nhận skipTests là chạy test. Biên bản/hình/JSON nằm trong docs/UI_REFERENCE_REFINEMENT_20261026.md và docs/demo/reference-20261026.

## ALL-MODULE-UI-20261026

Prompt “tích hợp đầy đủ ui nha đừng để thiếu ui bên các phần khác” được gửi hai lần, sau đó “tiếp tục”; nguyên văn giữ trong AI_PROMPT_HISTORY.md. Cập nhật sidebar/dropdown/footer theo role, picker catalog và Shipper report, mobile order nav; giữ một layout và theme. Kiểm render 13 test và browser trên H2 riêng, biên bản ALL_MODULE_UI_20261026.md.

## CART-GROUP-20261026

Thanh yêu cầu cập nhật giỏ như trải nghiệm mua sắm chuyên nghiệp, badge số món, đặt nhóm và ưu đãi theo số người; đã chọn mốc đề xuất 3/5/10 người giảm 5/10/15%, trần 100.000đ, không cộng dồn mã giảm món. Cùng đợt xử lý yêu cầu trước: trang chủ sau đăng nhập chậm và đưa User về trang chủ. Không đổi theme/layout/provider. Contract và kết quả: docs/features/order-payment/GROUP_ORDERS_20261026.md.

## VIETNAMESE-TYPOGRAPHY-20261026

Thanh: “với lại bạn hãy chỉnh full lỗi font luôn đi nha”, cùng yêu cầu sửa phản hồi cà phê bị dùng mẫu trà thơm. Rà UTF-8 giao diện, bảng glyph của font local, weight/line-height/tracking dùng chung và popup. Giữ theme/layout, kiểm render và font các role ở desktop/mobile, kết quả ghi riêng docs/demo/FEEDBACK_TYPOGRAPHY_20261026.json.

## GROUP-ENTRY-JOIN-FIRST-20261027

Đặt đơn nhóm xuất hiện ngay hero trang chủ và có trang tạo nhóm/cách thanh toán. Sau join menu hiển thị context nhóm, popup thêm vào phần riêng, về nhóm xem bill. Chỉ host thấy điều khiển cách trả; member không có nút xác nhận thanh toán. Giữ một layout/theme và CSS module cart/group; kết quả browser 11 kịch bản và contract trong docs/features/order-payment/GROUP_JOIN_FIRST_COD_20261027.md.


## 2026-10-27 — Popup xác nhận và đi thẳng checkout nhóm
Prompt Thanh: đặt Đặt đơn nhóm cạnh Đặt món ngay; thay thông báo localhost bằng popup; chốt nhóm mở thẳng checkout, cập nhật thông tin giao hàng và giữ phương thức thanh toán của project. Hỏi thêm cách mỗi thành viên thanh toán riêng.
Kết quả: đổi thứ tự hero; thêm xác nhận Bootstrap dùng chung trong site-ui.js, dùng cho giỏ/nhóm/địa chỉ/gỡ nội dung/ngừng bán; không còn native confirm/alert/prompt trong JS module. Hủy popup không thực hiện mutation. Nút chốt nhóm mở /checkout trực tiếp, giữ bước chi nhánh và địa chỉ/người nhận/điện thoại/khu vực/nhà vận chuyển. HOST dùng COD, bank transfer hoặc VNPAY khi provider hiện tại khả dụng; INDIVIDUAL giữ một đơn COD, mỗi người trả phần tiền đã giảm và phí giao được chia khi nhận, không chờ xác nhận và không tự trừ tiền online. Chỉ host đổi cách trả. Không đổi datasource/provider.
Sửa thêm race Bootstrap transition: khi thêm món trước khi popup hiện xong, ghi nhận yêu cầu đóng rồi đóng tại shown.bs.modal.
Kiểm thực: 77 tests (69 OrderCommerceIntegrationTest + 5 CatalogViewTest + 3 BaselineViewTest) qua; package bản cuối qua; python tools/check_all_ui.py --shopping: 11 kịch bản qua, gồm hủy/xác nhận popup, join/chọn món/chốt nhóm đi thẳng checkout/đơn COD. Lần browser đầu phát hiện race đóng popup, đã sửa và chạy lại qua. check_shared_ui và git diff --check qua. Không tạo đơn cloud, không thu tiền thực. Chưa commit/push.


## 2026-10-27 — Đồng bộ ba nút hero
Thanh yêu cầu ba nút Khám phá thực đơn, Đặt món ngay, Đặt đơn nhóm hài hòa hơn. Đổi Đặt món ngay sang cùng class btn btn-light đang dùng cho hai nút còn lại: ba nút cùng nền xanh trà, chữ trắng, viền/padding/bo góc và hover chung của hero. Không đổi chức năng, thứ tự hay layout chung.
Kiểm thực: CatalogViewTest 5/5 qua trên Tomcat/SiteMesh, WAR build thành công; shared UI và diff check qua. Chưa commit/push.


## 2026-10-27 — Nhập thông tin giao hàng trực tiếp
Prompt Thanh: khối địa chỉ thừa, phần chọn thông tin đã lưu chỉ có dropdown và không nhập được.
Thực hiện: gộp người nhận/điện thoại/địa chỉ vào chính checkout-form, bỏ khối thêm địa chỉ riêng và hướng dẫn thừa. Dropdown địa chỉ cũ chỉ để điền nhanh; các ô luôn sửa được. Khi xem báo giá, tự lưu thông tin mới qua API địa chỉ có validation hiện tại; thông tin không đổi không tạo bản trùng. Địa chỉ cũ không bị ghi đè; thay đổi thông tin làm mất hiệu lực báo giá cũ. Bổ sung phone trong checkoutChoices để điền đúng liên hệ. Khu vực/carrier/payment vẫn là danh sách cấu hình hợp lệ. CSS checkout.css scope checkout-page, đồng bộ chiều ngang các bước.
Kiểm thực: 77 tests render/commerce qua, WAR build qua; 11 shopping browser scenarios qua, gồm nhập trực tiếp, tự lưu, báo giá lần hai không nhân bản địa chỉ, đặt đơn và nhóm COD. Shared UI/diff check qua. Lần chạy JVM đầu hết CodeCache, chạy lại với ReservedCodeCacheSize=128m thành công. Không đặt đơn cloud; chưa commit/push.


## 2026-10-27 — Ô nhập có gợi ý cho checkout
Prompt Thanh: bỏ Địa chỉ đã lưu riêng, cho nhập và lưu địa chỉ để lần sau bấm chọn hoặc nhập mới; áp dụng thanh toán từ giỏ hàng và các ô khác nữa.
Kết quả: bỏ select địa chỉ riêng, chỉ giữ ô Địa chỉ editable có popup lịch sử theo tài khoản (tên/điện thoại đi kèm), hỗ trợ bàn phím. Nhập đủ thông tin hợp lệ và rời ô sẽ lưu qua API địa chỉ; trước báo giá vẫn bảo đảm lưu xong. Dùng lại bản trùng chính xác thay vì thêm bản mới. Tên/điện thoại có datalist lịch sử. Chi nhánh/khu vực/carrier/ưu đãi/phương thức thanh toán có ô gõ tìm bằng datalist, giữ ID hợp lệ nội bộ, không cho tạo phương thức thanh toán hoặc khu vực tùy ý. checkout-fields.js đồng bộ gợi ý ưu đãi theo availability, không đổi quy tắc nghiệp vụ. Ghi chú/điểm vẫn nhập trực tiếp (điểm nhóm COD theo giới hạn cũ). Giỏ hàng và nhóm cùng dùng /checkout nên cùng nhận giao diện này.
Kiểm thực: 8 tests render CatalogView/Baseline qua trước mở rộng các ô; bản cuối WAR package thành công, 11 shopping browser scenarios qua (gõ tìm chi nhánh, chặn khu vực không hợp lệ, nhập địa chỉ mới, chọn gợi ý và điền tên/điện thoại, không nhân bản khi báo giá lại, checkout từ giỏ và nhóm COD). Shared UI/diff check qua. Server mysql đã mở lại; không tạo đơn cloud, chưa commit/push.


## 2026-10-27 — QR và báo giá checkout
Prompt Thanh: chỉ thấy COD, cần QR; Xem báo giá không được; hỏi địa chỉ khác khu vực giao thế nào.
Tìm được lỗi ô gợi ý ưu đãi: nhãn Không dùng tương ứng ID rỗng bị matcher bỏ qua, làm custom validity chặn báo giá. Sửa matcher/list hỗ trợ lựa chọn rỗng cho ô optional, vẫn giữ ID bắt buộc cho khu vực/carrier/branch. Thêm báo lỗi có tên ô khi quote bị HTML validation chặn, thông báo khi thông tin thay đổi giữa chừng. QR Techcombank hiện đã bật; thêm nút chọn nhanh COD/QR để không phải tìm trong gợi ý. HOST/personal có QR; INDIVIDUAL giữ COD theo contract đã chốt, hiển thị giải thích muốn QR toàn đơn thì host chọn trả toàn bộ trước chốt. Không giả lập QR từng thành viên hoặc tự đánh dấu thanh toán. Đổi nhãn Khu vực giao thành Khu vực tính phí giao hàng; địa chỉ là điểm đến cụ thể, vùng tính phí vẫn do người dùng chọn theo cấu hình.
Kiểm thực: 77 tests render/commerce qua; 11 browser shopping scenarios qua gồm lỗi khu vực hiển thị, Không dùng cả hai ưu đãi vẫn xem báo giá, chọn QR/tạo đơn H2/hiện ảnh QR, nhóm COD. Sau đó chỉ đổi nhãn khu vực và package bản cuối thành công; shared UI/diff check qua. Không chuyển tiền/đặt đơn cloud. Chưa commit/push.


## 2026-10-27 — Chia phần bằng QR và popup thành viên
Prompt Thanh: phần thanh toán cồng kềnh/không thấy QR; chia phần vẫn cần QR; chủ nhóm đặt thì thành viên chỉ nhận popup, không chờ xác nhận.
Thực hiện: một nhóm radio COD/QR, bỏ chọn nhanh và input trùng; INDIVIDUAL cho BANK_TRANSFER và COD, giữ cấm điểm chủ nhóm, hash báo giá có paymentMode. Mỗi người xem số tiền được chia cùng QR/nội dung chuyển trong group view; host chia QR đến trang nhóm thay vì màn QR tổng đơn. Endpoint notices lọc user hiện tại và không gửi host, popup toàn trang cho USER/VENDOR (chuẩn hóa ROLE_ prefix), kiểm 8 giây khi tab hiện và khi quay lại, localStorage đánh dấu đã xem. Không chuyển sang host trả chung khi chọn QR, không có member-confirmation gate. Đối soát ngân hàng hiện tại vẫn cấp tổng đơn, không tự ghi thanh toán thành công. Tài liệu mới GROUP_SPLIT_QR_POPUP_20261027.md.
Kiểm thực: 77 tests trước bộ tinh chỉnh endpoint/hướng dẫn, sau đó 69 OrderCommerceIntegrationTest qua; package bản cuối qua. Shopping UI cuối 11 kịch bản qua, gồm QR chia phần tạo đơn ngay, host thấy đúng QR phần mình, member ở trang menu nhận popup rồi mở đúng phần QR; không JS errors. Lần kiểm đầu phát hiện role trong layout có tiền tố ROLE_USER nên popup bị skip; đã sửa và kiểm lại qua. Shared UI/diff check qua. H2 riêng, không đơn cloud/không chuyển tiền thực. Server đã mở lại. Chưa commit/push.


## 2026-10-27 — Sửa lỗi báo giá từ tình huống Chrome thực tế
Thanh báo nút Xem báo giá không dùng được. Dùng computer-use đọc cửa sổ Chrome checkout đang mở: order-message báo ưu đãi nhóm không cộng dồn mã giảm món; form đang chọn Giảm 10% đơn từ 60.000đ. Đây là lỗi lựa chọn xung đột, không phải thiếu QR. Không ghi thông tin liên hệ khách vào log.
Sửa CheckoutController xác định nhóm có ít nhất 3 người có món/confirmed. JSP khi có giảm nhóm thay ô mã món bằng Đã áp dụng ưu đãi nhóm và productPromotionId rỗng; backend tiếp tục chặn mã cộng dồn nếu gửi trực tiếp. Ưu đãi phí giao giữ như cũ. Thêm quote-error ngay gần nút với scroll khi lỗi, thay lỗi báo giá ở đầu trang; xóa cảnh báo cũ khi thử báo giá mới. Không đổi hạn mức hoặc phương thức thanh toán.
Kiểm thực: 77 tests render/commerce qua, WAR package thành công, 11 shopping UI scenarios qua (assert nhóm 3 người không có ô nhập mã món, mã món rỗng, báo giá và tạo đơn QR chia phần thành công, member popup đúng). Shared UI/diff check qua. Server mysql mở lại; không tạo đơn cloud, chưa commit/push.


## 2026-10-27 — Bảng chi tiết thanh toán tự động
Prompt Thanh: báo giá chuyên nghiệp hơn, có sẵn dưới phương thức thanh toán rồi chỉ xác nhận thanh toán.
Thực hiện: bỏ nút Xem báo giá, panel Chi tiết thanh toán luôn hiện dưới COD/QR; có tiền món, giảm món/nhóm, điểm, phí giao, giảm giao và tổng nổi bật định dạng VND. Debounce 650ms gọi quote server sau khi form hợp lệ, tự lưu địa chỉ trước quote. Khi sửa bất kỳ thông tin/availability đổi: bỏ hash, xóa số cũ, khóa confirm đến kết quả mới đúng generation. Không gửi quote khi form chưa hợp lệ; trạng thái thiếu thông tin/loading/lỗi rõ, chỉ có nút thử lại khi lỗi. Form submit chỉ chấp nhận hash ready và chặn gửi lặp; thanh toán QR vẫn sau tạo đơn, không tự ghi PAID. CSS scope checkout-page, cùng layout và API hiện tại.
Kiểm thực: 77 tests render/commerce qua, package bản cuối qua. Browser lần đầu phát hiện input+change (blur ghi chú) vô hiệu quote dù giá trị không đổi; thêm fingerprint dữ liệu và cập nhật sau quote để bỏ sự kiện trùng, vẫn force invalidate cho availability. Browser chạy lại 11 kịch bản qua, gồm tổng tiền tự lên không bấm báo giá, sửa ghi chú khóa confirm rồi tự mở với hash mới, blur/click xác nhận chỉ một lần tạo đơn QR, QR nhóm và popup thành viên. Đã xem screenshot mobile bảng tiền; shared UI/diff check qua. Server mysql mở lại, không đặt đơn cloud/không chuyển tiền thực, chưa commit/push.


## 2026-10-27 — Kiểm đồng bộ checkout cá nhân với nhóm
Prompt Thanh: kiểm thanh toán bình thường và làm giống thanh toán nhóm.
Đọc lại routing: cả giỏ cá nhân lẫn nhóm đều vào CheckoutController /checkout và cùng order/checkout.jsp, bảng Chi tiết thanh toán tự động, chọn COD/QR và nút Xác nhận và đặt đơn. Không tạo trang/layout riêng. Bổ sung browser scenario đi từ chính checkbox/nút thanh toán giỏ cá nhân, chọn chi nhánh/liên hệ đã nhập, tự tính tổng và tạo COD; kịch bản QR cá nhân và QR nhóm giữ nguyên để so sánh.
Kết quả thực: 12 shopping UI scenarios qua, 0 JS errors; COD cá nhân từ giỏ tự lên tổng 50.000đ trong fixture, tạo đơn xong giỏ trống; QR cá nhân hiện QR sau đặt; nhóm chia QR và popup thành viên vẫn qua. Lần đầu phần mới đã đặt COD thành công nhưng assertion kịch bản nhóm sau đó còn kỳ vọng giỏ giữ 1 ly; cập nhật kỳ vọng giỏ 0 vì COD đã mua hết và chạy lại cả bộ qua. Shared UI/diff check qua. Không sửa thêm nghiệp vụ hoặc tạo layout riêng; server đang chạy bản chung. Chưa commit/push.


## 2026-10-27 — Tên người dùng trên nút tài khoản
Prompt Thanh: sau đăng nhập thay chữ Tài khoản bằng tên người dùng, luôn một dòng.
NavigationAdvice cung cấp currentDisplayName lấy full_name hiện tại, fallback username nếu thiếu; bỏ query cho guest, POST và yêu cầu JSON. app.jsp escape nội dung và title bằng c:out. CSS dùng white-space nowrap/ellipsis/max-width desktop190px mobile125px, icon/mũi tên không co. Tên đổi trong hồ sơ được phản ánh ở lần tải trang sau, không lấy tên cũ trong token. Giữ menu tài khoản và một layout.
Kiểm thực: 8 tests render qua, WAR build thành công; 13 shopping browser scenarios qua gồm header tên User 1/full title/nowrap/ellipsis sau đăng nhập và các luồng đặt đơn. Shared UI và diff check qua. Server đã mở lại, chưa commit/push.


## 2026-10-27 — Trang thanh toán riêng cho thành viên nhóm
Prompt Thanh: chủ nhóm đặt QR xong mở trang thanh toán riêng, không quay lại nhóm; thành viên nhấn Tiếp tục popup cũng vậy.
Tạo GET /group-orders/{token}/payment và group-payment.jsp trong module order, dùng layout/theme chung. GroupOrderService.paymentPage bắt buộc tài khoản là thành viên có phần món trong đơn đã tạo, chỉ trả bill của người đó, không nhận userId phía client. HOST trả chung xem tổng đơn; thành viên HOST không có nghĩa vụ trả riêng. INDIVIDUAL xem tiền món/giảm/phí giao/amountDue của chính mình. QR chỉ hiện nếu còn UNPAID và đơn chưa hủy; PAID/cancel/COD có nội dung tương ứng, không tự ghi đã trả. Có nút cập nhật trạng thái. Chốt QR chia phần redirect thẳng /payment; popup Tiếp tục cũng /payment. Bỏ QR inline khỏi group.jsp, thay link mở lại trang cá nhân. Tránh popup thông báo lặp nếu đang ở trang payment của nhóm đó. CSS scope group-payment-page trong cart.css. Không thêm layout/provider/datasource.
Kiểm thực: 77 tests render/commerce qua; WAR build thành công; 13 shopping browser scenarios qua, gồm host checkout QR tới /payment và member popup Tiếp tục tới /payment có QR; không JS errors. Shared UI/diff check qua. Server mở lại. Không đặt đơn cloud, chưa commit/push.


## 2026-10-27 — Thay QR bằng ví giả lập theo repo tham khảo
Prompt Thanh: copy QR từ CNPM_PROJECT_CK, thay luồng cũ, đủ toàn bộ thành viên thanh toán mới được xác nhận đơn. Thanh đã chọn ví QR giả lập local, không trừ tiền thật.
Đã tham khảo repo commit 626379251de7d31b12e3093d369cbb3d318d5beb, triển khai LOCAL_QR có token/hash, QR nội bộ, trang ví CSRF+consent, ledger từng người và gate nghiệp vụ toàn bộ phần tiền trước CONFIRMED. Checkout mới thay lựa chọn chuyển khoản cũ bằng ví thử nghiệm; BANK_TRANSFER mặc định tắt, giữ lịch sử cũ. Không mang khóa repo hoặc thay datasource/JWT. 188 full tests trước tinh chỉnh cuối qua, 75 commerce tests bản cuối qua, 14 browser scenarios cuối qua (gồm 3/3 gating và QR local), shared UI/diff qua. Migration 2703 applied/server ready. Không đơn cloud hoặc chuyển tiền thật. Chưa commit/push. Chi tiết và hướng dẫn LAN: docs/features/order-payment/LOCAL_QR_WALLET_20261027.md.


## 2026-10-27 — Hủy/hết hạn nhóm, thẻ nhận diện và badge host
Prompt Thanh: host hủy nhóm và notify thành viên, nhóm hết hạn notify mọi người; nhiều nhóm khác nhau, QR cá nhân như nhóm, bỏ nhãn demo/thử nghiệm; thêm Chủ nhóm cạnh tên. Đã giải thích Thanh toán QR là luồng nội bộ, không số dư/ngân hàng.
Thực hiện: group lifecycle notices, EXPIRED riêng, cleanup chỉ cart nhóm, host hủy cả đơn NEW qua workflow nhưng không vượt trạng thái CONFIRMED. Thẻ nhóm có mã/tên host/số thành viên/time/status, cho tối đa 5 active nhóm; badge host cạnh tên. Các nhãn thanh toán mới trung tính, giữ dòng không trừ tiền ngân hàng. Lifecycle popup hoạt động cả ở QR, có khóa modal để không chồng. Kiểm 81 commerce tests, WAR build và 16 browser scenarios qua; clock fixture xác thực expiry không nằm trong WAR. Chi tiết GROUP_LIFECYCLE_20261027.md. Chưa commit/push.


## 2026-10-27 — Buộc rời nhóm đã đóng
Prompt Thanh: đã hủy nhóm nhưng thành viên vẫn ở được trong nhóm.
Nguyên nhân: group view trước đó vẫn hiển thị danh sách thành viên ở trạng thái readonly và popup Tiếp tục lại mở chính nhóm đã đóng.
Sửa: view terminal CANCELLED/EXPIRED trả joined=false, memberCount=0/members rỗng; giữ snapshot DB chỉ phục vụ lịch sử và notification. Nhóm terminal không còn trong mine/list Nhóm của bạn. HTML GET link cũ redirect /group-orders/new; host hủy cũng về danh sách. Poll trang nhóm và lifecycle kiểm trạng thái đóng, đẩy khỏi cả /group-orders/token và /menu?group=token; popup được hiện trên trang danh sách và Tiếp tục không quay lại nhóm đóng. Sự kiện hủy/hết hạn vẫn lấy từ lịch sử riêng nên không mất thông báo khi bỏ khỏi list active. Không xóa dữ liệu giao dịch/đơn hàng.
Kiểm thực: 82 commerce tests qua, WAR build thành công; 16 shopping browser scenarios qua, gồm member đang trong nhóm khi host hủy tự ra danh sách, popup đúng mã nhóm, thử link cũ vẫn bị chuyển ra; expiry tương tự. Shared UI/diff check qua. Server mở lại; chưa commit/push.


## 28/10/2026 — Merge integration, QR Internet, countdown 10 phút
Prompt: Thanh yêu cầu merge source integration trước, giữ UI mới; QR mọi thiết bị Internet, trang thanh toán chuyên nghiệp, quay về nhóm và hiển thị tiến độ, 10 phút đếm ngược, nhắc nhận đơn/để ý điện thoại. Thực hiện và kiểm chứng: QR_INTERNET_20261028.md trong docs/features/order-payment. Chưa push; không ghi khóa/token/mật khẩu.


## 28/10/2026 — CTA, typography, COD chủ nhóm và popup theo vai trò
Thanh yêu cầu ba nút Khám phá thực đơn / Đặt đơn / Đặt đơn nhóm; tiêu đề đồng nhất font/weight; chủ nhóm nhận nhắc điện thoại, thành viên nhận đặt thành công. COD chuyển HOST ở quote và khi tạo đơn, giữ chia tiền QR. Khách có danh sách/chi tiết/lịch sử; chi tiết tự tải trạng thái mới khi không thao tác form/modal. Kiểm chứng ghi tại GROUP_CHECKOUT_REFINEMENT_20261028.md. Không push.


## 28/10/2026 — Quản trị dễ dùng, dropdown rõ ràng
Prompt: Thanh yêu cầu dễ thao tác quản trị và nút/dấu mở danh sách. Thay đổi và kiểm chứng tại ADMIN_USABILITY_20261028.md. Giữ layout/theme/quyền hiện hành; không push.


## 28/10/2026 — Thành viên xem trạng thái đơn nhóm
Prompt: trạng thái đơn thì thành viên cũng phải coi được. Mở quyền đọc đúng participant snapshot, danh sách/chi tiết/link theo dõi và thông báo; không mở quyền thao tác chủ nhóm. Thực hiện và kiểm chứng tại docs/features/order-payment/GROUP_MEMBER_TRACKING_20261028.md. Chưa push.


## 28/10/2026 — Hủy khi thanh toán một phần và căn giữa mũi tên
Prompt: còn 1 người chưa thanh toán nhưng chủ nhóm hủy thì sao; căn giữa mũi tên thả xuống toàn web. Sửa ghi nhận phần đã trả/chờ hoàn, Admin hoàn đúng phần thực tế, khóa QR sau hủy; chevron flex-centered và sửa margin mobile. Chi tiết PARTIAL_GROUP_CANCEL_20261028.md. Chưa push bản sửa này.


## 28/10/2026 — Popup hủy nhóm và hoàn sau 30 phút
Prompt: chủ nhóm cần cảnh báo/xác nhận mới hủy; thành viên nhận popup nhóm bị hủy, đã trả có thêm dòng hoàn sau 30 phút. Notice dựa phần thanh toán cá nhân, thời gian dự kiến; không tự xác nhận hoàn. Thực hiện và kiểm chứng tại PARTIAL_GROUP_CANCEL_20261028.md. Chưa push.
