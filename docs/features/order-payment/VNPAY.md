# VNPAY trong bản tích hợp UTETra

Cập nhật 24/10/2026. **Có code và test giao thức; chưa nghiệm thu merchant sandbox thật.** Cấu hình thật vẫn `payment.vnpay.enabled=false`, không giả rằng đã có ví/cổng thanh toán hoạt động chỉ vì đã có QR Techcombank.

## Nguồn kỹ thuật

Đối chiếu [tài liệu PAY chính thức của VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html): yêu cầu thanh toán được ký; IPN là thông báo server tới server; merchant kiểm checksum, đơn, số tiền và trạng thái rồi trả ACK. Return trên trình duyệt không phải căn cứ duy nhất để cập nhật tiền. Phần triển khai dùng phiên bản 2.1.0/HMAC SHA512 và chỉ xác nhận thành công khi cả response code và transaction status đều 00.

## Cấu hình để thử sandbox

Điền thông tin **merchant sandbox của nhóm** vào properties. Không copy giá trị từ unit test hoặc tài liệu của người khác.

```properties
payment.vnpay.enabled=true
payment.vnpay.tmn-code=<merchant sandbox>
payment.vnpay.hash-secret=<merchant secret>
payment.vnpay.payment-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
payment.vnpay.return-url=https://<public-host>/payments/vnpay/return
payment.vnpay.ipn-url=https://<public-host>/payments/vnpay/ipn
```

Đăng ký public IPN/Return URL với merchant; chỉ điền property IPN không tự đăng ký endpoint lên VNPAY. Máy chạy localhost chưa có public HTTPS thì VNPAY không thể gửi IPN vào đó. Không tắt CSRF toàn ứng dụng; chỉ hai route GET callback công khai, còn tạo thanh toán/checkout vẫn kiểm JWT, chủ đơn và CSRF.

`VnpaySettings.available()` kiểm cờ, merchant/key, callback và gateway allowlist. Thiếu cấu hình sẽ không hiện tùy chọn VNPAY; client ép method cũng bị từ chối trước tạo order. COD/QR thủ công vẫn dùng được.

## Luồng và source

1. User/Vendor chọn VNPAY ở checkout, lấy báo giá rồi tạo đơn. Method nằm trong quote hash; order NEW/UNPAID, payment PENDING, txnRef ngẫu nhiên riêng, hết hạn URL sau 15 phút. Tồn/voucher/điểm giữ trong transaction như COD.
2. Chi tiết đơn có nút tiếp tục. `POST /orders/{id}/vnpay` kiểm chủ đơn/trạng thái/hạn dùng rồi trả URL ký, không tự ghi PAID. Không lấy tiền từ request của khách; dùng snapshot server.
3. `GET /payments/vnpay/ipn` từ provider: chặn query trùng trường/quá dài; kiểm chữ ký và merchant trước đọc DB. Kiểm ref, amount nhân 100, khóa order rồi payment, đối chiếu số tiền, receipt unique và status.
4. Callback hợp lệ thành công ghi payment SUCCEEDED và order PAID một lần, lưu hash/outcome vào `payment_callback_events`; callback lặp trả 02. Amount sai trả 04, checksum sai 97, đơn không có 01. Lỗi DB trả 99 để provider có thể retry; không lộ dữ liệu lỗi SQL.
5. Return chỉ redirect về lịch sử đơn; sửa `vnp_ResponseCode` ở browser không được ghi tiền. Socket/history/notification theo dữ liệu đã commit.
6. Failure cuối ghi FAILED; khách hủy đơn để trả tồn/quota trước khi đặt lại. Trạng thái nghi ngờ/chưa hoàn tất ghi REVIEW, chặn trả lại/hủy tự động đến khi có kết quả cuối hoặc đối soát merchant; không hướng dẫn khách trả thêm lần nữa.
7. IPN thành công đến sau khi hủy giữ CANCELLED và ghi REFUND_PENDING. Không tự mở lại đơn hoặc cộng điểm giao hàng.
8. Hoàn VNPAY hiện là **Admin thực hiện hoàn qua công cụ merchant rồi ghi bằng chứng thật vào endpoint refund**. Có ledger một lần, đảo điểm/settlement theo nghiệp vụ. Chưa có API query/refund VNPAY tự động trong ứng dụng.

Source: `VnpaySettings`, `VnpayService`, `VnpayController`, `CheckoutService`, `OrderWorkflowService`; JSP checkout/detail; `SecurityConfig` chỉ công khai callback đúng route. Không thêm layout/provider secret vào Markdown.

## Minh chứng và giới hạn

- Unit test có vector HMAC tính độc lập; gateway không tin cậy/cờ disabled bị chặn.
- Integration kiểm chữ ký sai, amount sai, ref/receipt, duplicate IPN, return không ghi PAID, tham số lặp, REVIEW, hết hạn link, hủy/hoàn và IPN đến sau hủy.
- Test dùng merchant/key giả trong H2. Browser mô phỏng IPN ký hợp lệ cũng chỉ là kiểm ứng dụng, không chứng minh cổng thật đã nhận tiền.
- Cần nhóm cấp merchant sandbox, public callback và chạy các case provider thật trước khi đánh dấu UC-ORD-06 nghiệm thu. Không cần tích hợp thêm MOMO nếu đã chọn và hoàn thiện VNPAY theo yêu cầu.

Ảnh kiến trúc/activity/source có chú giải trong [report-assets](../../report-assets/manifest.json); hình UI acceptance dùng H2 và được ghi rõ môi trường.
