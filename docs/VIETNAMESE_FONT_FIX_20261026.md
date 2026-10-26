# Đồng nhất font tiếng Việt — 26/10/2026

## Yêu cầu

> font chữ ở chữ Nhiều niềm vui. trang chủ bị lỗi font tiếng việt rồi, chọn một font khác không bị lỗi , chỉnh luôn các chữ khác sữ dụng font giống vậy

## Thay đổi

- Thay toàn bộ khai báo font Georgia trong CSS ứng dụng bằng `var(--bs-body-font-family)`, tức Be Vietnam Pro được đóng gói local với dự án. Không chỉnh Bootstrap/vendor, không nạp font CDN.
- Áp dụng cho các câu nhấn của trang chủ: “thật khác.”, “ly yêu thích.”, “Thêm ưu đãi.”, “Một chút thảnh thơi.”, “Nhiều niềm vui.” và chữ trên tem ảnh.
- Đồng nhất các chỗ dùng cùng font trong theme: chữ nhấn hero/story/auth/footer, tiêu đề collection, số trang trang trí và chữ thương hiệu header/footer.
- Chữ nhấn dùng font normal, weight 500 thay cho serif nghiêng; tránh kiểu nghiêng tổng hợp. Giữ nội dung và luồng chức năng.
- Tăng version URL CSS chung và CSS trang chủ để trình duyệt tải bản sửa.
- Hai file dùng chung thay đổi (`utetra.css`, `layouts/app.jsp`) đã nằm trong `SHARED_UI_FILES.txt`; đồng bộ cùng bundle khi tích hợp main/các feature. CSS trang chủ tiếp tục scope `.catalog-homepage`. Chưa commit/push bản sửa font.

## Kiểm chứng

- Java 21: `BaselineViewTest,CatalogViewTest` — 8 test đạt, không lỗi; JSP/SiteMesh render bằng Tomcat thật.
- `python tools/check_shared_ui.py` đạt: một layout, 21 file chung. `git diff --check` đạt.
- Edge/Chromium: 25 lần kiểm font thực tế bằng `CSS.getPlatformFontsForNode` đạt ở 390/768/1440px và trang đăng nhập. Các đoạn được kiểm dùng các file Be Vietnam Pro local (Regular/Medium/Bold), không lẫn glyph từ font fallback; không tràn ngang và không lỗi JavaScript.
- Kết quả kiểm font thực tế và ảnh xem tại [vietnamese-font-20261026](demo/vietnamese-font-20261026/verification.json). Máy chủ kiểm thử sử dụng H2 riêng; không sửa dữ liệu thật hay gọi provider.
