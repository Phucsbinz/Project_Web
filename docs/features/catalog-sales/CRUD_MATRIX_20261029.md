# Ma trận CRUD hiện có — 29/10/2026

## Phạm vi và cách đọc

Đối chiếu controller, service, JSP và JavaScript hiện tại trên workspace `integration/utetra`. Đây là **kiểm tra nguồn**, không thay cho kiểm thử từng thao tác trên Tomcat. Không đánh dấu một module hoàn tất chỉ vì có endpoint. `C` = tạo; `R` = xem dữ liệu thật; `U` = sửa; `D*` = xóa hoặc thao tác nghiệp vụ thay thế. Giao dịch đã chốt phải giữ lịch sử; thiếu DELETE không tự động là lỗi.

API catalog dưới đây dùng tiền tố `/api/{admin|vendor|manager}/catalog`, nhưng service vẫn hạn chế vai trò/phạm vi. Manager không có quyền sửa giá/khuyến mãi dù controller có chung mapping. Các thay đổi đang được agent chính thực hiện đồng thời cần được kiểm chứng trong lượt tổng hợp.

## Danh mục, chi nhánh và vận hành

| Dữ liệu | UI sử dụng thật | C / R / U | D* và khôi phục | Backend, kết luận nguồn |
| --- | --- | --- | --- | --- |
| Chi nhánh / hồ sơ shop | `/account/shop-registration`, `/vendor/stores`, `/admin/stores`; vận hành `/manager/operations` | Đăng ký; danh sách/chi tiết; Vendor sửa hồ sơ, người duyệt xử lý hồ sơ | Admin/Manager có phạm vi tạm khóa/mở lại; nhận đơn bật/tắt riêng; không xóa shop lịch sử | `CatalogController.registration`, `CatalogManagementController.store/review/state`, `StoreService`, `ManagerOperationsService.accepting`. Có luồng nghiệp vụ thay xóa. |
| Danh mục chung | `/admin/categories`, `/manager/categories` khi có quyền CATALOG_ADMIN | POST/GET `/categories`, PUT `/categories/{id}`; form nạp dữ liệu | `active=false/true` trong form; giữ ID | `CatalogService.categories/saveCategory`; có CRUD phù hợp. |
| Topping chung | `/admin/toppings`, `/manager/toppings` khi có quyền CATALOG_ADMIN | POST/GET `/toppings`, PUT `/toppings/{id}` | `active=false/true`; không xóa tham chiếu lịch sử | `CatalogService.toppings/saveTopping`; có CRUD phù hợp. |
| Sản phẩm | `/vendor/products`, `/admin/products` | POST/GET `/products?storeId=`, PUT `/products/{id}` | DELETE thực chất `disableProduct`; bật lại bằng sửa `active=true` | `CatalogService.saveProduct/managed/disableProduct`; có CRUD phù hợp, gồm optimistic version. |
| Size / biến thể | Cùng form sản phẩm; tồn kho tại `/manager/operations` | Form S/M/L, phụ thu và tồn; GET trả variants; Manager cập nhật số lượng | Bỏ chọn size ngừng phục vụ; dữ liệu biến thể đã dùng vẫn giữ trong service | `CatalogService.saveProduct`, `ManagerOperationsController /variants/{id}/stock`; nghiệp vụ nằm trong sản phẩm, không cần CRUD trang thứ hai. |
| Khả năng phục vụ sản phẩm | `/manager/operations` | Đọc sản phẩm chi nhánh và PUT availability | Tạm ngưng/cho phép bán, không xóa món | `ManagerOperationsService.availability`; giữ phân quyền. |
| Topping tại chi nhánh | `/vendor/stock`, `/admin/stock`, `/manager/operations` | PUT `/stores/{store}/toppings/{id}`; GET `/stores/{store}/toppings` vừa bổ sung | `available=false/true`; trạng thái thiếu cấu hình là false | `CatalogService.storeToppings/availability`. Đã sửa thiếu đọc lại và thiếu điều khiển Manager; 3 test `CatalogCrudReadbackTest` được agent chính xác nhận GREEN. |
| Khuyến mãi | `/vendor/promotions`, `/admin/promotions` | POST/GET `/promotions`, PUT `/promotions/{id}`; phạm vi shop/toàn hệ thống theo role | `active=false/true`, giữ sử dụng và snapshot | `PromotionService.managed/save`; có form chỉnh sửa thật. |
| Khu vực giao | `/vendor/areas`, `/admin/areas` | POST `/stores/{id}/areas`, đọc `StoreView.areas`, PUT `/areas/{areaId}` | `active=false/true` trong form | `StoreService.area/managedStores`; có CRUD phù hợp. |
| Ảnh shop / sản phẩm | Panel ảnh khi chỉnh sửa | Xem ảnh hiện tại; upload thay ảnh | Không có nút gỡ riêng; thay ảnh xử lý tài nguyên cũ qua media service | `/media/{type}/{id}`, `CatalogMediaService`. **Thiếu thao tác gỡ ảnh riêng nếu yêu cầu CRUD ảnh độc lập**, không cản thay ảnh. |

