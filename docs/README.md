# UTETra — Hồ sơ và cấu hình chung của nhóm

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

Repo có nền cấu hình, chưa có tất cả tính năng thương mại. Credential SMTP/Cloudinary/VNPAY chưa cung cấp không được ghi như đã tích hợp; feature giữ config từ main và bổ sung UC của mình.
