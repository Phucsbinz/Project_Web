# Đồng nhất trang chủ theo bản Guest — 28/10/2026

## Prompt

> đồng nhất lại trang chủ khi đăng nhập và chưa đăng nhập , chọn trang chủ lúc chưa đăng nhập

## Thay đổi

- `/` vẫn dùng chung `catalog/home.jsp`, SiteMesh `app.jsp` và theme hiện tại.
- Bỏ khối bốn bảng Top 20 chỉ xuất hiện sau đăng nhập và bỏ truy vấn `homeGroups` riêng của người đăng nhập.
- Hai tab nổi bật/mới dùng cùng điều kiện chọn món với Guest cho mọi trạng thái: tối đa 4 món, giữ điều kiện doanh số của trang chủ Guest. Tìm kiếm và các danh sách Top 20 ở `/menu` vẫn giữ nguyên.
- Nội dung banner, danh mục, ưu đãi, thương hiệu, chi nhánh và CTA dùng chung. Header tài khoản/đăng nhập và kiểm quyền chức năng vẫn do layout/security hiện có xử lý.
- Không đổi datasource, JWT, provider, giá/checkout, không tạo layout hoặc CSS thứ hai. Mô tả trang chủ có bốn khối Top 20 riêng trong biên bản cũ là lịch sử trước yêu cầu này.

## Kiểm chứng thực tế

- Java 21, Maven; `BaselineViewTest,CatalogViewTest`: **9 test đạt**, không failure/error/skipped, BUILD SUCCESS.
- Test mới gọi HTTP thật trên Tomcat/SiteMesh và so sánh toàn bộ vùng `.catalog-homepage` của Guest với User, Vendor, Manager và Admin; nội dung render giống nhau. Fixture có sản phẩm chưa có doanh số nên cũng kiểm được việc không tự thêm sản phẩm ngoài lựa chọn Guest sau đăng nhập.
- Các kiểm render quản trị, đăng ký shop và liên kết/bộ lọc Top 20 ở thực đơn vẫn qua.
- `python tools/check_shared_ui.py`: một layout, 21 file chung. `git diff --check` đạt.
- Kiểm thử trên bản sao H2 cô lập; không tác động DB/provider thật hoặc ứng dụng người dùng đang chạy. Thư mục build tạm được dọn sau khi ghi kết quả. Chưa commit/push.
