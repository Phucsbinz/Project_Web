# Review độc lập các thay đổi Shipper — 28/10/2026

## Yêu cầu nguyên văn

```text
d:\WEBCUOIKI\.agents\skills "Dùng skill requesting-code-review kiểm tra lại các thay đổi vừa làm xem có đạt yêu cầu chưa."
```

## Quy trình và phạm vi

Áp dụng `.agents/skills/requesting-code-review/SKILL.md`, giao reviewer độc lập với context riêng theo template `code-reviewer.md`. Reviewer chỉ đọc checkout, không sửa code/index/HEAD và không gọi reviewer khác. Điều phối đối chiếu kết quả với bằng chứng kiểm thử và trạng thái Git theo `verification-before-completion`; đánh giá phản hồi theo `receiving-code-review`.

- Base: `9d6b55333926f4a5de7495c0857767b80259e053`.
- Head: `269ea2c8c6d434078ca96846527dcd3d47a6873e`.
- Trọng tâm: hoàn thiện Shipper; đọc thêm phần merge tương tác trực tiếp từ integration khi cần, không coi đây là kiểm toán toàn bộ project.
- Nguồn yêu cầu: Shipper quản lý đơn được phân công và thống kê đơn của mình; contract tại `docs/features/order-payment/SHIPPER_COMPLETION_20261028.md`; quy tắc shared UI tại AGENTS.md và docs/SHARED_UI.md.

## Kết quả

**Ready to merge: Yes — không phát hiện lỗi cần sửa trước khi tích hợp trong phạm vi review.**

- Critical: không phát hiện.
- Important: không phát hiện.
- Minor: không có vấn đề đủ căn cứ cần ghi thành finding.

Không xem kết luận trên là bảo đảm phần mềm không còn lỗi hoặc mọi yêu cầu toàn project đã hoàn tất.

## Các điểm đã đối chiếu

1. Service kiểm role hiện tại, tài khoản hoạt động/đã xác minh, feature flag và assignment. Vendor/Manager kiểm phạm vi cửa hàng. Đổi Shipper/hủy assignment thu hồi quyền người cũ.
2. State machine và version/khóa đơn bảo vệ luồng `CONFIRMED → PICKED_UP → DELIVERING → DELIVERED`; không cho Shipper tự xác nhận đơn NEW, bỏ bước hoặc sửa đơn hoàn thành.
3. COD chỉ hoàn tất khi xác nhận đã thu tiền; đơn trả trước cần PAID. Thanh toán, điểm, assignment và settlement xử lý trong transaction.
4. Báo thất bại ghi lý do/sự kiện, giữ trạng thái để vận hành xử lý tiếp; không tự hủy hay hoàn tiền.
5. Dropdown chỉ chọn Shipper hoạt động/đã xác minh; cùng Shipper đang phụ trách thì không đổi ngày phân công/sinh event trùng.
6. Danh sách có thông tin lấy/giao hàng và COD; JSP escape dữ liệu và dùng layout/theme chung, không bổ sung framework/layout khác.
7. Báo cáo theo user từ principal; aggregate theo ngày, giải thích rõ assignment hiện tại, thất bại và COD gross.

## Bằng chứng và giới hạn

- Trong lượt review: reviewer và điều phối chạy shared UI checker thành công (một SiteMesh layout, 21 file chung); điều phối chạy `git diff --check` thành công.
- Đã đọc source/test/browser scripts, JSON và ảnh minh chứng. Tổng Surefire XML hiện có được đối chiếu là 214 tests, 0 failures/errors/skipped.
- Nhật ký clean verify trước lượt review ghi BUILD SUCCESS; JSON lưu 4 kịch bản browser Shipper và 18 shopping đạt. **Không chạy lại Maven hoặc ứng dụng trong lượt review này.**
- Fixture là H2/Tomcat/SiteMesh cô lập. Không ghi/reset DB cloud, không thử giao hàng/thu COD thật, không xác nhận vận hành MySQL/TiDB cloud từ kết quả H2.
- Minh chứng gốc: `docs/demo/shipper-20261028/`.

## Các hành vi cân nhắc nhưng không xem là lỗi

- Báo cáo theo assignment hiện tại, không phải mọi lượt phân công lịch sử: phù hợp contract đã công bố; chưa yêu cầu đổi schema.
- COD gross vẫn tính tiền từng thu dù sau đó hoàn: đúng nhãn tổng đã thu; không phải thu nhập/số dư cần nộp.
- Operator tiếp tục có quyền cập nhật giao hàng: giữ contract hiện có; yêu cầu Shipper không đòi loại quyền operator.
- Khoảng cách: chủ nhóm đã yêu cầu không triển khai. Lương/nộp COD, giao nhận và đối soát thật nằm ngoài thay đổi này.
- Refactor 3NF, kiểm toán toàn bộ DB, triển khai cloud: không thực hiện trong review này.

Các mục trên được điều phối chấp nhận giữ nguyên phạm vi. Vận hành thật và dữ liệu/tài khoản demo vẫn cần nhóm kiểm trên môi trường báo cáo.

## Trạng thái sau review

Không sửa mã nguồn hoặc cấu hình, không tạo commit/push trong lượt này. Chỉ thêm báo cáo và nhật ký prompt. Giữ nguyên các thay đổi cục bộ có trước (ảnh nguồn QR bị xóa, thư mục `.agents/` và `skills-lock.json` do người dùng bổ sung).
