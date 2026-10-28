# Kiểm kê và phân loại bảng thực tế

Schema `utetra_db`; 51 bảng vật lý gồm Flyway, 50 bảng ứng dụng; 13 bảng trống. Chụp 2026-10-28T10:24:58.196885200Z bằng COUNT(*) từng bảng trong transaction chỉ đọc, không dùng TABLE_ROWS ước lượng. Đây là số trước seed; không xuất dòng dữ liệu cá nhân.

A: danh mục được cân nhắc bổ sung. B: không seed dữ liệu thao tác/sự kiện/hệ thống. C: cần quyết định về tiền/quyền/vận hành, giữ nguyên. User xác nhận môi trường demo và giá demo; không tự tạo phí/quyền/voucher/tồn.

| Tên bảng | Số dòng | Ý nghĩa thực | Nguồn tạo dữ liệu | Có nên seed? | Lý do | Bảng phụ thuộc |
| --- | ---: | --- | --- | --- | --- | --- |
| `audit_logs` | 439 | Nhật ký thao tác thật của ứng dụng | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | users |
| `auth_sessions` | 129 | Phiên và hash refresh/access metadata | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | users |
| `carrier_area_rates` | 3 | Phụ phí nhà vận chuyển theo khu vực | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | carriers, store_delivery_areas |
| `carriers` | 1 | Đơn vị vận chuyển và phí cơ bản | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | stores |
| `cart_item_toppings` | 0 | Topping khách đã chọn trên dòng giỏ | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | cart_items, toppings |
| `cart_items` | 0 | Dòng món/cấu hình do người dùng thêm vào giỏ | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | carts, product_variants |
| `carts` | 10 | Giỏ DB của tài khoản, có thể tồn tại dù không có dòng | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | group_orders, stores, users |
| `categories` | 6 | Danh mục trình bày sản phẩm | Quản trị danh mục/import đã duyệt | A · đủ nhóm, giữ nguyên | Đã có6 nhóm phù hợp, không tạo trùng | Không FK outbound |
| `comment_media` | 0 | Media người dùng tải cùng bình luận | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | product_comments |
| `commission_policies` | 1 | Chính sách tỷ lệ hoa hồng | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | Không FK outbound |
| `coupon_redemptions` | 3 | Giữ/sử dụng/giải phóng quota voucher theo đơn | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, promotions, users |
| `delivery_assignments` | 0 | Phân công giao hàng của đơn thật | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `delivery_events` | 0 | Sự kiện lấy/giao/thất bại từ assignment | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | delivery_assignments, users |
| `flyway_schema_history` | 18 | Lịch sử migration do Flyway quản lý | Flyway tự quản lý | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | Không FK outbound |
| `group_order_members` | 11 | Người tham gia và phần món/tiền nhóm do thao tác thật | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | group_orders, orders, users |
| `group_orders` | 5 | Nhóm đặt hàng và lifecycle từ host | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `local_qr_payments` | 7 | Khoản thanh toán QR giả lập phát sinh từ checkout thật trong demo | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `loyalty_accounts` | 10 | Tài khoản điểm được tạo theo user/ledger | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | users |
| `loyalty_transactions` | 103 | Ledger cộng/trừ/hoàn điểm từ nghiệp vụ | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | loyalty_accounts, orders |
| `notifications` | 1637 | Thông báo riêng được hệ thống sinh sau sự kiện | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `order_item_toppings` | 4 | Snapshot topping/giá trên dòng đơn đã checkout | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | order_items, toppings |
| `order_items` | 150 | Snapshot món/size/giá từ đơn, không phải danh mục | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, product_variants |
| `order_status_history` | 543 | Lịch sử trạng thái/sự kiện đơn | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `orders` | 116 | Đơn do checkout của khách | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | carriers, commission_policies, group_orders, store_delivery_areas, stores, users |
| `otp_challenges` | 7 | OTP/challenge/grant đăng ký hoặc reset | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | users |
| `payment_callback_events` | 0 | Dấu sự kiện callback provider chống replay | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | payment_transactions |
| `payment_transactions` | 116 | Giao dịch thu tiền gắn đơn | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders |
| `product_comments` | 90 | Bình luận của người đã mua | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | order_items, products, users |
| `product_favorites` | 91 | Lượt thích do user thao tác | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | products, users |
| `product_toppings` | 270 | Danh sách topping được phép cho từng món | Quản trị danh mục/import đã duyệt | A · bổ sung chọn lọc | Theo manifest31, không ghi đè dòng cũ | products, toppings |
| `product_variants` | 270 | Size của món, phụ thu và tồn theo size | Quản trị danh mục/import đã duyệt | A · bổ sung chọn lọc | Theo manifest31, không ghi đè dòng cũ | products |
| `product_views` | 105 | Lịch sử xem sản phẩm của user | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | products, users |
| `products` | 90 | Món theo shop, giá cơ sở và khả dụng | Quản trị danh mục/import đã duyệt | A · bổ sung chọn lọc | Theo manifest31, không ghi đè dòng cũ | categories, stores |
| `promotion_products` | 0 | Giới hạn sản phẩm áp dụng cho promotion; rỗng có thể nghĩa toàn phạm vi | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | products, promotions |
| `promotions` | 2 | Ưu đãi, funding, hạn mức và thời gian | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | stores, users |
| `refund_transactions` | 0 | Ghi nhận hoàn tiền sau đối soát | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, payment_transactions, return_requests, users |
| `return_requests` | 0 | Yêu cầu trả hàng của người mua | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, users |
| `review_media` | 0 | Ảnh/video đánh giá do người mua tải | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | reviews |
| `reviews` | 90 | Đánh giá của người đã mua | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | order_items, products, users |
| `roles` | 5 | Vai trò phân quyền hệ thống | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | Không FK outbound |
| `store_delivery_areas` | 3 | Khu vực phục vụ để xác định phí giao | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | stores |
| `store_memberships` | 3 | Phân công Manager và quyền quản trị catalog theo store | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | stores, users |
| `store_registration_requests` | 0 | Hồ sơ đăng ký shop do User/Vendor gửi | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | stores, users |
| `store_settlements` | 103 | Kết toán/net doanh thu từ đơn và hoàn tiền | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | orders, stores |
| `store_toppings` | 9 | Khả dụng topping theo shop, không phải số lượng kho | Quản trị danh mục/import đã duyệt | A · bổ sung chọn lọc | Theo manifest31, không ghi đè dòng cũ | stores, toppings |
| `stores` | 3 | Chi nhánh và owner/chính sách/giờ/phạm vi vận hành | Người phụ trách chốt cấu hình qua quản trị | C · không bổ sung | Đổi cấu hình có thể ảnh hưởng tiền/quyền/phạm vi | commission_policies, users |
| `support_conversations` | 0 | Hội thoại CSKH do khách mở đúng scope | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | stores, users |
| `support_messages` | 0 | Tin nhắn CSKH phát sinh từ người tham gia | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | support_conversations, users |
| `toppings` | 3 | Topping danh mục chung, giá thêm | Quản trị danh mục/import đã duyệt | A · bổ sung chọn lọc | Theo manifest31, không ghi đè dòng cũ | Không FK outbound |
| `user_addresses` | 10 | Địa chỉ nhận hàng cá nhân | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | users |
| `users` | 24 | Tài khoản và thông tin cá nhân | Thao tác người dùng hoặc nghiệp vụ hệ thống thật | B · không bổ sung | Trống là hợp lệ; không tạo sự kiện hoặc tương tác giả | roles |

