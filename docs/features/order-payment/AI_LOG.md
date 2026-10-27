# ORD — Nhật ký AI của nhánh feature/order-payment

## ORD-AI-011 — Đồng bộ catalog/identity — 21/10/2026

Prompt nguyên văn: `hoàn chỉnh phần code bên nhánh order của tôi và xem code của cách nhánh chức năng khác rồi chình cho đồng bộ`.

Đọc catalog 812e26e và identify 78facd2; lưu sửa media thành 5882d52 rồi merge identify bằng 28e8c18. Đồng bộ ledger, khóa user, scope, danh mục tắt, JSON và validation số. Sửa helper Java để dùng JDK 21/Maven Wrapper trên máy thành viên. Thêm 5 test HTTP contract; clean verify 130 tests đạt, WAR mysql validate 6 migration không chạy mới và 7 URL trả 200. Phát hiện class policy dư trong WAR cũ, xử lý bằng clean build. Đã dừng process kiểm chứng. [Kết quả/giới hạn/lệnh](INTEGRATION_20261021.md); không ghi secret và không push sang nhánh thành viên khác.

## ORD-AI-010 — Rà bổ sung trên máy Windows

Prompt nguyên văn: `xem còn thiếu sót gì thì làm đi`.

Đã rà code giỏ/checkout/COD, quyền operator/shipper, trả hàng và media; sửa kiểm tra nội dung ảnh JPEG/PNG khớp MIME, ảnh hỏng/vượt giới hạn, phản hồi validation tiếng Việt và kiểm kết quả xóa Cloudinary. UI kiểm lý do/định dạng/dung lượng trước gửi; server vẫn kiểm độc lập. Thêm regression tests cho file giả MIME, hỏng, quá kích thước/dung lượng, provider delete lỗi và cleanup không che lỗi gốc. Maven Wrapper/JDK 21: verify đạt 62 tests, WAR thành công; upload/xóa PNG thật đạt. Chưa chạy lại full browser/native DB; chưa push các sửa code này. Chi tiết ở VERIFICATION.md; không đưa secret vào nhật ký.


Trạng thái khởi tạo hồ sơ (trước triển khai): chưa có prompt triển khai thật từ thành viên. Yêu cầu khởi tạo hồ sơ đã ghi tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi mọi prompt thực dùng cho 21 UC P0 và 3 UC Shipper P1 của order-payment theo IMPLEMENTATION hiện tại: giỏ/checkout/COD/transaction/socket/report, sửa lỗi, sơ đồ, ảnh diễn giải code/UI, setup và review. Dùng [mẫu prompt](../../templates/PROMPT_ENTRY.md), ID ORD-AI-001... Lưu response/code/nguồn hình/ảnh, cách nhóm sửa, testcase và commit. Không ghi QR gateway đã tích hợp hoặc test đã qua nếu chưa thực hiện.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.

## ORD-AI-001 đến ORD-AI-008 — triển khai và yêu cầu push ngày 14/10/2026

Prompt nguyên văn tập trung tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md#ord-ai-001). Người dùng yêu cầu làm đầy đủ order-payment trên chính nhánh feature, hoãn thanh toán ngân hàng, thay cửa sổ trả từ 48 giờ sang **3 giờ**, hoàn **cả đơn**, rồi yêu cầu push đúng nhánh. Không tự gán tên thành viên/reviewer hoặc giờ gửi prompt.

Phản hồi thực hiện: bổ sung Entity/JPA/DTO/Repository → Service → Controller → JSP/JS và test cho 20 UC bắt buộc (VNPAY hoãn) + 3 Shipper có flag. Code ở src/main/java/com/utetra/order; migration V2 tạo 19 bảng own; contract/schema module khác ghi ở SCHEMA_PROPOSAL, không tự migrate bảng của nhánh khác. OrderLoyaltyBridge là adapter cần review owner identity, không phải xác nhận identity module đã triển khai.

| Phần | Công việc thật | Kiểm chứng |
| --- | --- | --- |
| Giỏ/checkout | DB + session, merge/version/UUID, giá và snapshot server, quota/điểm, atomic rollback | Cart/checkout/merge/quote/concurrency tests |
| COD/trả–hoàn | State machine, thu tiền đúng quyền, return < 3h, review/receive/waive, evidence COD, full refund, reversal/debt | Workflow và cutoff/refund/point debt tests |
| Vận hành | Carrier/rate scope, hoa hồng mới giữ snapshot, doanh thu ngày thu/hoàn, CSV, assignment/failure/notification owner | Scoped operations, commission/report/shipper tests |
| Realtime/media | Durable SQL notification, after commit STOMP, revoked-session guard, reconnect/polling; upload rollback cleanup | Socket guard/event-delivery/media adapter tests; WebSocket trình duyệt thật, chưa test Cloudinary thật |
| Giao diện | JSP chung Bootstrap, cart/quote/order/refund, tools/report/notifications và Shipper | Playwright thao tác thật; desktop/mobile/tablet; ảnh test trong assets |

