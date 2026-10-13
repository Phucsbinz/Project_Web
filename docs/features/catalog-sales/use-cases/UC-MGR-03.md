# UC-MGR-03 — Quản lý danh mục/topping

> Hồ sơ UC-MGR-03, actor Manager, nhánh feature/catalog-sales; bắt buộc. Chưa triển khai. Nghiệm thu: Theo phân quyền quản trị catalog được giao, không sửa ngoài phạm vi. Sao chép sang `docs/features/<module>/use-cases/<UC-ID>.md`; điều chỉnh link tương đối theo vị trí mới. Mỗi UC làm từ đầu đến cuối theo đặc tả, không bỏ các luồng lỗi.

## 1. Phạm vi và người thực hiện

| Mục | Nội dung |
| --- | --- |
| UC/nhánh/người | [điền] |
| Vai trò và quyền dữ liệu | [điền] |
| Baseline main/commit implementation | [điền] |
| Quy tắc BR và bảng dữ liệu | [điền, theo đặc tả] |
| Trạng thái/ngày | [chưa làm/đang làm/đã kiểm chứng] |

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

Chụp code thật, đánh dấu validation/quyền/dữ liệu/transaction và giải thích dưới hình. Chèn ảnh bằng `![caption](path-to-image.png)` sau khi ảnh có thật; không tạo link ảnh giả chỉ để hoàn thành checklist.

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
