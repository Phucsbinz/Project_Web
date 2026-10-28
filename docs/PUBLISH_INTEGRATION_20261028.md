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

## Quyết định khi ghép catalog

- Commit lưu toàn bộ thay đổi ban đầu: `9f17146` (226 file). Ghép thêm integration `7c7ea82` trước catalog.
- Catalog `21d2d64` gây 16 file conflict do nhánh độc lập ghép chọn lọc từ integration cũ. Đối chiếu từng hunk, không chọn toàn bộ một phía: giữ kiểm version category/topping/area, JWT/CSRF/session hiện hành, luồng giỏ/đơn nhóm và review/comment đầy đủ; giữ homepage thống nhất mới nhất từ integration.
- Nhận capability `commerceAvailable` và meta `utetra-commerce` của catalog trong `NavigationAdvice`, `app.jsp`, `catalog.js`, `site-ui.js`; chúng kiểm module thực và không thay quyền backend. Giữ cùng một bundle/layout, không sửa main hay tự đồng bộ nhánh ngoài phạm vi người dùng yêu cầu.
- Nhận tài liệu/ảnh/proposal và các test mới của catalog. Test frame protection giữ SAMEORIGIN cho quick view và DENY cho trang thường; assertion chỉ có quote và không có community của nhánh catalog độc lập được đổi sang hành vi tích hợp có giỏ và community. Không loại test hoặc giảm kiểm quyền/frame.

## Kết quả sau giải quyết conflict

- `mvnw.cmd -B -ntp test`: **261 tests, 0 failures/errors/skipped, BUILD SUCCESS**.
- `py -3 tools/check_scoped_fixes_ui.py`: **16 nhóm kiểm đạt**, Tomcat/SiteMesh/Edge thực ở 390/768/1440; 0 lỗi JS/console, 0 request failure bất thường, 1 navigation abort logout có HTTP 200 và kiểm chứng revoke. Có chọn đủ 101 dòng mua, gửi review/comment của dòng cũ, host không có món thanh toán/theo dõi đúng payer.
- Catalog browser: **3/3 ca đạt** (`global`, `key`, `version`), 0 page errors. Lượt đầu chưa chạy được vì cache `.local/ui-audit-classes` có thêm controller của fixture ScopedFix, gây duplicate mapping. Lượt đạt dùng chính `check_all_ui.py --catalog-regressions` với output class folder mới `.local/publish-20261028/catalog-ui-classes`; không sửa business code để chữa fixture.
- Build riêng có đủ SQL proposal mà test đọc ngoài src. Lượt copy đầu thiếu hai file SQL nên 2 test báo NoSuchFile, được sửa bằng bổ sung đầu vào chứ không bỏ/sửa test. Đối chiếu **323 file đầu vào** (src, wrapper, pom, SQL proposal), không có khác biệt với workspace. Kết quả clean verify cuối cập nhật khi lệnh hoàn thành.
- Tài liệu seed được agent khác hoàn tất trong lúc kiểm chứng cũng được đưa vào phạm vi công bố đầy đủ. Agent công bố không chạy seed/migration cloud, không restart app/provider và không xác nhận các kết quả provider/DB của hồ sơ cũ như kiểm thử mới trong lượt này.

## Cách thành viên nhận bộ skill

Sau khi nhánh remote được công bố, thành viên lưu công việc local của mình, fetch rồi cập nhật `integration/utetra` hoặc merge nhánh đó vào nhánh feature của mình. Bộ skill nằm trong `.agents/skills`, metadata nguồn nằm ở `skills-lock.json`. Đọc `AGENTS.md` và quy ước UI trước khi giao agent sửa code. Không cần chép đường dẫn tuyệt đối từ máy người công bố sang máy khác.
