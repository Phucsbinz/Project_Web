# UTETra — Phân công nhánh theo use case và vai trò

Main là nền cấu hình/mã chung đã dựng; các nhánh phải merge main mới trước code. Integration ghép ba module theo contract; không tự đổi pom/config/schema để chữa xung đột.

| Nhánh | Owner và phần role/use case |
| --- | --- |
| main | Nhóm trưởng B0, config/contract/migration/template, review chéo |
| feature/catalog-sales | TV1, Guest/User catalog + Vendor shop/catalog/promo + Manager/Admin catalog/shop |
| feature/identity-membership | TV2, Guest/User OTP/auth/profile/community/points + Manager/Admin identity |
| feature/order-payment | TV3, User/Vendor mua/vận hành + Manager/Admin tài chính/carrier/refund/report + Shipper phần thêm |
| integration/utetra | Nhóm trưởng và owner các module, test xuyên suốt và ghép hồ sơ |

Mỗi UC trong USE_CASES có một module owner làm Entity/Repo/DTO/Service/Controller/JSP/test/hồ sơ. Mỗi thành viên viết báo cáo theo role của UC mình nhận; bảng nghiệm thu phải chỉ rõ role, không chỉ liệt kê class. Vendor có User, Manager scope store, Admin toàn app; không còn ROLE_STAFF/ROLE_CUSTOMER của thiết kế cũ.

Chỉ main điều phối pom, application config (được Git theo dõi), layout/template/contract/migration. Owner module tự sửa docs/features của mình; lịch sử prompt chỉ append/đính chính. Không sửa tài liệu bài giảng gốc hoặc database project khác.

```powershell
git switch feature/order-payment
git merge main
# Làm UC trọn luồng, test và cập nhật hồ sơ/prompt/hình.
git add <cac-file-da-review>
git commit -m "feat(order): implement checkout and payment flow"
# Chỉ khi đã cấu hình origin thật:
git push -u origin feature/order-payment
```

Repository đã có origin GitHub Phucsbinz/Project_Web và đủ ba feature cùng integration/utetra. Kiểm tra ngày 14/10/2026 cho thấy các nhánh remote vẫn ở đặc tả cũ 28 UC và chưa nhận commit baseline main. Bản bổ sung hiện tại đồng bộ các nhánh cục bộ; GitHub chỉ thay đổi khi chủ repository yêu cầu push/PR. Không bịa link PR hoặc lịch sử của thành viên.

Tích hợp: integration lấy main → catalog → identity → order; resolve cùng chủ file, kiểm giá/store/promo → checkout, quyền/OTP/address → đơn, payment/refund → điểm/quota/commission/report, realtime scope và responsive. Chạy ma trận 55 UC bắt buộc + phần thêm bật; phần chưa code ghi rõ. Merge về main sau review, rồi feature lấy main tiếp. Không force-push/reset làm mất công việc.

| UC/role | Người/nhánh | Commit/PR | Công việc thật | Test/minh chứng | Reviewer/ngày | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền] | [điền] |

## Quy trình cập nhật nền chung

Main giữ cả ba BRANCH_*.md để phạm vi UC và cấu hình được cập nhật cùng nhau. Danh mục chuẩn hiện tại: catalog 18 P0, identity 16 P0, order 21 P0 + 3 Shipper P1; tổng 55 P0 + 3 P1. Mọi mã đều có owner và hồ sơ trong docs/features. Nhóm chưa điền tên thành viên/reviewer; không tự gán người thật.

1. Review thay đổi chung trên main: pom/config/security/layout/contract/migration và hướng dẫn UC.
2. Build/test main; kiểm HTTP home/login/health/assets và quyền. TiDB/Flyway/TLS kiểm riêng khi có mạng native đến DB.
3. Trong từng feature, merge main; giữ commit chức năng riêng. Khi BRANCH_*.md cũ xung đột, lấy hướng dẫn hiện hành đã chốt trên main và ghi lại phần riêng còn phù hợp.
4. Kiểm 55+3 mã UC, owner/hồ sơ và tree nền chung với main. Test phần riêng khi nhánh có code mới.
5. Integration nhận main và ghép feature theo contract; chạy test xuyên suốt khi có nghiệp vụ. Không coi merge hồ sơ là triển khai UC.
6. Sau khi được yêu cầu push, kiểm remote mới nhất và push bình thường; không force-push/reset để bỏ commit của bạn trong nhóm.
