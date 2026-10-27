> Luồng nhóm hiện tại đã cập nhật theo yêu cầu mới: [tham gia trước và chủ nhóm đặt COD chia phần](GROUP_JOIN_FIRST_COD_20261027.md). Các đoạn về gửi giỏ cá nhân hoặc chờ xác nhận ở bản cũ bên dưới là lịch sử, không phải luồng đang dùng.

# Giỏ hàng và đặt đơn nhóm — 26/10/2026

## Yêu cầu đã thống nhất

Thanh yêu cầu làm giỏ hàng rõ ràng như trải nghiệm mua sắm phổ biến, badge số món trên thanh công cụ, đặt nhóm và giảm theo số người. Thanh chọn mốc đề xuất: 3 người giảm 5%, 5 người giảm 10%, từ 10 người giảm 15%, tối đa 100.000đ/đơn; không cộng dồn mã giảm giá sản phẩm. Giữ yêu cầu chọn chi nhánh ở checkout.

Tham khảo luồng mời người tham gia, từng người tự chọn món và chủ nhóm thanh toán tại [GrabFood giới thiệu đặt đơn nhóm](https://www.grab.com/vn/campaign/gioi-thieu-tinh-nang-dat-don-nhom-group-order-cua-grabfood/). Đây là chính sách riêng của UTETra do Thanh duyệt, không khẳng định các mốc này là ưu đãi hiện tại của Grab. Không sao chép giao diện/logo Grab hoặc Shopee.

## Cách sử dụng

1. Mỗi người đăng nhập bằng tài khoản User/Vendor đã kích hoạt, chọn món vào giỏ cá nhân.
2. Chủ nhóm vào `/cart`, bấm **Tạo đơn nhóm**, gửi link mời cho mọi người. Link dùng trong 24 giờ. Người mở link chưa đăng nhập được chuyển sang đăng nhập rồi trở về đúng nhóm.
3. Từng người mở link, bấm **Gửi món của tôi vào nhóm**. Gửi lại thay snapshot trước; không tăng số người. Chủ nhóm cũng có phần món riêng từ lúc tạo nhóm. Thay giỏ cá nhân sau khi gửi không tự sửa snapshot; phải bấm cập nhật lại.
4. Trang nhóm hiển thị người tham gia, món/size/đường/đá/topping/ghi chú, tiền món dự kiến và mốc ưu đãi. Các thay đổi được kiểm tra lại định kỳ 15 giây khi tab đang mở.
5. Chủ nhóm chốt. Giỏ chủ nhóm được thay bởi toàn bộ lựa chọn đã gửi; giỏ cá nhân của những người khác giữ nguyên. Nhóm ít nhất 2 người mới chốt; từ 3 người mới có ưu đãi.
6. Chủ nhóm chọn chi nhánh, địa chỉ, giao hàng và thanh toán một đơn chung bằng luồng hiện có. Không thu tiền riêng từng thành viên. Tổng tiền được xác nhận lại ở quote, giá dự kiến trước checkout chưa gồm phí giao.

Máy chạy localhost chỉ truy cập được từ chính máy đó. Muốn bạn bè mở link ở thiết bị khác, cần dùng địa chỉ ứng dụng mà các thiết bị đều truy cập được; nút sao chép dùng origin hiện tại, không tự công khai server ra Internet.

## Giỏ hàng và badge

Ảnh, đơn giá gồm topping, thành tiền, size, đường đá, ghi chú; tăng/giảm từ 1–20 ly/dòng, tối đa 50 ly/giỏ; xóa món/giỏ và tổng tiền dự kiến. API `/cart/count` chỉ đọc số dòng và số ly, không tải toàn bộ giá/topping. Badge đếm **số dòng cấu hình món**, không phải số đơn hàng đã tạo: cùng một cấu hình 3 ly vẫn là một dòng, hai cấu hình khác nhau là hai dòng. Hiện trên desktop và mobile, ẩn khi trống; cập nhật sau thêm/xóa/sửa/checkout, khi quay lại tab và sau popup.

## API và ràng buộc

- `POST /group-orders`: tạo nhóm từ giỏ host; mỗi host chỉ có một nhóm hoạt động chưa hết hạn.
- `GET /group-orders/{token}` và `/data`: xem trang/trạng thái; link UUID, chỉ User/Vendor đã xác thực.
- `POST /group-orders/{token}/join|leave|lock|cancel`: body `{version}`; CSRF và kiểm quyền backend, không tin số người hoặc tiền do client gửi.
- Mỗi nhóm tối đa 50 người, mỗi người phải có món; tổng tối đa 50 ly, mỗi dòng tối đa 20. Chỉ nhận món cùng chủ thương hiệu để chọn được chung chi nhánh. Nội dung snapshot giới hạn 60.000 byte UTF-8; ghi chú vẫn tối đa 255 ký tự.
- Khóa user → group → cart; version chống chốt khi có người vừa gửi/rời nhóm. Một tài khoản chỉ có một snapshot trong nhóm. Không giữ tồn kho khi tạo/gửi nhóm.
- Khi đã chốt, chặn sửa/xóa/thêm vào giỏ host; vẫn cho chọn chi nhánh và ánh xạ món bằng catalog_key. Hủy nhóm sẽ gỡ liên kết và ưu đãi, giữ món đã chốt trong giỏ host để chỉnh lại. Nhóm đã tạo order không tái sử dụng/hủy qua API nhóm; dùng workflow hủy đơn hiện có.
- Người bị khóa/chưa xác minh không đủ điều kiện. Kiểm lại người tham gia khi chốt/checkout. Hết hạn ngăn gửi/chốt/checkout ưu đãi nhưng host vẫn được hủy để giải phóng giỏ.

## Tính tiền

`groupDiscount = min(floor(subtotal * rate / 100), 100000)`.

Khoản này nằm trong `orders.product_discount` (tổng giảm món), đồng thời lưu riêng `group_discount`, `group_participants`, `group_order_id` để giải thích nguồn giảm; không trừ hai lần. Không cộng dồn productPromotionId khi có ưu đãi nhóm. Mã giao hàng và điểm vẫn theo giới hạn hiện có; giới hạn điểm tính trên tiền món sau giảm. Ưu đãi nhóm giảm tiền hàng trước khi tính hoa hồng/storeNet theo công thức cũ, không tạo giao dịch tài trợ APP giả. Quote hash gồm mã nhóm, số người và khoản giảm. Checkout vẫn kiểm tồn kho tổng, giờ mở cửa, giá, địa chỉ, idempotency, phân bổ giảm vào item; trạng thái nhóm ORDERED và clear giỏ cùng transaction tạo order.

## Schema

Migration `V2026102603__group_orders.sql` thêm `group_orders`, `group_order_members`; cart liên kết nhóm nullable, order lưu snapshot ưu đãi. FK/unique giữ dữ liệu nhất quán; không reset dữ liệu có trước. Các test H2 dùng schema riêng; không gọi cổng thanh toán/mail thật.

## Chạy kiểm UI cô lập

Sau khi Maven test/package tạo classpath và đã có Edge/Playwright theo công cụ kiểm giao diện của project:

```powershell
python tools/check_all_ui.py --shopping
```

Dựng server H2 riêng cổng 8882, kiểm badge/stepper/giỏ nhóm/host-only/chốt/branch/giảm giá/checkout, rồi tự dừng server fixture. Không chạy fixture trên datasource cloud. Mặc định không có `--shopping` vẫn kiểm toàn bộ module như trước; classpath hỗ trợ cả Maven fork và không fork.

## Kết quả

Xem `SHOPPING_VERIFICATION_20261026.json` trong cùng thư mục. Bằng chứng kiểm UI lần này ở `.local/shopping-ui`, không ghi đè ảnh kiểm của nhánh tích hợp. Chưa push. Không khẳng định các kiểm tra này nghiệm thu toàn bộ provider thanh toán/SMTP hoặc mọi trường hợp refund.
