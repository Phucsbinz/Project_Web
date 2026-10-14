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

## Nhật ký prompt nguyên văn của nhánh main — phiên cloud

Ngày ghi: 14/10/2026, Asia/Ho_Chi_Minh. Người gửi: chủ repository; công cụ: Codex. Không tự gán giờ gửi/model cụ thể khi không có dữ liệu. Phạm vi: môi trường, nền main và phân công/synchronization chung; không phải prompt code thương mại của từng feature.

### CLOUD-MAIN-001

```text
Sử dụng $cloud-environment-onboarding:setup để thiết lập môi trường đám mây này
```

### CLOUD-MAIN-002

```text
bạn đọc đầy đủ file md với file ngữ cảnh đi để hiểu tôi đã làm những gì và làm được gì
```

### CLOUD-MAIN-003

```text
ụa cái bạn làm và sửa này nó sẽ được commit vô hay github hay sao tại thấy bạn làm trên cloud gì nè
```

### CLOUD-MAIN-004

```text
ví dụ thay đổi thì bạn sẽ lưu sao á
```

### CLOUD-MAIN-005

```text
bạn hiểu hết ngữ cảnh đang làm chưa
```

### CLOUD-MAIN-006

```text
nhánh main sẽ là cấu hình chung rồi sẽ đồng bộ vào các nhánh nhóm các chức năng để bạn tôi dùng chung cấu hình thay vì mỗi người 1 cái
```

### CLOUD-MAIN-007

```text
xem cấu hình oke chưa bạn xem folder tài liệu cấu hình đi đọc hết các file
```

### CLOUD-MAIN-008

```text
đọc rồi xem cấu hình hiện tại đúng theo hướng dẫn cấu hình đó chưa
```

### CLOUD-MAIN-009

```text
và bạn xem các chức năng hiện tại đã đồng bộ cho project này chưa ý là các nhánh branch đầy đủ cho project chưa rồi mới đồng bộ cấu hình main qua các nhánh
```

### CLOUD-MAIN-010

```text
ý là đầy đủ use case chưa
```

### CLOUD-MAIN-011

```text
bổ sung tất cả đi
```

### CLOUD-MAIN-012

```text
à nhớ các prompt của tôi thì note vào prompt của nhánh đó nha
```

Kết quả chung: cài JDK21/cache/proxy ngoài checkout; verify nền13test; kiểm main55P0+3P1 và remote cũ28UC; bổ sung58đặc tả cùng3hướng dẫn nhánh; sửa prefixSiteMesh và thêm HTTP regression test, verify16/16. Commit code/tài liệu đầu: f11b6af. Đang hoàn tất đồng bộ local; GitHub chưa push. TiDB bị chặn mạng native, providers còn disabled. Chưa triển khai55chức năng thương mại hoặc3Shipper, chưa tạo screenshot/codeUC/khảo sát/DOCX giả.

Prompt riêng cho UC feature sau này lưu nguyên văn tại docs/features/<module>/AI_LOG.md và liên kết hồ sơ UC. Những prompt nền main ở trên được feature kế thừa khi merge main, không tính như thành viên đã dùng AI để code UC của mình. Chỉ append/đính chính lịch sử; không đổi câu chữ người dùng.
