# UTETra — Một layout dùng chung cho toàn bộ nhóm

## Quyết định chung

Từ 22/10/2026, **chỉ có một layout SiteMesh**: `src/main/webapp/WEB-INF/views/layouts/app.jsp`. Không tạo client.jsp/auth.jsp/admin.jsp/vendor.jsp/manager.jsp/shipper.jsp riêng. Bộ chuẩn lấy từ giao diện đã ghép trên identify/order: xanh trà, nền kem, Bootstrap 5.3.3 local, header/footer và sidebar responsive chung. Đây là chuẩn trình bày; không thay đổi quyền hoặc phạm vi nghiệp vụ.

Các trang khách, tài khoản, đăng nhập và quản lý khác nhau ở nội dung và menu theo role; cùng một khung HTML, cùng CSS/JS/Bootstrap. Auth vẫn có panel/ảnh riêng bên trong trang, workspace có sidebar; không phải frontend hoặc template khác.

## File nào được sửa ở đâu?

| Nhu cầu | Nơi dùng chung |
| --- | --- |
| Header, footer, icon, sidebar, meta CSRF, thứ tự nạp tài nguyên | layouts/app.jsp |
| Màu, font, spacing, button, form, table, breakpoint | static/assets/css/utetra.css |
| Bootstrap | static/assets/vendor/bootstrap; giữ 5.3.3 |
| Logo/ảnh thương hiệu | static/assets/images/utetra-* |
| currentPath/currentRole/workspace | controller/NavigationAdvice.java |
| Mapping decorator | config/SiteMeshConfig.java: chỉ một /* → app.jsp |
| Hành vi giao diện chung, định dạng tiền/trạng thái, logout | static/assets/js/site-ui.js |
| Hàm CSRF/WebSocket URL dùng chung | static/assets/js/utetra.js |
| Trang và tương tác nghiệp vụ | WEB-INF/views/<module>/, assets/js/<module>.js |

Đường dẫn CSS/JS/ảnh bắt đầu từ src/main/resources/. Danh sách đầy đủ có thể đồng bộ bằng Git nằm trong [SHARED_UI_FILES.txt](SHARED_UI_FILES.txt). Không có properties chứa khóa, SQL hoặc service nghiệp vụ trong bundle này.

## Quy trình cho agent của từng thành viên

1. Đọc AGENTS.md, tài liệu này và tài liệu chức năng của nhánh.
2. Dùng cùng bundle chuẩn của main. Không chọn template Bootstrap khác hay sao chép layout từ project khác.
3. Tạo JSP nội dung trong thư mục module. SiteMesh lấy title/head/body rồi chèn vào app.jsp đúng một lần; không include layout bằng tay.
4. Không chép navbar/footer/sidebar, meta CSRF hoặc Bootstrap vào trang nội dung. Menu phụ của chức năng được phép nằm trong body.
5. CSS riêng nếu cần đặt trong `<module>.css`, scope dưới class gốc như .order-page/.catalog-page/.identity-page. Không override body, :root, .navbar, .btn hoặc .container theo ý riêng.
6. Nạp CSS riêng trong head; nạp JS chức năng đúng một lần trong trang. Giao diện dùng c:url, c:out, meta app-context và helper CSRF chung; không hardcode localhost hoặc bỏ CSRF.
7. Khi cần thay đổi theme/navigation, chỉnh nguồn chung trên main, ghi lý do và đồng bộ bundle. Các bản khác nhau không được giữ song song thành layout thứ hai.

Ví dụ JSP chức năng:

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><title>Tên chức năng · UTETra</title></head><body>
<section class="order-page">
  <h1 class="h3">Tên chức năng</h1>
  <div class="card card-body">Nội dung được escape bằng c:out.</div>
</section>
<script src="<c:url value='/assets/js/orders.js'/>"></script>
</body></html>
```

## Cấu hình, màu và responsive

- SiteMesh filter đăng ký một lần; `setDecoratorPrefix("")`; một mapping /* tới app.jsp. Giữ exclusions /api/*, /assets/*, /ws, /payments/*, /error; JSON/static không bị bọc HTML.
- NavigationAdvice cung cấp currentRole từ principal hiện hành, currentPath và workspace cho URL quản lý. Thêm role/menu trong logic chung, không thêm decorator. Security/Service vẫn quyết định quyền thật.
- Theme tokens hiện có: --tea #315638, --cream #f5f2e8, --ink #263b2c, --orange #a5773d; font Be Vietnam Pro local, fallback Segoe UI/Arial. Dùng var(...) thay vì đặt màu/font mới theo từng module.
- Dùng breakpoint Bootstrap và quy tắc trong utetra.css; kiểm 390px, 768px, desktop. Table có table-responsive, form có label, trạng thái/loading/lỗi rõ ràng. Không tải Bootstrap/jQuery/font/icon từ CDN riêng.
- Chỉ thay file giao diện và config SiteMesh/NavigationAdvice cho nhu cầu này; không sửa datasource/JWT/Cloudinary/mail/pom để làm trang đẹp hơn.

## Đồng bộ giữa các nhánh

Nguồn chuẩn là **main sau khi commit Shared UI được công bố**. Nếu nhánh đã có bundle chuẩn, chỉ cần pull nhánh của mình. Nếu phát sinh lệch, lấy lại đúng bundle; không merge toàn bộ order chỉ để lấy header/footer.

```powershell
git fetch origin
# Sau khi origin/main đã có Shared UI v1; trước đó dùng main local đã cập nhật.
$uiFiles = Get-Content docs/SHARED_UI_FILES.txt | Where-Object { $_ -and -not $_.StartsWith('#') }
git restore --source=origin/main -- $uiFiles src/main/webapp/WEB-INF/views/layouts
py -3 tools/check_shared_ui.py --compare origin/main
git diff --stat
```

Đối chiếu thay đổi chức năng trước khi commit. Việc restore cả thư mục layouts cũng loại layout role cũ đang tracked. Không dùng git reset --hard/force push để đồng bộ giao diện.

Kiểm ngay trên nhánh hiện tại: `py -3 tools/check_shared_ui.py`. So với main local: `py -3 tools/check_shared_ui.py --compare main`. Mọi file trong manifest phải giống nguồn chuẩn; CSS/JS chức năng ngoài manifest vẫn thuộc module. Sau sửa JSP/layout, chạy kiểm render thật Tomcat/SiteMesh; MockMvc đơn thuần không chứng minh JSP đã compile.

## Triển khai và chứng cứ

Bundle này được tách từ main, không chứa code order/catalog/identity nghiệp vụ. Chỉ có một app.jsp; sáu file layout cũ là bản sao được thay thế bằng nguồn duy nhất, các trang chức năng vẫn giữ nguyên. Main đã qua clean verify với Java 21: 16/16 tests đạt, gồm render JSP/Tomcat/SiteMesh và kiểm JSON/static không bị decorate; WAR build thành công. Công cụ check_shared_ui xác nhận một layout và đủ 16 file chung. Việc này chưa đồng nghĩa các remote đã nhận thay đổi; phải xác nhận trạng thái push trước khi yêu cầu thành viên pull.

## Giao diện theo bản thiết kế — 26/10/2026

Thanh yêu cầu áp dụng bản thiết kế kem/xanh lá trên source hiện có. Thay đổi được thực hiện local trên identify, chưa đồng bộ/push main: vẫn một app.jsp, một Bootstrap và một utetra.css. Cập nhật token xanh lá/kem, header, nút, hero ảnh matcha, thẻ danh mục có ảnh, card món, bộ lọc trái/giỏ tóm tắt phải ở desktop, bố cục mobile, sidebar quản lý và bảng trạng thái đơn. Báo cáo dùng KPI/biểu đồ từ số liệu service hiện có, không thêm số liệu tăng trưởng giả. Trang chi tiết vẫn giữ URL và luồng tùy chọn hiện hành. Thumbnail ba ly có logo lá và 30 ảnh món mới nằm trong assets/images; manifest/prompt ở docs/demo.

Bundle thay đổi chung tập trung ở utetra.css và app.jsp; nội dung catalog/order giữ trong module. Chủ nhóm có thể review rồi tích hợp bộ chung sang main/các feature theo workflow cũ. Không tạo theme/layout theo role hoặc framework mới.


## Tinh chỉnh theo ảnh tham chiếu — 26/10/2026

Yêu cầu mới: `[Image #1] bạn thiết kế giao diện theo màu và phông như này được không vẫn giữ đầy đủ chức năng`. Ảnh tham chiếu do người dùng cung cấp: `2aOboR8CXHLLqRujaayM4hUEoJJikhocP0fkZwvY.jpg`. Không đưa toàn bộ poster thành nền website.

Bản thử nghiệm nằm trên integration/utetra: màu kem ấm, xanh trà và nhấn nâu vàng; Be Vietnam Pro 400/500/600/700 hỗ trợ tiếng Việt, logo/nhấn biên tập dùng Georgia. Font được chọn để gần tinh thần ảnh, không khẳng định nhận diện chính xác font trong poster. Font và giấy phép SIL OFL được đóng gói local trong assets/vendor/fonts/be-vietnam-pro, không phụ thuộc CDN lúc chạy. Nguồn: https://github.com/google/fonts/tree/main/ofl/bevietnampro.

Thay đổi chung: utetra.css (token/typography/nút/focus/card/sidebar/mobile), app.jsp (preload font, theme-color, cache version) và manifest thêm 5 file font/license. Không thêm decorator/layout/framework. Không đổi URL/action/name/id nghiệp vụ, provider, datasource, JWT hoặc quyền. Các KPI và trạng thái vẫn lấy từ service hiện có. Không ẩn chức năng để giống poster.

Khi phát hành bundle này, đồng bộ cả font/license trong SHARED_UI_FILES.txt. Main và các feature chưa được cập nhật tự động. Kết quả kiểm thử hiện tại xem UI_REFERENCE_REFINEMENT_20261026.md; các biên bản cũ là lịch sử.


## Tích hợp toàn bộ module — 26/10/2026

Bản tích hợp dùng chung cả Guest/User, tài khoản/OTP, Vendor, Manager, Admin và Shipper. Sidebar bổ sung khu vực giao, topping tại shop, danh mục topping, phân công/quyền danh mục (Admin), thông báo, hồ sơ và bảo mật. Dropdown người mua có địa chỉ, đã xem, đánh giá và bình luận. Footer chỉ dẫn sang chức năng phù hợp vai trò; không dùng menu để thay quyền backend.

Sidebar desktop cuộn độc lập khi dài; mobile dùng menu xuống dòng. Bộ chọn shop của catalog wrap trên màn hình nhỏ. Shipper report dùng cùng KPI/card/font/màu và bộ lọc ngày responsive. Nội dung catalog vẫn ở catalog/manage.jsp, shipper ở order/shipper-report.jsp, cùng được app.jsp decorate.

Kiểm đầy đủ và đường dẫn ảnh: ALL_MODULE_UI_20261026.md. Công cụ `py -3 tools/check_all_ui.py` dựng H2 fixture riêng, bật Shipper riêng fixture và tự dừng server sau kiểm. Phải package/test trước để có classpath và JDK 21; không chạy fixture trên datasource cloud. Không thay features.shipper.enabled của cấu hình dùng chung chỉ để lấy ảnh.
