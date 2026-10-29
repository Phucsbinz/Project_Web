# Sửa xóa giỏ hàng ngày 29/10/2026

Yêu cầu: món đã thêm vào giỏ không xóa được; sửa và kiểm lại.

Nguyên nhân: cart.js được JSP nội dung nạp trước utetra.js của layout. Hàm tính tổng chạy ngay và tham chiếu utetra.money khi helper chưa tồn tại. Trình duyệt báo `utetra is not defined`; các nút xóa chưa được đăng ký listener, không có request xóa gửi tới server.

Thay đổi: khởi tạo module cart/group khi DOMContentLoaded, sau khi tài nguyên chung được nạp. Không thay dữ liệu, TTL, quyền sửa giỏ nhóm hoặc API backend.

Kiểm chứng: tái hiện trước sửa trên Tomcat/SiteMesh H2 thật: giỏ 2 dòng, nhấn xóa vẫn còn 2, lỗi JavaScript và không có request delete. Sau sửa, `py -3 tools/check_all_ui.py --cart-delete` đạt với hai context cookie riêng Guest/User: 2 dòng còn 1, request delete HTTP 200, không lỗi JavaScript; xóa toàn bộ qua popup xác nhận trả giỏ rỗng. Build WAR `mvnw.cmd -q -DskipTests package` thành công; không coi build này là chạy lại toàn bộ suite.

Khởi động WAR mới phát hiện lớp LocalQrPaymentService có lỗi biên dịch trong readTunnelOrigin: ProcessHandle.info trả Info, không trả Optional. Sửa flatMap thành map trước bước flatMap đọc command. Build sạch và bộ QrPublicOriginTest, CheckoutPaymentPolicyTest, OrderCommerceIntegrationTest chạy lại thành công trước khi khởi động server.

Kết quả bổ sung: 117 ca của ba suite được chọn, 0 failures/errors; build sạch thành công. Server 8080 đã mở lại; một phiên khách độc lập trên MySQL chạy thực tế thêm hai dòng, xóa còn một và xóa toàn bộ thành công, không có lỗi JavaScript. Shared UI check đạt.
