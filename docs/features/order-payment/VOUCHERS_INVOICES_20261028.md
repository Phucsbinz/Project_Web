# Popup voucher và hóa đơn cá nhân/nhóm — 28/10/2026

## Yêu cầu đã thống nhất trong hội thoại
- Ưu đãi món/phí giao dùng popup như chọn voucher, có ô tìm mã/tên; voucher không đủ điều kiện bị khóa và có dòng đỏ Không đủ điều kiện áp dụng.
- Áp dụng chung tất cả checkout cá nhân và nhóm; giữ discount nhóm không cộng voucher món khi đã có ưu đãi nhóm.
- QR merchant chỉ để quét; bỏ nút Tiếp tục thanh toán. Bên cạnh là chi tiết hóa đơn/mã đơn/món/giảm giá/phí giao/phần cần trả.
- Sau trả tiền chuyển trang hóa đơn chi tiết và popup thành công, kèm thông tin giao hàng đã nhập, nút đơn mua/trang chủ và in/lưu PDF. Áp dụng cả nhóm. Yêu cầu mới thay hành vi trước đây quay về nhóm sau trả phần mình.

## Thiết kế và thực hiện
- POST /checkout/vouchers xác thực buyer hiện hành, chỉ lấy GLOBAL/đúng shop; thử quote thật cho từng mã nên dùng cùng điều kiện thời gian, hạn mức, tối thiểu, sản phẩm, điểm, phí giao, group, giá và quyền. Không reserve coupon khi xem. Trả eligible/reason cho UI; checkout vẫn kiểm lại dưới lock khi đặt.
- Popup Bootstrap dùng chung theme, search phía client trên tập đúng phạm vi; radio bị disabled khi không hợp lệ, lý do đỏ, chọn/bỏ chọn chỉ ghi vào hidden select sau Áp dụng. Hủy/đóng giữ lựa chọn. Tồn kho/hạn mức vẫn cập nhật từ endpoint availability cũ; không tin client để cấp giảm giá.
- Kiểm browser phát hiện NPE Java do ternary long/Long tự unbox null lúc xét shipping mà chưa chọn product voucher; đã box id tường minh và bổ sung shipping vào regression test.
- Merchant QR bỏ anchor thao tác, URL capability còn trong SVG/data cho kiểm quét và có quyền theo phần tiền cũ. Hiện snapshot các món, subtotal/discount/shipping của payer; không tính lại giá catalog sau đặt.
- Invoice GET dưới OrderController dùng read quyền đã có; chỉ chủ đơn/thành viên được cấp quyền xem, không mở public invoice qua gateway. Có receiver/phone/address/area/carrier/note, snapshot món và tùy chọn/topping, tổng/discount/points/phí giao, phần của mình, phương thức/trạng thái, mã/ngày, nút theo dõi/nhóm/đơn mua/home và print.
- QR SUCCESS điều hướng invoice?success=1, invoice hiển thị popup một lần theo viewer/order/stage. Cá nhân/host nhắc điện thoại; thành viên nhóm chỉ báo hoàn tất. Nếu riêng mình trả mà nhóm chưa đủ: báo phần mình thành công và chờ n/m, không tuyên bố cả đơn thanh toán đủ. Khi đủ nhóm, popup hoàn tất và trạng thái thanh toán cập nhật. Lịch sử mở lại invoice không tự lặp popup success.
- COD đặt xong cũng vào invoice; chủ nhóm trả toàn bộ như contract cũ. Nhóm đang ở group khi hoàn tất có thông báo rồi đi invoice, không bật hai popup cùng nội dung.
- Giữ Cloudflare, 10 phút, partial refund/popup 30 phút, owner actions, Shipper và provider IDs hiện hành. Public wallet không đăng nhập vẫn chỉ xác nhận phần được cấp capability; invoice hiện trên thiết bị đặt món đã đăng nhập.
- Một SiteMesh/app.jsp/Bootstrap/theme. CSS voucher scope voucher-picker/checkout-page; invoice scope invoice-page, không layout thứ hai.

## Kiểm tra
- Full verify 267 tests qua, bao gồm voucher eligible/min-order/inactive/shipping và không ghi redemption khi xem.
- Browser audit cuối đã qua 3 nhóm kiểm tra voucher/invoice (không lỗi); ảnh 390px và 1440px đã kiểm sau animation. Regression shopping 18 tình huống và partial-group 4 tình huống qua theo route invoice mới.
- Sau bổ sung tên voucher snapshot vào hóa đơn, chạy lại OrderCommerceIntegrationTest: 109 tests, 0 failures/errors; WAR build thành công. Shared UI và git diff --check qua. Server thật đã mở lại ở cổng 8080, trang đăng nhập trả HTTP 200; tunnel Cloudflare đang chạy.
- Chưa push các thay đổi trong đợt này, bao gồm bản QR Cloudflare từ đợt trước còn ở workspace.