## Tài khoản và cộng đồng

| Dữ liệu | UI | C / R / U | D* và khôi phục | Backend, kết luận nguồn |
| --- | --- | --- | --- | --- |
| Tài khoản hệ thống | `/admin/users`; `/manager/users` chỉ người trong phạm vi | Admin tạo; lọc vai trò/tìm kiếm; sửa vai trò/trạng thái; Manager không được đổi role | Khóa/mở lại `active`; thu hồi phiên; không xóa tài khoản đã phát sinh giao dịch | `UserManagementController`, `UserManagementService.create/search/manage`; UI hiện tại do subtask tài khoản bổ sung. Email mới vẫn qua xác minh. |
| Hồ sơ / mật khẩu / avatar | `/account/profile`, `/account/security` | GET `/account/profile/data`; POST profile/password/avatar | Đổi mật khẩu thu hồi phiên; không có tự xóa tài khoản | `AccountController`, `AccountService`; tự xóa tài khoản không có trong workflow hiện tại. |
| Địa chỉ cá nhân | `/account/addresses` | GET data; POST thêm; PUT theo ID; form sửa nạp đúng dữ liệu | DELETE theo chủ sở hữu; chọn địa chỉ mặc định thay thế | `AccountService.saveAddress/deleteAddress`; là sổ địa chỉ, đơn giữ snapshot riêng. Có CRUD thật. |
| Đánh giá của tôi | `/reviews`; xem công khai `/products/{id}` | Tạo từ món đã mua; GET purchases/data; PUT nội dung/sao có version | DELETE đổi HIDDEN, xóa nội dung và media; giữ chứng cứ một đánh giá/món | `CommunityController`, `CommunityService.review/entries/edit/delete`. Không có khôi phục nội dung chủ động xóa; đây là thao tác kết thúc. |
| Bình luận của tôi | `/comments`; xem công khai `/products/{id}` | POST multipart; GET data; PUT nội dung | DELETE soft hide + gỡ media | `CommunityService.comment/entries/edit/delete`; có CRUD cá nhân. |
| Kiểm duyệt đánh giá/bình luận | `/admin/moderation` | GET `/admin/moderation/data` theo reviews/comments, từ khóa, trạng thái, phân trang; có tác giả, món, đơn nguồn | PUT `/admin/moderation/{kind}/{id}` ẩn/hiện có version; giữ API hide cũ; không khôi phục nội dung tác giả đã gỡ | `UserManagementController.moderation/visibility`, `UserManagementService`, `identity/moderation.jsp`. Agent kiểm duyệt đã bổ sung luồng UI/Admin; restore kiểm chứng mua hàng và không hồi sinh nội dung đã làm rỗng. Chờ kiểm chứng tổng hợp. |
| Yêu thích | `/favorites`, nút sản phẩm | POST `/favorites/{id}`, GET `/favorites/data` | DELETE `/favorites/{id}` bỏ yêu thích | `CommunityController.favorite/products`; không có sửa vì quan hệ nhị phân. |
| Đã xem | `/viewed` | POST `/viewed/{id}`, GET `/viewed/data` | Chưa có xóa lịch sử xem | Dữ liệu tự ghi, không phải master data. Xóa lịch sử cá nhân là thiếu tính năng tùy phạm vi, không phải CRUD quản trị bắt buộc. |
| Điểm thành viên | `/account/loyalty` | GET `/account/loyalty/data`; điểm sinh từ nghiệp vụ | Không sửa/xóa sổ điểm tùy ý | `AccountController.loyalty`, `LoyaltyService`; bảo toàn sổ giao dịch. |
| Ví ưu đãi | Menu tài khoản → Ưu đãi của tôi (modal), `/checkout` chọn voucher | GET `/account/offers`, POST `/sync`, POST `/{id}/save` | Áp dụng/hết hạn theo nghiệp vụ; không CRUD sổ redemption | `OfferWalletController`, `OfferWalletService`; định nghĩa ưu đãi sửa tại promotions. |

