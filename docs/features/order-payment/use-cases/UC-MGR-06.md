# UC MGR 06 Nhà vận chuyển và phí theo cửa hàng

Cập nhật 24/10/2026. Manager hiện có CRUD tên/phí carrier riêng của shop, không chỉ biểu phí khu vực. Admin quản lý carrier global và theo shop. Khách tự chọn chi nhánh; không tính khoảng cách.

## Quy trình và cấu hình

1. Admin phân công Manager vào shop active. Manager đăng nhập, mở /manager/order-tools.
2. Form carrier chọn một shop được giao, nhập tên/phí nguyên VND/trạng thái. CarrierInput có storeId và version.
3. OperationsService.carrier kiểm role hiện tại, scope, khóa bản ghi khi sửa và optimistic version. Manager không được tạo global, đổi global thành private, đổi scope hoặc sửa shop khác.
4. carriers.managed_store_id NULL là global; giá trị shop là riêng shop đó. Migration 2026102403 thêm FK/index. Carrier global vẫn thuộc Admin.
5. OperationsService.rate kiểm scope khu vực và scope carrier; carrier riêng không áp dụng sang shop khác.
6. CheckoutChoices lọc carrier phù hợp shop, CheckoutService kiểm lại ở server. Tên/phí được snapshot trên order; sửa danh mục không đổi đơn cũ.
7. JSP tools.jsp dùng layout/Bootstrap chung, nút sửa theo canManage; ẩn nút không thay kiểm quyền Service.

## Minh chứng

- Source: OperationsService.carrier/rate/carriers/checkoutChoices; CheckoutService.calculate; Carrier; OrderRequests.CarrierInput; OperationsController; order/tools.jsp.
- Test: OrderCommerceIntegrationTest.managerCanManageCarrierNamesAndFeesWithinAssignedShopOnly, cùng test carrierRatesRequireStoreScopeAndNotificationsRequireOwner và checkout cũ.
- [Ảnh source có giải thích](../../../report-assets/code-manager-carrier.png).
- [UI mobile thật trên H2](../../../report-assets/acceptance/manager-carrier-mobile.png); [kết quả browser](../../../report-assets/acceptance/verification.json).

Nhóm điền owner/MSSV/reviewer/commit và diễn tập bằng tài khoản Manager đúng scope. Không khai rằng Manager sửa mọi carrier global; phạm vi đã thiết kế là carrier riêng shop và biểu phí khu vực, Admin chịu trách nhiệm danh mục toàn chuỗi.
