# Bàn giao ngữ cảnh cho agent tiếp theo

## Mục tiêu

Tiếp tục hoàn thiện tài liệu và cấu hình nền cho project website chuỗi trà sữa **UTETra** trong `D:\WEBCUOIKI`. Stack mục tiêu: Spring Boot, JSP/JSTL, SiteMesh Decorator, Bootstrap, JPA, MySQL, JWT, WebSocket và Cloudinary. Hãy làm tiếp trên workspace hiện tại, kiểm tra file trước khi sửa và không xóa tài liệu người dùng đã cung cấp.

## Quyết định đã thống nhất

- Kiến trúc là ứng dụng web 3 tầng: Presentation, Business/Service, Data/Repository. MVC dùng ở tầng Presentation: Controller → Service → Repository/JPA → MySQL.
- Khách chọn chi nhánh để mua hàng. **Không tính khoảng cách/định tuyến**. Bỏ riêng phần khoảng cách, giữ các chức năng khác; trọng tâm là bán hàng và thanh toán.
- Chia việc theo use case hoặc nhóm chức năng liên quan để mỗi thành viên làm trọn luồng trên một nhánh.
- Lưu prompt người dùng trong Markdown để nhóm dùng cho báo cáo AI.
- Database riêng là **`utetra_db`**. `project_idv` thuộc project khác, tuyệt đối không dùng hoặc sửa.
- Người dùng yêu cầu config lưu trực tiếp và Git-track trong repo private; không chuyển sang `.env`, không ignore cấu hình. Thông tin kết nối TiDB nằm trong `src/main/resources/application-mysql.properties`. Không lặp password trong tài liệu, log hay câu trả lời.
- Đọc tài liệu môn học và DOCX khảo sát do người dùng cung cấp. DOCX chỉ là tham khảo; không mô tả số liệu cũ như khảo sát thực tế hiện tại.

## Quy định đồ án đã ghi nhận

- Vai trò: Guest, User, Vendor/Seller, Manager, Admin; Shipper là phần mở rộng.
- Chức năng chung: tìm kiếm/lọc; đăng ký và kích hoạt OTP email; đăng nhập/đăng xuất; quên mật khẩu OTP; mã hóa mật khẩu và Spring Security.
- User: trang chủ/danh mục/chi tiết, phân trang hoặc lazy loading, hồ sơ và nhiều địa chỉ, giỏ hàng lưu database, COD và cổng trực tuyến (tài liệu chọn VNPAY sandbox), lịch sử/trạng thái đơn, yêu thích, đã xem, đánh giá, bình luận tối thiểu 50 ký tự kèm media, voucher.
- Vendor có quyền User và thêm quản lý shop/sản phẩm/đơn hàng/khuyến mãi/doanh thu.
- Manager/Admin quản lý user, sản phẩm, danh mục, chiết khấu nền tảng, khuyến mãi, nhà vận chuyển và phí theo phạm vi quyền.
- Shipper quản lý đơn được giao và thống kê.
- Dùng một Bootstrap template; cấu trúc 3 tầng; báo cáo Word gồm kiến trúc, sơ đồ hoạt động, giải thích source code/UI, hướng dẫn setup và prompt AI. Rubric được tổng hợp trong file quy định.

## Những gì đã được tạo/cập nhật

Tài liệu tổng quan gồm `PROJECT_PLAN.md`, `DATABASE_DESIGN.md`, `USE_CASES.md`, `docs/README.md`, workflow/setup, quy định, hướng dẫn ghép báo cáo, mẫu note use case/prompt, nhật ký AI và đối chiếu tài liệu môn học. Theo lần chạy script cập nhật gần nhất có 55 use case bắt buộc cộng 3 Shipper tùy chọn; thiết kế 43 bảng chính cộng 2 bảng Shipper. Rà lại nội dung thực tế trước khi kết luận hoàn thiện.

Tài liệu tham khảo/cấu hình môn học nằm trong `TAI_LIEU_CAU_HINH_PROJECT_CUOI_KI/`; DOCX khảo sát là `KhaoSat_PhanTichHeThong.docx`. Các bản tóm tắt ở `docs/SURVEY_SYSTEM_ANALYSIS.md`, `docs/COURSE_CONFIG_MAPPING.md`, `docs/QUY_DINH_VA_CAU_HINH_CHUNG.md`.

Baseline code được ghi nhận:

- Maven Wrapper, `pom.xml`, Java 21, Spring Boot 3.5.13, đóng gói WAR.
- Spring MVC/Web, JPA, Security, WebSocket, Mail, JSP/JSTL, SiteMesh, Flyway, Nimbus JWT, Cloudinary adapter, H2 cho test.
- Cấu hình ở `src/main/resources/application.properties` và `application-mysql.properties`; migration `src/main/resources/db/migration/mysql/V1__identity_baseline.sql` tạo identity/session và seed roles.
- Đã tạo các thành phần nền entity/repository, security/JWT, SiteMesh, WebSocket, Cloudinary và Bootstrap template.
- `.gitignore` đã chỉnh để không loại config theo yêu cầu, vẫn bỏ qua artifact/build/editor/log/temp.

## Trạng thái kiểm chứng cần xác nhận lại

Ghi chú trước cho biết kết nối TLS tới TiDB thành công, Flyway chạy V1, Hibernate/JPA validate và SiteMesh/WebSocket/Tomcat khởi động. Có 13 kiểm thử baseline từng chạy thành công. Một lượt `mvn verify` sau đó chạy tests nhưng đóng gói WAR vướng file bị tiến trình Java khóa. PID được ghi nhận là `12092`, chạy `target/utetra.war`; một lần dừng/build tiếp theo bị ngắt. **Không mặc định PID còn tồn tại; chỉ dừng nếu xác minh đúng command line.**