## Riêng13 bảng trống

- `cart_item_toppings`: B, không seed. Topping khách đã chọn trên dòng giỏ.
- `cart_items`: B, không seed. Dòng món/cấu hình do người dùng thêm vào giỏ.
- `comment_media`: B, không seed. Media người dùng tải cùng bình luận.
- `delivery_assignments`: B, không seed. Phân công giao hàng của đơn thật.
- `delivery_events`: B, không seed. Sự kiện lấy/giao/thất bại từ assignment.
- `payment_callback_events`: B, không seed. Dấu sự kiện callback provider chống replay.
- `promotion_products`: C, giữ nguyên. Thêm link sẽ thay eligibility của voucher, không phải bổ sung menu.
- `refund_transactions`: B, không seed. Ghi nhận hoàn tiền sau đối soát.
- `return_requests`: B, không seed. Yêu cầu trả hàng của người mua.
- `review_media`: B, không seed. Ảnh/video đánh giá do người mua tải.
- `store_registration_requests`: B, không seed. Hồ sơ đăng ký shop do User/Vendor gửi.
- `support_conversations`: B, không seed. Hội thoại CSKH do khách mở đúng scope.
- `support_messages`: B, không seed. Tin nhắn CSKH phát sinh từ người tham gia.

## Đối chiếu source/migration cho từng bảng

