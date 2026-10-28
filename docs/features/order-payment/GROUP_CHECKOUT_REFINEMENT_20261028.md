# Tinh chỉnh trang chủ, thanh toán nhóm và xác nhận nhận hàng — 28/10/2026

## Yêu cầu thực tế
Thanh yêu cầu ba CTA theo thứ tự Khám phá thực đơn / Đặt đơn / Đặt đơn nhóm, giữ Modern Matcha; tiêu đề Tìm thấy ly yêu thích dùng cùng font/weight. Popup chủ nhóm nhắc điện thoại, thành viên báo đặt thành công. COD mặc định chủ nhóm trả toàn bộ. Khách cần theo dõi trạng thái và mục Đã nhận hàng. Sau đó Thanh dặn hoãn đánh giá tài xế nếu shipper chưa có.

## Thay đổi
- Homepage scope catalog-homepage; ba nút cùng kiểu xanh, không thêm theme/layout. Tiêu đề bỏ em và font-weight khác biệt.
- Quote COD nhóm trả paymentMode HOST; khi tạo đơn lưu HOST vào group. Chọn QR giữ chế độ nhóm hiện hành; không sửa lựa chọn nhóm chỉ bởi một request báo giá. Không thay đơn cũ. Thành viên COD không phải người trả, chủ nhóm trả tổng khi nhận.
- Popup QR sau đủ tiền tách nội dung theo data-host; chỉ chủ nhóm được nhắc điện thoại. COD chủ nhóm có popup ngay trang chi tiết sau đặt thành công; thành viên nhận thông báo riêng qua polling hiện có. Shared site-ui.js chỉ đổi tiêu đề thông báo, app.jsp tăng cache version.
- Khách xem Đơn mua của tôi/chi tiết/lịch sử. Poll 15 giây tự tải trạng thái mới khi không thao tác form/modal; nếu đang thao tác vẫn hiện thông báo cập nhật.
- orders.received_at lưu xác nhận nhận hàng, migration V2026102801. POST received giữ CSRF, kiểm tài khoản hoạt động, buyer đúng chủ đơn, lock đơn, yêu cầu DELIVERED và PAID. Lặp lại không tạo event đôi. Không tự đổi DELIVERING thành DELIVERED, không đổi thời hạn trả hàng. Đơn nhóm xác nhận bởi chủ đơn nhận hàng.
- Mục Đã nhận hàng lọc theo received_at thật, không gộp đơn chỉ được cửa hàng đánh dấu DELIVERED. Có nút/modal xác nhận và lịch sử CUSTOMER_RECEIVED.
- Role SHIPPER, phân công đơn, giao hàng, thống kê đã có; features.shipper.enabled=false vẫn giữ nguyên. Chưa thêm/bật đánh giá tài xế theo chỉ đạo mới nhất.

## Kiểm tra
- 88 tests (83 commerce + 5 catalog view) qua, WAR thành công. Commerce gồm COD chuyển HOST, thành viên không trả COD; nhận hàng sai chủ/sớm bị chặn, retry không nhân lịch sử, lọc riêng chủ đơn.
- Kết quả migration và browser ghi thêm khi hoàn thành. Không push.

- IntegratedMigrationTest qua toàn bộ migration, gồm received_at.
- Browser Edge + Tomcat/SiteMesh H2 riêng: 17/17 scenarios qua, gồm đúng thứ tự CTA/font thống nhất, role khách tự thấy thay đổi sau operator giao, xác nhận bằng popup và bộ lọc Đã nhận hàng.
- Shared UI checker và git diff --check qua. Server MySQL đã chạy lại; Flyway áp dụng V2026102801 thành công. Shipper vẫn tắt, chưa thêm đánh giá tài xế. Không push.
