# ORD — Nhật ký AI của nhánh feature/order-payment

Trạng thái: chưa có prompt triển khai thật từ thành viên. Yêu cầu khởi tạo hồ sơ đã ghi tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md).

Ghi mọi prompt thực dùng cho 21 UC P0 và 3 UC Shipper P1 của order-payment theo IMPLEMENTATION hiện tại: giỏ/checkout/COD/transaction/socket/report, sửa lỗi, sơ đồ, ảnh diễn giải code/UI, setup và review. Dùng [mẫu prompt](../../templates/PROMPT_ENTRY.md), ID ORD-AI-001... Lưu response/code/nguồn hình/ảnh, cách nhóm sửa, testcase và commit. Không ghi QR gateway đã tích hợp hoặc test đã qua nếu chưa thực hiện.

| ID | Ngày/người/công cụ | UC/mục tiêu | Prompt/response | File/hình | Kiểm chứng/nhóm sửa | Commit |
| --- | --- | --- | --- | --- | --- | --- |
| [chưa có] | | | | | | |

## Bản ghi nguyên văn

[Thêm bản ghi thực theo mẫu.]

## Cập nhật baseline 3.0

Phạm vi UC/role theo IMPLEMENTATION và USE_CASES hiện tại, không dùng danh sách mã cũ. Code/prompt/hình/test của thành viên chưa thực hiện giữ trạng thái chưa có; journal khởi tạo ở AI_PROMPT_HISTORY. Cấu hình trực tiếp properties theo main, không .env.

## Prompt nền chung kế thừa từ main — 14/10/2026

Xem các bản ghi nguyên văn CLOUD-MAIN-001 đến013 tại [AI_PROMPT_HISTORY](../../AI_PROMPT_HISTORY.md); kết quả nền tại [BASELINE_AI_LOG](../../BASELINE_AI_LOG.md). Người dùng yêu cầu bổ sung đặc tả, sửa nền và đồng bộ nhánh; đây là công việc main, không phải prompt triển khai nghiệp vụ của thành viên. Chưa có prompt code UC riêng trong phiên này. Khi bắt đầu UC của nhánh, append prompt nguyên văn vào AI_PROMPT_HISTORY.md, ghi nhánh/UC; file này dẫn ID prompt và ghi note UC, code/commit/test/response thực.
