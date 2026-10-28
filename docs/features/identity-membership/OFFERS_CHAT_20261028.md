# Ví ưu đãi và chat hỗ trợ — 28/10/2026

## Prompt và quyết định
Thanh yêu cầu thêm Ưu đãi vào menu tài khoản, popup chia ưu đãi món/giao hàng, giữ voucher sở hữu và tự tặng theo mốc thành viên/mua hàng. Bỏ mục CSKH trong dropdown, thay bằng chat nổi góc phải, khách–admin nhắn qua phiên/cookie độc lập. Thanh duyệt các mốc đề xuất và xác nhận toàn bộ checkout áp dụng; sau đó chốt chỉ dùng COD và QR Cloudflare, gỡ lựa chọn ngân hàng/VNPAY.

## Thực hiện
- `promotion_wallet` lưu sở hữu/mốc, unique user+promotion và user+source. Lưu ưu đãi chung idempotent; quà tạo chương trình riêng, một lượt, 30 ngày, ownership bắt buộc phía backend.
- Bạc (100 lifetime points): giảm món 10% tối đa 30k. Vàng (500): 15% tối đa 50k. Mỗi 5 đơn DELIVERED+PAID: giảm 100% phí giao, tối đa 15k. Mỗi mốc một lần; đơn hủy/hoàn không tăng số đơn hợp lệ. Quà đã cấp không nhân lại khi rớt/rồi đạt lại mốc.
- Cấp quà sau commit giao hàng, độc lập với số điểm kiếm được; đối chiếu cả chủ đơn và mọi thành viên có phần món. Khóa user theo thứ tự; có lỗi cấp quà không làm giao hàng đã commit báo thất bại, mở ví sẽ đối chiếu lại idempotent. Đã bỏ hook EARN đồng bộ để lỗi cấp quà không rollback giao hàng. Không có quà cho giao dịch rollback.
- Ví có tìm tên/mã, hai loại, mặc định voucher sở hữu; bỏ lọc để xem/lưu chương trình đang có. Hiện phạm vi chi nhánh/toàn hệ thống, mức giảm, tối thiểu, hạn, đã dùng/hết hạn/tạm ngừng.
- Toàn bộ checkout cá nhân/nhóm và COD/QR dùng quote/reserve chung. Vouchers, choices và availability lọc theo chủ sở hữu; catalog công khai loại quà riêng. Nhóm dùng voucher của người đặt; giữ quy tắc không cộng giảm món với ưu đãi nhóm.
- Checkout chỉ còn radio COD và QR Cloudflare. Allowlist backend `order.payment-methods=COD,LOCAL_QR` chặn cả quote/checkout gửi tay phương thức cũ, độc lập cờ cấu hình provider. Không thêm nút ngân hàng/VNPAY vào hóa đơn. Dữ liệu/provider/migration lịch sử vẫn giữ để không làm mất đơn và chứng từ cũ; hai provider cũ đang tắt trong cấu hình chạy thật. Chỉ profile kiểm thử khai báo rõ provider cũ để kiểm regression chứng từ lịch sử.
- Chat nổi kết nối API Support hiện có, không AI và không lưu nội dung bằng cookie/localStorage. Tin lưu DB; admin đọc/trả lời ở `/admin/support`. CSRF, session, ownership và kiểm tra nội dung/retry vẫn do service cũ kiểm soát. Poll 3s, có tin mới, mở lại/đổi trang/Back vẫn đọc được, tải thêm lịch sử. Không tạo widget bất hoạt trên trang chat đầy đủ cũ.
- Dùng theme chung và một SiteMesh; module CSS scope account-offers/support-widget. Fragment JSPF khai báo UTF-8 rõ để không lỗi tiếng Việt; chat ẩn khi in hóa đơn.

## Cách thử
1. Khách mở menu tên tài khoản → Ưu đãi của tôi. Chọn loại, tìm mã, bật/tắt bộ lọc sở hữu; lưu mã đang có rồi chọn tại checkout.
2. Khách mở Chat với UTETra góc phải và gửi tin. Trình duyệt khác hoặc cửa sổ riêng đăng nhập Admin → Chăm sóc khách hàng, chọn khách và phản hồi. Không cần chung cookie, chỉ cần truy cập cùng server/database. Không dùng QR gateway giới hạn wallet để mở chat.
3. Thanh toán cá nhân và nhóm tiếp tục dùng COD/QR, chung hóa đơn/popup kết quả như đợt trước.

## Kiểm tra
Full verify cuối: 278 tests, 0 failures/errors; WAR build thành công. Bao gồm 6 integration cases ví/quà (quà tier, mốc 5 đơn, đơn zero-point, thành viên nhóm, cấp đồng thời, quyền sở hữu), policy COD/QR và các luồng cũ. Browser audit ví/chat qua 3 nhóm kiểm tra với hai cookie context, quyền riêng tư, tìm/lưu voucher, tiếng Việt, 390/1440px, Back và lịch sử; shopping regression 18 tình huống qua. Shared UI và git diff --check qua. Migration ví đã áp dụng thành công trên database hiện hành, không seed/reset. Không commit/push trong đợt này.

Review độc lập đã phát hiện và xử lý: ẩn quà khỏi catalog công khai, cấp quà độc lập EARN/zero-point và cho mọi thành viên có phần món, bỏ hook EARN đồng bộ, khôi phục polling sau Back, không render launcher bất hoạt trên /support, chặn provider cũ phía checkout server. Không thêm phương thức thanh toán mới.
