# UTETra — Khảo sát hiện trạng và phân tích thiết kế

## 1. Mẫu tham khảo đã đọc

Nguồn: [KhaoSat_PhanTichHeThong.docx](../KhaoSat_PhanTichHeThong.docx), dùng làm tham khảo theo chỉ dẫn của người dùng. Đã trích nội dung, gồm Chương 2 khảo sát Tiki/Shopee/Lazada/Etsy với ảnh và ưu/nhược; Chương 3 chức năng Guest/Customer/Vendor/Admin, bảng UC, use case diagram, sequence diagram, ERD và từ điển dữ liệu. File có 45 ảnh nhúng; không xác nhận đã kiểm từng hình/layout và không sao chép nhận xét trong mẫu như kết quả khảo sát mới.

Mẫu dùng schema ObjectId/arrays, ví nội bộ và những UC riêng của đề tài mẫu. UTETra dùng MySQL/JPA, OTP và role Manager theo quy định mới, nên chỉ học cách trình bày/quy trình. Không copy lỗi trùng UC_015 hoặc tên Name sai trong bảng mẫu; mã UTETra phải unique và kiểm chứng với nguồn.

## 2. Khảo sát thực tế cần nhóm làm

| Hệ thống khảo sát/URL | Ngày/người | Phạm vi/thiết bị | Ảnh nguồn | Ưu điểm đã quan sát | Nhược điểm đã quan sát | Bài học UTETra |
| --- | --- | --- | --- | --- | --- | --- |
| [CẦN BỔ SUNG] | | | | | | |

Khảo sát luồng tìm/lọc/chọn món, tùy biến, shop, OTP/profile/địa chỉ, cart/payment/promo, review/comment/media, order/return và responsive. Chỉ nêu nhận xét có quan sát thật/ảnh/ngày; không suy ra hệ thống khác không hỗ trợ chức năng vì chưa thấy nút.

Khảo sát quy trình cửa hàng bằng phỏng vấn/quan sát nếu có: nhận đơn, pha chế/lấy/giao, thu tiền/hoàn, nhân viên/owner, topping/availability và phí. Ghi câu hỏi, người trả lời/phạm vi, đồng ý sử dụng dữ liệu; nếu chưa khảo sát thì ghi chưa có kết quả, không dựng phỏng vấn giả.

## 3. Bố cục phân tích thiết kế

Mục tiêu/phạm vi → quy trình hiện tại/vấn đề → yêu cầu theo actor → use case diagram → bảng UC đầy đủ → activity/sequence trọng tâm → kiến trúc MVC/3 tầng → ERD/từ điển → ma trận quyền/validation/transaction → giao diện và setup/test.

Mỗi UC: ID/name/goal/actors/preconditions/postconditions/main flow/alternative/exception, thêm owner/branch/role/BR/file/commit/prompt và hình. Checkout/COD/VNPAY/refund/OTP có transaction, timeout, idempotency và quyền; flow chỉ happy path không đủ.

Nguồn dữ liệu: PROJECT_PLAN, DATABASE_DESIGN, USE_CASES, QUY_DINH_VA_CAU_HINH_CHUNG. Các model AI đọc các file này trước và không tự cắt role/OTP/return/promotion. Chi nhánh chọn thủ công; không tính khoảng cách.

## 4. Hồ sơ minh chứng

Lưu kiến trúc/activity/sequence/ERD nguồn .mmd/.puml và ảnh PNG/SVG; code screenshot có callout/path/symbol/commit; UI thật theo role và lỗi; setup/test; prompt thật và nhóm sửa. Mỗi người viết phần role/UC theo phân công, nhóm trưởng tổng hợp DOCX theo REPORT_ASSEMBLY.
