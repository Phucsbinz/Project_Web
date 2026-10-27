# UTETra — Hồ sơ và cấu hình chung của nhóm

> Mới nhất 27/10: [đối chiếu role/nhập địa chỉ/CSKH/bảo mật](ROLE_REQUIREMENTS_20261027.md), [kiểm kê DB thực 51 bảng và 14 bảng trống](DB_AUDIT_20261027.md), [hướng dẫn CSKH](features/customer-support/README.md). Clean verify 206 test và 5 browser scenarios đạt; 2704/2705 đã áp dụng cloud, hash 23 tài khoản không đổi.

Tích hợp UI tất cả module: [phạm vi vai trò, ảnh và kiểm tra desktop/mobile](ALL_MODULE_UI_20261026.md).

Giao diện tham chiếu 26/10: [màu/font, hình desktop–mobile và kết quả kiểm tra](UI_REFERENCE_REFINEMENT_20261026.md). Bản thử trên integration, chưa phát hành sang main.

> Đọc trạng thái mới nhất: [FINAL_REQUIREMENTS_STATUS](FINAL_REQUIREMENTS_STATUS.md). Clean verify 161 tests đạt; VNPAY có code/test nhưng thiếu merchant sandbox thật; Manager carrier theo shop đã bổ sung. Có [khảo sát có nguồn](SURVEY_FINDINGS_20261024.md), bộ hình report-assets và SETUP viết lại.

Mới nhất 24/10: [JWT lưu DB, tồn kho/voucher đồng thời và WebSocket checkout](features/order-payment/REALTIME_CHECKOUT_JWT.md). Verify 153 tests đạt; có minh chứng STOMP trên trình duyệt với H2 riêng và migration thứ 8 trên TiDB. Code đang trên integration, chưa push.

Bổ sung sau audit: [QR ngân hàng và việc giao từng thành viên](REMEDIATION_20261024.md); [hướng dẫn nghiệp vụ/setup bank transfer](features/order-payment/BANK_TRANSFER.md). Bản sửa đang trên integration, chưa push.

Kiểm tra theo yêu cầu thầy, cập nhật 24/10: [ma trận yêu cầu, mức hoàn thiện và các phần còn thiếu](TEACHER_REQUIREMENTS_AUDIT_20261024.md). Đây là đánh giá source/test/hồ sơ tại commit 8369b67, không phải xác nhận project đã nghiệm thu 100%.

Order, cập nhật 21/10: [đồng bộ catalog/identity, contract và kiểm chứng](features/order-payment/INTEGRATION_20261021.md).

Nhánh `identify`: xem [thiết lập gửi mail sau khi pull](features/identity-membership/MAIL_SETUP.md)
và [runbook identity](features/identity-membership/RUNBOOK.md).
**Giao diện chung:** đọc [SHARED_UI](SHARED_UI.md) và AGENTS.md. Toàn project dùng duy nhất layouts/app.jsp; agent các nhánh không tạo layout theo role/module.

> Cập nhật 13/10/2026 theo toàn bộ quy định mới của giảng viên. Đọc đầu tiên [QUY_DINH_VA_CAU_HINH_CHUNG.md](QUY_DINH_VA_CAU_HINH_CHUNG.md).

## 1. Bộ tài liệu chính

- [PROJECT_PLAN](../PROJECT_PLAN.md): stack, MVC/3 tầng, scope và phân công.
- [DATABASE_DESIGN](../DATABASE_DESIGN.md): 43 bảng bắt buộc + 2 Shipper thêm; V1 hiện thực 3 bảng identity.
- [USE_CASES](../USE_CASES.md): 55 UC P0 + 3 UC Shipper P1, actor/owner/validation/payment/state.
- [COMMON_BASELINE](COMMON_BASELINE.md), [SETUP](SETUP.md), [BASELINE_VERIFICATION](BASELINE_VERIFICATION.md): cấu hình thực, chạy và chứng cứ kiểm thử.
- [Khảo sát và phân tích](SURVEY_SYSTEM_ANALYSIS.md): bố cục mẫu DOCX và phần nhóm phải khảo sát thật.
- [BRANCH_WORKFLOW](BRANCH_WORKFLOW.md): main → feature → integration; từng UC do một owner làm đủ tầng.
- [AI_WORKFLOW](AI_WORKFLOW.md), [prompt đã gửi](AI_PROMPT_HISTORY.md), [REPORT_ASSEMBLY](REPORT_ASSEMBLY.md): nhật ký và báo cáo DOCX.

## 2. Nhánh và hồ sơ

| Thành viên/nhánh | Use case/role liên quan | Hồ sơ |
| --- | --- | --- |
| TV1 catalog-sales | Guest/User menu/top; Vendor shop/catalog/promo; Manager/Admin catalog/shop | [IMPLEMENTATION](features/catalog-sales/IMPLEMENTATION.md) |
| TV2 identity-membership | Guest/User OTP/auth/profile/community/points; Manager/Admin user/role/scope | [IMPLEMENTATION](features/identity-membership/IMPLEMENTATION.md) |
| TV3 order-payment | User/Vendor checkout/COD/VNPAY; Vendor vận hành; Manager/Admin carrier/commission/refund/revenue; Shipper thêm | [IMPLEMENTATION](features/order-payment/IMPLEMENTATION.md) |

Main có pom/config/layout/migration/common code. Không tự dựng project/thư viện khác khi chuyển nhánh. Vendor kế thừa quyền User; Manager scope shop; Admin toàn app. Giữ tất cả chức năng thầy yêu cầu, chỉ phần khoảng cách đã bỏ theo quyết định nhóm. MySQL/TiDB utetra_db là CSDL riêng; cấu hình trực tiếp application*.properties và được theo dõi bởi Git.

## 3. Cách ghi hồ sơ

Mỗi UC dùng [mẫu hồ sơ](templates/USE_CASE_NOTE.md), prompt dùng [mẫu prompt](templates/PROMPT_ENTRY.md). Ghi actor/owner/schema/file/symbol/commit, kiến trúc/activity/sequence, hướng dẫn từng bước, ảnh code/UI thật, setup, test và kết quả AI nhóm sửa. Prompt gợi ý không tính đã dùng; code/hình chưa thực hiện phải ghi rõ.

Repo đã có code thương mại tích hợp; mức hoàn thiện xem audit và biên bản bổ sung 24/10 ở đầu file. Cloudinary ảnh đã có cấu hình/kiểm provider trước đó; SMTP phải thiết lập theo máy; VNPAY/MOMO chưa triển khai. Chuyển khoản QR là đối soát thủ công, không thay bằng chứng tích hợp cổng online. Feature giữ cấu hình/layout chung và cập nhật hồ sơ UC của mình.
