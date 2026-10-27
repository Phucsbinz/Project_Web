# Quản trị dễ thao tác và dấu thả xuống — 28/10/2026

## Prompt
Thanh yêu cầu sửa trang quản trị dễ sử dụng, dễ hiểu, dễ thao tác; ô có danh sách cần dấu/nút thả xuống rõ ràng để biết có thể bấm chọn.

## Triển khai
- Giữ một app.jsp / Bootstrap / utetra.css. Không sửa quyền, datasource hay nghiệp vụ.
- Mũi tên native select bị mất vì nhiều shorthand background trong theme ghi đè background-image của Bootstrap. Quy tắc cuối trong utetra.css khôi phục mũi tên, chừa vùng bên phải và cursor; giữ select thật, bàn phím/trình đọc màn hình và trạng thái disabled. Select multiple vẫn là danh sách nhiều lựa chọn.
- Checkout searchable choices có nút mở toàn bộ lựa chọn, menu rõ ràng, tìm theo chữ, ArrowDown/ArrowUp/Enter/Escape, đóng khi ra ngoài. Input vẫn nhập được; hidden select giữ ID đúng cho server; không chấp nhận giá trị tự do ngoài lựa chọn. Đổi chi nhánh/option disabled vẫn được quan sát và kiểm lại. Không thay form validation hay CSRF.
- Trang đầu Admin có lối tắt đơn hàng/tài khoản/báo cáo/hỗ trợ. Các trang catalog có tiêu đề và giải thích theo chức năng; Chi nhánh đang quản lý thay nhãn Shop, Làm mới dữ liệu thay Tải lại; trạng thái chi nhánh tiếng Việt, tiền theo định dạng Việt Nam.
- Tìm trong danh sách chỉ lọc dữ liệu đã tải theo tên/địa chỉ/mã, không tuyên bố tìm toàn DB. Hiện số kết quả; Chỉnh sửa đánh dấu mục đang sửa, cuộn/focus đến form; nút thêm mới và hủy chỉnh sửa/làm trống biểu mẫu rõ ràng. Multi-select có hướng dẫn; slug có ví dụ cấu trúc. CSS scope #catalog-manager, responsive một cột trên màn hình nhỏ.
- Sidebar quản lý mobile thu gọn sau nút Chọn chức năng quản lý, có aria-expanded và mở/đóng rõ ràng; desktop giữ menu trực tiếp. Admin có link Tổng quan trong sidebar. Chi nhánh chưa chọn chỉnh sửa không còn form trống dài với nút lưu bị vô hiệu; chọn Chỉnh sửa sẽ mở form.
- Shared bundle đổi utetra.css, site-ui.js và cache trong app.jsp; menu checkout dùng class scope checkout-page. Không thêm layout hay thư viện.

## Kiểm chứng
Kết quả thực được bổ sung sau khi chạy. Không push.

- CatalogViewTest: 5/5 qua với Tomcat/SiteMesh, WAR build thành công.
- Admin browser audit: 17/17 qua (8 trang × 2 kích thước + hành vi menu chọn), không JS errors/overflow; đã xem ảnh desktop/mobile. Kiểm tra tìm kiếm, đánh dấu chỉnh sửa, thêm mới, mũi tên select và đóng/mở sidebar mobile; thao tác trên fixture H2 riêng.
- Phát hiện selector cũ chọn tất cả button trong form chi nhánh sau khi thêm nút menu; đã dùng submit button tường minh và cập nhật browser locator. Không đổi API chọn chi nhánh.
- Shared UI: một SiteMesh layout, 21 shared files; git diff --check qua.
- Shopping browser audit cuối: 17/17 qua, gồm thanh toán cá nhân/nhóm, nhận hàng, badge và hạn giỏ. Bộ lựa chọn mới không làm hỏng luồng checkout.
- Server MySQL đã mở lại bằng WAR cuối ở cổng 8080. Không commit/push trong đợt này.
