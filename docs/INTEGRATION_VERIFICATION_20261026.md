# Kiểm tra tích hợp ngày 26/10/2026

Yêu cầu nguyên văn: `merg vào nhánh kiểm thử đi`.

## Phạm vi và lịch sử

Nhánh đích `integration/utetra`. Fetch origin trước khi ghép; các nhánh feature đã nằm trong lịch sử, bản identify chứa các cập nhật mới. Fast-forward từ `5e5708f` tới `70d96c5`, không xung đột và không phải chọn bỏ code của module nào. Không sửa main hoặc push remote trong lượt này.

- `origin/feature/catalog-sales` → `ce46ce2`: đã nằm trong HEAD tích hợp.
- `origin/feature/identity-membership` → `115b0e9`: đã nằm trong HEAD tích hợp.
- `origin/feature/order-payment` → `e200a64`: đã nằm trong HEAD tích hợp.
- `origin/identify` → `70d96c5`: đã nằm trong HEAD tích hợp.

Bản mới nhận thiết kế chung và luồng menu/giỏ nháp → chọn chi nhánh tại checkout, không tính khoảng cách. Nhận migration `V2026102601__shared_drink_catalog.sql` từ identify, không sửa migration cũ.

## Giao diện chung

`py -3 tools/check_shared_ui.py`: đạt, một layout SiteMesh và 16 file chung. `--compare main` báo khác bốn file: SHARED_UI.md, UI_PROMPT_HISTORY.md, app.jsp, utetra.css. Đây là thay đổi thiết kế từ identify đang được kiểm trên integration, chưa phải bundle đã đồng bộ sang main. Không giữ thêm layout theo role để giải quyết khác biệt.

Node syntax đạt cho catalog-management.js, catalog.js, product-community.js. Git diff whitespace đạt. Không tự sửa nghiệp vụ hoặc provider chỉ để đồng nhất giao diện.

## Kết quả build

`py -3 tools/run_verify.py` (clean verify, JDK 21): **BUILD SUCCESS, 167 tests, 0 failures/errors/skipped**, hoàn tất 20:20:09 ngày 26/10/2026. WAR đã đóng gói thành công. [Tổng hợp XML thực](report-assets/acceptance/integration-20261026.json). Log local `.local/verify.log`. Các kiểm thử view dùng Tomcat/SiteMesh thực với fixture riêng, không thay bằng kiểm source tĩnh. Không chạy script seed hoặc kiểm thanh toán thật trên DB cloud trong lượt merge.

## Thay đổi cục bộ có trước

`docs/AI_PROMPT_HISTORY.md` có nội dung chưa commit từ trước. Đã tạm cất đúng file này khi chuyển nhánh và khôi phục sau merge, không xóa nội dung hoặc nhận là đã kiểm chứng lại các khẳng định của phiên khác. Nhật ký tiếp tục được bổ sung với yêu cầu và kết quả thật của lượt merge.


## Cập nhật và phát hành integration theo yêu cầu tiếp theo

Prompt nguyên văn: `pull code các nhánh về rồi push lên nhánh integration`.

Đã fetch toàn bộ origin. Đưa UI tham chiếu, local font/license, ảnh kiểm và tài liệu vào commit e394907; ghép identify ba355e0 bằng merge commit fae9778, không xung đột. Các remote tip đã nằm trong source tích hợp:

- `origin/feature/catalog-sales`: `ce46ce2`.
- `origin/feature/identity-membership`: `115b0e9`.
- `origin/feature/order-payment`: `e200a64`.
- `origin/identify`: `ba355e0`.
- `origin/integration/utetra`: `5e5708f`.
- `origin/main`: `3ceff77`.

Nhánh identify local đã fast-forward tới ba355e0; các nhánh feature còn lại đã khớp remote. Main không đổi. Không chạy reset_test_passwords.py, seed_demo.py hoặc ứng dụng kết nối cloud trong lượt publish. Commit mới có migration nullable password_plaintext và công cụ reset test chạy chủ động, được ghép nguyên theo identify; không coi merge code là xác nhận dữ liệu/thao tác reset đã được thực hiện.

Giữ việc xóa ảnh QR nguồn cục bộ có trước ngoài commit; ảnh QR dùng trong static assets vẫn có. Không đưa target/.local vào Git. Build mới và SHA remote cuối được đối chiếu trước khi báo hoàn tất.

Kiểm chứng sau merge: **Maven clean verify 167 tests, 0 failures/errors/skipped, BUILD SUCCESS**, WAR đóng gói đạt. Shared UI checker đạt một layout, 21 file chung; whitespace đạt. [Kết quả XML tổng hợp](report-assets/acceptance/integration-publish-20261026.json). Không chạy lại browser/provider ở lượt publish; bằng chứng browser của UI nằm trong UI_REFERENCE_REFINEMENT_20261026.md.
