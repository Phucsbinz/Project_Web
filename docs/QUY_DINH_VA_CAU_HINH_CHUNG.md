# UTETra — Tổng hợp quy định của giảng viên và cấu hình chung

> Cập nhật ngày 13/10/2026. Cả nhóm và AI của từng thành viên đọc file này trước khi làm việc. Đây là bản tổng hợp yêu cầu đã được người dùng cung cấp, bổ sung lựa chọn riêng của nhóm; không phải xác nhận mọi chức năng đã hoàn thành.

## 1. Nguồn và thứ tự ưu tiên

1. Quy định cuối kỳ người dùng gửi trong cuộc trò chuyện: phạm vi vai trò/chức năng, phân công, Bootstrap, báo cáo và thang điểm.
2. Quyết định riêng đã chốt: đề tài UTETra; khách tự chọn chi nhánh, bỏ tính khoảng cách; dùng MySQL/TiDB Cloud; database riêng `utetra_db`; cấu hình trực tiếp trong application properties, không dùng .env hoặc ignore file cấu hình.
3. [Hướng dẫn cấu hình tổng hợp](../TAI_LIEU_CAU_HINH_PROJECT_CUOI_KI/HUONG_DAN_CAU_HINH_TONG_HOP.md) trong bộ tài liệu thầy: cách ghép Boot, JSP/JSTL, SiteMesh, Bootstrap, JPA, MySQL, JWT, WebSocket, Cloudinary.
4. [Mẫu khảo sát và phân tích](../KhaoSat_PhanTichHeThong.docx): tham khảo bố cục, bảng use case, sequence, ERD và từ điển dữ liệu. Không sao chép kết luận khảo sát cũ hoặc mô hình MongoDB vào project JPA/MySQL.
5. PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md và docs/README.md phải được cập nhật đồng bộ theo các yêu cầu trên. Nếu bản cũ còn dùng Customer/Staff/Admin, COD-only hoặc OTP bằng link reset, không tiếp tục triển khai theo giới hạn đó.

## 2. Đề tài và công nghệ

**Xây dựng website chuỗi bán trà sữa UTETra bằng Spring Boot + JSP/JSTL + Decorator SiteMesh + Bootstrap + JPA + MySQL + JWT + WebSocket + Cloudinary.** SQL Server/PostgreSQL trong mô tả ban đầu là các lựa chọn CSDL; nhóm đã chọn MySQL/TiDB, không bắt buộc dựng ba hệ quản trị cho bản này.

Mô hình ba tầng: Controller/View nhận request và hiển thị → Service xử lý nghiệp vụ/validation/quyền/transaction → Repository/JPA truy cập dữ liệu. JSP không có SQL hoặc logic thanh toán. DTO không trả mật khẩu/hash/token/quan hệ Entity vòng lặp.

## 3. Các vai trò bắt buộc và phần thêm

| Vai trò | Phạm vi |
| --- | --- |
| Guest | Chưa đăng nhập, xem/tìm/lọc và đăng ký |
| User | Khách mua trà sữa và các chức năng mua hàng/tương tác |
| Vendor/Seller | Có quyền User, thêm đăng ký và vận hành shop của mình |
| Manager | Quản lý cửa hàng/nhóm shop được phân công, theo phạm vi dữ liệu |
| Admin | Quản lý toàn hệ thống |
| Shipper | Phần thêm: chỉ đơn được phân công giao và thống kê giao hàng |

Trong UTETra, shop là cửa hàng/chi nhánh trong chuỗi. Vendor sở hữu/quản lý shop đã được duyệt; Manager quản lý chi nhánh được phân công. Mỗi request phải kiểm tra quyền dữ liệu ở Service; ẩn nút giao diện không đủ. Vendor phải mua hàng được như User, không yêu cầu tạo tài khoản khách riêng.

## 4. Chức năng chung

