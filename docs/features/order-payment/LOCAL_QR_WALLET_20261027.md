# Ví QR local và xác nhận đủ thành viên — 27/10/2026

## Nguồn và phạm vi
Thanh yêu cầu thay QR cũ bằng luồng trong repo tham khảo, và đã chọn rõ ví QR giả lập chạy local, không trừ tiền thật. Tham khảo https://github.com/tho03072006-bot/CNPM_PROJECT_CK tại commit `626379251de7d31b12e3093d369cbb3d318d5beb`, cụ thể DemoWalletService, DemoWalletSecurity và DemoWalletPhoneController. Triển khai lại cho mô hình đơn hàng UTETra, không mang module vé phim, layout khác, cấu hình hay khóa của repo sang. ZXing core 3.5.3 tạo QR ngay trên server; không gọi website QR bên ngoài.

## Cách dùng
1. Chọn COD hoặc **Ví QR thử nghiệm** ở checkout. Báo giá tự cập nhật như trước.
2. Đặt đơn QR xong vào trang `/orders/{id}/qr-payment`. Đơn đã tạo nhưng còn NEW/UNPAID.
3. Quét QR để mở ví, hoặc bấm **Mở ví để thanh toán thử** trên cùng máy tính.
4. Ví hiện đúng số tiền đã chốt. Tích xác nhận đây là giao dịch giả lập, rồi bấm **Thanh toán thử**.
5. Trang thanh toán tự kiểm trạng thái mỗi 3 giây, hiện số người đã thanh toán và tổng số người phải trả.
6. Với chia phần, host và mỗi thành viên có một giao dịch riêng, đúng tiền món sau giảm + phần phí giao. 1/3 hoặc 2/3 vẫn UNPAID; đủ 3/3 mới PAID và cửa hàng mới được chuyển đơn sang CONFIRMED.
7. Chủ nhóm trả toàn bộ thì chỉ có một người chịu thanh toán tổng đơn. Thành viên không có món không phát sinh khoản phải trả; phần tiền bằng 0 được ghi nhận hoàn tất ngay. COD vẫn là thu tiền khi giao theo nghiệp vụ hiện có.

Đơn QR mới không dùng hình tài khoản Techcombank cũ và không cần Admin xác nhận sao kê. BANK_TRANSFER mặc định đã tắt cho việc tạo thanh toán mới; các đơn/lịch sử chuyển khoản cũ được giữ để không sửa sai dữ liệu đã có. Không tự chuyển đơn cũ thành đã thanh toán. VNPAY giữ cấu hình hiện tại, không bị giả lập bằng ví mới.

## Xác thực và lưu dữ liệu
- Migration V2026102703 tạo local_qr_payments, duy nhất (order_id,user_id), số tiền bất biến theo bill server. PaymentTransaction LOCAL_QR tổng đơn chỉ SUCCEEDED sau khi đủ toàn bộ giao dịch thành công.
- Backend kiểm toàn bộ người phải trả, số tiền từng người và tổng khớp đơn trước khi cho CONFIRMED, không chỉ tin cờ PAID trên order.
- Mã công khai UUID kèm capability 256 bit; token ở fragment URL, không nằm trong query hoặc đường dẫn request GET. DB chỉ giữ SHA-256 token. Không ghi token vào log/tài liệu/Git.
- Mọi POST kể cả ví trên điện thoại phải có CSRF. Xác nhận cần đúng token, đúng số tiền server và acknowledgement giao dịch thử. Không nhận userId hoặc trạng thái paid từ client.
- Khóa thứ tự order trước payment, thao tác lặp/concurrent không tính trùng. Callback ví local không phải callback ngân hàng và không thể xác thực tiền thật.
- QR có hạn 30 phút. Hết hạn không thanh toán được; người sở hữu tạo lại QR từ trang thanh toán. Link cũ mất hiệu lực. Đơn đã hủy/hoàn không nhận thêm thanh toán. Người không có phần tiền không truy cập QR của người khác được.
- Khóa ký local tạo tự động tại `.local/local-qr.key`, file được gitignore. Giữ file khi chạy lại từ cùng thư mục project. Không commit khóa hoặc chia sẻ link giao dịch.
- Thông báo đơn nhóm không chen vào trang ví/QR. Thành viên ở trang khách khác vẫn nhận popup và mở trang thanh toán của mình.

## Điện thoại cùng mạng
Mặc định URL là `http://127.0.0.1:8080`, phù hợp thử ngay trên máy bằng nút mở ví. Điện thoại không truy cập localhost của laptop. Cả nhóm dùng **cùng một server**; nếu thử bằng điện thoại, dừng server hiện tại rồi chạy từ thư mục project với IP LAN thật của máy, ví dụ:

```powershell
java -jar target/utetra.war --spring.profiles.active=mysql --server.address=0.0.0.0 --server.port=8080 --payment.local-qr.base-url=http://192.168.1.20:8080
```

Đổi IP ví dụ thành IP của máy và dùng cùng Wi-Fi, cho phép cổng 8080 trong mạng riêng nếu máy hỏi. Sau khi đổi base URL, tải lại trang QR để lấy link đúng máy chủ. Không tự bật cổng/public tunnel trong quá trình triển khai này. Không cần app ngân hàng hay MoMo Test.

## Kiểm chứng
- Một lần verify toàn bộ: 188 tests, 0 failure/error/skipped, WAR build thành công.
- Sau bổ sung kiểm đồng thời và điều chỉnh khóa: 75 OrderCommerceIntegrationTest qua, gồm 6 kịch bản ví mới: đủ mọi thành viên; sai token/số tiền/consent/quyền; retry/giả cờ PAID; concurrent cùng một phần; hết hạn/tạo lại/hủy; chỉ phát sự kiện aggregate một lần và không dùng bank-confirmation để vượt luồng.
- Browser cuối: 14 kịch bản qua, 0 JS errors. Có QR cá nhân, QR chia phần, consent, ví không đăng nhập có token/CSRF, thiếu CSRF bị chặn, 1/3 và 2/3 không CONFIRMED, đủ 3/3 thì xác nhận được. COD/giỏ checkbox/địa chỉ/báo giá/header vẫn qua.
- check_shared_ui và diff check qua. MySQL/TiDB thật chỉ áp dụng migration thêm bảng; không tạo đơn hoặc xác nhận tiền trên dữ liệu cloud để kiểm thử. Server mysql đã mở lại, migration v2026102703 thành công.
- Khắc phục trong quá trình thử: flush bill nhóm trước đọc JDBC; bổ sung LOCAL_QR vào validation DTO; không nuốt lỗi submit khi auto-quote cập nhật lại; không hiện popup notification chen vào trang ví. Dữ liệu fixture lịch sử dùng BANK_TRANSFER được bật riêng trong fixture, không bật lại trên ứng dụng thật.
- Chưa commit/push.


Cập nhật 27/10/2026: hủy và hết hạn có popup riêng cho thành viên; mã nhóm/host/time/status và badge Chủ nhóm. Nhãn QR trung tính, cơ chế nội bộ không đổi. Xem docs/features/order-payment/GROUP_LIFECYCLE_20261027.md.
