# Hiển thị tiền, bước giao hàng và email kiểm thử — 29/10/2026

Thanh yêu cầu đổi nhãn Phí giao sau ưu đãi thành Phí giao, toàn bộ số tiền hiển thị dạng 1.000đ, bỏ nút tiến hành giao khỏi Admin/Manager/Vendor từ lấy hàng trở đi, đổi email kiểm thử theo tên.role. Thanh xác nhận chỉ đổi .test/.example, giữ email thật/SMTP. Đơn đã trả không hiện khối QR thừa, thay bằng Xem hóa đơn.

## Thực hiện
- Shared utetra.money/utetra.moneyText định dạng VND không phần lẻ, dấu chấm phân nhóm, đ liền số. Site UI xử lý data-money và tiền còn xuất thô trong text/option, theo dõi cập nhật động; loại trừ script/style/textarea/code. Các bộ định dạng tiền checkout, cart, QR, voucher dùng helper chung. Không sửa numeric input, dữ liệu tính toán/API hay CSV.
- Chi tiết đơn chỉ hiện nút chuyển CONFIRMED cho operator khi NEW. PICKED_UP/DELIVERING/DELIVERED dành cho Shipper đang được gán, UI của operator không có các nút này; backend vẫn chặn gọi trực tiếp. Checkbox thu COD chỉ ở bước giao cuối của Shipper. Các kiểm thử dựng đơn giao hợp lệ đã chuyển qua vai trò Shipper.
- Đơn đã thanh toán có Xem hóa đơn, không khối Thanh toán QR/Mở thanh toán của tôi. Chưa trả tiền vẫn có đường thanh toán, hoàn/hủy vẫn giữ chứng từ. Đã đưa panel phân công ra ngoài điều kiện thao tác tiến trình để vẫn xem được khi nút giao hàng bị ẩn.
- Công cụ RenameTestEmails đổi theo tên không dấu + chữ đầu họ/tên đệm + role (USER→guest); ví dụ Phan Tuấn Thanh → thanhpt.guest@example.test. Trùng tên cùng role thêm số. Hai lượt đổi trong một transaction tránh đụng unique index khi hoán đổi. Giữ user id, username, password hash, xác minh, trạng thái, role, mọi FK và lịch sử. Không đổi email thật/SMTP.
- Đã áp dụng cho 29 tài khoản kiểm thử trên DB hiện hành, danh sách ở docs/demo/ACCOUNTS_20261029.md và .local/test-account-emails.json. Cập nhật tài liệu và script seed dùng tên mới; lookup username trước để không nhân đôi tài khoản khi seed lại.

## Kiểm tra thực
- Full verify: 347 tests, 0 failures/errors; WAR build thành công. Có kiểm backend ngăn operator chuyển sang PICKED_UP và cho Shipper được gán tiếp tục.
- Trình duyệt kiểm 8 trường hợp định dạng (raw decimal/grouped/VND symbol), CRUD/dispatch5 nhóm và voucher/invoice3 nhóm qua; Admin không có transition/thu COD khi CONFIRMED, Shipper hoàn thành giao được; QR merchant nhãn Phí giao, đơn trả đủ chỉ Xem hóa đơn.
- Đã mở lại server 8080 và xác nhận email mới của USER/ADMIN/SHIPPER đăng nhập HTTP200 trên DB thật. Không tạo đơn thử trên DB thật. Không tuyên bố đã chứng minh concurrency TiDB.
- Không commit/push trong đợt này.
