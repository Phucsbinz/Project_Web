# Phản hồi IDN-AI-002 — 20/10/2026

Đã đọc lại tài liệu/corpus Markdown và rà source identity hiện tại theo 16 UC. Đã sửa các lỗ hổng validation còn thiếu: số điện thoại chỉ có dấu/khoảng trắng; thiếu/ép kiểu version và boolean; API edit/assignment chưa @Valid; service nội bộ bỏ qua DTO; email/reset anonymous gây lỗi không đúng; resend chưa thu hồi grant cũ; ảnh giả chỉ có signature; độ dài comment Unicode không thống nhất; snapshot/REVERSAL điểm.

Kết quả kiểm chứng: Maven verify **53 test đạt**, WAR thành công; Edge/Playwright **23 check đạt**, không lỗi JavaScript. Lượt JVM fork thất bại do native memory đã được chạy lại bằng một JVM với heap/SerialGC/C1 giới hạn, không đổi pom hoặc bỏ test. Selector kiểm thử được sửa để phân biệt ô viết bình luận với ô sửa nội dung sẵn có.

Demo cục bộ chạy H2 in-memory, có banner và dữ liệu sản phẩm/đơn mua/điểm cùng User/Vendor/Manager/Admin. Sau QA đặt lại dữ liệu sạch; launcher chạy nền và stop script xác minh main class trước khi dừng. Chưa gửi email/upload Cloudinary/TiDB hoặc ghép thanh toán thật, không giả báo đã nghiệm thu provider. Source/ma trận/test/ảnh/demo tại [VALIDATION_REVIEW_20261020](../VALIDATION_REVIEW_20261020.md).

Nhóm chưa cung cấp reviewer hoặc sửa tay; không tự điền tên hoặc PR/commit. Mọi thay đổi vẫn ở feature/identity-membership và chưa commit/push.
