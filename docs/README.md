# UTETra — Hồ sơ phát triển chức năng và sử dụng AI

> Ngày khởi tạo: 13/10/2026. Đây là bộ tài liệu làm việc để nhóm cập nhật trong quá trình lập trình và tổng hợp thành báo cáo `.docx` theo yêu cầu giảng viên.

## 1. Yêu cầu báo cáo

Mỗi chức năng cần có hướng dẫn xây dựng chi tiết, hình kiến trúc, sơ đồ hoạt động, hình diễn giải source code, hình diễn giải giao diện, hướng dẫn setup và danh sách prompt AI đã dùng để sinh code/sinh hình. Markdown là bản nguồn để nhóm ghi chép; báo cáo Word được tổng hợp sau khi có code, hình và kết quả kiểm chứng thực tế.

Không đánh dấu chức năng đã hoàn thành khi mới có đặc tả hoặc sơ đồ đề xuất. Các vị trí `[CẦN BỔ SUNG]` phải được thay bằng thông tin thực trước khi đưa vào báo cáo nộp.

## 2. Cách đọc và sử dụng

1. Đọc [phân chia nhánh và quy trình Git](BRANCH_WORKFLOW.md).
2. Đọc [cấu hình chung và hợp đồng tích hợp](COMMON_BASELINE.md) cùng [hướng dẫn setup](SETUP.md).
3. Đọc [hướng dẫn cho AI của thành viên](AI_WORKFLOW.md).
4. Mỗi người nhận một nhánh và cập nhật hồ sơ của mình trong `docs/features/`.
5. Khi hoàn thành từng use case, nhân bản [mẫu hồ sơ use case](templates/USE_CASE_NOTE.md) vào `docs/features/<module>/use-cases/<UC-ID>.md`.
6. Ghi prompt thật vào nhật ký của module; tham khảo [mẫu ghi prompt](templates/PROMPT_ENTRY.md). Prompt của người dùng trong cuộc trò chuyện khởi tạo được lưu riêng tại [nhật ký yêu cầu](AI_PROMPT_HISTORY.md).
7. Lưu hình, sơ đồ và nguồn hình theo [quy trình tổng hợp báo cáo Word](REPORT_ASSEMBLY.md).

## 3. Phân công theo luồng nghiệp vụ

| Thành viên | Nhánh | Luồng phụ trách từ đầu đến cuối | Hồ sơ |
| --- | --- | --- | --- |
| TV1 | `feature/catalog-sales` | Quản lý danh mục/món/size/topping/chi nhánh → upload ảnh → khách chọn chi nhánh và xem/tùy biến món | [Danh mục và chi nhánh](features/catalog-sales/IMPLEMENTATION.md) |
| TV2 | `feature/identity-membership` | Đăng ký/đăng nhập → bảo vệ tài khoản → hồ sơ/địa chỉ → phân công nhân viên → điểm và hạng thành viên | [Tài khoản và thành viên](features/identity-membership/IMPLEMENTATION.md) |
| TV3 | `feature/order-payment` | Giỏ hàng → checkout → COD → xử lý/hủy đơn → điểm qua hợp đồng dùng chung → realtime và báo cáo | [Bán hàng và thanh toán](features/order-payment/IMPLEMENTATION.md) |

Mỗi người làm Entity/Repository, DTO, Service, Controller, JSP/JavaScript, kiểm thử và tài liệu cho use case của mình. Không chia nhánh riêng theo Entity/Service/Controller hoặc giao diện/backend khiến một người phải nhận các phần rời rạc. Trọng tâm nghiệm thu là luồng bán hàng và thanh toán; các chức năng khác vẫn giữ theo đặc tả hiện có.

`main` lưu cấu hình/baseline chung và những thay đổi đã được kiểm chứng. `integration/utetra` dùng ghép ba phân hệ trước khi đưa bản chạy chung về `main`. Khách tự chọn chi nhánh; không tính khoảng cách hoặc tự tìm cửa hàng gần nhất.

## 4. Tài liệu nguồn hiện có

- [PROJECT_PLAN.md](../PROJECT_PLAN.md): phạm vi, kiến trúc và lộ trình.
- [USE_CASES.md](../USE_CASES.md): mã use case và quy tắc nghiệp vụ.
- [DATABASE_DESIGN.md](../DATABASE_DESIGN.md): 26 bảng, ràng buộc và transaction.

Ba tài liệu này vẫn là nguồn đặc tả chung. Bộ hồ sơ mới bổ sung hướng dẫn làm việc và minh chứng, không tự cắt giảm chức năng hoặc thay đổi nghiệp vụ.
