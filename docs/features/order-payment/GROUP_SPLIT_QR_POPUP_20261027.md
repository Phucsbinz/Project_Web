# Chia phần COD/QR và thông báo thành viên — 27/10/2026

Yêu cầu Thanh: thanh toán gọn, có QR ngay cả khi chia tiền từng người; chủ nhóm đặt ngay, thành viên chỉ cần nhận popup thông báo.

- Chỉ một mục phương thức thanh toán với radio COD/QR (VNPAY chỉ khi cấu hình khả dụng và chủ nhóm trả chung). Bỏ nút chọn nhanh cộng ô nhập trùng lặp.
- Chủ nhóm quyết định trả chung/chia phần như trước. INDIVIDUAL nay cho COD hoặc BANK_TRANSFER, điểm cá nhân của chủ nhóm vẫn không dùng cho chia phần.
- Chủ nhóm tạo một đơn chung ngay, không đợi thành viên xác nhận thanh toán. Bảng chia tiền lưu số tiền món sau giảm và phí giao mỗi người, tổng khớp đơn.
- Với QR chia phần, host được dẫn về trang nhóm; mỗi thành viên thấy QR tài khoản nhận hiện có, số tiền của chính mình và nội dung UT + mã định danh đơn. QR hiện có là QR tài khoản tĩnh; người chuyển nhập đúng số tiền/nội dung được ghi bên cạnh.
- Các khoản chuyển dùng chung nội dung theo đơn. Đối soát ngân hàng hiện tại vẫn ở cấp đơn: Admin kiểm đủ tổng khoản nhận rồi xác nhận qua luồng cũ. Không tự ghi PAID và không tuyên bố có xác nhận giao dịch từng thành viên.
- /group-orders/payment-notices chỉ trả phần tiền của tài khoản hiện đăng nhập, các đơn nhóm mới còn chờ thanh toán, bỏ host. Customer/Vendor có thể nhận popup trên mọi trang dùng layout chung. Poll 8 giây khi tab hiện, kiểm lại khi quay lại tab; popup không chen vào modal khác. Browser ghi nhận đã xem theo nhóm/thành viên để không lặp liên tục.
- Khi tab đang đóng/offline thì popup không thể hiện tức thời; lần mở lại trang sẽ kiểm dữ liệu trên server. Không chặn host để chờ người nhận popup.

Tài liệu này cập nhật phần giới hạn COD trước đó trong GROUP_JOIN_FIRST_COD_20261027.md. Không đổi người chịu thanh toán khi chọn QR.


## 2026-10-27 — Trang thanh toán riêng cho thành viên nhóm
Prompt Thanh: chủ nhóm đặt QR xong mở trang thanh toán riêng, không quay lại nhóm; thành viên nhấn Tiếp tục popup cũng vậy.
Tạo GET /group-orders/{token}/payment và group-payment.jsp trong module order, dùng layout/theme chung. GroupOrderService.paymentPage bắt buộc tài khoản là thành viên có phần món trong đơn đã tạo, chỉ trả bill của người đó, không nhận userId phía client. HOST trả chung xem tổng đơn; thành viên HOST không có nghĩa vụ trả riêng. INDIVIDUAL xem tiền món/giảm/phí giao/amountDue của chính mình. QR chỉ hiện nếu còn UNPAID và đơn chưa hủy; PAID/cancel/COD có nội dung tương ứng, không tự ghi đã trả. Có nút cập nhật trạng thái. Chốt QR chia phần redirect thẳng /payment; popup Tiếp tục cũng /payment. Bỏ QR inline khỏi group.jsp, thay link mở lại trang cá nhân. Tránh popup thông báo lặp nếu đang ở trang payment của nhóm đó. CSS scope group-payment-page trong cart.css. Không thêm layout/provider/datasource.
Kiểm thực: 77 tests render/commerce qua; WAR build thành công; 13 shopping browser scenarios qua, gồm host checkout QR tới /payment và member popup Tiếp tục tới /payment có QR; không JS errors. Shared UI/diff check qua. Server mở lại. Không đặt đơn cloud, chưa commit/push.


Cập nhật 27/10/2026 theo yêu cầu Thanh: QR cho đơn mới dùng LOCAL_QR (ví giả lập, không tiền thật), local_qr_payments theo từng người. Đơn nhóm chia phần chỉ được CONFIRMED khi tất cả phần thanh toán thành công. Hướng dẫn/contract mới thay phần QR chuyển khoản trước đó: docs/features/order-payment/LOCAL_QR_WALLET_20261027.md.
