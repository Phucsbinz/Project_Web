# Đối chiếu role, nhập địa chỉ và CSKH — 27/10/2026

Nhánh integration/utetra; base origin/integration 9bc3cfb và identify efddd5f được ghép bằng bdbeeca. identify đã push/đối chiếu remote, không phát sinh code local mới trên identify. Năm xung đột storefront được ghép giữ trang chủ/header/search/modal mới và các chức năng giỏ/đặt nhóm của identify. Phần bổ sung CSKH/bảo mật được phát triển trên integration; main và các feature khác giữ nguyên.

## Kết luận có giới hạn

Các nhóm chức năng được yêu cầu đã có source/UI/test. Đã bổ sung CSKH text realtime lưu DB, ghép nhập địa chỉ tự do ở checkout, khôi phục bốn nhóm Top 20 ở home User/Vendor và bỏ cơ chế lưu mật khẩu rõ. Chưa coi toàn project đã nghiệm thu 100% với provider thật hoặc đủ hồ sơ nộp DOCX.

## Guest và User

- Guest home truy vấn món có số lượng đã giao/đã thanh toán trên 10, sort bán giảm dần; trang chủ mới hiển thị một phần nổi bật, menu có danh mục/lọc/sort/phân trang. Đây là cách hiểu điều kiện bán trên 10 ly, không phải tự tạo số bán hoặc giả doanh thu; nếu thầy yêu cầu trên 10 loại món ngay trên home thì cần chốt lại cách hiểu.
- User home mới trước merge chỉ có các preview bán chạy/mới; đã bổ sung lại bốn nhóm Top 20 mới/bán chạy/đánh giá/yêu thích từ homeGroups và link tới danh sách có phân trang. Không bỏ giao diện matcha của bản mới.
- Profile, nhiều địa chỉ, chi tiết món, size/đường/đá/topping, giỏ DB, chọn dòng và hết hạn, chọn chi nhánh ở checkout, checkout/COD, lịch sử trạng thái, hủy/trả/hoàn, yêu thích/đã xem, proof mua để đánh giá/bình luận, tối thiểu 50 ký tự bình luận và media đã có source/validation/test.
- Nhập địa chỉ: đã ghép recipientName/phone/addressDetail tự do, gợi ý địa chỉ đã lưu, lưu địa chỉ vào tài khoản rồi dùng ID thuộc chính User để quote/checkout. Browser H2 thực đã nhập địa chỉ mới, quote, tạo COD và kiểm snapshot đơn chứa đúng địa chỉ; buyer khác không đọc được.
- VNPAY có adapter/sign/IPN/idempotency, config dùng chung vẫn cần merchant để thử thật. MOMO chưa triển khai riêng. LOCAL_QR là mô phỏng, không được ghi thành giao dịch ví điện tử thật. Khôi phục lựa chọn chuyển khoản ngân hàng gốc khi bank config có sẵn ở đơn solo/chủ nhóm thanh toán; group chia phần giữ giới hạn của luồng nhóm. Chuyển khoản thật vẫn do Admin đối soát, không tự nhận QR là PAID.
- Voucher/điểm/tồn kho snapshot, transaction và realtime checkout có source/test. Không dùng WebSocket để thay transaction chống hai người cùng lấy suất cuối.

## Vendor

Giữ quyền mua hàng của User và UI đăng ký shop, sửa trang shop, quản lý sản phẩm/size/topping/tồn trong scope, trạng thái đơn shop gồm PICKED_UP, tạo khuyến mãi và doanh thu. CSKH mới chỉ mở cho User thường theo yêu cầu; chưa tự mở Vendor như một nhân viên CSKH hoặc thêm chat với staff khác.

## Manager và Admin

- Có tìm kiếm/quản lý user, sản phẩm/shop, danh mục/topping, chiết khấu/hoa hồng nền tảng, giảm % món/phí giao, carrier/tên/phí/khu vực và doanh thu, backend kiểm scope.
- Manager dùng membership của cửa hàng; quản trị danh mục chung cần Admin cấp CATALOG_ADMIN. Đây là chức năng có phân quyền bổ sung, không phải tự cấp quyền toàn hệ thống cho mọi Manager. Tài khoản demo/báo cáo cần được gán đúng.
- Admin quản lý hệ thống; không dùng quyền Admin để mở chat trực tiếp Manager. CSKH Admin chỉ các hội thoại khách tạo tới kênh Admin; Manager chỉ hội thoại của chi nhánh đang được phân công.

## Shipper — phần thêm

Có UI/API quản lý đơn được giao và thống kê, kiểm ownership/trạng thái. Feature thật giữ như cấu hình nhóm; các test H2 bật riêng để kiểm, không bật production chỉ để lấy ảnh. Không có CSKH cho Shipper theo phạm vi mới.

## CSKH mới

