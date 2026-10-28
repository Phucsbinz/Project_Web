# UC-VND-01 — Đăng ký shop

> Cập nhật 24/10/2026: đã có source `StoreService.register/review → StoreOperationsRepository` và test liên quan `approvalPromotesBuyerAndRejectsReplay`. Đọc [chỉ mục hiện tại](../CURRENT_IMPLEMENTATION.md) và [đối chiếu cuối kỳ](../../../FINAL_REQUIREMENTS_STATUS.md). Các bảng placeholder bên dưới là hồ sơ cần owner hoàn thiện, không phải kết luận UC chưa có code.

> Actor: User, Vendor. Owner: feature/catalog-sales. P0 bắt buộc. Đặc tả gốc; mã nguồn và kiểm thử bổ sung ngày 15/10/2026. Trạng thái nghiệm thu xem mục triển khai bên dưới.


## Triển khai thực ngày 15/10/2026

- Code: `StoreService.register / review`; Entity/Repository tại `src/main/java/com/utetra/entity/catalog` và `repository/catalog`.
- Giao diện: `/account/shop-registration`; JSP tại `src/main/webapp/WEB-INF/views/catalog`, JS tại `src/main/resources/static/assets/js/catalog*.js`.
- Test liên quan: `approvalPromotesBuyerAndRejectsReplay; CatalogViewTest`. Test H2/MockMvc/Tomcat; mapping này không khẳng định đã kiểm mọi nhánh lỗi riêng của UC trên TiDB.
- [Hướng dẫn, contract, transaction, setup và giới hạn](../RUNBOOK.md); [DDL đề xuất cho main](../proposals/catalog-sales.sql); [prompt thực CAT-AI-001](../AI_LOG.md).
- Chưa có commit/reviewer hoặc nghiệm thu cloud/provider thật. Những bảng testcase/ảnh/checklist dự kiến giữ bên dưới là hồ sơ gốc, không tự đánh dấu đạt toàn bộ.

## Đặc tả chức năng

Mục tiêu và điều kiện nghiệm thu: Hồ sơ chờ duyệt; duyệt mới hoạt động và cấp quyền Vendor.

Nguồn: [Danh mục và BR](../../../../USE_CASES.md), [từ điển dữ liệu](../../../../DATABASE_DESIGN.md), [quy định chung](../../../QUY_DINH_VA_CAU_HINH_CHUNG.md). Nền và contract: [COMMON_BASELINE](../../../COMMON_BASELINE.md).

- Kích hoạt: actor chọn chức năng đăng ký shop trên giao diện hoặc gọi route tương ứng.
- Tiền điều kiện: nền main đã đồng bộ; bảng cần dùng có migration đã review; dữ liệu tham chiếu tồn tại. Public chỉ đọc dữ liệu được duyệt; thao tác bảo vệ cần tài khoản active/verified, phiên hợp lệ và đúng phạm vi.
- Đầu vào: shop name/address/contact/hours/media. ID và số tiền từ client không thay thế kiểm quyền/tính toán phía server.
- Dữ liệu: `stores, store_registration_requests, users, roles, auth_sessions`. Các bảng nghiệp vụ đã có trong migration tích hợp; đối chiếu schema thật theo DATABASE_DESIGN và biên bản mới nhất.
- Contract dự kiến: `StoreService`; route dự kiến: `/vendor/registrations`. Chốt DTO/signature và migration trên main trước khi code; không tự tạo Service trùng chức năng module khác.
- Hậu điều kiện thành công: đạt điều kiện nghiệm thu nêu trên; trạng thái và dữ liệu nhất quán, trả DTO/view đã escape. GET không thay đổi dữ liệu nghiệp vụ; ghi nhiều bảng phải có transaction và kiểm quyền trước ghi.
- Hậu điều kiện thất bại: không ghi dở/mất dữ liệu; lỗi validation/quyền/version rõ ràng; provider thất bại có pending/retry hoặc lỗi thật, không giả thành công.

### Luồng chính

1. User gửi hồ sơ PENDING.
2. người đủ scope duyệt.
3. ACTIVE.
4. cấp Vendor lần đầu và revoke phiên.
5. dùng quyền mua User.

### Sơ đồ hoạt động dự kiến

