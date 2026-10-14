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

## B0-AI-002 — Hoàn thiện đặc tả, nền và đồng bộ nhánh

Ngày: 14/10/2026. Prompt nguồn P-REQ-022. Đối chiếu tài liệu cấu hình và remote branches: main mới55+3UC, feature/integration cũ28UC chưa mã nền. Bổ sung đầu vào/tiền-hậu điều kiện/luồng/lỗi/quyền/schema/service-route dự kiến/test và activity cho58hồ sơ; ba BRANCH_*.md chốt trên main. Sửa mục lục Word và các chỉ dẫn scope/remote cũ; giữ lịch sử và các ô bằng chứng chưa có.

Sửa SiteMeshConfig decoratorPrefix rỗng; thêm BaselineViewTest Tomcat thật. Verify16/16 và buildWAR đạt. Gặp assertion tên/version CSS có hai khoảng trắng; sửa kiểm nội dungCSS/navbar thay vì header literal rồi rerun. TLS/truststore giữ xác minh. TiDB cloud bị DNS/nativeegress và CONNECT403; không sửa DB/password/V1, không ghi là provider/cloud DB chạy thành công.

Đồng bộ Git cục bộ giữ commit hướng dẫn riêng trong merge history; remote GitHub chưa thay đổi. Code/profile/UC feature chưa triển khai giữ đúng trạng thái. Người/reviewer/ảnh thực của thành viên chưa được xác nhận thì chưa tự điền.

## Liên kết prompt của phiên cloud — 14/10/2026

Prompt nguyên văn được lưu tập trung tại [AI_PROMPT_HISTORY — phiên cloud](AI_PROMPT_HISTORY.md#phiên-cloud--nhánh-main-14102026), mã CLOUD-MAIN-001 đến013. Nhánh thực hiện: main; phạm vi nền/cấu hình/đặc tả và đồng bộ chung. Nhật ký này lưu kết quả/file/test/commit; không tạo một bộ prompt riêng thay file người dùng đã chọn.

Kết quả: bổ sung58đặc tả,3hướng dẫn nhánh, sửaSiteMesh và verify16/16; commit đầu f11b6af. TiDB cloud vẫn bị chặn mạng, providersdisabled; chức năng thương mại chưa triển khai. Các feature dẫn về cùng bản ghi nguồn cho công việc nền kế thừa.
