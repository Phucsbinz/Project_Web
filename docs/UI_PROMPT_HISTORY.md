# Nhật ký yêu cầu UI dùng chung

## UI-AI-001 — 22/10/2026

Prompt nguyên văn:

```text
hình như tôi chưa cấu hình sẵn 1 layout đồng bộ cho các nhánh để cách nhánh làm chung nên giờ mỗi người 1 kiểu frontend
có gì bạn chuẩn hóa 1 layout dùng chung tất cả các nhánh nha duy nhất 1 layout thống nhất và ghi md cho agent của thành viên khác biết sử dụng và chỉnh sửa cấu hình thống nhất ấy
```

Khảo sát: main/catalog dùng giao diện cũ; identify/order đã có giao diện mới; sáu JSP layout theo vai trò của bản mới sao chép cùng nội dung. Chọn bộ UI mới đang dùng trên identify/order, tập trung tại app.jsp, một mapping SiteMesh, shared assets và NavigationAdvice. Tách bundle khỏi nghiệp vụ order để đồng bộ vào cả sáu nhánh. Kết quả kiểm chứng/triển khai được ghi tại docs/SHARED_UI.md; không ghi secret.
