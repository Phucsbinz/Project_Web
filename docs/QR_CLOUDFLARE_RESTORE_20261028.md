# Khôi phục QR Internet và rà source mới — 28/10/2026

## Cập nhật
Đã fetch và fast-forward integration/utetra từ 57c6b88 lên 77d27ba. Đọc log cập nhật: 269ea2c hoàn thiện Shipper; 21d2d64 catalog; 7c7ea82/27eaa83 homepage; 9f17146 bộ skills và audit; bbd9182/3b15ca9/77d27ba ghép source. Giữ UI và nghiệp vụ mới, không restore cả bundle từ bản cũ.

## Nguyên nhân QR
LocalQrPaymentService và tools/qr_tunnel.py không bị xóa trong các commit. start_web.py trước đó chỉ mở Java, không mở tunnel. Khi thiếu metadata tunnel đang sống, walletOrigin() trả localhost. Đây là fallback/khởi động thiếu, không phải mất luồng Cloudflare.

## Sửa
- QrPublicOrigin chặn HTTP/loopback trong chế độ chạy thật. Chỉ chấp nhận HTTPS public origin hoặc Quick Tunnel hợp lệ. Môi trường test cô lập được phép loopback để không đưa dữ liệu H2 ra Internet.
- Khi Cloudflare chưa sẵn sàng: không phát hành QR localhost; QR bị ẩn khỏi phương thức sẵn sàng và các request tạo QR báo chưa sẵn sàng. Không tạo đơn QR mới trong tình trạng này.
- start_web.py gọi qr_tunnel.py ensure. ensure dùng lại gateway đang chạy hoặc khởi tạo, xử lý metadata cũ; gateway chỉ công khai wallet và assets whitelist, chặn admin/auth/trang chủ.
- Máy chưa có cloudflared: tải binary chính thức từ release GitHub của Cloudflare, yêu cầu digest SHA256 và kiểm đúng trước khi lưu. Binary/URL/metadata/key đều bỏ qua Git.
- Không xóa ledger LOCAL_QR/token/đơn cũ: đây là mã provider nội bộ đã dùng trong lịch sử; Cloudflare là đường truy cập Internet, không phải ngân hàng mới. Không còn lựa chọn QR localhost cho người dùng thật. Giữ cơ chế nội bộ đã được Thanh chọn, không tự đổi sang tiền ngân hàng thật.

## Rà theo cuộc trò chuyện
| Yêu cầu | Bằng chứng source / kiểm tra |
|---|---|
| Profile/avatar preview, security, OTP | Identity services/views và integration tests hiện hành; không đọc/ghi khóa mail |
| Giỏ checkbox/badge, hạn 1 giờ, nhắc hạn | CartService/cart.js; shopping UI |
| Join nhóm trước chọn món, host/chia phần, mốc 3/5/10 cap100k | GroupOrderService + commerce tests |
| Chọn chi nhánh lúc checkout, editable contact, báo giá | checkout.jsp/checkout-fields.js + shopping UI |
| COD host trả, QR từng người, phải đủ tiền mới xác nhận | CheckoutService/LocalQrPaymentService + commerce tests |
| QR 10 phút, countdown, quay về nhóm/progress | LocalQrPaymentService/local-qr.js + shopping UI |
| Thành viên theo dõi đơn, quyền owner riêng | OrderWorkflowService + commerce/shopping tests |
| Hủy/hoàn một phần, popup 30 phút, confirm chủ nhóm | GroupOrderService/workflow/site-ui + partial UI |
| Admin dễ thao tác, chevron center toàn web | catalog-management/utetra.css và audit hiện có |
| Catalog/ảnh và homepage mới, Shipper mới | Giữ nguyên source từ commit mới; không lùi về UI hoặc flag Shipper cũ |

## Skill setup
Có 15 SKILL.md trong .agents/skills, kiểm frontmatter/name/description/hash bằng tools/check_agent_skills.py; inventory ở .local/agent-skills-check.json. AGENTS.md chỉ dẫn cách sử dụng theo tác vụ. Không sao chép đè skill hoặc config cá nhân. Các skill workflow không được chạy đồng loạt hay làm thay đổi stack/đưa ra xác nhận thừa trái yêu cầu người dùng.

## Vận hành
`python tools/start_web.py` mở web và chuẩn bị Cloudflare cho cổng 8080. `python tools/qr_tunnel.py status` xem tình trạng. Cổng khác cần cấu hình public HTTPS origin tương ứng. Quick Tunnel cần server/tunnel hoạt động, URL có thể đổi khi khởi động lại. Không công khai toàn bộ cổng 8080. Không push trong yêu cầu này.

## Kiểm chứng
Full verify: 266 tests, không failure/error, WAR thành công. Test chính sách mới bắt việc fallback và từ chối URL local/insecure; test cô lập giữ loopback. Gateway scope tests và skill validator qua. Browser/public URL kiểm tiếp bên dưới.

- Browser H2/Tomcat/Edge mới: shopping 18/18, partial group 4/4, Shipper 4/4 qua.
- Cloudflare HTTPS thực: /local-wallet/health và wallet JSP trả 200; /admin, /auth/login, / trả 404. Không tạo thanh toán/đơn thật khi kiểm tunnel.
- Final package thành công; đã kiểm 15 skill, gateway scope và shared UI. Source mới được giữ tại integration/utetra, chưa commit/push bản sửa này.