### Lỗi đã phát hiện và sửa qua kiểm chứng

- Entity generator đọc nhầm header tiếng Việt thành cột: sửa parser và mapping trước test; native JPA validate xác nhận mapping/schema.
- Persist entity mới trước khi điền trường bắt buộc; stale resource V2 cũ trong target; normalize BigDecimal để quote hash không đổi vì scale; sửa và clean build.
- Cart version không tăng với đồng hồ cố định; guest merge key bị mất sau sửa giỏ; đổi timestamp microsecond và giữ merge key.
- Cart/checkout khóa khác thứ tự có nguy cơ deadlock: thống nhất store trước catalog, user serialize mutation giỏ/checkout.
- JSP EL diễn giải template literal JS; thay nối chuỗi và kiểm JSP chạy thật.
- CSRF cookie tạo quá muộn sau SiteMesh; materialize trước decorator. JWT bị SessionManagementFilter coi là login mới ở mỗi asset request, liên tục xoay cookie: dùng NullSecurityContextRepository và bỏ filter quản lý login tự động; giữ CSRF/XOR và DisableEncodeUrlFilter. Thêm test GET → tải assets → POST hợp lệ và header giả 403.
- MySQL REPEATABLE READ đọc snapshot cũ sau chờ user lock, tạo duplicate checkout trong test native dù H2 pass: mutation dùng READ_COMMITTED; quota program có current locking read. Native concurrency/idempotency/quota chạy lại.
- Mã đơn dài tràn màn hình mobile: text-break; kiểm 390px và 768px.
- Return bị từ chối vẫn có thể gửi lại dù unique một request/đơn; assignment còn ACTIVE sau hủy; sửa ràng buộc và regression test.
- Broker lỗi sau commit không được báo purchase thất bại; giữ thông báo SQL/polling và test broker outage/rollback không phát event.

[VERIFICATION](VERIFICATION.md) ghi lệnh, kết quả cuối và giới hạn. Build không đóng gói test helper/token vào WAR. Không sửa main hoặc feature của thành viên khác; commit code `29aac50`; commit tài liệu và push feature/order-payment theo ORD-AI-008, xác nhận SHA remote sau push. Review người thật, ghép integration, DOCX/ảnh code có callout và provider thật còn cần nhóm thực hiện.

Commit code thực: [29aac50](https://github.com/Phucsbinz/Project_Web/commit/29aac509163236b88f44834ad45c4036cca5a89f). Minh chứng test tóm tắt: [test-results.txt](assets/test-results.txt).

## ORD-AI-BANK-20261024 — Bổ sung QR ngân hàng trên integration

Prompt nguyên văn và ảnh nguồn được ghi tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md), mục ORD-AI-BANK-20261024. Source và quy trình thực: [BANK_TRANSFER](BANK_TRANSFER.md). Kết quả kiểm chứng, giới hạn và việc giao từng người: [REMEDIATION](../../REMEDIATION_20261024.md). Không sửa/push nhánh feature/order-payment trong lượt này; code đang ở workspace integration, chưa commit. QR tài khoản tĩnh do người dùng cung cấp, không phải ảnh AI sinh; Admin đối soát thủ công, không giả callback ngân hàng.

## CREATIVE-AI-20261024 — JWT lưu DB và checkout realtime

Prompt nguyên văn trong [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md). Đã bổ sung hash/JTI/hạn access JWT, finite stock và chống ghi đè số lượng, socket availability/voucher sau commit, polling và test tranh suất cuối. [Source, contract, kết quả và giới hạn](REALTIME_CHECKOUT_JWT.md). Verify cuối 153 test đạt; browser STOMP dùng fixture H2 riêng, WAR/TiDB migration thứ 8 đã chạy. Không giao dịch ví/ngân hàng thật, không seed cloud hoặc push.

## FINAL-AUDIT-20261024 — Đối chiếu đầy đủ và bổ sung

Prompt đầy đủ và lệnh “tiếp tục” được ghi trong [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md). Thực hiện trên integration: VNPAY URL ký và IPN, audit, chống callback trùng, REVIEW/late payment/refund; Manager carrier đúng shop; cập nhật UC-ORD-06, UC-MGR-06, setup và bằng chứng báo cáo. [Trạng thái đầy đủ](../../FINAL_REQUIREMENTS_STATUS.md), [VNPAY](VNPAY.md). Clean verify 161 test đạt; browser fixture H2 không lỗi JS; WAR/TiDB 9 migration và 8 HTTP checks. Chưa có merchant thật, không gọi giao dịch ngân hàng, không seed commerce cloud, không commit/push. Không coi ảnh H2 hoặc callback giả lập là giao dịch cổng thật.

