# Quản lý tài khoản tách khỏi phân công — 29/10/2026

## Yêu cầu
Tách quản lý tài khoản khỏi phân công nhân sự; hoàn thiện tạo, xem/tìm kiếm, cập nhật vai trò, vô hiệu hóa/kích hoạt lại. Admin cần tạo tài khoản nhân viên an toàn.

## Thực hiện
- `/admin/users` chỉ quản lý tài khoản, có bộ lọc vai trò và trạng thái xác minh email đọc từ dữ liệu thật. Link riêng sang `/admin/assignments` và `/admin/shipper-scopes`.
- Admin tạo USER/VENDOR/MANAGER/SHIPPER; dùng luồng đăng ký hiện có để băm mật khẩu và gửi OTP. Chủ tài khoản phải tự xác minh email trước khi đăng nhập; tạo tài khoản không tự cấp membership/khu vực giao, không tự đánh dấu email đã xác minh.
- Thay checkbox trạng thái chung bằng thao tác vô hiệu hóa/kích hoạt lại có popup xác nhận; đổi vai trò cũng xác nhận và giải thích thu hồi phiên. Không xóa lịch sử giao dịch.
- Không cho vô hiệu hóa/đổi vai trò Shipper đang giữ assignment ACTIVE cho đơn PICKED_UP/DELIVERING; phải giao xong vì chưa có bàn giao sau lấy hàng. Đơn CONFIRMED vẫn cho phép xử lý tài khoản và phân công lại.
- Backend vẫn kiểm quyền/phạm vi, phiên hiện tại, version, giữ ít nhất một Admin hoạt động. Giữ duy nhất layout/theme hiện có.

## Kiểm chứng
- RED shipper: Maven chạy riêng test pickup, lỗi đúng assertion chưa ném exception (1 test, 1 failure, 0 error).
- RED: lượt tích hợp chạy Maven và xác nhận lỗi compile đúng các form/overload mới chưa tồn tại trước sửa backend.
- Đã chạy `python tools/check_shared_ui.py`: 1 layout/21 file; `python tools/check_agent_skills.py`: 15 skill hợp lệ.
- Đã chạy `node --check src/main/resources/static/assets/js/identity.js`: exit 0.
- Thêm `AccountManagementIntegrationTest` cho tạo tài khoản nhân viên chưa xác minh, bộ lọc vai trò và từ chối Manager tạo nhân viên/Admin được tạo qua form.
- Chưa tự chạy Maven vì lượt tích hợp chính quản lý build; cần chạy `-Dtest=AccountManagementIntegrationTest test` và kiểm render Tomcat/SiteMesh thật tại lượt tích hợp. Chưa khẳng định kiểm Java/render đạt.
