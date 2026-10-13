# UTETra — Quy trình cho AI của thành viên

Đọc QUY_DINH_VA_CAU_HINH_CHUNG, 3 đặc tả gốc, README, COMMON_BASELINE, SETUP, IMPLEMENTATION và hồ sơ UC nhánh hiện tại. Kiểm Git/main version và source thật. Quy định thầy được ưu tiên; mẫu DOCX chỉ tham khảo bố cục, PDF/guide có phiên bản khác phải điều chỉnh được ghi rõ.

## Prompt mở phiên mẫu — chưa sử dụng

```text
Bạn hỗ trợ UTETra trên nhánh <nhánh>, người phụ trách <tên>, UC <mã>.
Đọc docs/QUY_DINH_VA_CAU_HINH_CHUNG.md, PROJECT_PLAN.md,
DATABASE_DESIGN.md, USE_CASES.md, docs/README.md, COMMON_BASELINE,
SETUP, hướng dẫn nhánh và IMPLEMENTATION trước sửa code.
Giữ Spring MVC/3 tầng, Java21/Boot3.5.13/WAR/JSP/JSTL/SiteMesh/Bootstrap,
JPA/MySQL utetra_db/TLS/JWT/STOMP/Cloudinary và cấu hình trực tiếp properties.
Vendor có User; Manager scope; OTP email kích hoạt/reset, COD+VNPAY,
coupon/community/return-refund/carrier/commission là scope bắt buộc.
Shipper là phần thêm. Không tính khoảng cách/tự chọn chi nhánh.
Thực hiện UC từ đầu đến cuối, phụ thuộc phải theo contract main.
Append prompt nguyên văn (che secret), response, phần tôi sửa, code/ảnh/setup,
test/kết quả thật/commit/reviewer vào AI_LOG và hồ sơ UC.
Không đánh dấu test hoặc provider thành công nếu chưa kiểm chứng.
```

Quy trình: đọc → ghi prompt → xác định flow/BR/schema/quyền → activity/sequence → code từng tầng → test → chụp code/UI/export hình → setup → nhóm review/commit. Không xóa toàn bộ tài liệu trước khi viết; chỉnh tại chỗ, giữ lịch sử và lỗi/sửa để tra lại.

Hình AI/mockup/diagram thiết kế ghi rõ nguồn/prompt và chưa triển khai. Hình source/UI để nghiệm thu lấy ứng dụng thật và code đúng commit, có callout/caption. Prompt gợi ý chưa gửi không tính đã dùng; model/thời gian không biết thì ghi không rõ. Không hiển thị password/OTP/token/private key dù config repo private được phép theo dõi.
