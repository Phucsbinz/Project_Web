# Nhận diện nhóm, hủy và hết hạn — 27/10/2026

## Yêu cầu Thanh
Chủ nhóm được hủy nhóm và thành viên nhận popup; nhóm hết hạn cũng báo mọi người; nhiều nhóm phải phân biệt được; QR cá nhân dùng cùng luồng, giao diện không có nhãn demo/thử nghiệm; đánh dấu Chủ nhóm cạnh tên.

## Hành vi
- Mỗi nhóm có mã UTG-xxxxx, tên chủ nhóm, số thành viên, thời điểm tạo và trạng thái trên thẻ Nhóm của bạn; giỏ hàng cũng hiện mã/tên/trạng thái thay Mở nhóm chung chung. Chủ nhóm có thể tạo tối đa 5 nhóm đang hoạt động; chỉ một nhóm được đưa vào giỏ chốt tại một thời điểm.
- Danh sách thành viên có badge Chủ nhóm sát tên người tạo, và badge Bạn cho tài khoản hiện tại.
- Chỉ host được hủy. OPEN/LOCKED có thể hủy; cart snapshot của chính nhóm được dọn để không checkout nhầm sau hủy. Giỏ của nhóm khác/cá nhân và các thành viên không bị xóa.
- Nếu nhóm đã lên đơn NEW, host hủy theo OrderWorkflow hiện có, hủy cả đơn và chặn mọi thanh toán tiếp theo. Nếu cửa hàng đã CONFIRMED thì host không được hủy vượt quy tắc đơn hàng. Trường hợp đã thanh toán dùng quy trình hoàn hiện có, không tự coi là đã hoàn.
- Nhóm chưa đặt hết hạn sau 24 giờ; giỏ nhóm đã chốt hết hạn sau 1 giờ không hoạt động như trước. Nhóm ORDERED không bị biến thành hết hạn chỉ vì đã qua thời gian draft.
- Trạng thái EXPIRED được tính ngay khi đọc và job mỗi 60 giây ghi lại/dọn draft. CANCELLED do host và EXPIRED được phân biệt, không còn dùng chung một trạng thái khi giỏ hết hạn.
- /group-orders/lifecycle-notices trả sự kiện hủy/hết hạn cho thành viên hiện tại, bao gồm host. Popup có tên/mã cụ thể, kiểm 8 giây khi trang đang hiện và ngay khi quay lại tab; ghi nhận đã xem theo tài khoản/nhóm/trạng thái để không lặp liên tục. Nếu offline/đóng trang thì lần mở lại mới nhận được.
- Thông báo hủy/hết hạn vẫn nhận trên trang QR đang mở. Public phone không đăng nhập theo dõi trạng thái giao dịch; nếu bị hủy giữa chừng thì cập nhật và báo rõ không thể thanh toán. Các popup không chồng lên nhau.
- Thanh toán cá nhân và nhóm đều dùng cùng LOCAL_QR đã có; nhãn hiển thị Thanh toán QR, nút Thanh toán. Bỏ các từ demo/thử nghiệm/giả lập trong các màn thanh toán mới, vẫn giữ thông tin đúng: thanh toán nội bộ, không trừ tiền ngân hàng. Không thêm số dư hay tính năng nạp/rút mang tên Ví UTETra.

## Kiểm thực
- 81 OrderCommerceIntegrationTest qua, có 6 ca mới: nhiều nhóm có metadata khác nhau; host-only cancel và đúng recipients; OPEN hết hạn và chặn chọn/join; LOCKED/cart hết hạn; hủy đơn NEW và chặn thanh toán; không hủy sau cửa hàng xác nhận.
- Full verify trước cập nhật kỳ vọng còn 1 test giữ hợp đồng cũ giữ món sau hủy; đã sửa theo yêu cầu mới dọn draft nhóm và chạy lại 81 tests qua, build WAR thành công. Các module khác đã qua trong full run; không khẳng định một full run mới hơn khi chưa chạy lại toàn bộ.
- 16 shopping browser scenarios cuối qua, 0 JS errors. Kiểm nhóm có mã/host/time/status riêng, badge Chủ nhóm, popup hủy tới member ngay trên trang QR, đồng hồ H2 thực advance 25 giờ rồi login lại nhận popup hết hạn, chặn menu ở nhóm EXPIRED. QR cá nhân/nhóm, COD, auto bill, địa chỉ, đủ 3/3 mới xác nhận vẫn qua.
- Clock control chỉ nằm tools/ui/AllUiServer.java, có profile test và guard đúng jdbc:h2:mem:utetra_ui_full, không đóng gói trong WAR; không có endpoint chỉnh giờ trong ứng dụng thật.
- Shared UI và git diff --check qua. Không reset DB, không đặt đơn cloud/không chuyển tiền thật trong kiểm thử. Không migration mới. Chưa commit/push.


## 2026-10-27 — Buộc rời nhóm đã đóng
Prompt Thanh: đã hủy nhóm nhưng thành viên vẫn ở được trong nhóm.
Nguyên nhân: group view trước đó vẫn hiển thị danh sách thành viên ở trạng thái readonly và popup Tiếp tục lại mở chính nhóm đã đóng.
Sửa: view terminal CANCELLED/EXPIRED trả joined=false, memberCount=0/members rỗng; giữ snapshot DB chỉ phục vụ lịch sử và notification. Nhóm terminal không còn trong mine/list Nhóm của bạn. HTML GET link cũ redirect /group-orders/new; host hủy cũng về danh sách. Poll trang nhóm và lifecycle kiểm trạng thái đóng, đẩy khỏi cả /group-orders/token và /menu?group=token; popup được hiện trên trang danh sách và Tiếp tục không quay lại nhóm đóng. Sự kiện hủy/hết hạn vẫn lấy từ lịch sử riêng nên không mất thông báo khi bỏ khỏi list active. Không xóa dữ liệu giao dịch/đơn hàng.
Kiểm thực: 82 commerce tests qua, WAR build thành công; 16 shopping browser scenarios qua, gồm member đang trong nhóm khi host hủy tự ra danh sách, popup đúng mã nhóm, thử link cũ vẫn bị chuyển ra; expiry tương tự. Shared UI/diff check qua. Server mở lại; chưa commit/push.
