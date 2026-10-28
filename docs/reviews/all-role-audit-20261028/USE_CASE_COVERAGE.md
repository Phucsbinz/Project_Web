# Phạm vi use case trong đợt audit 28/10/2026

Đối chiếu 58 use case từ USE_CASES.md. Mỗi dòng dưới xác định phạm vi đã đọc; không có nghĩa từng use case đã được chạy E2E riêng. Chi tiết bằng chứng và giới hạn nằm trong ALL_ROLE_AUDIT_20261028.md.

## UC-AUTH-01 — Đăng ký tài khoản
- Vai trò: Guest. Contract: Tạo User chưa xác minh, BCrypt và gửi OTP email
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-02 — Kích hoạt bằng OTP
- Vai trò: Guest. Contract: OTP đúng/hạn/một lần mới ghi email_verified_at
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-03 — Đăng nhập
- Vai trò: Guest. Contract: Chỉ tài khoản hoạt động đã xác minh; tạo phiên và JWT
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-04 — Đăng xuất
- Vai trò: Người đăng nhập. Contract: Thu hồi phiên, xóa cookie đúng path, ngắt socket
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-05 — Quên mật khẩu gửi OTP
- Vai trò: Guest. Contract: Phản hồi chung, OTP reset riêng mục đích, giới hạn gửi
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-06 — Đặt lại mật khẩu bằng OTP
- Vai trò: Guest. Contract: Xác minh rồi đổi BCrypt, dùng challenge một lần, thu hồi mọi phiên
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-AUTH-07 — Làm mới phiên
- Vai trò: Người đăng nhập. Contract: Khóa và xoay refresh hash, không kéo dài hạn tuyệt đối
- Phạm vi đọc source: AuthService, OtpService, SecurityConfig, JwtService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, BaselineSecurityTest, JwtServiceTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-01 — Trang chủ Guest
- Vai trò: Guest. Contract: Sản phẩm bán >10 ly, giảm dần số bán; giả định cần thầy xác nhận
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-02 — Tìm kiếm và lọc
- Vai trò: Mọi người. Contract: Tên, danh mục, shop, giá; sort allowlist, phân trang
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-03 — Trang chủ User và top 20
- Vai trò: User, Vendor. Contract: Bốn nhóm mới/bán chạy/đánh giá/yêu thích, mỗi nhóm tối đa 20 và phân trang
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-04 — Sản phẩm theo danh mục
- Vai trò: Mọi người. Contract: Chỉ món/shop đang hoạt động; filter, paging và empty state
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-05 — Chi tiết và tùy biến trà sữa
- Vai trò: Mọi người. Contract: Size S/M/L, đường/đá, topping hợp lệ, ảnh và giá dự kiến
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-CAT-06 — Xem/chọn chi nhánh và trang shop
- Vai trò: Mọi người. Contract: Chọn thủ công; thông tin, khu vực/phí; không tính khoảng cách
- Phạm vi đọc source: CatalogService, CatalogQueryRepository, StoreService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, CatalogViewTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-01 — Profile, avatar và đổi mật khẩu
- Vai trò: Người đăng nhập. Contract: Chỉ hồ sơ mình; Cloudinary; đổi mật khẩu thu hồi phiên
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-02 — Nhiều địa chỉ nhận hàng
- Vai trò: User, Vendor. Contract: CRUD của mình, tối đa một mặc định, snapshot đơn không đổi
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-03 — Thích/bỏ thích sản phẩm
- Vai trò: User, Vendor. Contract: Unique user/product, danh sách yêu thích và số thích đúng
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-04 — Sản phẩm đã xem
- Vai trò: User, Vendor. Contract: Upsert lần xem, danh sách gần nhất của mình
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-05 — Đánh giá sản phẩm đã mua
- Vai trò: User, Vendor. Contract: Đơn đã giao và trả tiền, sao 1–5, không đánh giá trùng order_item
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-06 — Bình luận text, ảnh/video
- Vai trò: User, Vendor. Contract: Đã mua; text 50–2000 ký tự sau trim; media kiểm nội dung/kích thước
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-USER-07 — Điểm và hạng thành viên
- Vai trò: User, Vendor. Contract: Ledger trừ/hoàn/cộng/thu hồi điểm đúng một lần, hạng và số dư nhất quán
- Phạm vi đọc source: AccountService, CommunityService, LoyaltyService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-01 — Thêm món vào giỏ
- Vai trò: Guest, User, Vendor. Contract: Validate tùy chọn, gộp đúng cấu hình, topping theo mỗi ly
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-02 — Sửa/xóa/hợp nhất giỏ
- Vai trò: Guest, User, Vendor. Contract: Giỏ database cho người đăng nhập, session cho Guest; merge chống lặp
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-03 — Checkout
- Vai trò: User, Vendor. Contract: Store/area/carrier/address/quote, server tính giá, snapshot và idempotency
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-04 — Chọn mã giảm giá
- Vai trò: User, Vendor. Contract: Scope shop/sản phẩm/thời gian/hạn mức; khóa quota tại checkout
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-05 — Thanh toán COD
- Vai trò: User, Vendor. Contract: Khách xác nhận tiền; chỉ người có quyền ghi thu tiền khi giao thành công
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-06 — Thanh toán VNPAY
- Vai trò: User, Vendor. Contract: Sandbox URL ký, IPN xác thực số tiền/ref/signature, callback idempotent
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-07 — Lịch sử và theo dõi đơn
- Vai trò: User, Vendor. Contract: Đơn mình theo mới/xác nhận/lấy hàng/giao/đã giao/hủy/trả/hoàn
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-08 — Hủy đơn
- Vai trò: User, Vendor. Contract: Chỉ NEW cho khách; hoàn điểm, tạo refund nếu đã trả, không refund lặp
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-09 — Yêu cầu trả hàng–hoàn tiền
- Vai trò: User, Vendor. Contract: Đơn đã giao, cửa sổ cấu hình, lý do/chứng cứ, theo dõi kết quả thật
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ORD-10 — Realtime và hộp thông báo
- Vai trò: Người đăng nhập. Contract: Lưu người nhận riêng, after commit, quyền subscribe, reconnect/polling
- Phạm vi đọc source: CartService, CheckoutService, OrderWorkflowService, Payment/Return/Notification services.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest, OrderSocketGuardTest, OrderEventDeliveryTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-01 — Đăng ký shop
- Vai trò: User, Vendor. Contract: Hồ sơ chờ duyệt; duyệt mới hoạt động và cấp quyền Vendor
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-02 — Quản lý trang chủ shop
- Vai trò: Vendor. Contract: Profile/banner/giờ/nhận đơn của shop mình, ảnh Cloudinary
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-03 — Quản lý sản phẩm của mình
- Vai trò: Vendor. Contract: CRUD món/size/topping/availability đúng shop, không xóa lịch sử đã bán
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-04 — Quản lý đơn shop
- Vai trò: Vendor. Contract: Shop mình, chuyển trạng thái hợp lệ/version, có bước PICKED_UP
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-05 — Tạo khuyến mãi shop
- Vai trò: Vendor. Contract: Giảm sản phẩm và mã giảm giá của shop mình, không sửa chính sách toàn app
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-06 — Doanh thu shop
- Vai trò: Vendor. Contract: Hàng hóa/phí/hoa hồng/hoàn/net tách riêng, filter shop/date
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-VND-07 — Xử lý yêu cầu trả hàng
- Vai trò: Vendor. Contract: Chấp nhận/từ chối có lý do theo shop; refund qua dịch vụ chung
- Phạm vi đọc source: StoreService, CatalogService, PromotionService, OrderWorkflowService, OperationsService.
- Bộ test liên quan đã chạy: CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-01 — Tìm/quản lý user
- Vai trò: Manager. Contract: Phạm vi user liên quan shop được giao, không tự cấp Admin
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-02 — Quản lý sản phẩm shop
- Vai trò: Manager. Contract: Shop được phân công, validation và audit
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-03 — Quản lý danh mục/topping
- Vai trò: Manager. Contract: Theo phân quyền quản trị catalog được giao, không sửa ngoài phạm vi
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-04 — Chiết khấu app cho shop
- Vai trò: Manager. Contract: Tỷ lệ/policy shop được giao, không đổi snapshot đơn cũ
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-05 — Quản lý khuyến mãi
- Vai trò: Manager. Contract: Giảm % món/giảm phí giao trong shop được giao
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-06 — Nhà vận chuyển và phí
- Vai trò: Manager. Contract: Tên/phí và biểu phí khu vực trong phạm vi được cấp, không tính kilomet
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-07 — Quản lý cửa hàng
- Vai trò: Manager. Contract: Thông tin/trạng thái cửa hàng đã phân công; duyệt hồ sơ trong phạm vi
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-08 — Đơn, trả hàng và hoàn tiền
- Vai trò: Manager. Contract: Kiểm quyền shop, transition/transaction/refund/audit
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-MGR-09 — Báo cáo cửa hàng
- Vai trò: Manager. Contract: Doanh thu/net/hoàn theo phạm vi shop được giao
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-01 — Tìm/quản lý user và phân quyền
- Vai trò: Admin. Contract: Toàn hệ thống, khóa/revoke, phân công Manager, giữ Admin cuối cùng
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-02 — Quản lý sản phẩm các shop
- Vai trò: Admin. Contract: Toàn chuỗi, duyệt/ngừng bán, không thay snapshot cũ
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-03 — Quản lý danh mục/topping
- Vai trò: Admin. Contract: CRUD chung, unique/validation và quy tắc tham chiếu
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-04 — Chiết khấu app cho shop
- Vai trò: Admin. Contract: Policy hoa hồng toàn app/shop, tỷ lệ snapshot và ledger doanh thu
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-05 — Quản lý chương trình khuyến mãi
- Vai trò: Admin. Contract: Giảm % sản phẩm, giảm phí vận chuyển, mã/quota/scope/thời hạn
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-06 — Nhà vận chuyển/phí
- Vai trò: Admin. Contract: CRUD carrier/rate toàn hệ thống; audit và giữ phí đơn đã chốt
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-07 — Duyệt/quản lý shop
- Vai trò: Admin. Contract: Duyệt đăng ký, khóa/mở shop, owner và scope Manager
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-08 — Đơn/giao dịch/trả–hoàn
- Vai trò: Admin. Contract: Tra cứu, đối soát, refund có chứng cứ, không giả PAID/REFUNDED
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-ADM-09 — Báo cáo toàn hệ thống
- Vai trò: Admin. Contract: Giá trị thu, hoàn, hoa hồng và shop net đối chiếu được
- Phạm vi đọc source: UserManagementService, CatalogGuard, StoreService, PromotionService, OperationsService.
- Bộ test liên quan đã chạy: IdentityIntegrationTest, CatalogIntegrationTest, OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-SHP-01 — Đơn giao được phân công
- Vai trò: Shipper. Contract: Phần thêm, chỉ assignment còn hiệu lực
- Phạm vi đọc source: OrderWorkflowService, OperationsService, OrderSupport.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-SHP-02 — Cập nhật giao hàng
- Vai trò: Shipper. Contract: Phần thêm, lấy/giao/thất bại/COD theo assignment, không tự refund
- Phạm vi đọc source: OrderWorkflowService, OperationsService, OrderSupport.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## UC-SHP-03 — Thống kê giao hàng
- Vai trò: Shipper. Contract: Phần thêm, thống kê đúng đơn giao cho mình
- Phạm vi đọc source: OrderWorkflowService, OperationsService, OrderSupport.
- Bộ test liên quan đã chạy: OrderCommerceIntegrationTest; đối chiếu tên ca trong source test, không mặc định mỗi nhánh logic có test.
- Native UI: role liên quan có kiểm render trong ui-verification.json; mutation/provider không suy ra từ render.

## Phần mở rộng ngoài danh sách UC gốc
- Đơn nhóm HOST/INDIVIDUAL, LOCAL_QR, chuyển khoản: đọc service và checkout UI; probe chủ nhóm không có món xác nhận lỗi mới.
- CSKH: đọc SupportService, API/socket guards và persistence; SupportIntegrationTest chạy trong Maven; UI User/Manager/Admin đã render.
- JWT lưu dấu token/phiên DB, refresh/revoke: kiểm security/identity và tests; không ghi giá trị token vào tài liệu.