## Vận chuyển, chính sách và phân công

| Dữ liệu | UI | C / R / U | D* và khôi phục | Backend, kết luận nguồn |
| --- | --- | --- | --- | --- |
| Nhà vận chuyển | `/admin/order-tools`, `/manager/order-tools` | POST `/.../carriers` dùng id để create/update; bảng có nút sửa nạp tên/phí/phiên bản/phạm vi | checkbox active bật/tắt; giữ nhà vận chuyển đã có đơn | `OperationsController`, `OperationsService.carriers/carrier`; Manager chỉ sửa carrier riêng trong chi nhánh. Có CRUD phù hợp. |
| Biểu phí theo carrier/khu vực | Cùng order-tools | Bảng đọc giá/trạng thái; POST `/.../rates` upsert theo cặp carrierId+areaId | checkbox active bật/tắt bằng chọn lại cặp và nhập phí | `OperationsService.rate/deliveryRates`. Đã bổ sung ID rate/cặp carrier/khu vực và nút Chỉnh sửa nạp đúng phí/trạng thái, kể cả tên hiển thị trùng. Role scope và khóa upsert cũ giữ nguyên. Test SQL `DeliveryRateReadbackTest` đã viết. Lần chạy đầu agent chính báo FORBIDDEN do fixture dùng role thiếu `ROLE_`; đã sửa fixture thành `ROLE_MANAGER`, không đổi quyền production, chờ chạy lại. |
| Chính sách hoa hồng | `/admin/order-tools` | Xem % hiện tại theo shop; POST commissions tạo chính sách mới và gán shop; global áp dụng mọi shop | Thay bằng chính sách mới, có thể 0%; đơn giữ snapshot | `OperationsService.commission/globalCommission/stores`. Không sửa/xóa policy cũ là đúng lịch sử. **Chưa có danh sách lịch sử policy** nếu cần quản trị tra cứu chính sách độc lập. |
| Phân công Manager / quyền catalog | `/admin/assignments`; `/admin/memberships` là form phụ | GET `/api/admin/operations/managers` có managers/stores/assignments; nút Sửa nạp phân công; POST catalog memberships | active false thu hồi, true cấp lại; lựa chọn catalogDelegation tường minh | `manager-assignments.js`, `StoreService.assign`, `ManagerOperationsService.managers`; có đọc lại và cấp/thu hồi. |
| Phạm vi Shipper | `/admin/shipper-scopes` | GET trang có choices/scopes; POST cùng path cấp mới/cấp lại với version/lý do | Thu hồi từ từng dòng; cấp lại bằng form, JS tìm đúng version cặp hiện hữu | `AdminShipperScopeController`, `ShipperScopeService`, `shipper-scopes.js`; có vòng đời thay xóa. |
| Phân công Shipper cho đơn | Trang chi tiết đơn operator | POST `/{id}/assignment`; đọc assignment tại chi tiết đơn | Phân công lại có kiểm trạng thái; lịch sử assignment giữ lại | `OrderController.assignment`, `OrderWorkflowService`; dispatch tự động đang được agent chính xử lý, không kết luận từ audit này. |

## Giao dịch và dữ liệu cộng tác