- Tìm kiếm và lọc sản phẩm.
- Đăng ký tài khoản, gửi OTP kích hoạt qua email, xác minh OTP trước khi được đăng nhập/mua hàng.
- Đăng nhập, đăng xuất, quên mật khẩu gửi OTP qua email; xác minh OTP để đặt lại mật khẩu.
- Mật khẩu băm bằng BCrypt; Spring Boot sử dụng Spring Security.
- OTP có thời hạn, giới hạn thử/gửi lại và dùng một lần; không lưu OTP rõ hoặc trả OTP qua API.

## 5. Chức năng Guest và User

**Guest:** Trang chủ hiển thị sản phẩm bán trên 10 sản phẩm của các shop, sắp xếp từ lớn đến nhỏ theo yêu cầu. Cụm từ này còn mơ hồ: tạm diễn giải là sản phẩm có số lượng đã bán >10, xếp theo số lượng bán giảm dần; nhóm cần xác nhận với giảng viên. Seed phải có món đạt ngưỡng, không hiển thị trang trắng do thiếu dữ liệu.

**User:**

- Trang chủ, danh sách theo danh mục, các nhóm top 20 sản phẩm mới/bán chạy/đánh giá/yêu thích; phân trang hoặc lazy loading.
- Hồ sơ và nhiều địa chỉ nhận hàng; chi tiết sản phẩm; tùy biến size/đường/đá/topping theo đề tài trà sữa.
- Giỏ hàng lưu database khi đăng nhập; giữ chức năng giỏ tạm/hợp nhất đã thiết kế nếu có.
- Chọn chi nhánh thủ công, địa chỉ/khu vực giao, đơn vị vận chuyển và mã giảm giá; không tính khoảng cách.
- Thanh toán COD và một lựa chọn trực tuyến VNPAY hoặc MOMO. Nhóm chọn **VNPAY sandbox** làm phương án tích hợp ban đầu; MOMO là phương án thay thế, không bắt buộc tích hợp cả hai.
- Lịch sử theo trạng thái: mới, xác nhận, đang giao, đã giao, hủy, trả hàng–hoàn tiền. Vendor có thêm bước đã lấy hàng.
- Thích sản phẩm, sản phẩm đã xem, đánh giá sản phẩm đã mua.
- Bình luận sản phẩm đã mua, text tối thiểu 50 ký tự; hỗ trợ hình ảnh/video và kiểm tra định dạng/quyền sở hữu.
- Chọn mã giảm giá; điểm/hạng và thông báo realtime đã có trong đặc tả tiếp tục được giữ.

## 6. Chức năng Vendor

Vendor có tất cả quyền User và thêm: đăng ký shop; quản lý trang chủ shop; quản lý sản phẩm của mình; quản lý đơn shop theo mới/xác nhận/đã lấy/đang giao/đã giao/hủy/trả–hoàn; tạo chương trình khuyến mãi; quản lý doanh thu shop. Không sửa sản phẩm/đơn/shop khác bằng cách đổi ID.

## 7. Chức năng Manager và Admin

Cả hai có tìm kiếm/quản lý user, sản phẩm theo shop, danh mục, chiết khấu app cho shop, chương trình giảm % sản phẩm/giảm phí vận chuyển, nhà vận chuyển với tên/phí. Admin toàn hệ thống; Manager theo cửa hàng được giao. Khóa tài khoản, đổi vai trò/phân công và thao tác nhạy cảm có audit, thu hồi phiên/quyền tương ứng.

Chiết khấu app (hoa hồng thu từ shop) tách khỏi giảm giá khách hàng; lưu snapshot tỷ lệ và giá trị tại đơn. Doanh thu phải đối chiếu được thanh toán, phí vận chuyển, chiết khấu và khoản hoàn.

**Shipper — phần thêm:** xem đơn được giao, cập nhật bước giao trong quyền cho phép, thống kê đơn được phân công. Không tự chọn đọc mọi đơn và không tự xử lý hoàn tiền. Việc quản lý nhà vận chuyển/phí thuộc phần bắt buộc, dù module Shipper chưa triển khai.

## 8. Một template Bootstrap dùng chung

