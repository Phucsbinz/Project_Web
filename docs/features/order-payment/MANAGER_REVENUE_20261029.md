# Báo cáo doanh thu Manager

Yêu cầu: Manager xem đầy đủ báo cáo bán hàng trong chi nhánh được phân công, không chỉ COD thu hộ.

Operational report giữ số đơn theo ngày tạo và tiền COD riêng, thêm số đơn ghi doanh thu, tiền món sau giảm, phí giao, hoa hồng, tiền hoàn, hoa hồng đảo và doanh thu ròng. Dùng cùng công thức settlement của Vendor nhưng kiểm scope Manager trước khi tổng hợp; không cấp quyền sửa giá hoặc cấu hình tài chính.

Doanh thu tính theo delivered_at, hoàn theo completed_at, múi giờ Việt Nam. Cả COD và QR đều có settlement sau giao và thanh toán. Không trộn số đơn tạo trong kỳ với số đơn giao trong kỳ.

Kiểm thử: test mới thất bại trước sửa do thiếu trường doanh thu; ShippingScopeIntegrationTest sau sửa đạt, gồm hai phương thức COD/QR, loại chi nhánh chưa được giao và từ chối chỉ định shop ngoài phạm vi. JSP được render trên Tomcat/SiteMesh trong suite. Shared UI check đạt. Không push tự động ở tác vụ này.
