# Ghép catalog-sales từ integration/utetra — 28/10/2026

## Yêu cầu

> merge phần catalog sales của nhánh utetra qua nhánh này

Đích: `feature/catalog-sales`, baseline `ce46ce21dce10e578cfeb74ba75474d03d173267`. Nguồn đã fetch: `origin/integration/utetra` tại `57c6b88`. Ghép chọn lọc theo module; không tạo merge commit hai cha đánh dấu cả Identity/Order/Support đã được ghép.

## Phạm vi đã ghép

- DTO/entity/repository/service catalog: stock và version theo size, mã món chung giữa các chi nhánh, gộp món trùng trước phân trang, dữ liệu thống kê từ owner modules khi có bảng và nạp các bảng xếp hạng theo lô.
- Tách đọc/ghi DB khỏi service sang `CatalogStatisticsRepository`, `StoreOperationsRepository`; bỏ lớp `CatalogStatistics` cũ đã được thay thế. Giữ `CatalogStoreAccessPolicy` của nhánh độc lập vì chưa có adapter Identity thay thế.
- Cập nhật Cloudinary adapter và test validation/cleanup. Không đổi credential, datasource, JWT, profile hoặc pom; không upload/xóa asset thật.
- UI Modern Matcha, danh mục, món nổi bật, bốn Top 20 theo tài khoản, chi nhánh, ưu đãi; form quản trị mới có tìm trong dữ liệu đã tải, chỉnh sửa/thêm mới, tồn kho, dấu select, upload và responsive.
- Đồng bộ bundle trong `SHARED_UI_FILES.txt`, font Be Vietnam Pro và các asset catalog; vẫn một SiteMesh layout/Bootstrap/theme.
- Hai phụ thuộc tối thiểu từ nguồn: `identity/StrictNumbers` (chỉ deserializer JSON) và `order/service/CheckoutAvailabilityEvents` (chỉ publisher sau commit). Không kéo các controller/service/entity tài khoản, giỏ, checkout, nhóm hoặc support.
- Đồng bộ tài liệu UC và chỉ mục source; DDL bổ sung thuộc catalog được ghi ở `proposals/catalog-sync-20261028.sql` để review, không đặt vào Flyway tự chạy khi nhánh chỉ có baseline V1. Không chạy DDL/data fixture trên database thật.

## Điều chỉnh để nhánh catalog độc lập hoạt động

- `CatalogController` không phụ thuộc `GroupOrderService`; giữ catalog/search/quote/store/registration/management thật.
- Chi tiết món tiếp tục gọi POST `/products/{id}/quote`. Nút và kết quả ghi rõ tính giá, không hiển thị thành công thêm giỏ/nhóm khi nhánh chưa có backend đó. Trang chủ dùng nhãn chọn món; không đưa CTA đặt nhóm vào nhánh này. Không kéo UI bình luận/đánh giá tương tác từ Identity.
- Không gọi endpoint giỏ/nhóm qua polling khi chưa có module commerce. `NavigationAdvice` nhận diện bean `cartService`; `app.jsp` xuất meta capability, ẩn lối vào giỏ khi thiếu module; `site-ui.js` kiểm capability trước polling nhóm. Đây là điều chỉnh trong đúng bundle chung, không tạo layout mới. Khi đưa các file này về main/integration phải đồng bộ cùng nhau.
- Menu standalone giữ lọc chi nhánh và bỏ giỏ tóm tắt chưa có backend. Quickview cùng origin được phép riêng `/products/{id}?quick=1`; các trang sản phẩm bình thường giữ `X-Frame-Options: DENY`.
- Sửa form quản trị để `catalogKey` thực sự được phép để trống, thống nhất contract DTO/service, tránh chặn cập nhật món cũ.

## Kiểm chứng thực tế

- Java 21, Maven `verify`: **55 test đạt**, 0 failure/error/skipped; WAR đóng gói thành công. Bao gồm render JSP/JSTL/SiteMesh trên Tomcat thật, scope/quyền/CSRF, quote/ranking, Cloudinary mock, batching và kiểm thêm stock/version, số JSON nghiêm ngặt, header quickview.
- `python tools/check_shared_ui.py`: một layout, 21 file chung. `git diff --check` đạt.
- Edge headless: **13 kiểm tra đạt**, không lỗi JavaScript, không tràn ngang ở 1440/768/390px. Gồm home Guest theo doanh số fixture, tìm món, quickview gọi quote server 80.000đ, form quản trị/tìm/chỉnh sửa/upload, lưu tồn kho 15 qua API thật trên H2 và trang khuyến mãi. Kiểm header desktop/mobile không mở giỏ khi thiếu backend. Các lỗi selector/điều kiện chờ trong script đã được chỉnh; chỉ lượt cuối đạt được tính là kết quả.
- Kết quả và ảnh trình duyệt: [assets/sync-20261028](assets/sync-20261028/verification.json). Fixture H2 riêng trên localhost; JWT chỉ phục vụ fixture, không đăng nhập giả trong ứng dụng và không lưu token vào biên bản.
- Chưa nghiệm thu login/OTP/giỏ/checkout/đơn nhóm hoặc provider thật trên nhánh độc lập; các chức năng đó vẫn cần tích hợp module sở hữu. Các tài liệu kiểm thử integration trong bundle là lịch sử tại nhánh nguồn, không chứng minh toàn bộ integration có trong catalog-sales.

Không thay đổi/push nhánh nguồn. Các file rubric, ảnh tham chiếu, báo cáo ngoài phạm vi, `.tools/` và cache Python được giữ ngoài commit đồng bộ.