[Contract, kiến trúc, API và use case](features/customer-support/README.md). Hai bảng support_conversations/support_messages lưu hội thoại và nội dung/role người gửi/time/UUID retry. User chủ động chọn Admin hoặc chi nhánh có Manager đủ điều kiện. Nhân viên chỉ trả lời hội thoại khách đã tạo; không có staff–staff conversation. Kênh là một khách với đội hỗ trợ cùng scope, không DM riêng giữa từng nhân viên.

WebSocket/STOMP /user/queue/support phát metadata sau commit, client đọc tin qua API kiểm quyền; có polling phục hồi 7 giây. Không phát body qua topic công khai. Có kiểm chống IDOR, giả người gửi, subscribe queue người khác, XSS text, gửi trùng đồng thời, cursor/pagination, mất membership và JWT/session bị thu hồi.

## Bảo mật và quy định thầy

- Spring Security, BCrypt, JWT trong HttpOnly cookie, refresh/access session registry ở DB và kiểm role/scope backend vẫn giữ nguyên. Không chuyển sang đăng nhập HttpSession hoặc dùng cookie role do client tự sửa để phân quyền. HttpSession phục vụ guest cart không phải nguồn quyền.
- Trước đó xác thực đã dùng BCrypt. identify bổ sung tool test ghi thêm bản rõ bên cạnh hash; không có chuyện toàn bộ mật khẩu trước đó chưa băm.
- Migration mới V2026102705 bỏ password_plaintext, User entity và Auth/Account/tools không tham chiếu/ghi cột này nữa. Hash, username, trạng thái tài khoản và mật khẩu đang dùng không đổi. Tài khoản/mật khẩu demo giữ trong hướng dẫn test; bỏ bản sao DB không xóa tài khoản demo.
- Không sửa migration 2026102602 đã có trong lịch sử; dùng forward migration để checksum không lệch. Đã chạy migration 2704/2705 trên cloud theo yêu cầu xóa cột; cột đã không còn, 23 tài khoản và toàn bộ ID/password_hash giữ nguyên. Không reset mật khẩu. Biên bản: docs/DB_AUDIT_20261027.md.
- Giữ mô hình Controller/View → Service → Repository, JPA, Java 21/Spring Boot/JSP/JSTL, một SiteMesh layout/Bootstrap/theme, MySQL-compatible datasource đã chọn, Cloudinary/WebSocket/JWT. Không thêm frontend framework hoặc layout theo role.

## Bằng chứng cuối

- Maven clean verify **206 tests, 0 failures/errors/skipped, BUILD SUCCESS**, sau cả sửa bảo mật và bank UI. [Tổng hợp từ XML](demo/support-20261027/test-summary.json).
- Browser H2: **5 kịch bản lớn đạt**, gồm User↔Manager và User↔Admin có frame STOMP riêng; User khác nhận 0 hint của khách đang thử; chặn staff tạo chat, đọc chéo, subscribe giả; HTML chỉ là text; 390/1440px không tràn ngang; nhập địa chỉ mới và lưu đúng vào đơn COD. [JSON kết quả](demo/support-20261027/verification.json).
- Vòng kiểm đầu gặp selector trùng giữa preview và nội dung chat; đã scope selector vào vùng messages. Sau đó selector chọn COD cũ không còn đúng với radio UI mới; đổi test sang radio hiện tại. Đây là lỗi kịch bản thử, không nhận các lượt thất bại là đã đạt.
- Một test home cũ đòi không có Top 20 trái yêu cầu hiện tại; đã đổi sang assert đủ bốn nhóm, giữ kiểm không có bộ lọc JSP cũ/không lộ source.
- Shared UI/Node/whitespace kiểm cuối; helper H2 tự dừng. Không gửi OTP thật, upload provider hoặc giao dịch ngân hàng/merchant thật.

## Hình và báo cáo

[Luồng kiến trúc/hoạt động CSKH](features/customer-support/assets/flow-support.png), [source quyền có chú giải](features/customer-support/assets/source-access-control.png), [manifest](features/customer-support/assets/manifest.json). Hình do script vẽ từ source, không phải ảnh AI tạo hoặc ảnh giao dịch thật. UI trong docs/demo/support-20261027: user/manager/admin-support và typed-address ở 1440/390px. Code dựng lại: tools/build_support_figures.py; test browser: tools/check_all_ui.py --support.

Còn cần nhóm thực hiện: merchant/public callback VNPAY và giao dịch thật; xác nhận SMTP/OTP thật và media/video trên máy báo cáo; chốt danh tính/MSSV/phân công/reviewer, bộ ảnh từng UC, ghép DOCX và thử setup máy khác. Nếu thầy yêu cầu đúng MySQL Server thì chốt việc đang dùng TiDB tương thích MySQL. Không dùng nhận định có source/test để dự đoán điểm hoặc xác nhận hoàn tất toàn bộ rubric.
