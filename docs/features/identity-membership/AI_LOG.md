# IDN — Nhật ký AI của nhánh feature/identity-membership

Trạng thái: chưa có prompt triển khai thật từ thành viên. Yêu cầu khởi tạo của người dùng nằm tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi mọi prompt thực dùng cho 16 UC P0 của identity-membership theo IMPLEMENTATION hiện tại: code/JWT/CSRF/ledger, sửa lỗi, sơ đồ, code/UI, setup, kiểm tra quyền và review. Dùng [mẫu prompt](../../templates/PROMPT_ENTRY.md), ID IDN-AI-001... Lưu response, file/hình và commit; che secret/token; nêu nhóm sửa và kiểm chứng. Không ghi prompt mẫu như đã gửi.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.
