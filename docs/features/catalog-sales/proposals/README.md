# Bàn giao schema catalog-sales

`V3__catalog_sales.sql` chỉ gồm 11 bảng do catalog sở hữu và FK của chúng. Nội dung lấy nguyên bản migration catalog hiện có tại `origin/integration/utetra` (`dac7ad0`), không tạo thêm một version cạnh tranh. File này là artifact bàn giao, chưa nằm trong Flyway runtime của nhánh catalog.

Điều kiện áp dụng bởi người phụ trách tích hợp:

- V1 có `users` trước khi tạo FK owner/applicant/reviewer.
- Order đã cung cấp `commission_policies` trước FK của `stores`.
- Identity cung cấp `store_memberships` và `audit_logs` để các service catalog vận hành; chúng không nằm trong DDL catalog này.
- Đối chiếu lịch sử Flyway của database thật trước khi ghép; nếu V3 đã áp dụng thì giữ nguyên checksum, không chạy lại thủ công hoặc đổi version để tạo trùng bảng.

Không đưa artifact vào runtime trên nhánh catalog khi chưa đủ prerequisite, không thay `ddl-auto=validate`, không triển khai schema Order/Identity trong nhánh này. `catalog-sales.sql` cũ được giữ làm hồ sơ đề xuất ban đầu; phần định nghĩa commission/membership/audit trong đó là scaffold tham chiếu, không phải phần catalog cần triển khai.

`CatalogSchemaProposalTest.catalogOnlyHandoffComposesWithOwnerPrerequisitesWithoutSeedingAccounts` kiểm V1 + DDL catalog bằng H2 với prerequisite tối thiểu, kiểm FK và không seed tài khoản. H2 không xác minh dialect MySQL/TiDB.
