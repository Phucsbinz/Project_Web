# Nhật ký yêu cầu UI dùng chung

## UI-AI-001 — 22/10/2026

Prompt nguyên văn:

```text
hình như tôi chưa cấu hình sẵn 1 layout đồng bộ cho các nhánh để cách nhánh làm chung nên giờ mỗi người 1 kiểu frontend
có gì bạn chuẩn hóa 1 layout dùng chung tất cả các nhánh nha duy nhất 1 layout thống nhất và ghi md cho agent của thành viên khác biết sử dụng và chỉnh sửa cấu hình thống nhất ấy
```

Khảo sát: main/catalog dùng giao diện cũ; identify/order đã có giao diện mới; sáu JSP layout theo vai trò của bản mới sao chép cùng nội dung. Chọn bộ UI mới đang dùng trên identify/order, tập trung tại app.jsp, một mapping SiteMesh, shared assets và NavigationAdvice. Tách bundle khỏi nghiệp vụ order để đồng bộ vào cả sáu nhánh. Kết quả kiểm chứng/triển khai được ghi tại docs/SHARED_UI.md; không ghi secret.

## 26/10/2026 — Giao diện theo ảnh tham chiếu của Thanh

Yêu cầu: giữ chức năng cũ, áp dụng thiết kế trang chủ/menu/chi tiết và quản lý theo ảnh kem–xanh lá; sử dụng 30 ảnh UTETra đã tạo. Thực hiện trong layout/theme chung và JSP module, không đổi provider/datasource/security để sửa UI. Ảnh thumbnail thêm logo lá ở ba vòng tròn xanh; ảnh menu riêng cho 30 món. Thêm giỏ tóm tắt dùng /cart/data, thẻ KPI/bảng trạng thái/biểu đồ so sánh số liệu thật, giữ bảng cũ và luồng checkout/chi tiết. Kiểm ảnh và kết quả trình duyệt ở docs/demo. Chưa push theo yêu cầu Thanh.

## 26/10/2026 — Chọn món trước, chi nhánh sau

Thanh yêu cầu chọn chi nhánh tại bước lên đơn, bỏ các chi nhánh khỏi menu và tiếp tục áp dụng thiết kế popup đồ uống. Menu hiện gộp theo mã món chung; popup dùng lại trang chi tiết trong tài liệu cùng origin và template chung, không tạo layout/framework thứ hai. Giữ trang cửa hàng để tham khảo và sidebar quản lý. Backend migration/giỏ/checkout được ghi riêng trong BRANCH_SELECTION_20261026.md; chưa push GitHub.
