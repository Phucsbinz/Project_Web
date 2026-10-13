# UTETra — Nhật ký AI dựng nền chung main

Trạng thái: chưa sinh mã nguồn B0. Prompt của người dùng trong cuộc trò chuyện soạn tài liệu đã được lưu nguyên văn trong [AI_PROMPT_HISTORY.md](AI_PROMPT_HISTORY.md).

Khi dựng B0, sao chép cấu trúc [PROMPT_ENTRY.md](templates/PROMPT_ENTRY.md) xuống dưới, dùng ID B0-AI-001, 002... Ghi prompt/response thật, lựa chọn version và contract, file sửa, kiểm chứng WAR/DB/layout, phần nhóm sửa và commit. Prompt mẫu trong hướng dẫn chưa được tính là đã dùng.

| Prompt ID | UC/công việc | Người/model | Kết quả/file | Kiểm chứng | Commit |
| --- | --- | --- | --- | --- | --- |
| [chưa có prompt triển khai B0] | | | | | |


## B0-AI-001 — Đọc tài liệu và dựng cấu hình chung

Prompt nguồn: P-REQ-009 đến021 tại AI_PROMPT_HISTORY (đã ghi nguyên văn, che password). AI hỗ trợ đọc DOCX mẫu/guide/PDF, chọn bộ version, cấu hình stack, tạo database UTETra riêng và mở rộng đặc tả. Nhóm cần review role/phạm vi/threshold và bổ sung credential provider.

File thực: pom/Wrapper, application properties, migration V1, Entity/Repo identity, Security/JWT/session, SiteMesh/layout/Bootstrap, STOMP, Media adapter, test và docs. Kiểm chứng: JDBC TLS tạo utetra_db, build WAR,13test, Flyway/JPA startup; ghi HTTP cuối ở BASELINE_VERIFICATION.

Lỗi/sửa: PowerShell Maven -D có dấu chấm bị tách → quote; PDF extraction stdout cp1252 → UTF8 (text files đã trích, không sửa PDF); SiteMesh decorator forward bị firewall // → Security dispatcher REQUEST/ASYNC/ERROR, giữ firewall request ngoài. Không coi code provider chưa credential hoặc UC nghiệp vụ chưa triển khai là đã hoàn thành. Commit/reviewer: [điền sau khi chốt].
