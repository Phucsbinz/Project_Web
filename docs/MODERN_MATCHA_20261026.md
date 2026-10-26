# UTETra — Modern Matcha, 26/10/2026

## Prompt của người dùng

> Hãy nâng cấp giao diện trang chủ UTETra hiện tại theo phong cách Modern Matcha hiện đại, cao cấp, trẻ trung và chuyên nghiệp, lấy cảm hứng từ các website trà sữa nổi tiếng nhưng phải có thiết kế riêng, không sao chép hoàn toàn hình mẫu.
>
> Giữ tông xanh matcha, kem sữa và trắng. Header có logo, menu, tìm kiếm, tài khoản và giỏ hàng. Hero có hình trà sữa lớn, slogan, hiệu ứng nhẹ và nút đặt hàng. Bổ sung danh mục, sản phẩm bán chạy, sản phẩm mới, khuyến mãi, giới thiệu thương hiệu và chi nhánh. Product Cards gọn, có hình, tên, giá, đánh giá và nút thêm vào giỏ. Cân đối typography, spacing, hình ảnh; thêm hover, transition, loading, thông báo; responsive desktop/tablet/mobile.
>
> Chức năng: tìm kiếm/lọc/chi tiết, size/đường/đá/topping/số lượng, giỏ/cập nhật/thanh toán, đăng nhập/đăng ký/tài khoản, chi nhánh/ưu đãi/theo dõi đơn. Nút và liên kết phải hoạt động thực tế. Giữ Spring Boot + JSP/JSTL + SiteMesh + Bootstrap 5 + JPA + JWT + WebSocket + Cloudinary; sửa dự án hiện tại, tái sử dụng endpoint/component/logic, giữ dữ liệu và xác định rõ phần thiếu backend, không giả lập hoàn chỉnh.

## Thực hiện

- Trang chủ giữ một hero lớn với ảnh UTETra đang có, hai tab bán chạy/mới nhất (tối đa 4 món mỗi tab), danh mục lấy từ DB, khu vực ưu đãi, thương hiệu và tối đa 3 thẻ chi nhánh. Bộ chọn chi nhánh chứa toàn bộ chi nhánh đang hoạt động. Không sao chép template bên ngoài, thêm framework hoặc tạo project/layout khác.
- Dùng token xanh trà/kem, font và Bootstrap local hiện có. CSS trang chủ scope `.catalog-homepage`, không thay `body`, palette hoặc layout theo vai trò.
- Tái sử dụng `product-list.jspf`; ảnh/tên dẫn đến chi tiết. Card trang chủ ghi **Thêm vào giỏ**, mở tùy chọn thực tế rồi mới gửi giỏ. Hiển thị đánh giá thật; chưa có đánh giá thì nói rõ, không tạo số sao giả.
- Tách `drink-modal.jspf` để trang chủ và thực đơn cùng dùng. Có trạng thái tải, phản hồi lỗi/thành công từ backend và toast sau khi lưu thành công. Xử lý phản hồi đến trong lúc Bootstrap đang mở modal. Giữ kiểm tra origin/source của iframe và CSRF.
- Ưu đãi hiển thị tối đa 2 ưu đãi hiện hành do `PromotionService.available` trả về cho chi nhánh được ghi tên. Không tạo mã/giảm giá giả khi không có dữ liệu. Backend checkout vẫn kiểm tra thời gian, hạn mức, phạm vi và điều kiện.
- Bộ chọn chi nhánh trên trang chủ dẫn tới thực đơn lọc theo `storeId`; bổ sung select chi nhánh ở form thực đơn để giữ lựa chọn khi tìm/lọc tiếp. Chi nhánh phục vụ giỏ được xác nhận bằng luồng checkout hiện có, không dùng localStorage giả như một lựa chọn đã được server chấp nhận.

## Chức năng tái sử dụng và giới hạn

