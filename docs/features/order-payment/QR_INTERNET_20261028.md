# QR qua Internet và thời hạn giao dịch — 28/10/2026

## Yêu cầu
Thanh yêu cầu merge integration/utetra trước khi làm; giữ UI mới, bổ sung phần còn thiếu. QR quét được trên mọi thiết bị Internet, trang thanh toán gọn hơn, người đã trả quay về nhóm xem tiến độ. Giao dịch có 10 phút và đếm ngược. Khi đủ tiền cần thông báo nhận đơn và nhắc để ý điện thoại.

## Thực hiện
- identify fast-forward từ efddd5f đến origin/integration/utetra a93e1c1, không conflict. Giữ Modern Matcha, hỗ trợ khách hàng và toàn bộ chức năng nhóm trước đó.
- QR mới/tạo lại có hạn 10 phút theo Clock máy chủ. Hai trang có countdown lệch giờ máy khách được bù bằng serverNow; tải lại không gia hạn. Hết hạn khóa nút thanh toán, server từ chối; chỉ chủ phần thanh toán được tạo lại mã, vô hiệu mã cũ.
- Merchant tự quay về nhóm khi phần của mình SUCCESS. Nhóm hiện paidCount/payerCount và badge người đã trả; polling theo tiến độ lẫn trạng thái đơn. Thiết bị đã đăng nhập mở trang xác nhận cũng quay về merchant rồi nhóm. Thiết bị Internet chưa đăng nhập chỉ xác nhận phần được cấp quyền bởi QR; trang nhóm trên thiết bị đặt món tự cập nhật, không công khai nhóm qua tunnel.
- Đủ mọi phần tiền mới PAID và mới cho cửa hàng CONFIRMED, giữ kiểm tra quyền operator hiện tại. Popup nhận đủ tiền dùng đúng trạng thái; khi cửa hàng xác nhận có popup riêng nhắc để ý điện thoại. Không tự giả mạo hành động xác nhận của cửa hàng.
- Không dùng ngân hàng thật. Luồng nội bộ có đồng ý số tiền, CSRF, capability token, idempotency và kiểm ledger hiện có. Không ghi token vào log/tài liệu.
- tools/qr_tunnel.py chạy gateway loopback 8086 + cloudflared; chỉ GET trang ví, tài nguyên định danh chính xác và POST status/confirm. Chặn root, auth, admin, orders, query lạ, path encode/traversal, body >4096 byte. Upstream cố định 127.0.0.1:8080; không forward JWT/Authorization, không theo redirect. QR dùng HTTPS tunnel đang sống; base URL được cấu hình riêng vẫn ưu tiên (fixture không trỏ dữ liệu cloud).

## Chạy
1. Chạy server bằng `python tools/start_web.py`.
2. Cài cloudflared chính thức tại `.tools/cloudflared/cloudflared.exe` (thư mục bỏ qua Git). Nguồn: https://github.com/cloudflare/cloudflared/releases . Máy hiện tại đã có binary được đối chiếu SHA256 với metadata release.
3. Chạy `python tools/qr_tunnel.py start`; xem `python tools/qr_tunnel.py status`. Không cần tài khoản Cloudflare cho Quick Tunnel.
4. Mở trang QR trên web; QR nhận URL HTTPS hiện hành, thiết bị bất kỳ có Internet mở được trang xác nhận. Chỉ chia sẻ QR cho người cần trả phần đó.
5. `python tools/qr_tunnel.py stop` đóng gateway và tunnel do script tạo. URL Quick Tunnel đổi khi mở lại; trang merchant đang chờ tự tải lại nếu origin đổi. Server và tunnel phải cùng chạy.

Tài liệu Cloudflare: https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/ . Public URL/QR không lưu vào source. Không push trong đợt này.

## Kiểm tra
- Full Maven verify sau merge: 206 tests, 0 failures, 0 errors; WAR thành công. Lượt đầu chạy trong môi trường hạn chế kết nối loopback bị lỗi start Tomcat; đã chạy lại với quyền mở server và toàn bộ qua.
- Shopping UI thực Tomcat/SiteMesh + Edge trên H2 riêng: 16 scenarios qua; screenshots QR/mobile, không overflow 390px. Phát hiện nút nhóm bị thiếu trong home mới và đã bổ sung cạnh đặt món, giữ Modern Matcha.
- Shared UI checker: một SiteMesh layout, 21 shared files; git diff --check qua.
- Gateway unit scope: cho phép 5 và từ chối 10 trường hợp route/query/path bất hợp lệ.
- Qua HTTPS Cloudflare thật: health 200; root/admin/auth/login 404; wallet JSP 200, đủ countdown/nút và một Bootstrap. POST status mã không tồn tại thiếu CSRF trả 403, đủ CSRF đi qua tới application trả 404, không tạo đơn/thanh toán thật.
- Kiểm tra riêng ranh giới đúng 10 phút và bản đóng gói cuối được ghi thêm bên dưới.

- Lượt verify đóng gói cuối: 82 OrderCommerceIntegrationTest qua, gồm assert hạn đúng 10 phút, hết hạn tại ranh giới, renew vô hiệu mã cũ, concurrency/idempotency và chặn xác nhận khi chưa đủ tiền. Server MySQL đã mở lại bằng WAR mới.
- Lượt browser cuối sau đóng gói: 16/16 shopping scenarios qua; đã xem screenshot QR desktop 1440px và mobile 390px. Server mới có Modern Matcha + nút Đặt đơn nhóm; tunnel khởi động lại health HTTPS 200. Chưa commit/push các chỉnh sửa mới.
