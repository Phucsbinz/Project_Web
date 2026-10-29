# Kiểm duyệt đánh giá và bình luận — 29/10/2026

## Yêu cầu
Hoàn thiện luồng quản trị nội dung: đọc danh sách, tìm/lọc/phân trang, ẩn và khôi phục hiển thị bằng xác nhận. Không xóa lịch sử hoặc sửa lời tác giả.

## Thiết kế
- `/admin/moderation` dùng JSP riêng trong module identity và layout/theme chung. Chỉ Admin được đọc hoặc thay đổi.
- Bộ lọc loại đánh giá/bình luận, trạng thái và từ khóa nội dung/tác giả/sản phẩm; danh sách trả kèm liên hệ tác giả, đơn mua và sản phẩm.
- Ẩn chỉ đổi trạng thái, tăng version, ghi audit; giữ nguyên tác giả, nội dung, bằng chứng mua và media.
- Khôi phục chỉ cho nội dung còn tồn tại, hợp lệ, đúng tác giả/item/sản phẩm. Nội dung trắng do tác giả gỡ không thể khôi phục; đánh giá trống cũng không tự phục hồi vì hệ thống cũ không lưu dấu phân biệt với tác giả gỡ.
- Xác nhận bằng popup dùng chung; version chống ghi đè và backend kiểm quyền độc lập với menu.

## Kiểm chứng thực tế
- RED: lượt tích hợp xác nhận compile lỗi đúng hai service method mới trước khi viết backend.
- `node --check src/main/resources/static/assets/js/moderation.js`: exit 0.
- Thêm regression test quyền Manager, đọc/tìm nội dung, ẩn/hiện giữ text và từ chối khôi phục nội dung đã gỡ. Maven do lượt tích hợp chính thực hiện; chưa ghi nhận GREEN tại tài liệu này.
- Cần kiểm render Tomcat/SiteMesh thật tại lượt tích hợp.