## 2026-10-26 — Giỏ hàng, badge và đặt nhóm

- Thanh yêu cầu giỏ chuyên nghiệp như trải nghiệm shopping, số món trên icon và đặt nhóm tham khảo Grab. Thanh đã duyệt 3/5/10 người giảm 5/10/15%, trần 100.000đ, không cộng dồn mã giảm món. Contract/luồng/phạm vi tại GROUP_ORDERS_20261026.md.
- Cart JSP/CSS/JS scoped theo module, ảnh/giá/tùy chọn/tổng, stepper, nhập giỏ guest; header dùng count endpoint nhẹ, cập nhật event và badge mobile. Nhóm UUID có hạn 24h, unique tài khoản có snapshot món, host-only chốt/hủy, version chống stale, cùng thương hiệu, 50 ly/20 dòng đơn vị, giới hạn byte snapshot, trạng thái khóa và lưu discount snapshot tại order. Mốc tính bằng số tài khoản xác thực đã gửi món, không tin số người/tiền client.
- Test: full verify 176 đạt, không lỗi/skipped. Sau bổ sung chi tiết món nhóm và tối ưu import đã chạy lại 62 OrderCommerceIntegrationTest + 5 CatalogViewTest + 3 BaselineViewTest, 70 đạt, build WAR thành công. Tests gồm unique/stale/concurrency, host-only, cap/tiers, 50 ly, expiry/cancel, giỏ khóa, branch, idempotent checkout, không cộng dồn mã và giữ giỏ người khác.
- Edge/Tomcat/H2 riêng: 7 kịch bản toàn luồng đạt; mobile390/desktop1440 không tràn ngang, không lỗi JavaScript. Đã xem ảnh cart/group mobile. Lượt đầu UI fixture thiếu image_url nên assertion ảnh thất bại; bổ sung ảnh fixture và chạy lại đạt, không sửa dữ liệu cloud. Công cụ source tools/check_all_ui.py --shopping đã chạy thành công; hỗ trợ cả classpath fork/nofork.
- Server thật mở lại cổng8080, migration nhóm được áp dụng không reset dữ liệu. Không tạo đơn nhóm cloud, không gọi thanh toán hoặc gửi mail thật. Kết quả tại SHOPPING_VERIFICATION_20261026.json. Chưa commit/push thay đổi đợt này.

## 2026-10-27 — Tick chọn, hạn 1 giờ, form checkout và toast

- Yêu cầu trước còn tiếp tục: bỏ chữ demo/mô phỏng trên nội dung hiển thị, giải thích local group, ô nhập bị không điền được, checkbox giỏ và hạn tự reset. Thanh chọn “1 tiếng đi”. Yêu cầu tiếp: sau bấm bỏ vào giỏ có popup thành công và dòng nhắc sau 1 tiếng nếu chưa thanh toán.
- Đã làm is_selected + endpoint chọn lưu server cho guest/đăng nhập, chọn tất cả và tổng chọn. Quote/branch/availability/checkout chỉ dùng selected; giữ dòng bỏ tick khi tạo order. Nhóm đã chốt khóa chọn/sửa.
- Hạn updated_at+1h, read không renew; timestamp thật + activity_revision để version thay đổi cả khi cùng timestamp. Đã bắt lỗi test mốc đúng 1h do touch cũ cộng microsecond; tách revision rồi test qua. Cleanup production mỗi phút, guest snapshot TTL/merge key và session1h, đồng hồ UI bù chênh giờ server/client. Không hủy order đã đặt.
- Form thêm địa chỉ inline, tự chọn sau lưu; note textarea; quote bị invalidated khi gõ/chọn. Browser bắt lỗi thứ tự nạp utetra.uuid, đã cho script checkout khởi tạo sau DOMContentLoaded và kiểm lại thao tác thật thành công. UUID fallback cho LAN HTTP, copy link có fallback chọn tay.
- Toast chung chỉ khi add thành công, header+small nhắc thời hạn giỏ 1h. Không gọi giỏ là đơn đã đặt để tránh hiểu nhầm tự hủy order.
- Full verify 180 đạt. Sau sửa init JSP cuối chạy 8 render tests đạt và build WAR. Browser H2 10 kịch bản qua, không lỗi JS; đã xem ảnh nhập liệu trên mobile. Không đặt đơn hoặc thêm địa chỉ cloud khi kiểm. Đã áp migration lên DB thật khi start server. Chưa push. Hướng dẫn local/LAN và contract: CART_SELECTION_EXPIRY_20261027.md; JSON kết quả CART_RETENTION_VERIFICATION_20261027.json.

