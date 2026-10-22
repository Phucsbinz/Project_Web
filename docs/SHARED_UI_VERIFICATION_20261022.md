# Kiểm chứng một layout dùng chung — 22/10/2026

## Bộ chuẩn

Nguồn chuẩn local: main, commit cb90cf9. Một file `src/main/webapp/WEB-INF/views/layouts/app.jsp`, một mapping /* trong SiteMeshConfig, 16 file chung trong SHARED_UI_FILES.txt. Bộ UI lấy từ bản identify/order đã tích hợp; tập trung header/footer/sidebar/theme/Bootstrap/assets, không đưa nghiệp vụ order vào các nhánh khác.

Sáu file layout cũ được thay bằng nguồn app.jsp duy nhất. Các trang JSP của module vẫn là nội dung mà SiteMesh chèn vào khung chung. Mọi role dùng cùng cấu trúc và theme; sidebar/menu phụ thuộc currentRole/currentPath/workspace, backend vẫn kiểm quyền.

## Nhánh đã đồng bộ local

| Nhánh | Commit UI | Kiểm blob trong bundle so với main |
| --- | --- | --- |
| main | cb90cf9 | 16 file giống |
| feature/catalog-sales | fa3efab | 16 file giống |
| feature/identity-membership | 115b0e9 | 16 file giống |
| feature/order-payment | a79133f | 16 file giống |
| identify | 285ae1a | 16 file giống |
| integration/utetra | 90e4b8e | 16 file giống |

Chỉ xử lý xung đột UI/docs: dùng theme main, thay layout role cũ bằng app.jsp, giữ liên kết hướng dẫn identity/order sẵn có khi ghép README. Không sửa datasource/provider/JWT/pom/SQL hoặc thay service/controller chức năng. Không force push.

## Kiểm thử

- Main: JDK 21 + Maven Wrapper, clean verify **16 tests**, 0 failure/error/skipped; WAR thành công.
- Order tích hợp cả catalog/identity: clean verify **130 tests**, 0 failure/error/skipped; WAR thành công.
- Catalog riêng với markup cũ: clean verify **37 tests**, 0 failure/error/skipped; WAR thành công.
- Các bộ ViewTest chạy Tomcat/JSP/SiteMesh thật: public/auth, trang theo role, CSS/JS/JSON không bị bọc sai; không chỉ dùng MockMvc.
- `tools/check_shared_ui.py --compare main` đạt trên cả 6 nhánh: đúng một JSP layout, một decorator và cùng 16 file canonical.
- Không có thiết kế mới hay ảnh AI mới trong lượt này; dùng assets hiện có. Không chạy visual QA/screenshot trình duyệt mới và không thay đổi database chung trong lượt chuẩn hóa UI.

## Bàn giao

Agent đọc [AGENTS.md](../AGENTS.md) và [SHARED_UI.md](SHARED_UI.md) trước khi sửa giao diện. Chỉ viết JSP/CSS/JS chức năng trong phạm vi module; thay theme/layout phải cập nhật nguồn main rồi đồng bộ bundle. Công cụ kiểm tra sẽ báo khi có layout thứ hai hoặc file chung lệch nguồn chuẩn.

Hiện các commit này mới ở local, chưa push các nhánh. Trước đó người dùng yêu cầu chỉ push nhánh order; lần này đã chuẩn bị bộ UI riêng cho cả nhóm nhưng chưa mở rộng thao tác push remote. Thành viên chỉ pull được bộ chuẩn sau khi các commit UI được công bố lên GitHub.
