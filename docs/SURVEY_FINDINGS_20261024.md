# Khảo sát sơ bộ hệ thống bán đồ uống công khai

Ngày quan sát 24/10/2026. Phương pháp: đọc trang công khai và chụp màn hình trình duyệt mới, không đăng nhập hay đặt hàng. Đây là khảo sát bàn có nguồn, **không phải phỏng vấn khách hàng, đo hiệu năng hoặc nghiệm thu backend của các hãng**.

Mẫu `KhaoSat_PhanTichHeThong.docx` của thầy đã được đọc lại: có phần ưu/nhược và bài học cho hệ thống, tiếp theo phân tích/thiết kế. Nhận xét cũ về Tiki/Shopee không được sao chép thành kết quả hiện tại. Link LMS không truy cập được bằng công cụ; dùng bản DOCX người dùng đã cung cấp trong repo.

## 1. ToCoToCo

Nguồn: [trang đặt món](https://tocotocotea.com/order/), [trang checkout công khai](https://tocotocotea.com/checkout/). [Ảnh quan sát](report-assets/survey/tocotoco-menu.png); thời điểm/HTTP trong [sources.json](report-assets/survey/sources.json).

Trang thực đơn có tìm kiếm, danh mục, thẻ món/giá và giỏ bên cạnh. Khi mở mới xuất hiện lựa chọn miền Bắc/miền Nam. Ưu điểm quan sát được: người dùng có thể thấy menu và giỏ trong một màn hình. Điểm cần cân nhắc: cửa sổ chọn vùng che nội dung ban đầu; chưa đo xem nó ảnh hưởng tỷ lệ đặt hàng hay không. Bài học cho UTETra: chọn chi nhánh rõ ràng, giữ giỏ dễ thấy, phản hồi trạng thái trống/lỗi.

Trang checkout công khai có nhãn giao/nhận tại cửa hàng, phương thức thanh toán, mã khuyến mãi và tổng phí. Có chuỗi ngày cũ trong nội dung trả về; chỉ dùng làm quan sát cấu trúc hiển thị, không khẳng định luồng thanh toán hiện tại đã hoạt động hay đã thử tiền thật.

## 2. Phúc Long

Nguồn: [FAQ hội viên](https://phuclong.com.vn/hoi-vien/faq). [Ảnh quan sát](report-assets/survey/phuclong-faq.png), cùng bản ghi HTTP/thời điểm trong sources.json.

Trang có phần hội viên, câu hỏi về điểm, đổi quà và voucher; nội dung FAQ giải thích tích điểm và điều kiện sử dụng. Ưu điểm quan sát: quy tắc được tách thành từng câu hỏi để tra cứu. Điểm cần cân nhắc: người mua cần được dẫn tới quy tắc liên quan ngay khi dùng điểm/voucher; chưa làm usability test để kết luận UI hãng khó dùng. Bài học cho UTETra: giải thích quy đổi/hạn mức ngay checkout, lưu ledger và lịch sử, nêu điều kiện rõ trước xác nhận.

Đây là tham khảo cách trình bày; không tự sao chép chính sách hãng thành yêu cầu của thầy. UTETra vẫn chọn chi nhánh thủ công và bỏ tính khoảng cách theo quyết định người dùng.

## 3. Phân tích và quyết định cho UTETra

1. Phân rã actor Guest/User/Vendor/Manager/Admin; Shipper là phần thêm. Guest xem/tìm; User mua/tương tác; Vendor mua như User và vận hành shop; Manager scope shop; Admin toàn hệ thống.
2. Chọn MVC ba tầng trong một app: Controller/JSP → Service → Repository/JPA/JDBC → MySQL tương thích. Một SiteMesh layout và Bootstrap chung, có JSON phục vụ thao tác trong trang.
3. Giỏ đăng nhập lưu DB. Checkout tính giá phía server, snapshot địa chỉ/phí/giảm giá, khóa tồn/quota và idempotency; socket thông báo sau commit, không thay khóa DB.
4. Đơn có state machine và audit; review/comment yêu cầu đã mua hợp lệ. OTP/JWT/CSRF và scope được kiểm ở backend.
5. Liên kết đặc tả tại PROJECT_PLAN, USE_CASES và DATABASE_DESIGN; [hình kiến trúc](report-assets/application-architecture.png), [luồng thanh toán](report-assets/activity-vnpay.png), source/hình UI và test ở hồ sơ module.

## 4. Phần nhóm cần xác nhận

- Thầy chấp nhận phạm vi khảo sát bàn và đối tượng khảo sát; nếu cần phỏng vấn/phiếu khảo sát thì nhóm bổ sung người tham gia, ngày và dữ liệu thật.
- Hoàn thiện yêu cầu nghiệp vụ từ trao đổi nhóm/khách hàng, ERD/từ điển dữ liệu/sequence theo bản source cuối; xác nhận cách hiểu Guest bán trên 10 và TiDB/MySQL.
- Đọc lại nhận xét và ảnh, ghi tên người chịu trách nhiệm rồi đưa vào DOCX; không tự ghi nhóm đã phỏng vấn hoặc thử checkout thật trên website khác.
