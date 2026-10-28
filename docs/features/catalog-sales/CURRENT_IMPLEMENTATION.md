# Catalog source và test hiện tại

Ngày 24/10/2026, working tree integration. Đây là chỉ mục source/test đã có, thay cách hiểu bảng mẫu cũ ghi UC chưa triển khai. Testcase dưới đây là minh chứng liên quan, không tự khẳng định nghiệm thu toàn bộ UI/provider của mỗi UC.

Các case ghi trong CatalogIntegrationTest thuộc lượt Maven mới nhất; CatalogViewTest kiểm render JSP/Tomcat. Stock stale form có thêm OrderCommerceIntegrationTest.staleCatalogFormCannotOverwriteStockConsumedByCheckout.

## UC-CAT-01 — Trang chủ Guest

- Source thực: `CatalogController.menu → CatalogService.search → CatalogQueryRepository.search`.
- Test liên quan: `CatalogIntegrationTest.guestRankingUsesActualSalesAndTopLimitIsStable`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-01.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-CAT-02 — Tìm kiếm và lọc

- Source thực: `CatalogService.search → CatalogQueryRepository.search`.
- Test liên quan: `CatalogIntegrationTest.actualRankingExcludesUnpaidAndRefundedAndPagesTopTwenty`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-02.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-CAT-03 — Trang chủ User và top 20

- Source thực: `CatalogController.menu → CatalogService.search`.
- Test liên quan: `CatalogIntegrationTest.actualRankingExcludesUnpaidAndRefundedAndPagesTopTwenty`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-03.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-CAT-04 — Sản phẩm theo danh mục

- Source thực: `CatalogService.categories/search`.
- Test liên quan: `CatalogIntegrationTest.hiddenCategoryAndSuspendedShopCannotBePurchased`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-04.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-CAT-05 — Chi tiết và tùy biến trà sữa

- Source thực: `CatalogService.detail/quote → ProductVariantRepository`.
- Test liên quan: `CatalogIntegrationTest.quoteCalculatesEveryToppingPerCupAndRejectsInvalidOptions`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-05.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-CAT-06 — Xem/chọn chi nhánh và trang shop

- Source thực: `StoreService.publicStores/publicStore/deliveryRates`.
- Test liên quan: `CatalogIntegrationTest.shopOpeningHoursAreEnforcedInVietnamTime`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-CAT-06.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-VND-01 — Đăng ký shop

- Source thực: `StoreService.register/review → StoreOperationsRepository`.
- Test liên quan: `CatalogIntegrationTest.approvalPromotesBuyerAndRejectsReplay`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-VND-01.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-VND-02 — Quản lý trang chủ shop

- Source thực: `StoreService.update; CatalogMediaService.upload`.
- Test liên quan: `CatalogIntegrationTest.cannotTransferProductOrModifyAnotherShop`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-VND-02.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-VND-03 — Quản lý sản phẩm của mình

- Source thực: `CatalogService.saveProduct/disableProduct; finite stock/version`.
- Test liên quan: `CatalogIntegrationTest.updatesKeepVariantIdsAndRejectStaleVersions`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-VND-03.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-VND-05 — Tạo khuyến mãi shop

- Source thực: `PromotionService.save/quote`.
- Test liên quan: `CatalogIntegrationTest.promotionScopeMathAndValidation`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-VND-05.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-MGR-02 — Quản lý sản phẩm shop

- Source thực: `CatalogGuard.operates → CatalogService.saveProduct`.
- Test liên quan: `CatalogIntegrationTest.cannotTransferProductOrModifyAnotherShop`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-MGR-02.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-MGR-03 — Quản lý danh mục/topping

- Source thực: `CatalogGuard.catalog → CatalogService.saveCategory/saveTopping`.
- Test liên quan: `CatalogIntegrationTest.scopePolicyUsesDbOwnershipAndMembership`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-MGR-03.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-MGR-05 — Quản lý khuyến mãi

- Source thực: `PromotionService.save/quote theo store scope`.
- Test liên quan: `CatalogIntegrationTest.promotionScopeMathAndValidation`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-MGR-05.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-MGR-07 — Quản lý cửa hàng

- Source thực: `StoreService.review/state/update/assign`.
- Test liên quan: `CatalogIntegrationTest.approvalPoliciesAreActiveAndVisibleOnlyToScopedReviewers`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-MGR-07.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-ADM-02 — Quản lý sản phẩm các shop

- Source thực: `CatalogService.saveProduct/disableProduct với Admin`.
- Test liên quan: `CatalogIntegrationTest.httpRequiresCsrfAndScopeAndDoesNotSerializeEntities`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-ADM-02.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.

## UC-ADM-03 — Quản lý danh mục/topping

- Source thực: `CatalogService.saveCategory/saveTopping với Admin`.
- Test liên quan: `CatalogIntegrationTest.scopePolicyUsesDbOwnershipAndMembership`.
- Hồ sơ quy trình và sơ đồ: [use case](use-cases/UC-ADM-03.md).
- Phần nhóm cần chốt: owner/MSSV/reviewer/commit, ảnh UI và hình code/activity riêng phù hợp phần báo cáo; không dùng một test liên quan thay mọi bước nghiệm thu.
