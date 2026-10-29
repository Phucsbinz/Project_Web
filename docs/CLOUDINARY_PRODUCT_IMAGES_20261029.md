# Chuyển ảnh sản phẩm lên Cloudinary

Yêu cầu đã chọn: lưu dữ liệu ảnh trên Cloudinary, lưu URL và public_id trong DB để minh chứng. Không lưu bytes ảnh trong DB.

Công cụ tools/publish_catalog_images.py --apply chỉ xử lý bảng products với đường dẫn /assets/images/. Upload ảnh tồn tại trong static, kiểm đường dẫn giới hạn trong thư mục này và ghi secure_url/public_id. Public ID dựa trên SHA-256 nội dung để tránh tạo trùng. UPDATE có điều kiện URL cũ; giữ ảnh local dự phòng. Không chuyển avatar hoặc ảnh cá nhân. Manifest kết quả nằm ở .local/cloudinary-image-manifest.json; không chứa credential.

Lượt công cụ ban đầu có phạm vi cả ảnh cửa hàng/avatar bị duyệt tự động từ chối trước khi chạy. Đã thu hẹp công cụ còn ảnh sản phẩm công khai rồi mới thực hiện. Credential được đọc từ cấu hình hiện hành, không thêm vào công cụ hoặc log.

Kết quả: 32 ảnh khác nhau đã upload, cập nhật 96 bản ghi products, không thiếu file local. Cả 32 URL gốc được kiểm HTTP 200 và Content-Type image. URL trong DB dùng f_auto,q_auto,w_1000 để giảm tải, giữ nguyên bản gốc. Trình duyệt thật đã xác nhận menu đọc URL Cloudinary và ảnh trong vùng nhìn tải thành công. Báo cáo Manager cũng render thành công trên server 8080. Không push trong tác vụ này.
