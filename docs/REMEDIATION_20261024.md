# UTETra — Bổ sung sau audit và việc cần thành viên hoàn thiện

> Trạng thái mới nhất sau lượt kiểm yêu cầu đầy đủ: [FINAL_REQUIREMENTS_STATUS](FINAL_REQUIREMENTS_STATUS.md). VNPAY đã có adapter/test nhưng chưa merchant thật; carrier Manager theo shop đã bổ sung. Các kết luận theo snapshot/lượt kiểm cũ bên dưới giữ để tra lịch sử.

Ngày 24/10/2026. Thay đổi trên `integration/utetra`, sau snapshot audit `8369b67`. Chưa commit/push các thay đổi này.

> Bổ sung theo prompt tiếp theo: [JWT DB và realtime checkout](features/order-payment/REALTIME_CHECKOUT_JWT.md). Kết quả mới nhất là **153 tests đạt**, 8 migration; mục kiểm chứng QR bên dưới giữ kết quả của lượt sửa QR trước đó. Nhóm đọc phần bàn giao source/stock/JTI trong tài liệu mới trước khi chạy các nhánh cùng DB.

## Phần agent đã bổ sung trong source

1. [Chuyển khoản Techcombank bằng QR](features/order-payment/BANK_TRANSFER.md): chọn ở checkout, hiển thị QR/tài khoản/tiền/nội dung trên đơn, Admin đối soát, chống trùng, chặn giao chưa trả, hỗ trợ hủy/trả/hoàn và chuyển đến sau hủy. Không giả lập callback hoặc tự đánh dấu đã nhận tiền.
2. Sửa trách nhiệm tầng dữ liệu catalog: chuyển `CatalogStatistics` thành `repository/catalog/CatalogStatisticsRepository` và `@Repository`; repository tìm sản phẩm không còn phụ thuộc Service.
3. Thêm `StoreOperationsRepository` cho SQL phí giao, policy, nâng role/revoke session và kiểm Manager; `StoreService` chỉ điều phối và validation/transaction. Di chuyển source, giữ chức năng cũ.
4. Kết nối rule điểm identity với hủy bank REFUND_PENDING, có test chống trả điểm hai lần. Không thay JWT, datasource, mật khẩu, SMTP hay quyền nhận tiền sang Vendor.
5. Tài liệu luồng/setup/API/sơ đồ bank, nhật ký prompt và bảng bàn giao bên dưới. Không bịa tên thành viên, khảo sát thực, sao kê hoặc giao dịch thật.

## Việc cần giao cho từng người

| Người phụ trách theo nhánh | Việc cụ thể | File / bằng chứng cần bàn giao | Vì sao cần người đó |
| --- | --- | --- | --- |
| **Bạn — order-payment / chủ tài khoản** | Đối chiếu QR/tên/số tài khoản, thử quy trình Admin đối soát với giao dịch do bạn chủ động thực hiện; xác định cách nhận thông tin hoàn tiền khách | BANK_TRANSFER.md; ảnh che thông tin nhạy cảm; mã đơn/kết quả thu–hoàn, không ghi credential | Agent không có sao kê/quyền truy cập tài khoản ngân hàng và không tự chuyển tiền. |
| **Bạn + giảng viên** | Xác nhận chuyển khoản QR thủ công có được thay VNPAY/MOMO không; nếu không, cấp merchant sandbox để tiếp tục tích hợp một cổng | Ghi câu trả lời/ngày vào phạm vi và UC-ORD-06 | Không tự coi QR là VNPAY hay tự miễn yêu cầu cổng online. |
| **identity-membership / identify** | Cấu hình Gmail App Password/SMTP trên máy báo cáo theo MAIL_SETUP; kiểm OTP đăng ký và reset thật | `tools/configure_mail.py`, MAIL_SETUP, biên bản email/OTP không lộ mã/secret | Code đã có; thiếu credential máy hiện tại. Không thể tự tạo App Password thay chủ Gmail. |
| **catalog-sales + người quản trị dữ liệu** | Chuẩn bị shop được duyệt/đang nhận đơn, danh mục/món/size/topping/ảnh/giá, khu vực; phối hợp order tạo carrier/rate/policy/voucher qua nghiệp vụ | Dataset demo có nhãn, tài khoản owner/scope, ảnh menu/top/phân trang | Cần nhóm xác nhận menu, giá và dữ liệu hợp lệ; agent không tự tạo giao dịch giả trên cloud như doanh số thật. |
| **catalog-sales + giảng viên** | Chốt cách hiểu Guest “bán trên 10”; xác nhận quyền danh mục `CATALOG_ADMIN`; kiểm Manager đúng shop | UC Guest/Manager, tài khoản demo và ảnh quyền được/chặn | Hiện là diễn giải và phân quyền nhóm đã thiết kế, cần chấp nhận phạm vi. |
| **order-payment + giảng viên** | Chốt Manager được sửa tên/phí cơ bản carrier dùng chung hay chỉ biểu phí khu vực | UC-MGR-06; thiết kế scope nếu thay; test không sửa ngoài shop | Mở CRUD toàn cục cho Manager sẽ tác động mọi shop; không tự làm vậy để đánh dấu đủ checklist. |
| **Nhóm + người quản trị DB** | Xác nhận TiDB được chấp nhận; rà CHECK đang OFF và test trên DB đích, không bật GLOBAL cluster chung tùy tiện | DATABASE_DESIGN, biên bản ràng buộc MySQL/TiDB | Đây là quyết định môi trường/chấp nhận của thầy; không phải chỉ sửa một property của project. |
| **Mỗi thành viên** | Khảo sát thực tế, viết phần mình, điền tên/MSSV/UC/role/nhánh/commit/reviewer, giải thích AI đã hỗ trợ gì | SURVEY_SYSTEM_ANALYSIS, PROJECT_PLAN, hồ sơ UC và AI_LOG | Không thể bịa danh tính, trải nghiệm khảo sát hoặc tự xác nhận thay người làm. |
| **catalog-sales và order-payment** | Xuất activity riêng, thêm hình code có diễn giải/callout và ảnh UI của từng UC; đối chiếu đúng source tích hợp | `docs/features/<module>/use-cases`, `assets`, caption/nguồn/commit | Bộ hình hiện chưa đủ đồng đều; cần owner đọc và xác nhận quy trình họ sẽ báo cáo. Agent đã thêm nguồn activity bank để tiếp tục ghép. |
| **Người tổng hợp báo cáo** | Ghép DOCX cuối kỳ, thêm kiến trúc tổng thể/ERD, danh mục hình, setup, prompt và phần cá nhân; xem lại bố cục và bỏ placeholder | DOCX UTETra và REPORT_ASSEMBLY | Markdown hiện là nguồn, chưa phải bản Word nộp. Cần danh tính/phần viết và hình hoàn chỉnh của nhóm. |
| **Cả nhóm** | Chạy trên máy sạch và bản tích hợp cuối: native DB, OTP, media ảnh/video, checkout/giao/thu/trả/hoàn, quyền chéo shop, responsive; diễn tập | Biên bản từng môi trường/provider, ảnh mobile/tablet/desktop; commit release | Test H2 không thay nghiệm thu máy báo cáo/provider và khả năng giải thích cá nhân. |

