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

Repo chưa có remote origin trong lúc dựng nền; chỉ nhánh local chưa đủ điểm minh chứng GitHub. Nhóm cung cấp URL private repository rồi cấu hình remote/push các nhánh. Không bịa link PR hoặc lịch sử của thành viên.

Tích hợp: integration lấy main → catalog → identity → order; resolve cùng chủ file, kiểm giá/store/promo → checkout, quyền/OTP/address → đơn, payment/refund → điểm/quota/commission/report, realtime scope và responsive. Chạy ma trận 55 UC bắt buộc + phần thêm bật; phần chưa code ghi rõ. Merge về main sau review, rồi feature lấy main tiếp. Không force-push/reset làm mất công việc.

| UC/role | Người/nhánh | Commit/PR | Công việc thật | Test/minh chứng | Reviewer/ngày | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền] | [điền] |
