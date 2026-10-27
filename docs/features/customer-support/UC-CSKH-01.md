# UC-CSKH-01 — User nhận hỗ trợ từ quản lý chi nhánh hoặc Admin

Owner/reviewer: nhóm tự điền đúng người thực hiện; không lấy tên tác giả Git thay xác nhận phân công.

Tiền điều kiện: User đăng nhập/verified/active; chi nhánh có Manager hợp lệ hoặc có Admin hoạt động. Scope và dữ liệu theo README module.

Luồng chính:

1. User mở /support, chọn Admin hoặc chi nhánh và bắt đầu.
2. Server lấy customer từ JWT, tạo hoặc tái sử dụng hội thoại trong transaction.
3. User nhập nội dung, client tạo UUID và POST có CSRF; server kiểm scope, nội dung và lưu DB.
4. Sau commit, WebSocket gửi hint riêng. Manager/Admin mở hộp thư tương ứng và đọc lịch sử bằng API kiểm quyền.
5. Nhân viên trả lời trong hội thoại khách. User nhận hint và đọc tin mới. Reload vẫn giữ lịch sử DB.

Ngoại lệ cần demo: User khác biết UUID vẫn không đọc/gửi được; staff không tạo staff chat; Manager không có membership hoặc customer đổi thành staff thì không trả lời được; UUID retry không nhân đôi; HTML chỉ hiển thị text; broker đứt có polling và giữ nội dung đã lưu.

Báo cáo chức năng: dùng ảnh user/manager/admin-support-1440/390 trong docs/demo/support-20261027, sơ đồ sequence trong README, trích SupportService/SupportRepository/SupportEvents để diễn giải. Ghi rõ dữ liệu H2 fixture và test case tương ứng, không ghi là khách hàng thật. Prompt nằm trong AI_PROMPT_HISTORY.md và AI_LOG.md.