Không có yêu cầu bắt buộc bật Shipper vì thầy ghi phần thêm. Không bổ sung khoảng cách/định vị; khách vẫn chọn chi nhánh thủ công theo quyết định đã chốt.

## Kiểm chứng của lượt sửa

- Java 21/Maven Wrapper: **`clean verify` đạt 143 tests, 0 failures/errors/skipped**, WAR build thành công. Log `.local/bank-transfer-verify-20261024.log`; XML trong `target/surefire-reports`. Có 6 test mới, gồm xác nhận đồng thời, DB unique và JSP/Tomcat với login/CSRF.
- Lượt test đầu phát hiện rule điểm identity chưa hỗ trợ hủy bank đã trả, đã sửa contract và regression; lỗi tên bảng trong testcase cũng đã sửa. Không bỏ assertion hoặc tắt test để build đạt.
- Sau kiểm tra hình đã sửa ký hiệu tiền bị lặp và thêm nhãn `REFUND_PENDING` trong JS riêng order. Đóng gói lại WAR thành công sau các sửa trình bày; không đổi Java nghiệp vụ sau lượt clean verify đạt. Node syntax check `orders.js` đạt.
- Playwright/Edge trên **H2 fixture riêng**, JSP/Tomcat/SiteMesh thật: **6 checks đạt**, ảnh Buyer ở 1440/768/390px và Admin ở 1440/390px, QR tải được, không tràn ngang, không có lỗi JavaScript; thao tác Admin đối soát trên UI thành PAID. [Kết quả browser](features/order-payment/assets/bank-20261024/verification.json).
- Đã xem ảnh desktop/mobile để kiểm bố cục; ảnh chụp là dữ liệu test, không phải đơn/sao kê thật. [Buyer desktop](features/order-payment/assets/bank-20261024/bank-buyer-1440.png), [Buyer mobile](features/order-payment/assets/bank-20261024/bank-buyer-390.png), [Admin mobile](features/order-payment/assets/bank-20261024/bank-admin-390.png).
- WAR chạy với `utetra_db`/TiDB: Hibernate validate đạt, Flyway validate **7 migration**, migration mới `2026102401` thêm unique đã được áp dụng. Không thay migration cũ, không seed user/shop/order vào cloud.
- **6 HTTP checks trên WAR cuối đạt**, gồm health/home/menu, QR và JS byte-for-byte khớp source, API Admin chưa login trả 401. WAR không còn class `service/CatalogStatistics.class`, đã có Repository thay thế. [Kết quả native](features/order-payment/assets/bank-20261024/native-http-checks.json).
- `py -3 tools/check_shared_ui.py --compare main` và `git diff --check` đạt. Giữ một layout và 16 file chung khớp main.
- Không có chuyển tiền/sao kê ngân hàng thật; chưa kiểm bank checkout trên dataset cloud vì dữ liệu commerce hiện chưa đủ. Không thử gửi mail hoặc upload video thật trong lượt này. Còn các việc giao thành viên ở bảng trên; không tự coi mọi yêu cầu thầy đã hoàn tất.
- Chưa commit/push. Các tiến trình phục vụ kiểm thử được dừng khi bàn giao.

Audit gốc [TEACHER_REQUIREMENTS_AUDIT_20261024](TEACHER_REQUIREMENTS_AUDIT_20261024.md) giữ số liệu tại snapshot cũ. Phần SQL trong Service/phụ thuộc ngược đã được xử lý ở source mới; VNPAY/MOMO vẫn chưa triển khai dù đã có bank transfer thủ công.