Ở lượt vừa qua, `exec_command` nhiều lần báo `helper_unknown_error: setup refresh had errors`, vì vậy build cuối và HTTP smoke check chưa được xác nhận. Thử lại shell ở `D:\WEBCUOIKI`, kiểm tra `git status`, wrapper và tiến trình WAR; sau đó chạy `clean verify`. Khi build đạt, kiểm tra `/`, health endpoint JSON, Bootstrap asset và endpoint admin/WebSocket cần xác thực. Cập nhật kết quả thật vào `docs/BASELINE_VERIFICATION.md` và `docs/BASELINE_AI_LOG.md`.

## Rà soát tài liệu còn lại

- Kiểm tra `docs/AI_PROMPT_HISTORY.md` ghi đủ prompt về: bỏ riêng tính khoảng cách; ghi prompt; chia nhánh theo use case; đọc quy định và tài liệu cấu hình thầy; dùng MySQL; tạo database mới; MVC; yêu cầu tiếp tục. Không ghi password.
- Rà `docs/REPORT_ASSEMBLY.md` xem đánh số có bị trùng không.
- Kiểm tra `BRANCH_CATALOG_SALES.md`, `BRANCH_IDENTITY_MEMBERSHIP.md`, `BRANCH_ORDER_PAYMENT.md` có khớp phân công mới không.
- Rà `docs/SETUP.md` xem lệnh PowerShell/Maven Wrapper có lỗi escape `\m` không.
- Đảm bảo `docs/BASELINE_VERIFICATION.md` nêu đúng trạng thái và không đánh đồng baseline với nghiệp vụ đã xong.

## Giới hạn

Đây mới là tài liệu và cấu hình nền; không được nói toàn bộ website bán hàng đã hoàn thành. OTP/email thật, nghiệp vụ catalog/đơn hàng/thanh toán và tích hợp nhà cung cấp cần kiểm tra theo code hiện tại. SMTP, Cloudinary và VNPAY sandbox có thể chưa được cấp/cấu hình. Không commit/push nếu người dùng chưa yêu cầu.

## Tóm tắt yêu cầu người dùng để đối chiếu prompt log

1. Chỉnh sửa ba file Markdown chuyên nghiệp theo đề tài UTETra và stack đã nêu.
2. Nhấn mạnh không xóa file; hỏi tiến độ.
3. Bỏ tính khoảng cách khi chọn chi nhánh nhưng giữ chức năng khác; tập trung bán hàng/thanh toán.
4. Soạn file Markdown theo nhánh/use case để thành viên ghi chú, tích hợp; `main` giữ cấu hình chung; hướng tới báo cáo `.docx`.
5. Ghi prompt vào Markdown; mỗi nhánh có hướng dẫn việc và cấu hình; bám các file kế hoạch/database/use case/readme đã nêu.
6. Gửi quy định vai trò, chức năng, rubric, báo cáo và dặn ghi nhớ.
7. Chỉ rõ `KhaoSat_PhanTichHeThong.docx` là tham khảo.
8. Nhắc đọc quy định và cấu hình project theo tài liệu thầy trong `TAI_LIEU_CAU_HINH_PROJECT_CUOI_KI`.
9. Cung cấp config TiDB/MySQL; yêu cầu bỏ database project khác và tạo DB mới `utetra_db`.
10. Yêu cầu file tổng hợp quy định/cấu hình; đọc kỹ tài liệu thầy; ghi đủ prompt.
11. Hỏi mô hình; được giải thích MVC ở tầng trình bày trong kiến trúc 3 tầng.
12. Yêu cầu tiếp tục, sau đó yêu cầu tạo file Markdown bàn giao cho agent mới. File này là bản bàn giao đó.


## Bổ sung từ phiên cloud — 14/10/2026

Người dùng xác nhận main là nền/cấu hình chung cho ba feature, yêu cầu kiểm toàn bộ tài liệu và bổ sung phạm vi UC trước đồng bộ. Danh mục main có 55 P0 + 3 Shipper P1; cả 58 hồ sơ có đặc tả đầu vào/tiền-hậu điều kiện/luồng/lỗi/quyền/data/route/test dự kiến và sơ đồ hoạt động. Chức năng thương mại vẫn chưa triển khai; hình/test/prompt/commit UC thực chưa được tạo thay bằng chứng.

Ba BRANCH_*.md hiện được quản lý trên main và thay hướng dẫn 28 UC cũ. Scope: catalog 18 P0, identity 16 P0, order 21 P0+3 P1. Remote GitHub có đủ các nhánh nhưng vẫn ở commit cũ tại thời điểm kiểm; đồng bộ cục bộ không tự đồng bộ GitHub. Không push hoặc force-push khi chưa được yêu cầu.

SiteMesh 3.2.2 default decorator prefix gây đường dẫn /WEB-INF/decorators//WEB-INF/views/layouts/...; sửa bằng setDecoratorPrefix("") vì mappings là servlet path đầy đủ. BaselineViewTest dùng Tomcat thật kiểm JSP home/login và API/assets không bị decorate. Xem BASELINE_VERIFICATION cho kết quả lượt mới.

Cloud dùng /workspace/Project_Web, JDK21 và Maven cache/settings ngoài checkout tại /workspace/onboarding-utetra và /workspace/.m2. Các lệnh Windows/PID trước đây là lịch sử, không dùng PID cũ để dừng process cloud. TiDB runtime cloud chưa kết nối do DNS/native egress; không đổi DB, V1 hoặc TLS. SMTP/Cloudinary/VNPAY chưa kiểm thật và còn disabled.