| Chức năng | Luồng thật đang dùng |
| --- | --- |
| Tìm kiếm, lọc danh mục/chi nhánh/giá/sắp xếp | GET `/menu`, `CatalogService.search` |
| Chi tiết và tùy chọn | `/products/{id}`, `catalog/product.jsp`, variant/đường/đá/topping/số lượng từ backend |
| Thêm, đọc, sửa giỏ | POST `/cart/draft/items`, GET `/cart/data`, trang `/cart` và các endpoint cập nhật hiện có |
| Chọn chi nhánh, báo giá, đặt hàng | `/checkout`, POST `/checkout/branch`, `/checkout/quote`, `/checkout` |
| Đăng nhập/đăng ký/tài khoản | `/auth/login`, `/auth/register`, `/account/profile`; các luồng JWT/OTP hiện có |
| Chi nhánh và ưu đãi | `/stores`, `/stores/{id}`, `StoreService`, `PromotionService` |
| Theo dõi đơn | `/orders` và trang đơn hiện có; giữ nguyên WebSocket và kiểm quyền hiện tại |
| Ảnh | URL DB/Cloudinary hoặc asset đang có, giữ provider hiện tại |

Phạm vi này không thêm API nghiệp vụ, lưu dữ liệu mẫu vào DB thật, sửa datasource/JWT/provider, hoặc giả lập thanh toán/thông báo realtime. Không có chức năng mới nào được đánh dấu hoàn chỉnh bằng backend giả. Kiểm thử H2 không xác nhận cấu hình mail, thanh toán hoặc Cloudinary của môi trường triển khai đang hoạt động.

## Bundle dùng chung

Hai file dùng chung thay đổi là `layouts/app.jsp` (form tìm kiếm GET có label, cache version) và `assets/css/utetra.css` (header search responsive, link chi tiết trên card). Cả hai đã nằm trong `SHARED_UI_FILES.txt`. Lý do: header phải dùng chung cho tất cả trang/vai trò, không nhúng header thứ hai vào trang chủ. Khi tích hợp cần đồng bộ hai file này cùng bundle từ main sang các feature; lần làm việc này không đổi nhánh hoặc commit/push main/feature.

## Kiểm chứng

- Java 21 / Tomcat thật: `BaselineViewTest,CatalogViewTest` — **8 test đạt**; trang chủ khách/người đăng nhập, JSP/JSTL/SiteMesh, các trang quản lý, escaping dữ liệu và thực đơn Top 20.
- `OrderCommerceIntegrationTest`: ba test có sẵn về quickview same-origin, giỏ trung lập/chọn chi nhánh giữ tùy chọn, đăng nhập/merge giỏ/COD/hoàn tiền — **3 test đạt**.
- Edge headless: **17 lượt kiểm tra đạt**, không lỗi JavaScript. Kiểm khách/người đăng nhập ở 390/768/1440px, tìm kiếm thật, tab mới, mở/đóng quickview và không tràn ngang. Kiểm lưu món với đường/đá/số lượng, chọn size L/topping, sửa số ly tại giỏ, chọn chi nhánh và nhận báo giá checkout thật, giữ lọc chi nhánh và liên kết danh mục. Không submit đơn trong lượt trình duyệt này.
- `python tools/check_shared_ui.py` đạt: một layout, 21 file chung. `git diff --check` đạt. `mvn -DskipTests package` với Java 21 build thành công `target/utetra.war`; đây là đóng gói sau các lượt test nêu trên, không phải xác nhận toàn bộ test suite.
- Chứng cứ trình duyệt và kết quả cuối: [modern-matcha-20261026](demo/modern-matcha-20261026/verification.json). Ảnh và các ưu đãi/tên/giá trong ảnh là dữ liệu fixture H2 riêng, không phải dữ liệu production.

Các biên bản trang chủ trước đó ghi lại thiết kế và kiểm thử lịch sử, không mô tả bản Modern Matcha này.
