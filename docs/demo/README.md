# Dữ liệu demo UTETra — 26/10/2026

Bộ dữ liệu phục vụ kiểm thử đồ án, không phải hoạt động kinh doanh hoặc phản hồi khách hàng thật. Tên tài khoản là tên tiếng Việt giả lập; Phan Tuấn Thanh do người dùng xác nhận. Seed không sửa tài khoản đang có; lệnh reset riêng bên dưới thay mật khẩu tất cả tài khoản theo yêu cầu Thanh. Địa chỉ chi nhánh là khu vực mô phỏng tại TP.HCM, không khẳng định UTETra có cửa hàng thật ở đó. Ảnh menu được tạo bằng imagegen và mang logo lá UTETra.

## Tài khoản demo

Mật khẩu hiện tại cho **toàn bộ tài khoản có sẵn trong database kiểm thử**, gồm cả tài khoản cũ và demo: `123456` (reset theo yêu cầu Thanh ngày 26/10/2026).

| Vai trò | Tên hiển thị | Email đăng nhập |
| --- | --- | --- |
| Admin | Nguyễn Đức Minh | demo.admin@example.test |
| Manager | Trần Hoài An | demo.manager@example.test |
| Vendor | Lê Phương Thảo | demo.vendor@example.test |
| Shipper | Nguyễn Quốc Bảo | demo.shipper@example.test |
| User | Phan Tuấn Thanh | demo.thanh@example.test |
| User | Nguyễn Minh Anh | demo.minhanh@example.test |
| User | Trần Ngọc Linh | demo.ngoclinh@example.test |
| User | Lê Hoàng Nam | demo.hoangnam@example.test |
| User | Phạm Thu Hà | demo.thuha@example.test |
| User | Võ Gia Huy | demo.giahuy@example.test |
| User | Đặng Khánh Vy | demo.khanhvy@example.test |
| User | Bùi Quang Hưng | demo.quanghung@example.test |

Các email `.test` đã được xác minh trong fixture để đăng nhập demo, không nhận email thật. Kiểm OTP/mail bằng tài khoản và hộp thư thật của bạn. Shipper chỉ hoạt động khi feature tương ứng được bật; fixture không thay cấu hình feature. Admin/Manager/Vendor dùng quyền ứng dụng hiện có; Manager được phân công cả ba chi nhánh demo. Tài khoản cũ của Thanh cũng nằm trong lần reset toàn bộ theo yêu cầu mới. Trạng thái active/xác minh và vai trò không thay đổi.

## Phạm vi

- Ba chi nhánh demo: Thủ Đức, Bình Thạnh, Quận 1. Cùng 30 món mỗi chi nhánh, tức 90 bản ghi sản phẩm theo mô hình sản phẩm thuộc shop.
- Sáu danh mục, 30 ảnh món riêng mang logo lá, 3 size S/M/L mỗi món, tồn kho hữu hạn khởi đầu 200 mỗi size, 3 topping và khu vực/phí giao hàng.
- 90 đơn COD cơ bản và 9 đơn nhóm văn phòng (44 ly/đơn, dưới giới hạn 50 ly), tổng 99 đơn giao thành công, tạo qua CartService → CheckoutService → OrderWorkflowService. Không nhảy thẳng trạng thái bằng SQL. Tiền, chiết khấu, snapshot, thanh toán, lịch sử và điểm do service tính.
- Review và comment qua CommunityService, chỉ cho tác giả có order item DELIVERED + PAID đúng sản phẩm. Comment đủ 50 ký tự; sao 4–5, không bịa thống kê bằng số hardcode.
- Các đơn thêm để xem NEW, CONFIRMED, PICKED_UP, DELIVERING, CANCELLED, chuyển khoản chờ xác nhận và chuyển khoản đã xác nhận thủ công bằng mã DEMO. Đây không phải bằng chứng tiền đã vào ngân hàng.
- Lượt bán, rating và yêu thích trên UI được truy vấn từ các bảng thật của dataset. Một số người dùng demo có địa chỉ/default, yêu thích, đã xem và lịch sử điểm.

## Nhập lại trên database có chủ đích

Không tự chạy khi ứng dụng khởi động. Seed chỉ thêm theo slug/email/key demo cố định, không DELETE/TRUNCATE, không tắt FK/CHECK, không reset mật khẩu tài khoản đã có.

```powershell
# JDK 21; đứng tại thư mục gốc repository
.\mvnw.cmd -B -ntp package -DskipTests
.\mvnw.cmd -B -ntp dependency:build-classpath '-Dmdep.outputFile=.local/identity-classpath.txt'
python tools/seed_demo.py          # chỉ xem kế hoạch
python tools/seed_demo.py --apply  # ghi vào utetra_db đã cấu hình
```

Script seed tắt mail trong tiến trình của riêng nó và không gọi cổng thanh toán thật. Từng đơn dùng transaction của service; có thể chạy lại sau gián đoạn để tiếp tục các đơn demo. Giữ tên/mật khẩu demo không đổi nếu muốn tiếp tục seed. Dữ liệu view có thể tăng khi chạy lại; không tạo trùng order/review/comment đã có. Log cục bộ: `.local/demo-seed.log`.

## Tham khảo và hình ảnh

