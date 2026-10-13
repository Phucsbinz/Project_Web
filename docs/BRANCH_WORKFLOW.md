# UTETra — Chia nhánh theo nhóm use case

## 1. Nhánh và trách nhiệm

| Nhánh | Người phụ trách | Use case | Kết quả bàn giao |
| --- | --- | --- | --- |
| `main` | Nhóm trưởng, các thành viên review | B0 — Nền tảng chung | Phiên bản thư viện, WAR, profile DB, migration, layout, contract, tài liệu chung |
| `feature/catalog-sales` | TV1: [điền họ tên] | UC-CAT-01 đến UC-CAT-09 | Luồng quản lý và bán danh mục: Entity → JSP → kiểm thử → hồ sơ hình/prompt |
| `feature/identity-membership` | TV2: [điền họ tên] | UC-AUTH-01 đến 05; UC-USER-01 đến 03; UC-ADMIN-01 | Luồng tài khoản/thành viên, phân quyền, địa chỉ, dịch vụ điểm cho đơn hàng |
| `feature/order-payment` | TV3: [điền họ tên] | UC-ORD-01 đến 08; UC-NOT-01; UC-RPT-01 | Luồng giỏ → đặt → COD → xử lý đơn → realtime → báo cáo |
| `integration/utetra` | Nhóm trưởng và cả nhóm | Tích hợp toàn bộ | Ghép các nhánh, kiểm thử xuyên suốt, tổng hợp hồ sơ và đưa bản ổn định về main |

Một nhánh chức năng chứa nhiều use case có cùng luồng nghiệp vụ. Người phụ trách hoàn thiện mọi lớp của use case, không giao tách riêng Controller/Service/JSP cho những người khác. Mỗi nhánh có file giải thích riêng ở gốc: `BRANCH_CATALOG_SALES.md`, `BRANCH_IDENTITY_MEMBERSHIP.md`, `BRANCH_ORDER_PAYMENT.md`. Tên khác nhau để khi ghép nhánh không xung đột một file hướng dẫn chung.

## 2. Cửa kiểm tra baseline B0

Hiện repository khởi tạo chỉ có đặc tả và hồ sơ Markdown. Chưa có `pom.xml`, Maven Wrapper hoặc ứng dụng Spring Boot chạy được. Các nhánh Git được tạo trước để phân công; lập trình tính năng bắt đầu sau khi B0 trên main được hoàn thành và đồng bộ.

Nhóm trưởng triển khai B0 theo [COMMON_BASELINE.md](COMMON_BASELINE.md), với các thành viên review: khóa version, khởi tạo WAR/JSP/JSTL/SiteMesh, profile DB, layout, config mẫu, contract Java và migration đã chốt. Không yêu cầu cả ba người khởi tạo ba ứng dụng riêng.

Sau B0, mỗi nhánh lấy lại `main`. Nếu B0 chưa xong, thành viên vẫn có thể chuẩn bị flow, testcase, mockup và prompt; ghi rõ chúng là thiết kế dự kiến.

## 3. Phân quyền sửa file chung

| File/khu vực | Quy tắc |
| --- | --- |
| pom.xml, Maven Wrapper, application*.yml, cấu hình WAR/JSP/SiteMesh | Thay đổi trên main qua đề xuất baseline và review; nhánh chức năng không tự đổi version |
| Entity/migration/DTO dùng chung | Theo chủ sở hữu tại COMMON_BASELINE; thay đổi contract phải được nhánh dùng nó đồng ý |
| Ba đặc tả gốc, docs/README.md, COMMON_BASELINE.md, SETUP.md | Nhóm trưởng điều phối; ghi lý do, cập nhật liên kết và các tài liệu bị ảnh hưởng |
| docs/features/catalog-sales/** | TV1 ghi hồ sơ và minh chứng của mình |
| docs/features/identity-membership/** | TV2 ghi hồ sơ và minh chứng của mình |
| docs/features/order-payment/** | TV3 ghi hồ sơ và minh chứng của mình |
| docs/AI_PROMPT_HISTORY.md | Nhật ký yêu cầu chung; không sửa lại nội dung nguyên văn đã ghi |

## 4. Làm việc trong nhánh chức năng

Ví dụ cho TV3; thay tên nhánh nếu nhận module khác:

```powershell
git switch main
git pull --ff-only origin main
git switch feature/order-payment
git merge main
# Code và cập nhật hồ sơ use case/prompt/hình.
git add <cac-file-da-kiem-tra>
git commit -m "feat(order): implement checkout with COD"
git push -u origin feature/order-payment
```

Lệnh liên quan `origin` chỉ dùng sau khi nhóm cấu hình remote thật. Bộ nhánh được tạo local; không giả định đã có GitHub hoặc đã push. Không dùng `git add .` khi chưa kiểm tra secret/ảnh thừa/file build. Trước commit xem `git diff`, chạy kiểm thử liên quan và cập nhật hồ sơ; không force-push nhánh chung.

Chia commit theo từng use case có thể review trên cùng nhánh. Không cần tạo một nhánh cho từng tầng kỹ thuật. Khi đổi máy hoặc giao cho AI, gửi prompt mở phiên trong `AI_WORKFLOW.md` kèm tên nhánh.

## 5. Ghép chức năng

1. Chốt/đồng bộ B0 trên main và xác nhận contract giữa ba nhánh.
2. Trên `integration/utetra`, merge `main`, sau đó lần lượt `feature/catalog-sales`, `feature/identity-membership`, `feature/order-payment`.
3. Chủ file bị xung đột tham gia giải quyết; không chọn toàn bộ ours/theirs làm mất chức năng của người khác.
4. Kiểm thử luồng AT-01–16 trong USE_CASES.md; đặc biệt giá catalog → checkout, quyền/địa chỉ → đơn, điểm → hủy/hoàn tất, WebSocket → đúng chi nhánh.
5. Mỗi chủ module ký checklist bàn giao; cập nhật SETUP và kiểm kê hình/prompt.
6. Đưa nhánh tích hợp đã qua kiểm chứng vào main bằng PR hoặc merge được cả nhóm review. Main trở thành bản ứng dụng tích hợp ổn định, đồng thời tiếp tục là nguồn cấu hình chung.
7. Các nhánh còn làm tiếp lấy main mới để cùng baseline. Không tự merge các nhánh feature vào nhau mà không cập nhật hợp đồng và hồ sơ.

## 6. Checklist trước ghép nhánh

- [ ] UC được giao có đủ Entity/Repository/Service/Controller/JSP và kiểm thử liên quan.
- [ ] Giữ toàn bộ chức năng được giao theo đặc tả; chỉ loại tính khoảng cách/tự chọn chi nhánh gần nhất.
- [ ] Không đổi stack, profile, schema hoặc quy tắc tiền/điểm ngoài quy trình review.
- [ ] Có ghi prompt đã dùng thật, vai trò của AI, cách nhóm sửa và kiểm chứng.
- [ ] Có nguồn sơ đồ, ảnh sơ đồ, ảnh code có chú giải, ảnh giao diện thật và chú thích hình.
- [ ] Có setup và dữ liệu demo tối thiểu riêng cho module.
- [ ] Liệt kê chức năng còn thiếu và lỗi chưa xử lý; không ghi hoàn thành giả.

## 7. Nhật ký thay đổi baseline

| Ngày | Commit main | Thay đổi | Nhánh bị ảnh hưởng | Người review | Đã đồng bộ |
| --- | --- | --- | --- | --- | --- |
| [điền] | [điền hash] | [mô tả] | [liệt kê] | [điền] | [điền] |
