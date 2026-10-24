# Rà xung đột và chạy bản tích hợp — 24/10/2026

## Nguồn code

Nhánh làm việc: integration/utetra. Workspace sạch trước khi làm. Đã fetch tất cả remote, cập nhật local bằng fast-forward: catalog từ fa3efab lên **ce46ce2**, identify từ 285ae1a lên **4d0dc73**. Main cb90cf9, identity-membership 115b0e9 và order e200a64 không có commit remote mới.

Integration nhận identify bằng fast-forward từ dac7ad0 lên 4d0dc73, sau đó merge catalog bằng **65cf65a**. Đã kiểm cả main, catalog, identity-membership, order và identify là ancestor của integration. Fetch lại sau kiểm thử và xác nhận các đầu remote mới nhất đều được ghép; không bỏ sót nhánh identify đang chứa code identity mới hơn feature/identity-membership.

## Xung đột và cách xử lý

| File | Phần giữ và kết nối |
| --- | --- |
| assets/js/catalog-management.js | Giữ data-section từ controller, tab có URL riêng, giữ storeId, form duyệt/chọn policy mới; giữ Accept JSON và empty-state của UI đã tích hợp |
| views/catalog/manage.jsp | Giữ trình bày hiện tại và thêm data-section để JavaScript chọn đúng trang products/promotions/areas/stock/memberships |
| views/catalog/menu.jsp | Giữ hero/bộ lọc theo theme chung và bổ sung hidden top=true khi gửi lại bộ lọc |
| views/catalog/product-list.jspf | Giữ card/icon/định dạng tiền/escape của bản tích hợp và pageEncoding UTF-8 mới để không mất dấu |

Không chọn toàn bộ ours/theirs cho các file để làm mất chức năng bên kia. Tab quản lý mới dùng const section và điều hướng bằng URL: fallback empty-state được đổi sang link thay vì callback gán lại const. Các link tab dùng aria-current thay aria-pressed của phiên bản nút cũ.

Bộ 16 file Shared UI vẫn giống main, chỉ có layouts/app.jsp và một decorator. Profile/security/OTP email mới của identity được giữ nguyên; code thêm món từ catalog vào giỏ order và community của identity vẫn có trong bản ghép. Không đổi pom, datasource, credential hoặc migration runtime để chữa merge.

## Lỗi tích hợp ngoài Git conflict

Lượt verify đầu có 136 tests, một lỗi ở fixture mới của CatalogIntegrationTest: INSERT commission_policies không ghi tên cột, nên chuỗi tên policy rơi vào cột rate_percent khi JPA dựng schema theo thứ tự khác. Đã đổi sang danh sách cột rõ ràng; giữ nguyên các assertion kiểm policy active và scope, không đổi schema thật hoặc tắt test.

Thêm test shopApprovalUsesOrderPolicyRevokesIdentitySessionAndPreservesBuyerCart trong OrderCommerceIntegrationTest. Test chạy HTTP thật với H2 isolated: login User → lưu giỏ → đăng ký shop → Admin đọc policy Order và duyệt → phiên User cũ nhận 401 → login lại nhận quyền Vendor → checkout từ giỏ cũ thành công, đơn vẫn thuộc đúng user và shop mới dùng đúng policy.

## Kết quả cuối

- Java 21.0.12.1, Maven Wrapper: **clean verify đạt 137 tests, 0 failures/errors/skipped**, WAR build thành công. Log local .local/integration-verify-20261024.log; Surefire XML ở target/surefire-reports.
- Đóng gói lại sau chỉnh ARIA của JavaScript, không đổi Java production sau lượt test đạt. Các JS trong assets qua node --check; git diff --check đạt.
- Shared UI checker --compare main đạt; không có layout thứ hai hoặc bundle chung bị lệch.
- WAR profile mysql khởi động với **utetra_db**, Hibernate validate đạt, Flyway validate **6 migration**, không có migration mới cần chạy.
- **16 HTTP checks thật đạt**, xem [HTTP_CHECKS_20261024.json](HTTP_CHECKS_20261024.json). Bao gồm trang công khai, profile CSS, catalog JS, redirect trang bảo mật/order khi chưa login, API profile/catalog/order bị chặn 401 và checkout thiếu CSRF bị chặn 403.
- HTML được kiểm có một header thương hiệu/footer/main-content, một lần nạp Bootstrap CSS/JS, không lộ JSP/SiteMesh directive.
- Không seed, tạo user/order hay gửi email/upload provider trên DB thật. Sau smoke check đã dừng đúng process WAR PID 8820 và giữ workspace trên integration/utetra.

## Giới hạn và bàn giao

Luồng nghiệp vụ xuyên module được xác minh trong H2; chưa thực hiện checkout/refund bằng dữ liệu khách hàng thật trên TiDB. Không chạy bộ screenshot/visual QA mới hoặc thao tác tay mọi tổ hợp UI. SMTP thật trên máy này vẫn cần file cấu hình riêng; VNPAY và Shipper production giữ cờ như đã thống nhất. Không coi unit/mock provider là bằng chứng gửi mail/thanh toán thật.

Chỉ sửa và commit integration trong lượt này. Các nhánh chức năng được pull về nhưng không nhận các sửa tích hợp qua push. Chưa push remote, chưa merge integration ngược về main.
