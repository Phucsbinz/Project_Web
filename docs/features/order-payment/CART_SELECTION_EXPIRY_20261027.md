> Luồng nhóm hiện tại đã cập nhật theo yêu cầu mới: [tham gia trước và chủ nhóm đặt COD chia phần](GROUP_JOIN_FIRST_COD_20261027.md). Các đoạn về gửi giỏ cá nhân hoặc chờ xác nhận ở bản cũ bên dưới là lịch sử, không phải luồng đang dùng.

# Tick chọn, giỏ 1 giờ và dùng đặt nhóm trên local

## Hành vi đã thống nhất

Thanh yêu cầu bỏ nhãn demo/mô phỏng trên nội dung hiển thị, sửa ô nhập checkout, tick chọn món, đặt thời gian tự reset giỏ. Thanh chọn **1 tiếng**. Sau đó yêu cầu thêm popup thành công và dòng nhắc thời hạn.

- Sau thêm món hiện thông báo **Đã thêm vào giỏ hàng thành công**, dưới là **Giỏ hàng được giữ trong 1 giờ kể từ lần cập nhật gần nhất. Hãy hoàn tất đặt hàng trước khi hết hạn.** Toast không hiện khi chỉ tick/xóa/đổi số lượng.
- Checkbox từng dòng/chọn tất cả được lưu ở server. Tóm tắt chỉ tính món được tick; không cho đi checkout khi không tick món. Hàng bỏ tick còn trong giỏ sau khi đặt phần đã chọn.
- Chi nhánh, tồn kho, tiền món, voucher/điểm và quote hash chỉ tính dòng được chọn. Đổi chi nhánh chỉ đổi dòng được chọn. Khi ánh xạ gây trùng cấu hình với dòng bỏ tick, báo rõ cần chọn cả hai hoặc đổi chi nhánh, không tự đưa món bỏ tick vào đơn.
- Nhóm đã chốt giữ chọn toàn bộ; không được bỏ tick để gian lận số người/ưu đãi. Thành viên gửi vào nhóm cũng chỉ gửi món đang chọn.
- Mỗi lần cập nhật giỏ sẽ bắt đầu lại 1 giờ. Đọc trang, badge hoặc polling không gia hạn. Thời gian server quyết định, đồng hồ client chỉ hiển thị; checkout từ tab cũ cũng bị từ chối khi hết hạn. Không tự hủy đơn đã tạo. Không giữ tồn kho trong thời gian để giỏ.
- Giỏ đăng nhập hết hạn được ẩn ngay và xóa bằng cleanup mỗi phút/ở lần ghi tiếp theo; nhóm đã chốt gắn giỏ hết hạn bị hủy để gỡ liên kết. Guest snapshot hết hạn được loại bỏ khi truy cập; đổi merge key để không vướng chống nhập trùng. Session guest có thời hạn 1 giờ, snapshot vẫn kiểm riêng dù cookie được dùng tiếp.
- Dùng activity_revision để làm dirty cart/version khi hai cập nhật chung timestamp; không đẩy timestamp giả thêm microsecond nên mốc hết hạn đúng 1 giờ.

## Ô nhập checkout

Các danh sách địa chỉ/khu vực/nhà vận chuyển là select, không phải ô gõ tự do. Nếu chưa có địa chỉ, form **Thêm địa chỉ nhận hàng mới** mở sẵn: nhập người nhận, điện thoại, địa chỉ đầy đủ; lưu và tự chọn địa chỉ vừa tạo. Ghi chú là textarea. Thay đổi input hoặc select làm hết hiệu lực quote cũ. Khởi tạo script checkout sau DOMContentLoaded để helper UUID/CSRF chung đã sẵn sàng, không bỏ qua CSRF hoặc kiểm quyền.

## Dùng đặt nhóm local

### Cùng một máy (không cần đổi cấu hình)

1. Server chạy `http://127.0.0.1:8080`.
2. Chủ nhóm dùng một hồ sơ trình duyệt để đăng nhập, chọn món, vào giỏ và tạo nhóm; sao chép link.
3. Người thứ hai dùng trình duyệt khác hoặc cửa sổ riêng tư (cookie riêng), đăng nhập tài khoản khác, chọn món rồi mở link và gửi món vào nhóm. Không dùng hai tab thường cùng hồ sơ cho hai tài khoản vì chúng dùng chung cookie.
4. Nếu muốn thử mốc 3 người: dùng Edge thường, Chrome thường và một cửa sổ riêng tư, mỗi nơi đăng nhập tài khoản riêng. Nhiều cửa sổ riêng tư cùng trình duyệt có thể dùng chung phiên nên không coi là các hồ sơ độc lập.
5. Chủ nhóm chốt, chọn chi nhánh, địa chỉ và thanh toán chung. Từ 3 người có món mới nhận mốc giảm đầu tiên.

### Các máy/điện thoại cùng mạng Wi-Fi/LAN

1. Trên máy chạy server, xem IPv4 bằng `ipconfig`, ví dụ `192.168.1.20`.
2. Dừng tiến trình đang dùng cổng 8080, sau đó chạy WAR từ thư mục gốc project:

```powershell
java -jar target/utetra.war --spring.profiles.active=mysql --server.address=0.0.0.0 --server.port=8080
```

3. Nếu Windows hỏi quyền truy cập mạng của Java, cho phép mạng Private đang dùng. Các thiết bị phải kết nối được tới máy này qua cổng 8080.
4. **Tất cả**, gồm chủ nhóm, mở `http://192.168.1.20:8080` (thay bằng IP thực), rồi tạo link nhóm từ địa chỉ này. Không gửi link `127.0.0.1` hoặc `localhost` cho máy khác vì đó là chính máy của người nhận.
5. Từng người đăng nhập, chọn món, gửi vào link; chủ nhóm chốt/thanh toán. Tất cả dùng cùng server và DB, không cần mỗi người mở server riêng.

Mặc định tools/start_web.py vẫn chỉ mở loopback; thay đổi lần này không tự công khai máy ra mạng. HTTP LAN có thể không hỗ trợ clipboard API/crypto.randomUUID; nút copy có đường chọn link thủ công và helper UUID có getRandomValues dự phòng. Không cấu hình SMTP qua giao diện nhóm hoặc đưa khóa ra trang khách.

## Nội dung hiển thị

Bỏ suffix Nội dung demo khỏi 90 review + 90 comment do seed tạo; dọn marker ở mô tả danh mục/chi nhánh, địa chỉ mẫu, tên ưu đãi/nhà vận chuyển và các ghi chú/snapshot đã tạo bằng chuỗi đã biết. Giữ tài liệu nguồn gốc, mã định danh, liên kết và trạng thái tài chính; không coi tập dữ liệu này là hoạt động thật. Generator cũng không thêm lại nhãn vào UI. Không sửa bình luận tự viết không khớp chuỗi đã biết.

## Kiểm tra

Full Maven verify: 180 test đạt (0 failure/error/skipped). Có test checkout chỉ dòng chọn/giữ dòng bỏ tick, foreign ID/stale/group lock, hết hạn đúng một giờ, cập nhật gia hạn, guest và không sửa order đã tạo. Browser H2: 10 kịch bản qua, gồm popup+badge, checkbox/counter, countdown, đặt nhóm, thêm địa chỉ/nhập points/note/quote/checkout, giữ món bỏ tick và UUID trên HTTP LAN. Chi tiết kết quả ở CART_RETENTION_VERIFICATION_20261027.json. Không tạo order cloud hoặc gọi payment/email thật khi kiểm UI.
