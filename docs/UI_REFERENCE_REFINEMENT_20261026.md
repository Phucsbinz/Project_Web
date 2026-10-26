# Giao diện theo màu và phông ảnh tham chiếu — 26/10/2026

Nhánh: integration/utetra, phát triển tiếp từ 70d96c5. Chưa commit/push hoặc cập nhật main/feature khác.

## Yêu cầu và kết quả thiết kế

Prompt nguyên văn (kèm ảnh do người dùng cung cấp):

```text
[Image #1] bạn thiết kế giao diện theo màu và phông như này được không vẫn giữ đầy đủ chức năng
```

Giữ bố cục đã tích hợp, tinh chỉnh theo chất liệu kem ấm/xanh trà của poster, logo serif và UI sans rõ tiếng Việt. Dùng Be Vietnam Pro local thay Segoe UI mặc định. Không khẳng định font này chính là font trong ảnh. Không dùng ImageGen: phần này được làm bằng CSS/JSP và asset font, ảnh đồ uống có sẵn được giữ.

- Xanh trà #315638; xanh đậm #193d2d; nền kem #f5f2e8; giấy #fffef9; chữ #263b2c; nhấn nâu vàng #a5773d.
- Một hệ thống font 400/500/600/700, card và input gọn, nút có trạng thái focus/hover/disabled rõ, giá dùng tabular numerals, menu mobile dễ chạm.
- Hero kem/xanh, card đồ uống có ảnh, giỏ bên phải, popup tùy chọn món và sidebar workspace cùng bảng màu.
- Font/license nằm trong assets/vendor/fonts/be-vietnam-pro, nguồn Google Fonts, giấy phép SIL OFL. App preload font regular local; không gọi CDN runtime. Shared manifest đã thêm đủ 5 file.

## Phạm vi chức năng

Không sửa Java, schema, datasource, JWT, provider, API, quyền hoặc state machine. Giữ nguyên search/filter/sort/pagination, size/đường/đá/topping/số lượng, chọn chi nhánh ở checkout, giỏ, thanh toán, tài khoản và các module quản lý. Không tạo layout theo role hoặc thêm frontend framework. Không dùng số liệu doanh thu tự bịa để giống dashboard trong poster.

File triển khai dùng chung: utetra.css, layouts/app.jsp và font/license. Giữ nguyên các thay đổi có sẵn trước lượt làm này, gồm nhật ký và trạng thái ảnh nguồn; không tự commit chúng.

## Kiểm chứng thực tế

1. Shared UI checker đạt: một SiteMesh layout, 21 file shared (16 file cũ + 5 file font/license). Bản mới khác main là có chủ đích, chưa đồng bộ các nhánh khác.
2. Maven package với BaselineViewTest, CatalogViewTest, IdentityViewTest: **10 tests, 0 failures/errors/skipped, BUILD SUCCESS**. Có Tomcat/SiteMesh thực. [Kết quả render](demo/reference-20261026/render-tests.json).
3. Browser trên WAR/TiDB: **21 lượt route/viewport**: trang chủ/menu/detail, profile/security/orders User và trang sản phẩm/đơn/báo cáo Admin, Manager, Vendor; 390/768/1440px. Font load được, ảnh không hỏng, không tràn ngang, không pageerror; một header/footer. [Biên bản vòng đầu](demo/reference-20261026/verification.json).
4. Khi xem ảnh vòng đầu, phát hiện wordmark footer xanh đậm trên nền xanh. Đã sửa màu chữ thành kem #fff9e8; sửa thêm khối ghi chú sidebar quản lý sang nền xanh trong suốt với chữ sáng để tránh chữ nhạt trên nền kem. Đóng gói lại WAR thành công với skipTests (lượt cuối 20:39:45). Không gọi lượt đóng gói đó là lần chạy test mới. Vòng kiểm browser cuối và ảnh popup được ghi riêng trong [final-verification.json](demo/reference-20261026/final-verification.json). **6 kiểm tra cuối đạt**, gồm font/footer ở 390/1440px, popup chọn size/số lượng/tạm tính/cuộn tới nút thêm giỏ khả dụng và sidebar/báo cáo Admin ở 390/1440px; không JS exception hoặc tràn ngang. Không gửi mua hàng hoặc thanh toán.
5. WAR xác nhận 10 migration, schema đã up to date; không chạy seed hoặc thao tác thu/hoàn tiền. Login/view có thể cập nhật session/lịch sử xem; không gọi đó là kiểm thử không ghi DB hoàn toàn.

Kiểm này tập trung giao diện và render, không thay nghiệm thu mọi nghiệp vụ hoặc provider thật. Bộ 167 test toàn hệ thống trước đổi giao diện nằm ở INTEGRATION_VERIFICATION_20261026.md; không trộn số test cũ vào lượt render mới.

## Hình để review

- [Trang chủ desktop](demo/reference-20261026/home-desktop.png).
- [Trang chủ mobile](demo/reference-20261026/home-mobile.png).
- [Thực đơn desktop](demo/reference-20261026/menu-desktop.png).
- [Popup desktop](demo/reference-20261026/popup-desktop.png).
- [Popup mobile](demo/reference-20261026/popup-mobile.png).
- Bộ ảnh vai trò quản lý, hồ sơ và đơn hàng nằm cùng thư mục.

Prompt được giữ trong AI_PROMPT_HISTORY.md và UI_PROMPT_HISTORY.md. Khi phát hành, đồng bộ bundle theo SHARED_UI_FILES.txt; không chép riêng CSS mà quên font/license.


## Chạy lại kiểm tra giao diện

Khởi động WAR bằng tools/start_web.py sau khi package. Chạy `py -3 tools/check_reference_theme.py`: kiểm font/footer, popup và độ rộng 390/1440px. Cần dataset có món đang bán; script không tự seed. Nếu muốn kiểm thêm báo cáo Admin, truyền UTETRA_DEMO_EMAIL/UTETRA_DEMO_PASSWORD của tài khoản demo được phép; không ghi credential thật vào script hoặc nhật ký. Script chỉ xem trang và đổi tùy chọn trong popup, không bấm thêm giỏ/đặt đơn/thu tiền. Với phần Admin, login vẫn tạo session thông thường.

Ảnh home/menu/popup và Admin reports trong thư mục đã được cập nhật sau sửa tương phản; các trang vai trò còn lại là ảnh vòng 21 lượt ban đầu. Ảnh đều dùng dữ liệu demo hiện có, không phải giao dịch ngân hàng thật. Server kiểm thử do lượt này mở sẽ được dừng sau nghiệm thu.
