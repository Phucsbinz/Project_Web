# Thiết kế lại UI UTETra — IDN-AI-005

Ngày: 20/10/2026. Nhánh: `feature/identity-membership`. Yêu cầu: đơn giản hóa mật khẩu bộ tài khoản khởi tạo và nâng cấp toàn bộ UI website trà sữa, giữ các nghiệp vụ/ràng buộc đồ án.

## Thiết kế và phạm vi

Hệ thiết kế Bootstrap chung dùng xanh trà đậm, kem ấm, xanh lá nhạt và điểm nhấn cam; typography rõ cấp độ, ảnh đồ uống chủ đạo, biểu tượng SVG nội bộ, nút/field/card/table thống nhất. Không thêm template thứ hai hoặc thư viện UI ngoài. Menu và thao tác mobile dùng Bootstrap có sẵn.

| Khu vực | Thay đổi |
| --- | --- |
| Layout chung | Thanh thương hiệu, header sticky, menu có trạng thái chọn, giỏ hàng/tài khoản, dropdown theo vai trò, logout có CSRF, footer thương hiệu với link thật |
| Trang chủ | Hero ảnh riêng, CTA đến thực đơn/cửa hàng, ba bước chọn món/tùy biến/tích điểm, khối thương hiệu; số liệu sản phẩm vẫn từ DB |
| Thực đơn | Danh mục chip, tìm tên/danh mục/cửa hàng/giá/sort, phân trang, card giá VND và số liệu thật, empty state không giả sản phẩm |
| Sản phẩm | Gallery/tùy chọn hai cột; size, đường, đá, topping, số lượng, quote và add-to-cart giữ nguyên API; khu review/comment/favorite từ identity |
| Cửa hàng | Card trạng thái/giờ/địa chỉ theo dữ liệu thật và trạng thái rỗng; chọn shop thủ công, không định tuyến khoảng cách |
| Đăng nhập/tài khoản | Login có ảnh và form hai cột, hiện/ẩn mật khẩu, focus/loading/error; giữ OTP/register/reset; hồ sơ/member/loyalty/user cards; điều hướng tài khoản có tab hiện tại |
| Giỏ hàng/đặt hàng | Empty state có CTA đặt món, bảng responsive, checkout hai cột trên desktop/một cột mobile; giữ quote trước xác nhận COD |
| Đơn hàng | Navigation/pagination/table/timeline/actions chung, status tiếng Việt chỉ tại lớp hiển thị và tiền VND; trạng thái/giá trị gửi API không đổi |
| Quản trị/Vendor/Manager | Sidebar theo vai trò, navigation responsive, bảng/form/card dùng cùng palette, section/tab khớp URL; giữ scope/ownership từ backend |
| Shipper tùy chọn | Sidebar chỉ hiển thị đơn giao và thống kê, không gán nhầm đường dẫn catalog; tính năng vẫn theo flag sẵn có |

Không thêm doanh số, đánh giá, sản phẩm hay biểu đồ giả để làm đầy UI. Cửa hàng/sản phẩm/đơn hàng thực hiện chưa được nhập nên các trạng thái rỗng vẫn xuất hiện đúng dữ liệu. SMTP/VNPAY/provider acceptance và SQL CHECK của cụm TiDB giữ giới hạn trong [báo cáo tích hợp](INTEGRATION_20261020.md).

Tính năng truy cập: label và autocomplete, aria-live cho thông báo, toggle có aria-pressed, focus rõ, link bỏ qua đến nội dung, tôn trọng prefers-reduced-motion; các field và validation hiện có được giữ nguyên. Mật khẩu không được đưa vào giao diện sẵn, form tự động điền hoặc tài liệu Git.

## Ảnh thương hiệu

Áp dụng skill `imagegen` tại `C:/Users/luxip/.codex/skills/.system/imagegen/SKILL.md`, dùng built-in `image_gen` (không CLI). Ảnh là tư liệu thương hiệu tạo bằng AI, không phải ảnh chứng minh sản phẩm đã có trong catalog.

Artifact: `src/main/resources/static/assets/images/utetra-tea-ritual.png`; bản WebP tối ưu dùng trên trang: `src/main/resources/static/assets/images/utetra-tea-ritual.webp`. Icon/fav icon SVG được viết trực tiếp theo hệ icon nội bộ.

Prompt cuối:

