# Schema của nhánh order-payment

[V2__order_payment.sql](../../../src/main/resources/db/migration/mysql/V2__order_payment.sql) tạo **19 bảng thuộc order-payment**, giữ V1 nguyên vẹn. FK nội bộ và FK đến users được tạo. FK đến stores/products/variants/promotions/areas thuộc module khác cần migration chung sau review; Service kiểm tham chiếu và scope trước ghi. Không giả định catalog/identity đã có code hoặc dữ liệu nghiệp vụ.

Coupon redemption lưu snapshot tên, loại và nguồn tài trợ khuyến mãi; đơn lưu giá/địa chỉ/carrier/rate/hoa hồng. DATABASE_DESIGN đã bổ sung ba cột snapshot này để đối chiếu schema và JPA.

[shared-commerce-schema-proposal.sql](shared-commerce-schema-proposal.sql) là bản DDL tham khảo **42 bảng còn lại** theo DATABASE_DESIGN, nằm ngoài vị trí Flyway: không tự chạy và không tạo bảng của nhánh khác khi ứng dụng khởi động. Các bảng test trong reference-schema.sql cũng không được đưa vào migration production.

Nhóm trưởng cần review thứ tự FK/schema/version trước khi integration nhận nhánh. V2 này chưa được áp dụng lên utetra_db; khi có migration V2 của module khác, điều phối version trước lần deploy đầu tiên. Không đổi checksum của migration đã áp dụng hoặc tự chạy flyway clean trên cloud. Sau khi catalog/identity có schema thật, thêm FK ngoài module bằng migration mới được review.

Flyway V1 + V2, replay và Hibernate validate đã kiểm trên MySQL 8.4 riêng với TLS xác minh CA; **chưa chạy DDL trên TiDB thật**. Xem [VERIFICATION](VERIFICATION.md).
