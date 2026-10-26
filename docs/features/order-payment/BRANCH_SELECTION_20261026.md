# Chọn chi nhánh tại bước lên đơn — 26/10/2026

Yêu cầu của Thanh thay luồng cũ: chọn đồ uống/tùy chọn trước; chỉ chọn chi nhánh ở checkout. Không tính khoảng cách. Source hiện giữ local trên identify, chưa push.

## Hành vi

- Trang chủ/menu dùng một thẻ cho mỗi món chung, không có bộ lọc hoặc nhãn chi nhánh. Các trang giới thiệu cửa hàng và quản trị từng chi nhánh vẫn được giữ.
- Click thẻ món mở popup Bootstrap theo cùng template; có size, đường/đá dạng nút, topping, tăng/giảm số lượng và tạm tính. URL chi tiết vẫn mở trực tiếp được.
- Thêm món vào giỏ chưa quyết định chi nhánh; tài khoản đăng nhập vẫn lưu giỏ trong database, khách lưu snapshot trong session. Giữ cơ chế merge khách vào tài khoản.
- Checkout bước 1 chọn chi nhánh. Hệ thống đối chiếu toàn bộ món/size/topping, giờ hoạt động và tồn kho cộng dồn trước khi thay giỏ trong một transaction. Nếu không đáp ứng hoặc version cũ thì trả lỗi và giữ nguyên giỏ.
- Checkout bước 2 mới chọn địa chỉ, khu vực, nhà vận chuyển, ưu đãi/điểm và phương thức thanh toán. Báo giá phải được lấy lại; chỉ khi đặt đơn mới giữ tồn kho. Chuyển chi nhánh không làm thay đổi đơn hàng đã có.

## Mã món và dữ liệu

Migration mới `V2026102601__shared_drink_catalog.sql` thêm `products.catalog_key` nullable và unique `(store_id,catalog_key)`. Không sửa migration đã áp dụng, không thay IDs/FK/snapshot của sản phẩm hoặc đơn cũ. Mã món dùng chung cùng owner và cùng danh mục xác định một món ở nhiều chi nhánh; không suy diễn chỉ từ tên. Món chưa có mã vẫn là sản phẩm riêng. Không gộp món của các owner khác nhau.

Vendor/Admin có trường “Mã món chung giữa các chi nhánh” trong form sản phẩm; mã chỉ gồm chữ thường/số/gạch ngang, tối đa 150 ký tự. Các bản sao thực sự cùng món dùng cùng mã. Mỗi chi nhánh chỉ có một sản phẩm cho mã đó. Để trống cho món riêng; API client cũ không gửi trường vẫn giữ mã đang có.

90 sản phẩm demo đã được gán 30 mã rõ ràng bằng `tools/demo/AssignCatalogKeys.java`, chỉ chạm đúng slug và owner demo đã biết. Bộ seed mới cũng ghi mã ngay khi tạo. Menu phân trang/lọc/sắp xếp sau gộp; lượt bán/rating/yêu thích và bình luận tổng hợp theo mã cùng owner/danh mục. Đánh giá vẫn giữ bằng chứng order item theo chi nhánh đã mua.

## API và ràng buộc

- UI chọn món dùng `POST /cart/draft/items`. Các API giỏ cũ giữ tương thích cho luồng tích hợp cũ.
- `POST /checkout/branch` nhận `{storeId,version}`, kiểm positive ID/version, quyền buyer và version giỏ; đối chiếu size/topping vào đúng sản phẩm của chi nhánh đích. Không âm thầm đổi sang món khác hoặc làm mất giỏ.
- Giới hạn 20 ly/dòng, 50 ly/giỏ, đường/đá allowlist, version/CSRF/ownership vẫn được kiểm. Stock được cộng theo variant qua mọi cấu hình, không chỉ kiểm từng dòng.
- Chọn chi nhánh và lấy báo giá không ghi giao dịch thanh toán hoặc gửi email.
- Popup chỉ nhúng tài liệu `/products/<id>?quick=1` cùng origin. Header X-Frame-Options là SAMEORIGIN đúng trường hợp đó, còn các trang khác giữ DENY; không tắt CSRF hoặc JWT.

## Kiểm chứng

Toàn bộ bộ test sau thay đổi nghiệp vụ: 167 tests đạt, 0 failures/errors/skipped. Sau chỉnh câu chữ JSP: 6 smoke/render tests đạt. Các test mới kiểm chuyển branch giữ cấu hình, stock cộng dồn, stale version/thiếu món/topping/đóng cửa rollback, chỉ gộp theo owner và quick-view framing. Kiểm browser luồng khách → popup → login → merge → chọn branch → báo giá (không đặt đơn), và responsive/quản trị: kết quả ở `docs/demo/branch-flow/verification.json` và `docs/demo/ui/verification.json`.

Kết quả browser cuối: 21 lượt trang/viewport qua các vai trò và 5 kiểm tra luồng mới đạt, không JS exception/tràn ngang. Kiểm database giữ 106 đơn, 90 review/comment, tồn kho và điểm nhất quán. Server local đã mở lại, chưa commit/push.
