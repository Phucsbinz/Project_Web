# CAT — Nhật ký AI của nhánh feature/catalog-sales

Trạng thái: chưa có prompt sinh code/sinh hình thực tế từ thành viên. Các prompt yêu cầu khởi tạo hồ sơ được lưu tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi tại đây mọi prompt thành viên gửi khi làm 18 UC P0 của catalog-sales, bao gồm code, sửa lỗi, hình kiến trúc/activity, diễn giải code/UI, setup và review. Sao chép [mẫu prompt](../../templates/PROMPT_ENTRY.md), dùng CAT-AI-001... Lưu response dài trong ai-responses; liên kết code/ảnh/commit và ghi nhóm đã chỉnh/kiểm thử gì. Không chuyển prompt gợi ý thành “đã dùng” khi chưa gửi.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu, giữ lịch sử cả kết quả AI sai và lần sửa.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến012 tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào file này và liên kết note UC, code/commit/test/response thực.