## 2026-10-27 — Tham gia trước, chủ nhóm quyết định trả tiền

- Thanh sửa luồng: join trước rồi mới chọn món; entry Đặt đơn nhóm ở trang chủ. Bổ sung chọn host trả hết/tự trả từng phần. Khi làm rõ, Thanh yêu cầu không chờ thành viên xác nhận thanh toán và chọn COD: chủ nhóm đặt ngay, mỗi người trả phần sau giảm khi nhận. Chỉ host quyết định cách trả.
- Thực hiện create/enroll rỗng, menu/product mang group context và API giỏ riêng từng member; không import giỏ cá nhân. Add/remove chỉ phần của chính mình. Người có món tự đủ điều kiện đếm, không có nút xác nhận thanh toán. Host chốt phần hiện có, không đợi người chưa chọn. Giữ unique/version/50 ly/cùng thương hiệu và khóa sửa sau chốt.
- payment_mode HOST/INDIVIDUAL chỉ host được đổi lúc OPEN. Một đơn chung, COD chia phần không dùng bank/VNPAY hoặc điểm host. Lưu bill member từ đơn giá/topping snapshot sau ánh xạ branch; giảm phân bổ phần dư lớn nhất để không gán giảm âm cho phần miễn phí, shipping ròng chia đều, tổng bill đúng order.finalAmount. Không tự trừ tiền/PAID. V2702 thêm trạng thái member và bill columns, FK order.
- Test: full verify 182 đạt; sau refinement làm tròn và tránh submit form trước JS sẵn sàng chạy 69 order + 5 catalog view + 3 baseline view, 77 đạt, WAR build thành công. Tổng inventory report 183 case đạt; không cộng các lượt chạy thành test mới.
- Browser H2 11 kịch bản đạt, gồm homepage entry, join khi chưa có món, menu/iframe nhóm, giữ giỏ cá nhân, host chọn COD và một order với bill từng người. Fixture vòng đầu chọn sản phẩm không có topping khiến test chờ input; chỉ định đúng sản phẩm fixture. Bổ sung nút submit chỉ enable sau JS init để tránh native submit sớm. Không tạo order cloud hoặc gọi payment thật.
- Đã restart server và áp migration trên DB cấu hình. Contract ưu tiên GROUP_JOIN_FIRST_COD_20261027.md; kết quả GROUP_JOIN_COD_VERIFICATION_20261027.json. Chưa push.


## 2026-10-27 — Popup xác nhận và đi thẳng checkout nhóm
Prompt Thanh: đặt Đặt đơn nhóm cạnh Đặt món ngay; thay thông báo localhost bằng popup; chốt nhóm mở thẳng checkout, cập nhật thông tin giao hàng và giữ phương thức thanh toán của project. Hỏi thêm cách mỗi thành viên thanh toán riêng.
Kết quả: đổi thứ tự hero; thêm xác nhận Bootstrap dùng chung trong site-ui.js, dùng cho giỏ/nhóm/địa chỉ/gỡ nội dung/ngừng bán; không còn native confirm/alert/prompt trong JS module. Hủy popup không thực hiện mutation. Nút chốt nhóm mở /checkout trực tiếp, giữ bước chi nhánh và địa chỉ/người nhận/điện thoại/khu vực/nhà vận chuyển. HOST dùng COD, bank transfer hoặc VNPAY khi provider hiện tại khả dụng; INDIVIDUAL giữ một đơn COD, mỗi người trả phần tiền đã giảm và phí giao được chia khi nhận, không chờ xác nhận và không tự trừ tiền online. Chỉ host đổi cách trả. Không đổi datasource/provider.
Sửa thêm race Bootstrap transition: khi thêm món trước khi popup hiện xong, ghi nhận yêu cầu đóng rồi đóng tại shown.bs.modal.
Kiểm thực: 77 tests (69 OrderCommerceIntegrationTest + 5 CatalogViewTest + 3 BaselineViewTest) qua; package bản cuối qua; python tools/check_all_ui.py --shopping: 11 kịch bản qua, gồm hủy/xác nhận popup, join/chọn món/chốt nhóm đi thẳng checkout/đơn COD. Lần browser đầu phát hiện race đóng popup, đã sửa và chạy lại qua. check_shared_ui và git diff --check qua. Không tạo đơn cloud, không thu tiền thực. Chưa commit/push.


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
