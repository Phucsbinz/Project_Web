# Rút gọn trang chủ UTETra — 26/10/2026

## Yêu cầu

> trang chủ của web quá nhiều thông tin , chỉ nổi chọn phần cốt lõi nỗi bật thôi , rồi chuyển đến phần thực đơn

## Kết quả

- Trang `/` dùng JSP module `catalog/home.jsp`, giữ chung SiteMesh `layouts/app.jsp` và theme UTETra.
- Giữ banner ngắn, nút chính **Xem thực đơn** dẫn đến `/menu` và tối đa **4 món nổi bật** theo lượt bán. Giữ điều kiện sản phẩm dành cho khách chưa đăng nhập của service hiện có.
- Bỏ các khối danh mục, bộ lọc, giỏ tóm tắt, bốn danh sách Top 20 và nội dung giới thiệu dài khỏi trang chủ. Khi chưa có món phù hợp, trang chủ vẫn có banner và nút vào thực đơn.
- Trang `/menu` giữ bộ lọc, phân trang, giỏ tóm tắt và xem nhanh sản phẩm; bổ sung bốn liên kết Top 20 theo món mới, bán chạy, đánh giá và yêu thích.
- CSS trang chủ scope dưới `.catalog-homepage`. Điện thoại dùng banner một cột và lưới sản phẩm hai cột.
- Không sửa layout/theme chung, cấu hình nghiệp vụ, datasource, JWT hoặc provider. Không commit/push.

## Kiểm chứng đã chạy

- Java 21, Maven: `BaselineViewTest,CatalogViewTest` — **8 test đạt**, không lỗi; render JSP/SiteMesh trên Tomcat thật. Kiểm tra trang chủ gọn và cả bốn liên kết Top 20 ở thực đơn.
- `python tools/check_shared_ui.py` — đạt, một layout và 21 file dùng chung.
- Edge headless với H2 riêng, dữ liệu mẫu có đơn đã giao; không gọi mail/Cloudinary. Kiểm tra khách và người đăng nhập ở chiều rộng 390, 768, 1440px: **12 lượt kiểm tra đạt**, không lỗi JavaScript, không tràn ngang. Nút banner chuyển đến `/menu`, trang chủ đúng 4 thẻ sản phẩm, thực đơn giữ bộ lọc và bốn liên kết Top 20.
- Ảnh và kết quả trình duyệt: [homepage-compact-20261026](demo/homepage-compact-20261026/verification.json). Tên/giá sản phẩm trong ảnh là dữ liệu H2 phục vụ kiểm tra bố cục.

Các kiểm tra trên không thay thế kiểm thử toàn bộ hệ thống hoặc dữ liệu production.
