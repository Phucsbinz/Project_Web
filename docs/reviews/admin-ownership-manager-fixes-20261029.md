# Admin đổi Vendor sở hữu và sửa phân công Manager

Ngày: 29/10/2026. Nhánh `integration/utetra`, baseline `963c277`. Giữ nguyên các tài liệu/ảnh/file untracked có sẵn. Không commit/push/merge; không tác động runtime 8080 (PID 16908), DB demo/shared, provider hoặc schema.

## Prompt thực

```text
cập nhật thêm giao diện để Admin chọn hoặc đổi Vendor sở hũu chi nhánh có sẵn, mục phân công manager có nút sửa nhưng thao tác không được
```

Công cụ: Codex; không suy đoán tên model. Áp dụng systematic-debugging, test-driven-development và verification-before-completion. Kết quả code được áp dụng local, chưa commit. Báo cáo là nhật ký prompt/kết quả thực theo `docs/templates/PROMPT_ENTRY.md`.

## Nguyên nhân và thay đổi

1. **Chọn/đổi Vendor**: StoreInput cố ý không nhận ownership; chưa có endpoint/form riêng để Admin đổi chủ của cửa hàng đã tạo. Bổ sung danh sách Vendor đủ điều kiện và endpoint `/api/admin/catalog/stores/{id}/owner`, kiểm quyền Admin, Vendor active/verified và version bắt buộc. Khóa tài khoản đích rồi cửa hàng; cập nhật sở hữu, version và audit chủ cũ/mới cùng transaction. Không sửa role, membership, lịch sử đơn hay số tiền đã ghi nhận. Form nằm trong màn chi nhánh hiện có, nút lưu riêng và có xác nhận.
2. **Sửa phân công**: baseline UI tái hiện sửa active/revoke vẫn thành công. Lỗi có căn cứ là khi sửa Manager/chi nhánh, API upsert cặp đích nhưng không biết cặp gốc nên phân công cũ vẫn active. Bổ sung cặp gốc tùy chọn khi sửa; thu hồi đúng cặp gốc và tạo/cập nhật cặp đích trong một transaction, khóa tài khoản theo ID tăng dần. Không ghi đè đích đã active. UI có trạng thái sửa rõ, focus, nút Hủy; tạo thêm phân công vẫn độc lập.
3. Khi kiểm UI mới, H2 trả key JDBC uppercase cho danh sách Vendor khiến select không có ID đúng; đã sửa bằng ánh xạ `id/name` tường minh, kiểm lại qua browser và test HTTP.

## Bằng chứng trước và sau

- Baseline `CatalogWorkflowRegressionTest`: 12 tests, 0 fail/error/skipped.
- Test ownership trước sửa: 2 failures do endpoint chưa có; `.local/admin-store-fixes/red.log`.
- Test chuyển phân công trước sửa: 1 failure vì cặp cũ vẫn có quyền; `manager-red.log`.
- Sau sửa: `CatalogWorkflowRegressionTest` cùng các bộ hồi quy dưới đây đạt **169 tests, 0 failures, 0 errors, 0 skipped**; `regression-final.log`.
- Ca mới: đổi chủ và đọc lại, chủ cũ bị 403 ở service; sai role/tài khoản khóa/missing version/stale version bị từ chối; danh sách Vendor đúng key và loại tài khoản khóa; chuyển phân công thu hồi cũ; đích đã active bị từ chối mà nguồn không đổi.

## Lệnh và môi trường

Java 21, Maven Wrapper; toàn bộ thao tác ghi vào H2 cô lập `test`, không cloud. Chạy:

```powershell
.\mvnw.cmd "-Dtest=CatalogWorkflowRegressionTest,CatalogIntegrationTest,CatalogCrudReadbackTest,RoleOperationsIntegrationTest,ManagerPermissionConcurrencyTest,OrderCommerceIntegrationTest" test
py -3 tools/check_shared_ui.py
py -3 tools/check_agent_skills.py
git diff --check
```

Maven exit 0; Shared UI đạt (một layout, 21 shared files); 15 skills đạt; diff check không lỗi whitespace. Không chạy lại full suite/build WAR cho lượt thay đổi nhỏ này.

Browser: harness local `.local/admin-store-fixes/run.py` và `browser.py`, AllUiServer hiện có với fixture H2 và một Vendor bổ sung chỉ trong DB test. Edge headless/Tomcat/SiteMesh cổng 8882; launcher chỉ dừng JVM do nó tạo. Kiểm sửa/chuyển phân công, reload, cancel, đổi Vendor có xác nhận và readback; viewport 390/768/1440, không tràn ngang. **10 kiểm tra browser đạt, 0 pageerror** (`ui-final3.log`, `ui-results.json`). Đã xem ảnh form ở 390/768/1440px. Kết quả và ảnh trong `.local/admin-store-fixes/`; không chứa thông tin đăng nhập trong báo cáo.

## Phạm vi và giới hạn

File chính: CatalogRequests, CatalogManagementController, StoreService, StoreOperationsRepository, StoreRepository, CatalogGuard; JS catalog-management/manager-assignments; JSP workspace/assignments; CatalogWorkflowRegressionTest và contract catalog-sales/CURRENT_IMPLEMENTATION.md.

Hồi quy gồm catalog CRUD, role operations, thu hồi quyền cạnh tranh H2 và bộ OrderCommerceIntegrationTest (checkout/nhóm/điểm/voucher/thanh toán/giao hàng theo các test hiện có). Không thay theme/layout dùng chung. Không tuyên bố đã kiểm mọi chức năng hay cạnh tranh TiDB/MySQL. Runtime 8080 vẫn là tiến trình cũ, chưa triển khai bản sửa này. Không migration cần áp dụng.


## Yêu cầu phát hành tiếp theo

Prompt thực: `push code lên nhánh main và integration đi ghi đè lên`.

Ngày 29/10/2026: fetch xác nhận origin/main và origin/integration/utetra cùng SHA `963c277f43439901369542a6736c793456ccb3c5`. Full `mvnw.cmd test` trước commit đạt **353 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS, exit 0 (`.local/admin-store-fixes/pre-push-full.log`). Kết quả này bổ sung cho bộ hồi quy 169 tests phía trên. Chỉ commit bản sửa code/test/contract và báo cáo liên quan; không thêm Word, bản sao source hoặc hồ sơ audit ngoài phạm vi. Hai nhánh có thể cập nhật fast-forward, không cần ghi đè lịch sử. Không deploy/restart hay cập nhật DB cloud.