| Dữ liệu | UI | C / R / U | D* phù hợp nghiệp vụ | Backend, kết luận nguồn |
| --- | --- | --- | --- | --- |
| Giỏ hàng / dòng giỏ | `/cart`, nút thêm món | POST `/cart/items`, `/draft/items`; GET `/data`; POST `/items/{id}` sửa cấu hình/số lượng | POST `/items/{id}/delete`, `/clear`; có merge | `CartController`, `CartService`, `GuestCartService`; CRUD dữ liệu tạm có thật. |
| Checkout / đơn hàng | `/checkout`, `/orders`, `/vendor/orders`, `/manager/orders`, `/admin/orders`, `/shipper/orders` và `/{id}` | POST checkout tạo; GET list/data/detail/invoice; cập nhật qua state machine | POST cancel thay DELETE; không sửa dòng/giá của đơn đã chốt | `CheckoutController`, `OrderController`, `OrderWorkflowService`; không áp CRUD tùy ý lên lịch sử tài chính. |
| Thanh toán | Chi tiết đơn, QR/invoice | Tạo lúc checkout; VNPAY, QR status/confirm/renew, bank confirmation | Hoàn tiền bằng refund; không xóa payment | `VnpayController`, `LocalQrController`, `OrderController.bank-confirmation/refund`; nghiệp vụ giữ snapshot và audit. |
| Trả hàng / bằng chứng / thất bại giao | Chi tiết đơn theo role | POST returns/evidence/review/receive/failure; đọc lại trong detail | Từ chối/duyệt/xử lý theo trạng thái; không xóa record sau chốt | `OrderController`, `ReturnEvidenceService`, `OrderWorkflowService`; CRUD thay bằng workflow có kiểm role. |
| Nhóm đặt món / thành viên | `/group-orders/new`, `/group-orders/{token}` | POST tạo; GET data; join/leave/lock/confirm/payment-mode | Host cancel, hết hạn; không DELETE lịch sử nhóm/đơn | `GroupOrderController`, `GroupOrderService`; có vòng đời đầy đủ theo thiết kế. |
| Món trong nhóm | Trang nhóm và menu có group token | POST `/{token}/items`; GET data; tăng/chọn lại thông qua thêm món | POST `/{token}/items/remove`; nút Xóa món khi nhóm OPEN | Không endpoint chỉnh trực tiếp cấu hình món đã thêm; có thể xóa rồi thêm lại trước chốt. **Thiếu tiện ích sửa dòng nhóm trực tiếp**, không sửa sau khóa/thanh toán. |
| Thông báo / báo cáo | `/notifications`, reports/revenue theo role | GET dữ liệu/báo cáo/CSV; POST notifications/{id}/read | Không tự sửa/xóa log tài chính | `OperationsController`; dữ liệu phát sinh chỉ đọc/đánh dấu đọc, không master CRUD. |
| Hỗ trợ khách hàng | Widget buyer, `/manager/support`, `/admin/support` | POST conversations mở; GET conversations/messages; POST messages gửi | Không sửa/xóa tin hỗ trợ hiện có | `SupportController`, `SupportService`; hội thoại là lịch sử trao đổi. Không có close/archive conversation hoặc sửa/xóa tin nếu yêu cầu mở rộng. |

## Những phần chưa thể tuyên bố hoàn tất

1. **Kiểm duyệt cộng đồng Admin đã được agent phụ trách bổ sung**: danh sách toàn hệ thống, tìm kiếm/lọc, ẩn/hiện theo phiên bản, kiểm chứng mua hàng khi mở lại. Đã đối chiếu nguồn cập nhật, còn kiểm chứng tổng hợp do agent chính thực hiện.
2. **Biểu phí đã sửa khoảng trống đọc lại/prefill trong lượt này**: có nút sửa từ bản ghi và ID cặp thật. Chưa kiểm thao tác trên Tomcat; test SQL mới chờ agent chính chạy.
3. Những thiếu tiện ích có điều kiện: gỡ ảnh shop/sản phẩm; tra lịch sử hoa hồng; sửa trực tiếp dòng món nhóm trước khóa; xóa lịch sử xem; đóng/lưu trữ hỗ trợ. Không tự mở rộng phạm vi bằng xóa lịch sử giao dịch.
4. Audit này không chạy thao tác UI cho toàn bộ ma trận. Các dòng “có” dựa trên nối controller → service → JSP/JS; vẫn cần smoke render và thao tác theo role trên Tomcat trước khi báo mọi CRUD đều hoạt động.

## Nguồn đối chiếu

- `src/main/java/com/utetra/controller/CatalogManagementController.java`, `publicweb/CatalogController.java`, `ManagerOperationsController.java`; `CatalogService`, `StoreService`, `PromotionService`, `ManagerOperationsService`.
- `src/main/java/com/utetra/identity/{AccountController,UserManagementController,CommunityController,OfferWalletController}.java`; các service tương ứng.
- `src/main/java/com/utetra/order/web/{OperationsController,AdminShipperScopeController,CartController,CheckoutController,OrderController,GroupOrderController,LocalQrController,VnpayController}.java`.
- JSP `catalog/manage.jsp`, `workspace/manager.jsp`, `workspace/assignments.jsp`, `identity/page.jsp`, `order/tools.jsp`, `order/shipper-scopes.jsp`, `order/group.jsp`; JS `catalog-management.js`, `manager-operations.js`, `manager-assignments.js`, `identity.js`, `shipper-scopes.js`.
