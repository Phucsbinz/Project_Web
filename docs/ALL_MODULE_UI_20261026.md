# Tích hợp UI đầy đủ các module — 26/10/2026

Nhánh integration/utetra, phát triển tiếp từ bản đã push 0920112. Giữ nguyên main/feature khác. Phạm vi là các màn hình đã có nghiệp vụ, không biến placeholder thành chức năng mới hoặc bỏ quyền để mở UI.

## Nhóm màn hình được rà

- Guest: trang chủ, menu, shop/list shop, chi tiết món, giỏ khách, login/register/forgot/OTP/reset.
- User: giỏ, bước chọn chi nhánh và checkout, danh sách/chi tiết đơn, hồ sơ, bảo mật, nhiều địa chỉ, điểm thành viên, yêu thích, đã xem, đánh giá, bình luận, đăng ký shop và thông báo.
- Vendor: storefront User được giữ; trang shop, sản phẩm/size/tồn, khuyến mãi, khu vực giao và topping tại shop, đơn shop/chi tiết đơn, doanh thu, đăng ký shop và thông báo.
- Manager: sản phẩm/shop, danh mục/topping theo delegation, chương trình khuyến mãi, khu vực/topping tại shop, user trong phạm vi, carrier/phí/hoa hồng, đơn/chi tiết đơn và báo cáo; hồ sơ, bảo mật và thông báo.
- Admin: các phần quản trị trên theo scope toàn hệ thống và phân công Manager/quyền danh mục dùng chung, hồ sơ/bảo mật/thông báo.
- Shipper: danh sách đơn được giao và thống kê, thông báo/hồ sơ/bảo mật. Bật riêng trong H2 fixture để kiểm UI, cấu hình feature của cloud không đổi.

## Bổ sung thực

1. Một app.jsp, Bootstrap, Be Vietnam Pro và utetra.css tiếp tục dùng chung cho mọi vai trò. Không tạo layout role thứ hai.
2. Sidebar có đường dẫn trực tiếp tới khu vực giao, topping tại cửa hàng, danh mục topping, phân công Manager/catalog delegation (Admin), thông báo, hồ sơ và bảo mật. Sidebar dài cuộn được trên desktop, không cắt mất menu cuối.
3. Dropdown User/Vendor thêm địa chỉ, đã xem, đánh giá, bình luận; footer tránh dẫn Manager/Admin/Shipper tới trang mua hàng không thuộc quyền.
4. Shipper report có bộ lọc ngày wrap trên mobile, bốn KPI từ service thật và nút trở về đơn được phân công; không dùng số giả để lấp UI.
5. Picker shop catalog wrap trên mobile. Navigation phụ của phần đơn hàng wrap thay vì khuất nhãn trên mobile; mọi route/action nghiệp vụ giữ nguyên.

## Kết quả kiểm chứng

- Package chạy 13 render tests: BaselineViewTest (3), CatalogViewTest (5), IdentityViewTest (2), ba case render/CSRF của OrderCommerceIntegrationTest. 0 failures/errors/skipped, BUILD SUCCESS lúc 21:25:11. Có Tomcat/SiteMesh thực.
- Sau xem ảnh sửa thêm wrap menu mobile, package lại với skipTests lúc 21:30:56. Không nhận đó là chạy lại Maven test.
- Browser vòng đầu: 158 route/viewport checks ở 390px và 1440px, 0 failures/pageerror. Vòng cuối kiểm thêm API/assets HTTP lỗi và Admin chọn shop để form cấu hình được dựng đầy đủ; kết quả cuối đọc [verification.json](demo/all-modules-20261026/verification.json).
- Scope: 79 tổ hợp role/path, mỗi tổ hợp hai viewport. Có cả trạng thái có dữ liệu và trạng thái chưa có đơn/đánh giá trong fixture. Chọn shop Admin là bước cần thiết để cấu hình; màn chờ chọn shop không phải thiếu UI.
- H2 fixture và tài khoản giả lập, không phải giao dịch/provider thật hoặc dữ liệu doanh thu thật. Shipper/delegation chỉ bật/gán trong fixture. Test không thay cấu hình provider, datasource/JWT hoặc phân quyền của bản thật.

## Minh chứng và chạy lại

Ảnh nằm trong docs/demo/all-modules-20261026, tên gồm role, path và viewport. Xem [Shipper mobile](demo/all-modules-20261026/shipper-shipper-reports-390.png), [địa chỉ User](demo/all-modules-20261026/user-account-addresses-390.png), [phân công Admin](demo/all-modules-20261026/admin-admin-memberships-1440.png). Các màn popup/theme trước đó có trong UI_REFERENCE_REFINEMENT_20261026.md.

Chuẩn bị JDK 21, Maven package/test để sinh target/test-classes và Surefire classpath; có Playwright/Edge như tool browser của nhóm. Chạy `py -3 tools/check_all_ui.py`: compile helper tools/ui/AllUiServer.java, khởi động H2-only trên 8882, chạy tools/check_all_module_ui.py, ghi JSON/ảnh, tự dừng tiến trình do chính tool tạo. Port bận thì dừng với thông báo, không kill ứng dụng khác. Fixture OrderCommerceIntegrationTest có guard chỉ cho phép DB thử riêng trước khi reset dữ liệu. Không deploy helper hoặc đóng gói nó vào WAR.

Đây là kiểm phủ UI/render/navigation/responsive và lỗi tải dữ liệu ở các route được liệt kê; không khẳng định đã thao tác mọi nhánh CRUD/trả–hoàn/OTP/provider thật. Các mutation nghiệp vụ có test riêng, còn kiểm provider thật theo báo cáo yêu cầu thầy.

## Kết quả vòng cuối

**158/158 checks đạt; 0 lỗi UI/JS/API hoặc assets được theo dõi.** Admin products/areas/stock/memberships được mở với shop fixture đã chọn để kiểm form thực. Số lượt theo vai trò: guest 22, user 32, vendor 28, manager 32, admin 34, shipper 10. Đã xem ảnh cuối Shipper mobile/menu wrap và form phân công Admin. Server H2 do launcher tạo đã tự dừng sau kiểm.