Tham khảo nhóm đồ uống từ [KATINAT](https://katinat.vn/) (cà phê/trà sữa/trà trái cây) và [Highlands Best Seller](https://order.highlandscoffee.com.vn/best-seller) (cà phê phin, trà, đá xay). [Phúc Long](https://phuclong.com.vn/) được tham khảo như định hướng trà/cà phê; không trích giá/menu chưa đọc được. Tên món, giá và mô tả trong `tools/demo/menu.json` là danh mục demo UTETra, không phải menu/giá chính thức của các thương hiệu đó. Không dùng ảnh/logo thương hiệu khác.

Prompt ảnh sản phẩm: ảnh chụp sản phẩm vuông, một ly nước đúng thành phần từng món, nền kem, bệ đá, ánh sáng studio; mặt trước có nhãn tròn xanh trà, logo lá màu kem và chữ UTETra. Prompt hero: giữ ảnh gốc ba ly, thêm logo lá vào đúng ba vòng tròn xanh. Công cụ: built-in imagegen. Asset lưu trong `src/main/resources/static/assets/images/menu/` và `utetra-tea-ritual-branded.png`.

## Kiểm chứng

Đã nhập xong: 22 tài khoản tổng cộng (10 cũ + 12 demo), 3 chi nhánh, 6 danh mục, 90 sản phẩm/30 món, 270 size, 106 đơn, 90 review, 90 comment, 99 giao dịch điểm EARN. Có thêm 2 voucher demo. Kết quả 12 đối chiếu dữ liệu không có vi phạm ở [database-verification.txt](database-verification.txt). Bộ test H2/Tomcat không thay thế nghiệm thu provider thật. Các trường hợp VNPAY, upload media thật, return/refund cần dùng luồng phù hợp và không tự coi mẫu dữ liệu là bằng chứng đã kiểm hết mọi tổ hợp.

Bộ ảnh: [manifest](image-manifest.json), [prompt đầy đủ](image-prompts.json), [kiểm tra file/size/hash](image-verification.json). Kết quả UI ở [ui/verification.json](ui/verification.json), ảnh chụp trong cùng thư mục.

Kết quả bản cuối: **161 tests đạt** (0 failure/error/skipped), WAR build thành công; **18 lượt kiểm trình duyệt Edge đạt** ở 1440/768/390px với User/Admin/Manager/Vendor, không JavaScript exception hoặc tràn ngang ở trang được kiểm. 30 file ảnh menu đã kiểm signature/kích thước/hash và đã xem logo/thành phần; ảnh trang chủ/menu/chi tiết tải được. Kiểm ảnh bỏ qua khung avatar chưa chọn file (không có src), không coi đó là ảnh hỏng. [Test summary](test-verification.json).

Server local cuối cùng ở http://127.0.0.1:8080/. Chưa commit/push. Chưa nghiệm thu giao dịch VNPAY/SMTP/Cloudinary thật; dataset là mô phỏng và không khẳng định bao phủ mọi tổ hợp đầu vào.

Cập nhật luồng 26/10: khách chọn món trước, chỉ chọn chi nhánh khi lên đơn. Menu gộp theo `catalog_key`, vẫn giữ 90 bản ghi theo shop trong DB nhưng hiển thị 30 món. Xem [hướng dẫn luồng mới](../features/order-payment/BRANCH_SELECTION_20261026.md). `tools/demo/AssignCatalogKeys.java` dùng để gán lại mã cho bộ demo có trước migration, không gán tên trùng tùy ý. Bộ seed mới tự ghi mã ngay từ đầu.

Bản chọn chi nhánh tại checkout: toàn bộ 167 test đạt, 6 smoke/render cuối đạt; 21 kiểm tra trang/viewport và 5 kiểm tra guest→popup→login→branch→quote đạt. Bộ seed hiện tự gán 30 mã món chung cho 90 bản ghi demo sau migration.

## Reset mật khẩu kiểm thử toàn bộ tài khoản

Bản tích hợp từ migration 2026102705 chỉ lưu BCrypt trong password_hash; cơ chế tham chiếu mật khẩu rõ đã bỏ. Tool reset test cũng chỉ ghi hash.

Sau khi pull nhánh `identify`, build và khởi động bản mới để Flyway áp dụng migration, tạo dependency classpath như hướng dẫn phía trên, rồi chạy khi có chủ đích:

```powershell
python tools/reset_test_passwords.py                      # xem phạm vi, không ghi DB
python tools/reset_test_passwords.py --apply-all-accounts # reset mọi tài khoản trong utetra_db
```

Git chỉ đồng bộ code và migration, không đồng bộ dữ liệu DB. Người dùng chung đúng database TiDB đã reset không cần chạy lại lệnh. Người dùng database riêng cần áp dụng migration rồi chủ động chạy lệnh. Không đổi datasource để sử dụng script. Script không bật tài khoản bị khóa hoặc xác minh email thay người dùng.

Bản tích hợp từ migration 2026102705 chỉ lưu BCrypt trong password_hash; cơ chế tham chiếu mật khẩu rõ đã bỏ. Tool reset test cũng chỉ ghi hash.

## Phản hồi đúng món — 26/10/2026

Đã thay 90 đánh giá và 90 bình luận demo dùng câu mẫu chung bằng phản hồi riêng cho 30 món. tools/demo/menu.json có 3 tasting_notes cho mỗi món; FeedbackCopy.java dùng trong seed và công cụ sửa dữ liệu. Size/đường/đá lấy từ order_items đã mua, không đoán. Mỗi phản hồi vẫn ghi rõ là nội dung demo, không giả phản hồi khách thật.

`python tools/check_demo_feedback.py` kiểm đủ 30 món/90 nội dung, Unicode NFC, độ dài và ngữ cảnh cà phê. `python tools/repair_demo_feedback.py` chỉ xem kế hoạch đọc DB; thêm `--apply` để thay **đúng** các chuỗi boilerplate cũ của tác giả demo trên món do vendor demo sở hữu, có order item DELIVERED/PAID khớp tác giả. Không sửa phản hồi mới do người dùng viết, không đổi sao/ảnh/liên kết mua hàng. Chạy lại sau sửa trả eligible=0, không viết lại dữ liệu đã cập nhật. Không cần seed lại toàn bộ đơn hàng.
