# Khảo sát nguồn menu chính thức — 28/10/2026

Công cụ UI `cua.listBrowsers()` trả danh sách rỗng; mở in-app browser báo không khả dụng. Đã tìm nguồn qua tìm kiếm và mở trực tiếp bằng công cụ web. Không giả nhận đã thao tác native browser. Không vượt chặn, đăng nhập, đặt món hoặc tải tài sản thương hiệu để dùng trong UTETra.

## Nguồn quan sát, không phải menu UTETra

- **Gong Cha Việt Nam**: [menu chính thức](https://gongcha.com.vn/danh-muc/menu-hien-tai/) chuyển bản EN; có nhóm trà sữa, trà nguyên chất, đá xay và topping. [Trà sữa xoài](https://gongcha.com.vn/en/san-pham/tra-sua-xoai-tran-chau-den/): M51.000, L59.000 VNĐ; không thấy S. [Khoai môn đá xay](https://gongcha.com.vn/en/san-pham/khoai-mon-da-xay/): chỉ M63.000 VNĐ. [Sương sáo](https://gongcha.com.vn/en/san-pham/suong-sao/):10.000 VNĐ; gợi ý dùng với trà sữa caramel/chocolate/khoai môn/oolong. Chỉ ba trang chi tiết này được dùng làm giá trong seed.
- **ToCoToCo**: tìm được [website](https://tocotocotea.com.vn/), theo mục sản phẩm tới [menu](https://tocotocotea.com.vn/order/). Có trà sữa/trái cây/macchiato/sữa chua. Ô long xoài kem cà phê49.000 và trà đào bưởi hồng trân châu baby38.000 VNĐ hiển thị một giá, không xác nhận size. Nhiều món có29.000 kèm giá cao hơn và dấu giảm%; **không dùng các giá này làm giá thường xuyên hoặc gán cho size**. Không dùng giỏ mẫu trong HTML làm chứng cứ giao dịch.
- **Chapa Tea**: [trang chính thức](https://chapatea.vn/), [menu matcha](https://chapatea.vn/matcha-shan-tuyet/), [matcha latte](https://chapatea.vn/menu/matcha-latte-shan-tuyet/) công bố49.000 VNĐ nhưng không nêu size. Matcha mochi55.000 và món matcha khác89.000 trong nhóm; chỉ tham khảo cấu trúc nhóm, **không nhập giá/size hoặc tự gán topping**.
- **Phúc Long**: [FAQ chính thức](https://phuclong.com.vn/hoi-vien/faq) truy cập được, nói menu phụ thuộc cửa hàng. Trang chủ không trích xuất được nội dung; không lấy giá ở trang tổng hợp thay cho nguồn chính thức. Không sử dụng nội dung hội viên/khoảng cách trong FAQ để đổi nghiệp vụ UTETra.
- **KOI Thé**: [menu chính thức toàn cầu](https://koithe.com/menu.php) có mục VIETNAM trỏ đến ảnh menu; công cụ không đọc được bảng giá ảnh trong lượt này. Không suy diễn từ menu quốc gia khác hoặc dùng giá từ trang tổng hợp.

Một số trang Gong Cha bản Việt chuyển EN/không có nội dung tiếng Việt hoặc timeout; nhiều trang Chapa topping cache miss. Các trường chưa quan sát được được giữ là chưa xác định, không thay bằng số đoán. Ngày truy cập cho mọi nguồn trên:2026-10-28. Giá là thông tin được trang hiển thị lúc khảo sát, không cam kết là giá tại quầy mọi chi nhánh.

## Biên soạn cho UTETra

- Chỉ bổ sung hai món khác với30 món canonical hiện có: **Trà sữa xoài** (sữa + xoài), **Khoai môn đá xay** (dạng đá xay vị khoai môn). Tên/mô tả do nhóm biên soạn, không gắn Gong Cha hay nhãn hàng khác vào sản phẩm, không sao chép marketing.
- Giá demo lấy từ ba trang chi tiết đã mở thành công; không phải bảng giá thương mại của UTETra. M/L món xoài và M món khoai môn giữ đúng độ chi tiết nguồn; không thêm S/L suy đoán.
- Topping sương sáo chỉ liên kết với các món trà sữa khoai môn/chocolate/oolong hiện có, theo gợi ý nguồn. Liên kết trân châu đen tùy chọn cho món xoài là **công thức demo UTETra biên soạn**, không tuyên bố nguồn cho phép topping đó hoặc bao gồm sẵn trong giá. Giá topping trân châu giữ nguyên DB.
- Không xác nhận được tồn kho món mới: stock_quantity NULL chỉ biểu thị chưa biết, **is_available=false** nên không cho mua không kiểm tồn. Cần người phụ trách nhập tồn thật/demo đã chốt bằng luồng quản trị rồi mở bán; không tự bỏ kiểm tồn.
- Ảnh mới đểNULL, dùng `.catalog-placeholder` đã có trong JSP. Không dùng ảnh món khác, URL ảnh thương hiệu hoặc public_id provider giả.
- Đường/đá0/30/50/70/100 là contract sẵn của UTETra, **không phải dữ liệu suy ra từ các website**. Không thêm tùy chọn ngoài code hoặc đổi công thức tính giá.
