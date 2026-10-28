# Trang chủ — Top 20, hai hàng trượt ngang

## Prompt

> chỗ món bán chạy và món mới hiện top 20 món nhưng để thành 2 hàng xem thêm thì trượt ngang qua

## Kết quả

- Hai tab Bán chạy/Món mới nhất lấy tối đa 20 món theo thứ tự bán hoặc ngày tạo. Giữ điều kiện chọn món của trang chủ Guest đã thống nhất cho cả trước/sau đăng nhập; không thêm món giả khi dữ liệu hợp lệ ít hơn 20.
- Grid hai hàng, trượt theo cột: desktop bốn cột, tablet ba cột, mobile hai cột trong mỗi khung nhìn. Các cột tiếp theo nằm trong vùng cuộn ngang của tab.
- Nút Xem thêm chuyển sang phần kế tiếp; nút quay lại, thanh cuộn/trackpad, vuốt cảm ứng và phím trái/phải/Home/End đều sử dụng được. Nút vô hiệu ở đầu/cuối; trạng thái cho biết khoảng món đang xem. Tôn trọng tùy chọn giảm chuyển động.
- Dùng lại `product-list.jspf`, modal và endpoint tùy chọn món hiện có. CSS/JS carousel scope trang chủ; không đổi grid thực đơn hoặc tạo layout/framework thứ hai. Trạng thái rỗng dùng khung đầy đủ; chỉ một món thì không tạo hàng rỗng dư thừa.
- File mới: `catalog/home-product-carousel.jspf` và `assets/js/catalog-home.js`; nội dung trang chủ nạp CSS/JS với version mới.

## Kiểm chứng thực tế

- Java 21, Maven; `BaselineViewTest,CatalogViewTest`: **10 test đạt**, không failure/error/skipped. Render JSP/JSTL/SiteMesh trên Tomcat thật.
- Test 22 món fixture có doanh số hợp lệ: mỗi tab đúng 20 thẻ, bán chạy và mới nhất chọn đúng thứ tự/cắt đúng nhóm. Giữ test nội dung Guest giống User/Vendor/Manager/Admin và các Top 20 ở thực đơn.
- Edge headless, H2 riêng: **14 kiểm tra đạt**, không lỗi JavaScript. Guest và Vendor ở 1440/768/390px, mỗi tab 20 món/2 hàng, đúng số cột, không tràn ngang trang; nút tới/lùi, Home/End, tới món cuối và vuốt cảm ứng thật bằng CDP trên mobile. Hai lượt kiểm thẻ mở quickview thật cũng qua.
- `python tools/check_shared_ui.py`, `node --check catalog-home.js` và `git diff --check` đạt. Chỉ tính lượt kiểm thành công sau khi script chờ nút quay lại cập nhật trạng thái cuộn.
- Ảnh và kết quả: [home-top20-two-rows-20261028](demo/home-top20-two-rows-20261028/verification.json). Đây là dữ liệu H2 để kiểm giao diện, không phải doanh số/tên món production. Header/skip link chỉ ẩn lúc chụp riêng component để không che ảnh; không đổi source chung.
- Không thay datasource/JWT/provider; không ghi dữ liệu cloud. Máy chủ và thư mục build/browser tạm được dọn sau kiểm. Chưa commit/push.