- `audit_logs`: `src/main/java/com/utetra/identity/AuditService.java`; `src/main/java/com/utetra/order/service/OrderSupport.java`; `src/main/java/com/utetra/entity/catalog/CatalogAudit.java`.
- `auth_sessions`: `src/main/java/com/utetra/entity/AuthSession.java`; `src/main/java/com/utetra/identity/AccountService.java`; `src/main/java/com/utetra/identity/AuthService.java`; `src/main/java/com/utetra/repository/AccessTokenRegistry.java`; `src/main/java/com/utetra/repository/catalog/StoreOperationsRepository.java`.
- `carrier_area_rates`: `src/main/java/com/utetra/service/StoreService.java`; `src/main/java/com/utetra/repository/catalog/StoreOperationsRepository.java`; `src/main/java/com/utetra/order/entity/CarrierAreaRate.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `carriers`: `src/main/java/com/utetra/service/StoreService.java`; `src/main/java/com/utetra/repository/catalog/StoreOperationsRepository.java`; `src/main/java/com/utetra/order/entity/Carrier.java`; `src/main/java/com/utetra/order/service/OperationsService.java`; `src/main/java/com/utetra/order/web/OperationsController.java`.
- `cart_item_toppings`: `src/main/java/com/utetra/order/entity/CartItemTopping.java`.
- `cart_items`: `src/main/java/com/utetra/order/entity/CartItem.java`; `src/main/java/com/utetra/order/service/CartService.java`.
- `carts`: `src/main/java/com/utetra/config/CartExpiryConfig.java`; `src/main/java/com/utetra/order/entity/Cart.java`; `src/main/java/com/utetra/order/service/CartService.java`; `src/main/java/com/utetra/order/service/GroupOrderService.java`; `src/main/java/com/utetra/order/service/GuestCartService.java`.
- `categories`: `src/main/java/com/utetra/controller/CatalogManagementController.java`; `src/main/java/com/utetra/service/CatalogService.java`; `src/main/java/com/utetra/repository/catalog/CatalogQueryRepository.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/order/service/CartService.java`.
- `comment_media`: `src/main/java/com/utetra/identity/entity/CommentMedia.java`.
- `commission_policies`: `src/main/java/com/utetra/repository/catalog/StoreOperationsRepository.java`; `src/main/java/com/utetra/order/entity/CommissionPolicy.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `coupon_redemptions`: `src/main/java/com/utetra/order/entity/CouponRedemption.java`; `src/main/java/com/utetra/order/service/CheckoutAvailabilityService.java`; `src/main/java/com/utetra/order/service/OrderPromotionService.java`.
- `delivery_assignments`: `src/main/java/com/utetra/order/entity/DeliveryAssignment.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `delivery_events`: `src/main/java/com/utetra/order/entity/DeliveryEvent.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `flyway_schema_history`: `src/main/resources/db/migration/mysql/` / Flyway metadata.
- `group_order_members`: `src/main/java/com/utetra/order/entity/GroupOrderMember.java`; `src/main/java/com/utetra/order/service/GroupOrderService.java`; `src/main/java/com/utetra/order/service/LocalQrPaymentService.java`; `src/main/java/com/utetra/order/web/CheckoutController.java`.
- `group_orders`: `src/main/java/com/utetra/order/entity/GroupOrder.java`; `src/main/java/com/utetra/order/service/GroupOrderService.java`; `src/main/java/com/utetra/order/service/LocalQrPaymentService.java`; `src/main/java/com/utetra/order/service/OrderWorkflowService.java`; `src/main/java/com/utetra/order/web/CheckoutController.java`.
- `local_qr_payments`: `src/main/java/com/utetra/order/entity/LocalQrPayment.java`; `src/main/java/com/utetra/order/service/GroupOrderService.java`; `src/main/java/com/utetra/order/service/LocalQrPaymentService.java`.
- `loyalty_accounts`: `src/main/java/com/utetra/identity/LoyaltyService.java`; `src/main/java/com/utetra/identity/entity/LoyaltyAccounts.java`.
- `loyalty_transactions`: `src/main/java/com/utetra/identity/LoyaltyService.java`; `src/main/java/com/utetra/identity/entity/LoyaltyTransactions.java`.
- `notifications`: `src/main/java/com/utetra/config/WebSocketConfig.java`; `src/main/java/com/utetra/config/WebSocketSecurityConfig.java`; `src/main/java/com/utetra/order/entity/Notification.java`; `src/main/java/com/utetra/order/service/OperationsService.java`; `src/main/java/com/utetra/order/service/OrderSupport.java`.
- `order_item_toppings`: `src/main/java/com/utetra/order/entity/OrderItemTopping.java`.
- `order_items`: `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`; `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/repository/catalog/CatalogQueryRepository.java`; `src/main/java/com/utetra/repository/catalog/CatalogStatisticsRepository.java`; `src/main/java/com/utetra/order/entity/OrderItem.java`.
- `order_status_history`: `src/main/java/com/utetra/order/entity/OrderStatusHistory.java`.
- `orders`: `src/main/java/com/utetra/config/SecurityConfig.java`; `src/main/java/com/utetra/config/WebSocketConfig.java`; `src/main/java/com/utetra/config/WebSocketSecurityConfig.java`; `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`; `src/main/java/com/utetra/identity/CommunityService.java`.
- `otp_challenges`: `src/main/java/com/utetra/identity/AccountService.java`; `src/main/java/com/utetra/identity/AuthService.java`; `src/main/java/com/utetra/identity/OtpService.java`; `src/main/java/com/utetra/identity/entity/OtpChallenges.java`.
- `payment_callback_events`: `src/main/java/com/utetra/order/entity/PaymentCallbackEvent.java`.
- `payment_transactions`: `src/main/java/com/utetra/order/entity/PaymentTransaction.java`.
- `product_comments`: `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/identity/UserManagementService.java`; `src/main/java/com/utetra/identity/entity/ProductComments.java`.
- `product_favorites`: `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/repository/catalog/CatalogQueryRepository.java`; `src/main/java/com/utetra/repository/catalog/CatalogStatisticsRepository.java`; `src/main/java/com/utetra/identity/entity/ProductFavorites.java`.
- `product_toppings`: `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/order/service/CartService.java`; `src/main/java/com/utetra/entity/catalog/ProductTopping.java`.
- `product_variants`: `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`; `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/repository/catalog/CatalogQueryRepository.java`; `src/main/java/com/utetra/repository/catalog/CatalogStatisticsRepository.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`.
- `product_views`: `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/identity/entity/ProductViews.java`.
- `products`: `src/main/java/com/utetra/config/SecurityConfig.java`; `src/main/java/com/utetra/controller/CatalogManagementController.java`; `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`; `src/main/java/com/utetra/identity/CommunityController.java`; `src/main/java/com/utetra/identity/CommunityService.java`.
- `promotion_products`: `src/main/java/com/utetra/order/service/OrderPromotionService.java`; `src/main/java/com/utetra/entity/catalog/PromotionProduct.java`.
- `promotions`: `src/main/java/com/utetra/config/WebSocketConfig.java`; `src/main/java/com/utetra/config/WebSocketSecurityConfig.java`; `src/main/java/com/utetra/controller/CatalogManagementController.java`; `src/main/java/com/utetra/identity/UserManagementService.java`; `src/main/java/com/utetra/service/PromotionService.java`.
- `refund_transactions`: `src/main/java/com/utetra/order/entity/RefundTransaction.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `return_requests`: `src/main/java/com/utetra/order/entity/ReturnRequest.java`.
- `review_media`: `src/main/java/com/utetra/identity/entity/ReviewMedia.java`.
- `reviews`: `src/main/java/com/utetra/config/SecurityConfig.java`; `src/main/java/com/utetra/identity/CommunityController.java`; `src/main/java/com/utetra/identity/CommunityService.java`; `src/main/java/com/utetra/identity/IdentityPages.java`; `src/main/java/com/utetra/identity/UserManagementController.java`.
- `roles`: `src/main/java/com/utetra/entity/Role.java`; `src/main/java/com/utetra/identity/AccountService.java`; `src/main/java/com/utetra/identity/AuthService.java`; `src/main/java/com/utetra/identity/IdentityRepository.java`; `src/main/java/com/utetra/identity/OtpService.java`.
- `store_delivery_areas`: `src/main/java/com/utetra/repository/catalog/StoreOperationsRepository.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/order/service/OperationsService.java`; `src/main/java/com/utetra/entity/catalog/StoreDeliveryArea.java`.
- `store_memberships`: `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`; `src/main/java/com/utetra/identity/IdentityStoreAccessPolicy.java`; `src/main/java/com/utetra/identity/UserManagementService.java`; `src/main/java/com/utetra/support/SupportRepository.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`.
- `store_registration_requests`: `src/main/java/com/utetra/entity/catalog/StoreRegistrationRequest.java`.
- `store_settlements`: `src/main/java/com/utetra/order/entity/StoreSettlement.java`; `src/main/java/com/utetra/order/service/OperationsService.java`.
- `store_toppings`: `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/entity/catalog/StoreTopping.java`.
- `stores`: `src/main/java/com/utetra/config/SecurityConfig.java`; `src/main/java/com/utetra/config/WebSocketConfig.java`; `src/main/java/com/utetra/config/WebSocketSecurityConfig.java`; `src/main/java/com/utetra/controller/CatalogManagementController.java`; `src/main/java/com/utetra/identity/CommerceIdentityAdapter.java`.
- `support_conversations`: `src/main/java/com/utetra/support/SupportConversation.java`; `src/main/java/com/utetra/support/SupportRepository.java`.
- `support_messages`: `src/main/java/com/utetra/support/SupportMessage.java`; `src/main/java/com/utetra/support/SupportRepository.java`.
- `toppings`: `src/main/java/com/utetra/controller/CatalogManagementController.java`; `src/main/java/com/utetra/dto/CatalogViews.java`; `src/main/java/com/utetra/service/CatalogService.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/order/repository/OrderDataRepository.java`.
- `user_addresses`: `src/main/java/com/utetra/identity/AccountService.java`; `src/main/java/com/utetra/order/repository/CommerceReferenceRepository.java`; `src/main/java/com/utetra/order/service/OperationsService.java`; `src/main/java/com/utetra/identity/entity/UserAddresses.java`.
- `users`: `src/main/java/com/utetra/config/AuthenticationConfig.java`; `src/main/java/com/utetra/controller/NavigationAdvice.java`; `src/main/java/com/utetra/entity/User.java`; `src/main/java/com/utetra/identity/AccountService.java`; `src/main/java/com/utetra/identity/AuthService.java`.

## Điểm quan trọng
- `cart_items`, `cart_item_toppings`, `order_items`, `order_item_toppings` là dữ liệu lựa chọn/snapshot giao dịch. Không phải bảng item danh mục còn thiếu.
- `promotion_products` rỗng không chứng minh thiếu dữ liệu: OrderPromotionService chỉ giới hạn tập món khi có liên kết; tự thêm sẽ thay chính sách khuyến mãi.
- Mọi bảng trống trước seed được chủ động giữ trống. Bộ seed chỉ bổ sung5 bảng A vốn đã có dữ liệu, không chạy seed_demo cũ.
- Schema cloud18 migration chưa cóV2026102802: categories/toppings/store_delivery_areas thiếu version so với source mới. Không tự chạy migration/ddl-auto; báo người phụ trách đồng bộ schema trước khi chạy WAR mới.
