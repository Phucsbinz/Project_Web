# Catalog CRUD và đọc lại trạng thái — 29/10/2026

## Yêu cầu
Hoàn thiện CRUD cho dữ liệu đang dùng; ngừng hoạt động thay vì xóa dữ liệu ảnh hưởng lịch sử; giữ chức năng và quyền hiện tại.

## Đối chiếu và thay đổi
- Danh mục và topping chung đã có tạo, xem, sửa cùng `active` và phiên bản. Khuyến mãi và khu vực giao cũng đã có đọc lại, cập nhật và bật/tắt qua biểu mẫu. Không thêm endpoint trùng hoặc xóa cứng.
- Topping tại chi nhánh trước đây chỉ có PUT và biểu mẫu mặc định sẵn bán, không đọc cấu hình đã lưu. Thêm GET `/api/{admin|vendor|manager}/catalog/stores/{storeId}/toppings`, kiểm quyền bằng `CatalogGuard.store` trước khi đọc.
- Kết quả giữ cả topping đã ngừng hoạt động chung, trạng thái sẵn bán được lấy từ `store_toppings`; chưa cấu hình là không sẵn bán, phù hợp luồng quote hiện tại.
- Trang cấu hình topping của admin/vendor hiển thị danh sách thật và nạp trạng thái khi chỉnh sửa. Workspace Manager có bảng bật/tắt topping của chi nhánh được phân công; topping đã ngừng toàn hệ thống không được bật từ bảng này.
- Không đổi layout, Bootstrap, datasource, JWT, dữ liệu giao dịch hoặc quyền sửa giá của Manager.

## Kiểm chứng thực tế
- `node --check` cho catalog-management.js và manager-operations.js: thành công.
- `python tools/check_shared_ui.py`: thành công, một layout và 21 file chung.
- `python tools/check_agent_skills.py`: thành công, 15 skill hợp lệ.
- `git diff --check`: không có lỗi khoảng trắng.
- `py -3 tools/check_shared_ui.py` không chạy được vì Python launcher không tìm thấy Python; lệnh `python` thay thế chạy thành công.
- Thêm `CatalogCrudReadbackTest`: kiểm phạm vi trước khi đọc, giữ định nghĩa inactive, trạng thái thiếu cấu hình và trạng thái đã lưu bật/tắt. Chưa chạy Maven trong subtask để tránh tranh chấp với build của agent chính; cần chạy `mvnw.cmd -Dtest=CatalogCrudReadbackTest test` trong lượt kiểm chứng chung.
- Chưa kiểm render Tomcat/SiteMesh trực tiếp trong subtask; cần kiểm workspace Manager và trang topping sau khi build chung.

## Bổ sung biểu phí cùng ngày
- Bảng biểu phí trả ID bản ghi và ID cặp carrier/khu vực. Nút Chỉnh sửa nạp đúng cặp, phí và trạng thái active vào form, không suy đoán từ tên hiển thị.
- Giữ service upsert theo cặp, kiểm quyền và khóa hiện có; không thêm xóa cứng hoặc thay chính sách version.
- Thêm DeliveryRateReadbackTest thực thi SQL H2 với tên trùng và kiểm lọc phạm vi Manager. Chưa chạy Maven trong subtask; cần agent chính chạy.
- Đã kiểm syntax JavaScript inline tools.jsp bằng node --check, shared UI check và git diff --check thành công. Render Tomcat/SiteMesh cần agent chính kiểm chung.
