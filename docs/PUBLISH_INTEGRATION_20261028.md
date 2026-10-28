# Chia sẻ thay đổi và bộ skill — 28/10/2026

## Yêu cầu thực đã nhận

```text
bạn push các commit và cái này d:\WEBCUOIKI\.agents lên bạn tôi dùng chung nha hiện tại đang nhánh nào á thì push nhánh đó lên và merge qua nhánh integration trước hết hãy pull code các nhánh về rồi merge để tránh conflict rồi hãy push
```

Yêu cầu bổ sung:

```text
push đầy đủ các phần đã change nha
```

## Phạm vi và trạng thái ban đầu

- Nhánh hiện tại: `feature/order-payment`, HEAD `269ea2c`; có bốn commit phía trước upstream trước khi tích hợp.
- Đích tích hợp được người dùng chỉ định: `integration/utetra`.
- Bao gồm toàn bộ thay đổi tracked/untracked không bị ignore: code, migration, test, hồ sơ audit, công cụ/dữ liệu catalog, ảnh kiểm thử, `.agents/skills`, `skills-lock.json`, và thao tác xóa ảnh ở root đã có trong workspace.
- Giữ nguyên `.local`, build output và công cụ cài riêng trong ignore; không đưa credentials mới vào hồ sơ.
- Đã chạy `git fetch --all --prune`. Remote cập nhật catalog tới `21d2d64`, integration tới `7c7ea82`; identity/identify/main không có commit ngoài lịch sử HEAD hiện tại.
- Các cập nhật remote được ghép bằng merge, không rewrite lịch sử hoặc force push. Không sửa datasource/provider/JWT vì nhu cầu tích hợp.

## Kiểm chứng của lượt công bố

Log thô ở `.local/publish-20261028/` (không công bố log có thể chứa request/credential fixture). Kết quả baseline, merge và kiểm thử cuối được cập nhật sau khi từng lệnh hoàn thành. Không dùng các báo cáo audit cũ làm bằng chứng cho lượt này.

- Baseline đầu: `mvnw verify` chạy 257 tests, 1 failure tại `CatalogIntegrationTest.updatesKeepVariantIdsAndRejectStaleVersions` (version vẫn bằng 0). Lượt chạy riêng đạt; lượt baseline lặp lại có 257 tests đạt nhưng repackage thất bại vì không rename được WAR đang bị giữ khóa. Không ghi nhận hai lượt verify này là build thành công.
- Điều tra: `Product.touch()` gán `Instant.now()` có thể không đổi timestamp khi hai lần save cùng một nhịp đồng hồ; parent không dirty và version không tăng trước khi thay size/topping. Giữ nguyên assertion, cố định `Instant.now()` bằng static mock có scope trong test hiện có để tái hiện ổn định: 1 test, 1 failure đúng version 0 trước sửa. Bản sửa bảo đảm `touch()` tạo giá trị khác khi đồng hồ không tiến; không đổi schema/API.
- Reviewer độc lập đã đọc thay đổi và không thấy blocker khác trong phạm vi tích hợp. Reviewer không chạy test/DB và không sửa file; kiểm chứng thực do agent chính thực hiện.
- Sau bản sửa: `mvnw.cmd -B -ntp test` chạy **257 tests, 0 failures/errors/skipped, BUILD SUCCESS** trên JDK 21. Cố định thời gian trong test vẫn giữ nguyên kiểm version cũ bị từ chối và ID variant không đổi. Shared UI, `git diff --check`, syntax Python và JSON metadata skill đạt.

## Cách thành viên nhận bộ skill

Sau khi nhánh remote được công bố, thành viên lưu công việc local của mình, fetch rồi cập nhật `integration/utetra` hoặc merge nhánh đó vào nhánh feature của mình. Bộ skill nằm trong `.agents/skills`, metadata nguồn nằm ở `skills-lock.json`. Đọc `AGENTS.md` và quy ước UI trước khi giao agent sửa code. Không cần chép đường dẫn tuyệt đối từ máy người công bố sang máy khác.
