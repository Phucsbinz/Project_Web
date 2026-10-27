# Luồng hiện tại: tham gia trước, chọn món và chủ nhóm đặt

Yêu cầu Thanh ngày 27/10 thay thế các mô tả nhóm ở bản 26/10: tham gia nhóm trước rồi chọn món; có mục Đặt đơn nhóm trên trang chủ; chỉ chủ nhóm quyết định trả hết hay chia phần. Thanh xác nhận lựa chọn chia phần là **COD**, chủ nhóm đặt ngay, không đợi từng người xác nhận thanh toán.

## Thao tác

1. Trang chủ → **Đặt đơn nhóm**. Chủ nhóm tạo link kể cả giỏ cá nhân đang trống.
2. Chủ nhóm chọn **Chủ nhóm trả toàn bộ** hoặc **COD · Mỗi người trả phần mình khi nhận**. Trước khi chốt, chỉ chủ nhóm được đổi cách trả; lựa chọn lưu ở server.
3. Mọi người mở link và bấm **Tham gia nhóm** trước. Không yêu cầu chọn món vào giỏ cá nhân.
4. Bấm **Chọn món cho tôi** để vào menu có nhãn đang chọn cho nhóm. Size/đường/đá/topping được thêm vào phần riêng của người đó trong nhóm, không thêm vào giỏ cá nhân. Có thể xóa món của mình khi nhóm còn mở.
5. Chủ nhóm chốt các phần hiện có rồi chọn chi nhánh/địa chỉ/giao hàng và bấm đặt. Không cần thành viên xác nhận thanh toán. Người chưa có món không tính mốc ưu đãi, không có phần tiền phải trả và không chặn chủ nhóm đặt. Tối thiểu hai người có món mới đặt nhóm.
6. Tạo **một đơn chung** và một khoản COD cần thu cho cả đơn. Trang nhóm hiển thị từng người: tiền món, giảm giá, phần phí giao và số cần trả khi nhận. Không tạo đơn riêng cho từng người, không giả lập trừ tiền hay tự đánh dấu PAID.

Chốt đưa món nhóm vào giỏ chủ nhóm để dùng checkout hiện có và có thông báo thay thế giỏ cá nhân. Giỏ cá nhân của thành viên không bị thay đổi khi tham gia/chọn món nhóm. Hạn giỏ host sau chốt vẫn 1 giờ; link nhóm còn mở có hạn 24 giờ. Thử local/LAN dùng các phiên trình duyệt độc lập như CART_SELECTION_EXPIRY_20261027.md.

## Tiền và quyền

- Mốc 3/5/10 người **có món**: giảm 5/10/15%, trần 100.000đ cả nhóm, không chồng mã giảm món.
- Ví dụ mỗi người có phần 26.000đ, ba người có món: giảm tổng 3.900đ, tiền món mỗi người **24.700đ**. Nếu phí giao ròng 15.000đ, chia ba là 5.000đ/người, tổng COD mỗi người 29.700đ.
- Giảm món phân bổ tỷ lệ tiền món thực tế ở chi nhánh đã chọn; phí giao sau ưu đãi chia đều cho những người có món. Giảm giá làm tròn theo phần dư lớn nhất; phần lẻ phí giao vào dòng cuối để tổng từng phần bằng đúng finalAmount của order. Thành viên có phần tiền món 0 không bị nhận khoản giảm âm. Cách chia COD không dùng điểm cá nhân của chủ nhóm hoặc bank/VNPAY.
- Khi chủ nhóm trả toàn bộ, vẫn dùng phương thức thanh toán/điểm hợp lệ hiện có; thành viên không phải bấm trả. Bảng phân bổ là tham chiếu phần món mà chủ nhóm đã thanh toán.
- Kiểm quyền backend: thành viên không thể đổi paymentMode, chốt/hủy nhóm, sửa phần người khác hoặc khai số tiền/giảm giá từ client. Nhóm chốt khóa sửa món; chống stale version.
- Phân bổ lấy đơn giá/topping thực từ order item snapshot, đối chiếu catalog_key + owner/category/size/đường/đá/topping/ghi chú sau ánh xạ chi nhánh. Sai tổng hoặc thiếu đối chiếu sẽ rollback đặt đơn, không lưu bill lệch.
- `group_order_members` lưu order_id, billed_subtotal, discount_share, shipping_share, amount_due sau checkout. Các giá trị này không phải giao dịch thanh toán đã thu; payment_status vẫn do COD/provider workflow hiện có quyết định.

## Kiểm tra

Có test tham gia khi giỏ trống, phần món độc lập, không tính người chưa chọn, quyền chủ nhóm và ví dụ ba phần 26.000đ; không gọi payment thật hoặc tạo order cloud khi test. Công cụ browser `python tools/check_all_ui.py --shopping` kiểm cả entry trang chủ → tham gia → menu nhóm → chủ nhóm đặt COD chia phần. Kết quả cuối: GROUP_JOIN_COD_VERIFICATION_20261027.json. Chưa push.


Cập nhật 27/10/2026 theo yêu cầu Thanh: chia phần hỗ trợ cả COD và QR, chủ nhóm đặt ngay, thành viên nhận popup thông báo và xem phần tiền/QR của mình. Chi tiết và giới hạn đối soát ở docs/features/order-payment/GROUP_SPLIT_QR_POPUP_20261027.md.
