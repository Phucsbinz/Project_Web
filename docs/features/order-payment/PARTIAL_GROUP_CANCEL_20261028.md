# Hủy đơn nhóm khi mới thanh toán một phần — 28/10/2026

## Yêu cầu
Thanh hỏi khi còn một người chưa thanh toán mà chủ nhóm hủy thì sao; yêu cầu căn giữa mũi tên thả xuống toàn web.

## Xử lý
- Phát hiện lỗi: LOCAL_QR chỉ được xem là có tiền để hoàn khi trạng thái cả đơn PAID; hai phần SUCCESS của đơn UNPAID bị bỏ qua khi hủy.
- Hủy vẫn cần đúng chủ đơn/operator và đúng thời điểm cho phép. Khóa order trước, cùng thứ tự khóa với confirm QR. Đối chiếu ledger với phần món snapshot; cộng chính xác SUCCESS, không dùng tổng đơn làm khoản đã thu.
- Nếu có phần đã trả: CANCELLED / REFUND_PENDING, giữ aggregate PENDING khi mới thu một phần, không ghi FAILED làm mất dấu. Chưa có khoản dương đã trả thì CANCELLED / CANCELLED và không tạo hoàn tiền.
- Khóa thanh toán/renew sau hủy. Public status tách người đã trả REFUND_PENDING với người chưa trả CANCELLED; không chấp nhận QR cũ dù request tới cùng lúc hủy.
- Admin xử lý hoàn nội bộ qua endpoint hiện có: chỉ số SUCCESS thực tế, refund record ghi đúng số đó, giữ chứng cứ/tham chiếu, chỉ thực hiện một lần. Đánh dấu đúng các phần đã hoàn REFUNDED; phần chưa trả vẫn CANCELLED và không phát sinh khoản phải trả. Đơn đã hủy vẫn CANCELLED, payment REFUNDED; không làm nhóm hoạt động trở lại.
- Không tự chuyển tiền ngân hàng: LOCAL_QR là giao dịch nội bộ. Không đổi refund BANK_TRANSFER/VNPAY/COD. UI hiển thị số đã trả, chờ hoàn, đã hoàn và tham chiếu xử lý phù hợp.
- Shared UI utetra.css dùng hình chevron từ border và flex-center thay baseline glyph; select native giữ ảnh mũi tên căn giữa. Reset margin của choice-toggle và giới hạn rule margin mobile cũ vào nút submit chi nhánh; không làm menu lệch xuống 14px. app.jsp tăng cache. Giữ một layout/theme.

## Kiểm chứng
- 88 OrderCommerceIntegrationTest qua, WAR thành công. Bao gồm 2/3 trả rồi hủy/hoàn đúng số, chưa ai trả, đã trả đủ, trả cuối đồng thời hủy, chặn trả/renew sau hủy và chặn refund lặp/sai quyền. Các kiểm bank/cash hiện có cũng qua.
- Admin UI audit 17/17 qua. Lượt kiểm partial UI đầu phát hiện margin mobile và đã sửa; lượt kiểm cuối ghi thêm bên dưới.
- Không push bản sửa này.

- Lượt browser cuối trên H2 + Edge/Tomcat: 3 kịch bản qua: hình học mũi tên tâm khung 1440/390px và pseudo-chevron; hủy khi 2/3 trả khóa wallet/POST người cuối; Admin hoàn đúng tổng 2 phần, paid user REFUNDED, unpaid CANCELLED, chặn refund lặp. Không JS errors. Đã xem screenshot desktop/mobile. Shared UI checker và diff --check qua; server mở lại.

## Popup xác nhận và thông báo cá nhân — yêu cầu tiếp theo 28/10
Thanh yêu cầu phải có popup, chủ nhóm xác nhận rồi mới hủy; thành viên thấy nhóm đã bị hủy, ai đã trả thấy thêm hoàn sau 30 phút.
- Hủy tại trang nhóm và chi tiết đơn đều hỏi xác nhận, có nút rõ Xác nhận hủy; quay lại/đóng không gọi API hủy.
- Lifecycle notice truy vấn đúng phần thanh toán của actor, không lấy trạng thái tiền chung để áp cho mọi người. REFUND_PENDING/số tiền dương mới có dòng dự kiến hoàn sau 30 phút kể từ lúc hủy; trả refundExpectedAt theo cancelled_at máy chủ +30 phút. Người chưa trả không có dòng này.
- Khi Admin đã hoàn, notice riêng báo đã hoàn, không tiếp tục hứa 30 phút. noticeKey có trạng thái hoàn để không mất thông báo mới.
- Các trang khách và wallet công khai đều có popup thích hợp; dialog chung hỗ trợ xuống dòng, label nút tùy ngữ cảnh, vẫn dùng textContent/Bootstrap. Cache app.jsp tăng; một layout/theme.
- 30 phút là thời gian dự kiến trong thông báo; không tự đặt REFUNDED khi hết 30 phút và không tự chuyển tiền ngân hàng. Admin vẫn xác nhận xử lý thực tế theo workflow hiện hành.
- 88 commerce tests qua, có kiểm pending/refunded notices, thời điểm 30 phút và không gán dòng hoàn cho người chưa trả. Kết quả browser ghi thêm sau chạy.
- Browser partial audit mới: 4/4 qua, có kiểm hủy bỏ dialog giữ ORDERED, xác nhận mới hủy, popup thành viên paid có dòng 30 phút/unpaid không có, bên cạnh khóa QR và hoàn đúng phần đã trả. Server đã mở lại.
- Shopping browser regression: 18/18 qua sau thay đổi popup chung. Shared UI và diff --check qua; chưa push.
