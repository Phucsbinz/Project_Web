# Thành viên theo dõi đơn nhóm — 28/10/2026

## Yêu cầu
Thanh yêu cầu thành viên cũng xem được trạng thái đơn nhóm sau khi Admin xác nhận, không chỉ chủ nhóm.

## Thực hiện
- Danh sách Đơn mua của tôi gồm đơn cá nhân và đơn nhóm mà user có snapshot phần món đã được đưa vào đơn. EXISTS theo cả groupOrderId/orderId/userId tránh nhân bản đơn chủ nhóm và không mở quyền chỉ nhờ biết link mời.
- GET chi tiết/data kiểm tài khoản/vai trò hiện hành và snapshot thành viên đúng đơn. Người ngoài hoặc người tham gia nhưng không có món trong đơn không được cấp quyền đọc.
- Thành viên xem chi tiết món, giao nhận chung, thanh toán và lịch sử; trạng thái dùng cùng polling 15 giây khi không đang nhập/modal, không tạo một nguồn trạng thái khác.
- Đơn nhóm có badge trong danh sách, trang nhóm có trạng thái và nút Theo dõi đơn hàng. Chi tiết ghi rõ vai trò chủ nhóm/thành viên và link về nhóm.
- Event đơn lưu notification và gửi private websocket cho các thành viên có phần trong đơn, ngoài các người nhận hiện tại. Chỉ gửi sau commit; tập recipients không trùng.
- Không mở quyền hủy, trả hàng/hoàn tiền, xác nhận nhận hàng hoặc đổi trạng thái. Server giữ owner/operator checks; member UI không hiện các thao tác owner. QR chỉ hiện đường dẫn thanh toán khi user có phần cần trả trong ledger; không hiển thị nút trả toàn bộ của chủ nhóm cho thành viên.
- Giữ một app.jsp/theme/Bootstrap, JSP trong module order. Không đổi datasource/provider hoặc bật shipper.

## Kiểm chứng
Kết quả sẽ ghi sau khi chạy. Không push.

- OrderCommerceIntegrationTest: 84/84 qua; mới kiểm participant đọc/lọc đơn, thông báo TRANSITION sau Admin xác nhận, host không bị trùng, người ngoài/không có snapshot bị chặn, member không hủy/nhận/trả thay chủ nhóm. WAR thành công.
- Edge + Tomcat/SiteMesh fixture H2: 18/18 shopping scenarios qua. Thành viên tìm thấy đơn trong Đơn mua của tôi, đọc CONFIRMED và tự cập nhật DELIVERING sau operator đổi; không xuất hiện nút hủy/nhận/trả.
- Shared UI checker và git diff --check qua; server MySQL đã mở lại. Không push.
