# UC ORD 06 Thanh toán VNPAY

Cập nhật 24/10/2026, integration working tree. Actor User/Vendor; owner order-payment. Có code và test, **chưa merchant sandbox thật**. Không dùng bản ghi hoãn cũ để nói source hiện không có adapter; cũng không coi test IPN giả lập là đã thanh toán cổng thật.

## Mục tiêu và tiền điều kiện

Thanh toán tổng đơn qua cổng, chỉ ghi nhận tiền từ IPN đã xác minh. Cần người mua active/verified, giỏ hợp lệ, địa chỉ thuộc mình, shop/carrier/policy/voucher/stock đủ điều kiện, merchant và public callback hợp lệ. Khi thiếu merchant, option không hiện và server từ chối method VNPAY.

## Hướng dẫn xây dựng theo source thực

1. DTO Checkout nhận method VNPAY trong whitelist; CheckoutService khóa nguồn giá/tồn/quota, tạo order và PaymentTransaction với txnRef riêng, amount snapshot và expiresAt 15 phút.
2. VnpaySettings tạo canonical query, HMAC SHA512 và so hash constant time. Không nhận secret hoặc số tiền quyết định từ client.
3. VnpayService.start kiểm chủ đơn/trạng thái/hạn, trả URL cổng allowlist. Controller POST /orders/{id}/vnpay kiểm JWT/CSRF và lấy IP từ server request.
4. VnpayController.ipn nhận GET public, chặn duplicate parameter, giới hạn trường. Service kiểm chữ ký/merchant trước DB, kiểm amount/ref, khóa order rồi payment, xác nhận thành công khi cả hai status 00.
5. Ghi một lần, lưu callback hash/outcome và phát event sau commit. Return chỉ redirect về lịch sử; không đổi payment bằng redirect của trình duyệt.
6. Callback lặp trả ACK 02; lỗi chữ ký 97, amount 04, ref không có 01. REVIEW khóa việc trả lại/hủy tự động đến khi có kết quả cuối hoặc đối soát merchant; failure cuối cho hủy/giải phóng nguồn lực.
7. Thành công đến sau hủy vẫn CANCELLED/REFUND_PENDING. Admin thực hiện hoàn qua merchant rồi lưu bằng chứng; chưa có API query/refund tự động.

## Source và minh chứng

- Service: src/main/java/com/utetra/order/service/VnpaySettings.java, VnpayService.java, CheckoutService.java, OrderWorkflowService.java.
- Controller: order/web/VnpayController.java; JSP order/checkout.jsp và detail.jsp trong layout chung.
- Bảng thực: orders, payment_transactions, payment_callback_events, refund_transactions; các bảng tồn/quota/ledger liên quan cùng transaction.
- Test: VnpaySettingsTest; OrderCommerceIntegrationTest.vnpayChecksSignatureAmountAndDuplicateIpnBeforeMarkingPaid; lateVnpayIpnDoesNotReopenCancelledOrderAndAdminCanRecordActualRefund; vnpayBrowserReturnCannotMarkPaidAndIpnRejectsDuplicatedParameters; vnpayReviewBlocksNewPaymentAndCancellationUntilDefinitiveIpn; vnpayExpiredLinkIsRejectedAndSignedFailureCanBeCancelledToReleaseStock.
- [Setup và giới hạn](../VNPAY.md); [activity](../../../report-assets/activity-vnpay.png); [source có diễn giải](../../../report-assets/code-vnpay-ipn.png).
- [UI chờ thanh toán H2](../../../report-assets/acceptance/vnpay-pending-desktop.png); [UI sau IPN giả lập](../../../report-assets/acceptance/vnpay-confirmed-desktop.png). Chưa có ảnh giao dịch merchant thật.

## Phần nhóm cần hoàn thiện khi nộp

Ghi tên/MSSV/reviewer/commit, đối chiếu từng lỗi, thực hiện sandbox với merchant thật và gắn prompt/result; đặt caption/nguồn hình trong DOCX. Không chụp secret, JWT hoặc thông tin thẻ.
