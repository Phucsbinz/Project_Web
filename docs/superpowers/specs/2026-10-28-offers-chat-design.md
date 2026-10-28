# Ví ưu đãi và chat nổi

Thanh yêu cầu Ưu đãi trong menu tài khoản, popup chia món/phí giao, lưu voucher sở hữu và quà thành viên; bỏ link CSKH khỏi dropdown, thay bằng chat nổi 1:1 với admin qua các phiên riêng. Thanh duyệt mốc đề xuất và yêu cầu áp dụng toàn bộ thanh toán.

## Cách thực hiện
Tái sử dụng promotions/quote/redemption và support service có quyền riêng. Không thêm bộ khuyến mãi hoặc chat tách rời. Ví voucher lưu ownership trên DB; chương trình thường có thể lưu, quà có ownership bắt buộc. So với chỉ lưu localStorage, DB giữ được giữa thiết bị và backend chặn dùng quà của người khác. Chat nổi dùng API hỗ trợ hiện có; admin dùng hộp thư hiện tại, không giả AI.

## Mốc đã duyệt
Bạc 100 điểm: 10% món, tối đa 30k; Vàng 500 điểm: 15%, tối đa 50k. Mỗi 5 đơn DELIVERED+PAID tặng giảm 100% phí giao, tối đa 15k. Quà một lượt, 30 ngày từ khi cấp; mỗi mốc chỉ cấp một lần. Không cộng voucher món với ưu đãi nhóm. Đồng bộ dữ liệu lịch sử khi mở ví và cấp tự động sau EARN. Hoàn/hủy không đếm vào mốc mới; quà đã cấp không cấp lại khi giảm/rồi đạt lại mốc.

## Ràng buộc
Một layout, Bootstrap hiện tại, CSS scope module. Theo chốt mới: chỉ COD và QR Cloudflare cho đơn mới, 10 phút; backend allowlist chặn provider cũ ngoài UI, giữ lịch sử để không mất chứng từ. Mọi checkout dùng cùng quote/reserve; nhóm áp dụng voucher của người đặt, không tự lấy voucher các thành viên. Tin chat text-only escape, lịch sử DB, CSRF, không lộ hội thoại tài khoản khác. Không tự push.

## Kết quả cần kiểm
Ownership/quota/expiry/race/idempotency, voucher ở checkout cá nhân/nhóm; popup desktop/mobile, customer/admin khác browser contexts, quyền admin/customer khác nhau, không ảnh hưởng hóa đơn và thanh toán đã có.