> Create a premium photorealistic commercial beverage photograph for the Vietnamese milk tea brand UTETra website. Landscape 3:2 image. Three elegant transparent takeaway cups with clear flat lids: front hero cup creamy caramel brown sugar milk tea with glossy black tapioca pearls and natural brown sugar marbling; second cup pale matcha milk tea; third amber fruit tea with citrus. Realistic condensation and ice. Small subtle deep forest green circular brand marks without any text or lettering. Arranged on warm ivory travertine podiums, deep forest green studio background, a few tea leaves, warm angled morning sunlight, rich tactile surfaces, refined magazine food photography, high end Asian tea boutique campaign. Strong sculptural composition centered, cups fully visible, generous breathing room. No human, no text, no typography, no price badges, no watermark. This is brand mood imagery, not a product listing or a UI mockup.

## Mật khẩu và kiểm chứng

Đã thay mật khẩu đúng 8 tài khoản khởi tạo theo yêu cầu mới: cập nhật BCrypt cost 12, tăng version, thu hồi phiên cũ và ghi audit trong cùng transaction. Không thay email/role/người dùng khác. Mật khẩu dễ nhớ hiện hành chỉ ở `.local/TAI_KHOAN_UTETRA.md`; không copy vào Git. Chính sách đăng ký tối thiểu 8 ký tự và kiểm tra UTF-8/BCrypt không bị hạ bỏ.

Kiểm chứng cuối cùng được ghi tại `assets/redesign-20261020/http-verification.json`, `assets/redesign-20261020/visual-verification.json`, các ảnh desktop/mobile cùng thư mục và `UI_REDESIGN_AUDIT_20261020.json`. Script thao tác tài khoản dùng credential trong `.local`, không ghi chúng vào báo cáo. Kiểm tra gồm JSP thật, đăng nhập thật với TiDB, hồ sơ/role/logout, ảnh, JavaScript, tràn ngang và trạng thái rỗng.

Môi trường Windows gặp native-memory/JVM self-attach errors khi giữ nhiều Spring contexts. Launcher test nạp Mockito premain từ dependency đã cài, giới hạn heap/thread và chạy test theo nhóm JVM riêng; **đã qua đủ 110 test và package WAR thành công**, không tắt assertions, Mockito hoặc nghiệp vụ test. Cả 7 file JavaScript đã qua Node syntax check.

Sau kiểm tra trực quan, đã sửa thêm màn sản phẩm/khu vực/phân công/topping: nếu chưa có shop thì hướng dẫn chọn/đăng ký shop và không dựng form hay gọi API thiếu storeId. Chưa có dữ liệu trong phạm vi được hiển thị như trạng thái rỗng, không đánh đồng với phiên đăng nhập lỗi. Tab quản lý theo URL; nút lưu login chọn đúng submit thay vì nút hiện mật khẩu.

Edge còn gặp lỗi Windows paging file khi mở cùng JVM web. Phương án QA ít bộ nhớ: xác nhận route/login/profile/role/logout bằng HTTP trên web TiDB trước, lưu HTML/JSON phản hồi thật trong `.local`, dừng JVM tạm thời, rồi kiểm tra trực quan trên bản phản hồi read-only. Bản snapshot không nằm trong WAR, không seed dữ liệu, không giả API thành công và không cho POST. Báo cáo phân biệt kiểm tra live HTTP với kiểm tra render snapshot; WebSocket/backend transaction được kiểm chứng qua JUnit, không coi bản snapshot là nghiệm thu các luồng realtime.


## Kết quả cuối

110/110 JUnit test thành công; 7/7 JavaScript syntax check; 65 kiểm tra HTTP live với TiDB. Tất cả 8 tài khoản đăng nhập/đọc hồ sơ/đăng xuất thành công bằng mật khẩu dễ nhớ mới; các vai trò có scope đúng.

101 kiểm tra trực quan/client interaction thành công trên HTML/JSON phản hồi thật được lưu read-only, gồm desktop 1440 và mobile 390; không có lỗi JavaScript hoặc tràn ngang/ảnh hỏng. Xem [visual-verification.json](assets/redesign-20261020/visual-verification.json). Đây là kiểm chứng render; đăng nhập/role/logout được kiểm chứng live qua [http-verification.json](assets/redesign-20261020/http-verification.json).

Bản chạy cuối: WAR mysql tại http://localhost:8080. Không push.
