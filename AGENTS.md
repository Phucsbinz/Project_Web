# Quy ước làm việc UTETra cho agent

Đọc `docs/SHARED_UI.md` trước khi làm hoặc sửa giao diện. Project có **một** SiteMesh layout: `src/main/webapp/WEB-INF/views/layouts/app.jsp`, một Bootstrap 5.3.3 và bộ theme `utetra.css` dùng chung.

- Header, footer, sidebar, màu, font, breakpoint và cách nạp tài nguyên dùng bộ chung. Không tạo layout theo role hoặc template/frontend framework thứ hai.
- Chức năng viết nội dung JSP trong thư mục module, CSS/JS riêng phải scope theo module. Không chép navbar/footer/Bootstrap vào trang chức năng.
- NavigationAdvice cung cấp currentPath/currentRole/workspace; SiteMeshConfig chỉ có một decorator. Thêm trang hoặc vai trò không đồng nghĩa thêm layout. Menu hiển thị không thay kiểm tra quyền tại Security/Service.
- Thay đổi phần dùng chung tập trung trong bundle `docs/SHARED_UI_FILES.txt`, ghi lý do và đồng bộ từ main sang các feature. Không giải quyết xung đột bằng cách giữ thêm một bộ giao diện.
- Chạy `py -3 tools/check_shared_ui.py`; khi so với main đã cập nhật, thêm `--compare main`. JSP/layout thay đổi phải kiểm render Tomcat/SiteMesh thực tế.
- Giữ Java 21, Spring Boot 3/JSP/JSTL/SiteMesh/Bootstrap/JPA/MySQL; không đổi cấu hình nghiệp vụ, datasource, JWT hoặc provider chỉ để sửa giao diện.
- Ghi prompt và kết quả thực vào Markdown; không ghi khóa, token, password hoặc khẳng định đã kiểm thử khi chưa chạy.

Chỉ commit/push các nhánh và phạm vi người dùng yêu cầu. Tài liệu use case và contract nghiệp vụ vẫn nằm trong PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md và docs/features/.

## Skill của repository
Bộ skill dùng chung nằm ở `.agents/skills/*/SKILL.md` (không phải `.agent`). Đọc skill phù hợp với tác vụ; sửa lỗi ưu tiên systematic-debugging, test-driven-development và verification-before-completion. Hướng dẫn trực tiếp của người dùng và quy ước UTETra ở trên có ưu tiên cao hơn workflow chung trong skill. Không tự ghi đè cấu hình Codex cá nhân hay cài plugin bên ngoài. Kiểm cấu trúc bằng `python tools/check_agent_skills.py`.
