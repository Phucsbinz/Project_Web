# Tự phân công shipper và quản trị CRUD

Thanh yêu cầu tách quản lý tài khoản khỏi phân công, bổ sung CRUD/vô hiệu hóa cho dữ liệu đang dùng, thêm shipper cho các chi nhánh và tự chia đơn khi Admin/Manager/Vendor xác nhận.

Thanh duyệt: thêm 3 shipper cho mỗi chi nhánh, chọn người hoạt động/đã xác minh/đúng phạm vi chi nhánh có ít chuyến ACTIVE nhất, hòa chọn id ổn định. Không có GPS nên khu vực được xác định bằng scope chi nhánh, không giả định khoảng cách. Không có ứng viên: giữ CONFIRMED, báo chờ phân công; không gán ngoài phạm vi.

Thiết kế: khóa tập ứng viên cùng actor/buyer theo thứ tự user id trước khóa order; kiểm tra lại eligibility sau khóa và đếm chuyến hiện hành bằng READ_COMMITTED. Không đổi phân công hiện có, không sửa chuyến đã lấy hàng. Ghi audit/history/thông báo bằng cơ chế hiện tại. Đơn QR phải đủ tiền trước khi xác nhận. COD là thu hộ, không phải thu nhập shipper.

Tài khoản bổ sung bằng công cụ additive có cờ apply rõ: chỉ thêm user mới theo định danh cố định và scope mới, không reset mật khẩu/vô hiệu hóa/tái kích hoạt user cũ. Chạy lại không tạo trùng, lưu danh sách account không lưu password vào tài liệu. Mật khẩu cấp từ biến môi trường lúc chạy và chỉ lưu hash.

Kế hoạch:
- [x] Test tự chia ít chuyến/cùng khu vực/tài khoản khóa/chờ người/concurrency và không nhân đôi (5 integration cases).
- [x] Backend auto dispatch tại CONFIRMED; hiển thị kết quả/chờ trên chi tiết đơn, có thử phân công lại khi chưa có ứng viên.
- [x] CRUD tài khoản riêng với phân công; readback topping, biểu phí, kiểm duyệt ẩn/khôi phục. Ma trận module: docs/features/catalog-sales/CRUD_MATRIX_20261029.md. Đơn/giao dịch dùng hủy/hoàn và snapshot, không CRUD tùy ý hoặc xóa lịch sử.
- [x] Đã provision 9 tài khoản trên database hiện hành: 3 cho mỗi UTETra Thủ Đức/Bình Thạnh/Quận 1. Danh sách không có mật khẩu ở .local/branch-shippers.json. Không sửa tài khoản cũ. Công cụ tools/provision_branch_shippers.py chỉ apply khi có cờ rõ và mật khẩu từ env.
- [x] Full verify 344 tests, 0 failures/errors, WAR build thành công. Bao gồm hồi quy liên module. Browser CRUD/dispatch 5 nhóm qua trên H2 riêng, các role cookie độc lập; kiểm tài khoản ở 390/768/1440px. Shared UI 1 layout/21 files và git diff --check qua. Không push.

Browser chứng minh: lọc/vô hiệu hóa/kích hoạt và chặn đăng nhập, topping lưu/readback giữa Manager/Vendor, tự giao COD từ Admin sang Shipper và hoàn tất, moderation ẩn/khôi phục bình luận mua thật trong fixture, biểu phí nạp đúng cặp và giá trị. Source review phân công/provisioning không có lỗi chặn. Ảnh/kết quả: .local/crud-dispatch-ui/.

Runtime thật mở lại cổng 8080, login HTTP 200; đã đăng nhập và mở workspace của một Shipper mới ở mỗi chi nhánh. Không tạo đơn thử trên database hiện hành để kiểm concurrency; kết quả concurrency ở trên là H2, chưa thay thế kiểm TiDB riêng.