```mermaid
flowchart TD
    Start([Bắt đầu]) --> Permission{Đúng điều kiện và quyền?}
    Permission -->|Không| Denied[Từ chối; giữ dữ liệu]
    Permission -->|Có| S1
    S1["User gửi hồ sơ PENDING"]
    S1 --> S2
    S2["người đủ scope duyệt"]
    S2 --> S3
    S3["ACTIVE"]
    S3 --> S4
    S4["cấp Vendor lần đầu và revoke phiên"]
    S4 --> S5
    S5["dùng quyền mua User"]
    S5 --> Result([Trả kết quả])
    Denied --> Result
```

Sơ đồ mô tả luồng thiết kế; các nhánh lỗi/cạnh tranh cụ thể ở mục tiếp theo. Cập nhật theo code thực và xuất ảnh khi lập báo cáo.

### Luồng thay thế, lỗi và cạnh tranh

Không tự duyệt; hồ sơ trùng/invalid giờ; rejected cần reason; hai approval không cấp quyền hoặc seed lặp. Request thay đổi phải có CSRF; dùng version/unique/locking theo schema khi ghi. Event ra ngoài transaction chỉ phát sau commit; không đưa credential, OTP rõ, token hoặc password vào log/DTO.

### Kiểm thử nghiệm thu cần thực hiện

| Case | Mong đợi | Kết quả hiện tại |
| --- | --- | --- |
| Luồng chính với shop name/address/contact/hours/media hợp lệ | Hồ sơ chờ duyệt; duyệt mới hoạt động và cấp quyền Vendor | Chưa điền kết quả riêng tại bảng này — xem source/test hiện tại |
| Input hoặc tham chiếu không hợp lệ | Lỗi rõ ràng, không ghi dở | Chưa chạy |
| Actor/owner/scope không hợp lệ | 401/403 hoặc 404 theo contract, không lộ dữ liệu | Chưa chạy |
| Tình huống đặc thù: Không tự duyệt; hồ sơ trùng/invalid giờ; rejected cần reason; hai approval không cấp quyền hoặc seed lặp | Tuân thủ luồng lỗi và tính nhất quán nêu trên | Chưa chạy |

### Triển khai theo ba tầng

1. Chốt DTO và các bảng `stores, store_registration_requests, users, roles, auth_sessions`; nhóm trưởng đặt version migration duy nhất trên main, giữ nguyên V1 đã áp dụng.
2. Entity/Repository ánh xạ FK/unique/version và query scoped; map DTO trong Service khi `open-in-view=false`.
3. `StoreService` xử lý luồng chính, validation/quyền/transaction và các lỗi đặc thù ở trên.
4. Controller cho `/vendor/registrations` nhận DTO, principal và CSRF; JSP dùng Bootstrap/layout chung, hiển thị loading/empty/error.
5. Viết và chạy các case nghiệm thu; bổ sung code path/symbol/commit và ảnh chạy thật vào các mục hồ sơ bên dưới.

Các ô minh chứng bên dưới được điền khi có kết quả thực; không dùng đặc tả hoặc sơ đồ dự kiến làm bằng chứng chức năng hoàn thành.

## 1. Phạm vi và người thực hiện

| Mục | Nội dung |
| --- | --- |
| UC/nhánh/người | UC-VND-01 / feature/catalog-sales / TV1; tên cá nhân chưa phân công |
| Vai trò và quyền dữ liệu | User, Vendor; lấy identity từ principal, kiểm owner/shop/membership/assignment tại Service |
| Baseline main/commit implementation | [điền] |
| Quy tắc BR và bảng dữ liệu | stores, store_registration_requests, users, roles, auth_sessions; theo USE_CASES.md và DATABASE_DESIGN.md |
| Trạng thái/ngày | Có mã nguồn 15/10/2026; H2/Tomcat đã kiểm; chờ tích hợp/TiDB/reviewer |

Mục tiêu: [điền]. Tiền điều kiện/kích hoạt: [điền]. Đầu vào/đầu ra/hậu điều kiện: [điền]. Luồng chính, luồng lỗi, cạnh tranh và quyền: [điền].

## 2. Kiến trúc và sơ đồ hoạt động

