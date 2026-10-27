# Nhật ký AI — CSKH

Prompt đầy đủ trong docs/AI_PROMPT_HISTORY.md, mục ROLE-SUPPORT-20261027. Người dùng yêu cầu kiểm đủ các role theo thầy, giữ phần nhập địa chỉ mới, bổ sung User chat Manager/Admin và chặn staff–staff, dùng WebSocket và lưu DB, sửa trên integration. Các câu hỏi tiếp theo làm rõ Spring Security/JWT/cookie/session/RBAC và yêu cầu không vi phạm quy định thầy.

Thực hiện: ghép identify efddd5f (xung đột storefront được xử lý giữ cả group và home), thêm CSKH ba tầng + JPA/migration + JSP/CSS/JS chung, queue private sau commit, scope backend, idempotency, lịch sử DB. Khôi phục bốn nhóm Top 20 trên home User. Bổ sung bỏ cơ chế password_plaintext bằng migration tiến tới, giữ hash BCrypt; không đổi JWT sang session hoặc bỏ CSRF.

Kiểm chứng thực và các lỗi gặp: xem ROLE_REQUIREMENTS_20261027.md. Không ghi đã đạt test sau sửa bảo mật cho tới khi chạy xong; không chạy email/bank/merchant/Cloudinary thật hoặc migrate cloud trong lượt này.

Cập nhật sau yêu cầu xóa cột và kiểm DB: đã áp dụng 2704/2705 trên cloud bằng Flyway, 17 migration thành công; 23 ID/hash không đổi. Kiểm kê SELECT cho 51 bảng/14 bảng trống, không xóa hoặc gộp bảng. Xem DB_AUDIT_20261027.md.