Chọn **Start Bootstrap Shop Homepage** làm nền giao diện chung: [repository nguồn](https://github.com/StartBootstrap/startbootstrap-shop-homepage). Ghi tag/commit và giữ LICENSE khi đưa vào project. Các layout khách/Vendor/Manager/Admin/auth/Shipper dùng cùng CSS, component, font, màu, Bootstrap; bổ sung bảng/sidebar từ component cùng bộ, không ghép nhiều theme khác nhau.

Responsive tối thiểu kiểm tra mobile/tablet/desktop; có loading/empty/error, form label, validation, thao tác dễ dùng. Bootstrap CSS/JS cùng phiên bản, không trộn class Bootstrap 3 và 5.

## 9. Bộ cấu hình kỹ thuật đã chọn

| Công cụ | Cấu hình |
| --- | --- |
| Java/Boot | Java 21, Spring Boot 3.5.13 theo mẫu tích hợp đã cung cấp; khóa trong pom |
| Build/View | Maven Wrapper; WAR; Tomcat/Jasper; Jakarta JSTL API + implementation; JSP dưới WEB-INF |
| SiteMesh | 3.2.2, đăng ký filter một lần; loại trừ API/static/WebSocket; không dùng decorator để phân quyền |
| Bootstrap | 5.3.3, một template chung, assets cùng phiên bản |
| JPA | Spring Data JPA; Service @Transactional; open-in-view=false; DTO/BigDecimal |
| MySQL | TiDB Cloud cổng 4000; database mới utetra_db; JDBC TLS VERIFY_IDENTITY; UTC và utf8mb4 |
| Migration | Flyway quản lý thay đổi có version; ddl-auto=validate; không create-drop trên cloud hoặc database khác |
| Security/JWT | Spring Security 6 theo BOM; Nimbus JOSE JWT 10.10 theo hướng dẫn; BCrypt; JWT kiểm chữ ký/hạn/issuer/audience/phiên/quyền hiện tại |
| JWT trên JSP | Cookie HttpOnly và CSRF theo thiết kế chung; nếu điều chỉnh từ mẫu session của thầy phải ghi rõ và kiểm thử |
| WebSocket | Spring STOMP native /ws; destination theo user/shop/quyền; gửi sau commit; reconnect đọc lại DB |
| Cloudinary | cloudinary-http5 2.4.0; upload server; lưu secure_url/public_id; dọn ảnh đúng sau commit/rollback |
| Email/OTP | Spring Mail; tài khoản SMTP thật cần nhóm cung cấp; giới hạn gửi/nhập và token kích hoạt một lần |
| Payment | COD + VNPAY sandbox, kiểm chữ ký/IPN/số tiền/mã đơn/chống callback lặp; ReturnUrl không tự xác nhận PAID |

Các thông số chưa có (SMTP, Cloudinary, VNPAY merchant) phải ghi chưa cấu hình và không giả vờ đã tích hợp thành công. Khi có mã nguồn, cập nhật đường dẫn class/config/test và commit thật ở SETUP.

## 10. Quyết định về file cấu hình

Theo yêu cầu của chủ repository private, cấu hình nằm trực tiếp tại `src/main/resources/application*.properties` và được Git quản lý; không import .env, không ignore application-local hoặc các file cấu hình. Credential thực chỉ nằm tại file cấu hình cần thiết; bản tổng hợp và prompt log dùng bản che mật khẩu để tránh tạo bản sao không cần thiết. Không hiển thị credentials ra giao diện hoặc log.

Database project khác `project_idv` không được sửa/seed/migrate. Mọi thao tác ứng dụng phải hướng vào `utetra_db`. Không đổi database theo giả định hoặc dùng ddl-auto=update/create để giải quyết lỗi schema.

## 11. Phân công và commit

Mỗi thành viên nhận nhóm use case/role liên quan và làm Entity/Repository → Service → Controller → JSP → test → báo cáo. Main giữ nền/cấu hình/contract chung; feature ghép qua integration. Không chia một người làm Entity rồi người khác chỉ Controller của cùng UC.

Bảng giao việc phải có UC, role, người, nhánh, commit, ngày, trạng thái, reviewer và minh chứng. Commit thường xuyên theo chức năng có thể review; thực hiện GitHub remote/PR thật trước nộp. Nhánh local chưa push không đủ minh chứng quá trình GitHub theo tiêu chí thầy.

## 12. Khảo sát và phân tích

Từ mẫu DOCX: khảo sát từng hệ thống → ảnh và ngày khảo sát → ưu/nhược → bài học cho UTETra; phân tích chức năng theo actor; bảng UC ID/name/goal/actors/preconditions/postconditions/main/alternative/exception; use case diagram; sequence/activity; ERD; từ điển dữ liệu có PK/FK/unique/nullable/default.

Nhóm tự khảo sát thực tế và lưu minh chứng, không coi các nhận xét Tiki/Shopee/Lazada/Etsy trong mẫu là sự thật hiện tại. Không sao chép ví điện tử nội bộ/MongoDB/số lượng UC trong mẫu thành chức năng bắt buộc nếu quy định không yêu cầu.

## 13. Báo cáo với AI

Báo cáo cuối kỳ là DOCX. Mỗi người viết phần role/use case được giao, gồm hình kiến trúc, hoạt động, diễn giải source code, diễn giải UI, setup, prompt AI đã dùng sinh code/hình/sửa lỗi. Markdown là nguồn ghi chép để tổng hợp; hình thật, nguồn sơ đồ, code path/symbol/commit và kết quả test phải tra lại được.

Giữ nguyên văn prompt, response hoặc link file response, cách nhóm sửa và kiểm chứng. Prompt gợi ý chưa gửi không tính đã dùng. Mockup/sơ đồ dự kiến không thay screenshot ứng dụng chạy. Phần chưa làm/không có credential/test chưa chạy ghi rõ.

## 14. Thang điểm và minh chứng

| Tiêu chí | Điểm /10 | Tương đương /100 | Minh chứng |
| --- | ---: | ---: | --- |
| Quản lý/phân công/quá trình commit GitHub | 1 | 10 | Bảng giao việc, lịch sử commit/PR của từng người |
| Project đúng ba tầng | 0,5 | 5 | Package/lớp, luồng Controller–Service–Repository |
| Logic, validation, security và đầy đủ chức năng từng role | 6 | 60 | Ma trận UC/role, test quyền/input/transaction/payment |
| UI đẹp, dễ dùng, responsive | 1 | 10 | Một template, ảnh mobile/tablet/desktop, validation |
| Sáng tạo: ví điện tử/JWT/voucher/WebSocket... | 1 | 10 | Demo và giải thích luồng thật; không hứa điểm tự động |
| Báo cáo và trả lời câu hỏi | 0,5 | 5 | DOCX theo phân công, source/hình/prompt, diễn tập |
| **Tổng** | **10** | **100** | |

Ưu tiên phần logic/validation/security 60%, không bỏ chức năng bắt buộc để làm thêm hiệu ứng. Hạn nộp và link nộp chưa có trong tin nhắn; nhóm điền theo LMS khi giảng viên công bố, không tự bịa ngày.

## 15. Checklist đọc và ghi nhớ

- [ ] Mỗi người đã đọc quy định và file này; điền tên/role/nhánh/UC được giao.
- [ ] Đúng các chức năng bắt buộc của Guest/User/Vendor/Manager/Admin, Shipper ghi phần thêm.
- [ ] OTP email kích hoạt và reset; Vendor có quyền User; thanh toán trực tuyến và trả–hoàn không để ở mục ngoài phạm vi.
- [ ] Database riêng utetra_db, TLS; không tác động project khác.
- [ ] Cùng phiên bản/pom/layout/template/migration/contract; không tự đổi cấu hình.
- [ ] Có khảo sát thực, diagram/code/UI/setup/prompt và commit/test thật.
- [ ] Cập nhật tất cả đặc tả/hồ sơ nhánh khi thay đổi yêu cầu; không xóa lịch sử prompt.