| Mã hình | Loại | Nguồn Mermaid/PlantUML | PNG/SVG xuất | Caption và giải thích | Prompt/commit | Trạng thái |
| --- | --- | --- | --- | --- | --- | --- |
| [MOD-UC-ARCH-01] | Kiến trúc | [path] | [path] | [điền] | [điền] | [dự kiến/kiểm chứng] |
| [MOD-UC-ACT-01] | Activity | [path] | [path] | [điền] | [điền] | [dự kiến/kiểm chứng] |

Giải thích thành phần, chiều dữ liệu và ranh giới transaction; activity có start/end, quyết định và nhánh lỗi. Ghi điểm khác giữa thiết kế AI đề xuất và triển khai thực.

## 3. Hướng dẫn xây dựng từng bước

| Bước | File/symbol tạo hoặc sửa | Công việc và lý do | Input/output | Dependency | Prompt/commit |
| --- | --- | --- | --- | --- | --- |
| 1 — Dữ liệu/migration | [điền] | [schema, FK/UQ/CHECK] | [điền] | [điền] | [điền] |
| 2 — Entity/Repo/DTO | [điền] | [mapping/query/validation] | [điền] | [điền] | [điền] |
| 3 — Service | [điền] | [quyền, nghiệp vụ, transaction] | [điền] | [điền] | [điền] |
| 4 — Controller | [điền] | [route, lỗi, PRG/JSON] | [điền] | [điền] | [điền] |
| 5 — JSP/JS | [điền] | [form/list/error/loading] | [điền] | [điền] | [điền] |
| 6 — Kiểm thử | [điền] | [case quan trọng] | [điền] | [điền] | [điền] |

Thêm văn bản giải thích đủ để người khác xây lại chức năng theo các bước, không chỉ bảng tên class. Đoạn code trọng tâm trong fenced block dùng code thực, nêu path/symbol/commit để tra lại; không chép toàn dự án vào báo cáo.

## 4. Hình diễn giải source code

| Hình | File/symbol/commit | Dòng hoặc đoạn trọng tâm | Callout 1/2/3 | Ý nghĩa/kiểm chứng |
| --- | --- | --- | --- | --- |
| [MOD-UC-CODE-01] | [điền] | [điền] | [điền] | [điền] |

Chụp code thật, đánh dấu validation/quyền/dữ liệu/transaction và giải thích dưới hình. Chỉ chèn đường dẫn ảnh sau khi ảnh có thật; không tạo link ảnh giả chỉ để hoàn thành checklist.

## 5. Hình diễn giải giao diện

| Hình | Trang/vai trò/dữ liệu test | Thao tác và trạng thái | Ảnh thực/mockup | Caption/commit |
| --- | --- | --- | --- | --- |
| [MOD-UC-UI-01] | [điền] | [luồng chính] | [điền] | [điền] |
| [MOD-UC-UI-02] | [điền] | [lỗi/rỗng/quyền] | [điền] | [điền] |

Giải thích các vùng giao diện, lựa chọn và kết quả. Mockup phải ghi rõ chưa triển khai; báo cáo nghiệm thu cần screenshot ứng dụng chạy.

## 6. Setup và dữ liệu demo

Baseline/version/profile: [điền]. Biến config cần thêm và nơi ánh xạ: [điền, không secret]. Migration/seed: [điền]. Lệnh build/chạy/test thực: [điền]. URL/role/thao tác demo: [điền]. Lỗi setup và cách sửa: [điền].

## 7. Kiểm thử và đánh giá AI

| Case | Input/quyền/tình huống | Mong đợi | Kết quả thực | Lệnh/log/ảnh/commit |
| --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [chưa chạy/qua/lỗi] | [điền] |

Prompt liên quan: [ID/link]. AI hỗ trợ phần nào: [điền]. Nhóm chỉnh/sửa/bỏ gì và vì sao: [điền]. Cách xác minh source/hình/cấu hình: [điền]. Hạn chế/lỗi chưa giải quyết: [điền].

## 8. Checklist kết thúc

- [ ] Đúng use case, schema, cấu hình và contract main.
- [ ] Có hướng dẫn từng bước, hình kiến trúc/activity/code/UI với caption và nguồn.
- [ ] Setup và testcase được chạy thực, có kết quả và commit.
- [ ] Prompt nguyên văn/response/đánh giá nhóm được liên kết.
- [ ] Reviewer/ngày/commit được điền, không còn placeholder ở phần nộp.
