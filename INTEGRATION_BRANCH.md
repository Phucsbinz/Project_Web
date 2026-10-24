# Hướng dẫn nhánh integration/utetra — Ghép chức năng

> Mới nhất 24/10/2026: đã pull catalog ce46ce2 và identify 4d0dc73, xử lý 4 conflict JS/JSP và một fixture lệch thứ tự cột; clean verify 137 tests đạt, WAR TiDB và 16 HTTP checks đạt. [Báo cáo/cách sửa](docs/integration/VERIFICATION_20261024.md) thay trạng thái kiểm chứng của các phiên cũ bên dưới. Server kiểm thử đã được dừng.

> Cập nhật 22/10/2026: đã fetch toàn bộ remote và merge đủ main/catalog/identity-membership/order/identify. Clean verify 130 tests đạt; WAR MySQL chạy được, 6 migration validate và 11 HTTP checks đạt. [Báo cáo hiện tại](docs/integration/VERIFICATION_20261022.md). Các bảng baseline ở phần dưới là lịch sử trước khi có nghiệp vụ, không phải trạng thái hiện hành.

Người phụ trách: nhóm trưởng và reviewer ba module; tên cá nhân chưa được nhóm xác nhận. Đọc PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md, docs/README.md, docs/COMMON_BASELINE.md, docs/SETUP.md và docs/BRANCH_WORKFLOW.md.

## Phạm vi và cấu hình chung

Danh mục hiện tại có 55 UC P0 và 3 UC Shipper P1: catalog18P0, identity16P0, order21P0+3P1. Thiết kế43bảng chính+2Shipper; hiện chỉ V1 roles/users/auth_sessions được hiện thực. Không dùng phạm vi28UC/schema26bảng/ba profile DB của hướng dẫn cũ.

Nền main khóa Java21/JDK đầy đủ, Maven Wrapper3.9.16, Boot3.5.13/WAR/JSP/Jakarta JSTL/SiteMesh3.2.2/Bootstrap5.3.3/JPA/Flyway/MySQLTiDButetra_db/TLSVERIFY_IDENTITY, NimbusJWTcookie+CSRF, MediaService và STOMP. Cấu hình trực tiếp properties, không .env. H2test isolated không thay kiểm TiDB; providerdisabled đến khi có cấu hình và code thật. Không tự đổi pom/security/schema để chữa lỗi ghép.

Main giữ cả INTEGRATION_BRANCH.md và ba BRANCH_*.md, để các nhánh kế thừa cùng phạm vi và config. Giữ lịch sử feature; không reset/force-push hoặc dùng worktree khi không được yêu cầu trong cloud. Localmerge/commit không tự thay remoteGitHub.

## Trình tự ghép

1. Lấy main mới đã kiểm build/HTTP/security; merge main vào từng feature trước.
2. Integration nhận main, rồi catalog-sales → identity-membership → order-payment; phối hợp owner khi xung đột.
3. Kiểm quote/store/promo/quota từ catalog, quyền/OTP/address/loyalty/audit từ identity, transaction/payment/refund/notification từ order.
4. Mọi migration version mới và contract chung phải review trên main; không tạo ba V2 hoặc sửa V1 đã chạy.
5. Build16test baseline; khi feature có code thật chạy ma trận55P0 và P1 đã bật. Merge hồ sơ không chứng minh nghiệp vụ chạy.
6. Chỉ đưa thay đổi tích hợp về main sau review; các feature lại lấy main. Push/PR khi chủ repository yêu cầu.

## Checklist ghép và hồ sơ

| Bước | Kiểm tra | Trạng thái hiện tại |
| --- | --- | --- |
| Main | WAR/JSP/SiteMesh/API/assets/JWT/CSRF | Build và16test baseline đạt trên H2; TiDB cloud bị chặn mạng |
| Catalog18P0 | Public/top/filter/shop/Vendor/catalog/promotion/media | Đặc tả đầy đủ; nghiệp vụ chưa triển khai |
| Identity16P0 | OTP/session/profile/community/loyalty/role/scope | Đặc tả đầy đủ; nghiệp vụ chưa triển khai |
| Order21P0+3P1 | Cart/COD/VNPAY/return/refund/carrier/commission/report/socket | Đặc tả đầy đủ; nghiệp vụ chưa triển khai |
| Toàn luồng | OTP→shop/cart→checkout→payment→delivery→review/return/refund/ledger | Chưa chạy vì module nghiệp vụ chưa có |
| Báo cáo | Diagram/code/UI/setup/prompt/DOCX và khảo sát thực | Khung hồ sơ đã có; cần minh chứng thực từ nhóm |

Kiểm quyền Vendor cóUser, Manager membership, Adminglobal, Shipperassignment; callback giả/trùng/sai tiền/đến muộn; points/quota/checkoutrace/refundledger/netreport; media validation/cleanup; reconnect/revoked socket; responsive mobile/tablet/desktop. Không bỏ chức năng thầy; chỉ bỏ tính khoảng cách.

Prompt nguyên văn lưu docs/AI_PROMPT_HISTORY.md, ghi nhánh integration/utetra và UC liên quan; nhật ký kết quả dẫn ID trong docs/BASELINE_AI_LOG.md hoặc AI_LOG module. Ghi code/commit/test/ảnh thật và phần nhóm sửa, không bịa provider hoặc UC hoàn thành.
