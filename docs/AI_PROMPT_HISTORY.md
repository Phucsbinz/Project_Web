# UTETra — Nhật ký yêu cầu và prompt AI của cuộc trò chuyện khởi tạo

## 1. Nguồn và cách ghi

Ghi ngày 13/10/2026, từ các tin nhắn người dùng còn hiển thị trong cuộc trò chuyện này. Thời điểm chính xác của từng tin nhắn/model không có đủ dữ liệu nên không tự gán. Nội dung bên dưới giữ nguyên câu chữ, lỗi gõ và đường dẫn như người dùng gửi; tin nhắn cấu hình môi trường tự động không tính là prompt nghiệp vụ.

Đây là prompt yêu cầu/chỉnh tài liệu và tổ chức nhánh. Cuộc trò chuyện này chưa tạo source code ứng dụng, chưa sinh ảnh kiến trúc bằng công cụ tạo ảnh và chưa chạy ứng dụng. Sơ đồ Mermaid trong hồ sơ là thiết kế dạng văn bản. Prompt mẫu của các tài liệu khác chỉ được xem đã dùng khi thành viên thực sự gửi cho AI và ghi nhật ký.

Nhật ký prompt triển khai của mỗi thành viên đặt tại `docs/features/<module>/AI_LOG.md`, theo mẫu [PROMPT_ENTRY.md](templates/PROMPT_ENTRY.md). Các phản hồi/tệp liên quan được liên kết khi có; không bịa toàn văn phản hồi hoặc đường dẫn chat không tồn tại.

## P-REQ-001 — Viết lại ba đặc tả

**Loại:** Yêu cầu tài liệu. **Nội dung nguyên văn:**

```text
bạn chỉnh sửa lại cả 3 file md cho hợp lí với đầy đủ và chuyên nghiệp cho project  Xây dựng website chuỗi bán trà sữa UTETra bằng Spring boot + JSP/JSTL + Decorator Sitemesh + Bootstrap + JPA + SQLServer/MySQL/ postgreSQL+JWT+websocket+Cloudairy
```

**Kết quả ghi nhận:** Chỉnh PROJECT_PLAN.md, DATABASE_DESIGN.md và USE_CASES.md về kiến trúc, nghiệp vụ và dữ liệu. Tên dịch vụ chuẩn hóa thành Cloudinary. **Kiểm chứng được:** File tài liệu có tồn tại; chưa chứng minh phần mềm hoạt động.

## P-REQ-002 — Phản hồi về thao tác xóa file

**Loại:** Chỉnh cách làm việc. **Nội dung nguyên văn:**

```text
ụa sao xóa hết file vậy ???
```

**Kết quả ghi nhận:** AI thừa nhận đã xóa rồi tạo lại khiến tài liệu tạm biến mất; ba file được tạo lại đầy đủ. **Yêu cầu cho các phiên tiếp:** Cập nhật tài liệu tại chỗ; không lặp lại cách xóa toàn bộ trước khi viết.

## P-REQ-003 — Kiểm tra hoàn thành

**Loại:** Hỏi tiến độ. **Nội dung nguyên văn:**

```text
là xong chưa
```

**Kết quả ghi nhận:** Xác nhận đã hoàn thành phần ba tài liệu; chưa triển khai code website.

## P-REQ-004 — Trọng tâm bán hàng/thanh toán, chọn chi nhánh thủ công

**Loại:** Điều chỉnh phạm vi. **Nội dung nguyên văn:**

```text
cái phần chi nhánh cho chọn thôi chứ đừng tính toán khỏag cách thế thì phức tạp lắm mình chủ yêu chú tâm vào khoảng bán và thanh toán đó là mấy use case chính
```

**Kết quả ghi nhận:** Ưu tiên luồng bán hàng/thanh toán, khách tự chọn chi nhánh. AI ban đầu hiểu rộng và định đơn giản hóa phí giao; yêu cầu tiếp theo đã đính chính phạm vi đó.

## P-REQ-005 — Chỉ bỏ khoảng cách, giữ các chức năng khác

**Loại:** Đính chính phạm vi. **Nội dung nguyên văn:**

```text
bỏ phần khoảng cách thôi chứ đửng bỏ chức năng khác nha
```

**Kết quả ghi nhận:** Giữ khu vực giao/phí giao và toàn bộ chức năng khác. Chỉ loại tính khoảng cách và tự tìm chi nhánh gần nhất. Đây là chỉ dẫn áp dụng cho mọi nhánh.

## P-REQ-006 — Hồ sơ theo yêu cầu giảng viên và chia nhánh use case

**Loại:** Yêu cầu bộ hồ sơ và tổ chức Git. **Nội dung nguyên văn:**

```text
1. Với sự hỗ trợ của AI, Nhóm hãy viết tài liệu bằng file .docx chi tiết hướng dẫn xây dựng từng chức năng của ứng dụng gồm các phần:các hình kiến trúc úng dụng, các hình sơ đồ hoạt động của chức năng + hình diễn giải Source Code, hình diễn giải giao diện, file hướng dẫn setup ứng dụng, danh sách các prompt nhóm đã sử dụng để AI hỗ trợ sinh code, sinh hình,...

đây là yêu cầu của thầy tôi khi dùng AI cho project này bạn tạo file md cho các nhánh của từng chức năng để bạn tôi note vào file md đó và sau đó tôi sẽ đổi nó thánh file docx bạn viết sẵn md thì ghi trong đó để cho các model của bạn tôi đọc và hiểu quy trình làm của mỗi công việc đó như kiểu note lại prompt hình kiến trúc sơ đồ hoạt động ... của chức năng của bạn tôi làm bạn bám vào yêu cầu đó và soạn các file md, ở đây chúng mình sẽ chia thành viên phụ trách cho các chức năng sau đó sẽ ghép các chức năng đó lại của từng nhánh còn nhánh main là nhánh cấu hình chung để bạn tôi từ đã phát triển các chức năng để k lệch cấu hình với nhau bạn làm các nhánh cho các chức năng nha bạn làm sao để mà các nhánh chức năng của 1 use case liên quan nhau để tôi chia cho mỗi người những nhánh có liên quan nhau về 1 use case để họ đảm nhiệm thay vì nhiều nhánh k liên quan nhau mà do 1 người làm tốt nhất là bạn chia nhánh theo use case cho người đó làm từ đầu đến cuối
```

**Kết quả trong lượt thực hiện này:** Soạn bộ docs chung, hướng dẫn ba module theo luồng nghiệp vụ, template UC/prompt/hình và quy trình tích hợp; tạo các nhánh Git local khi hoàn tất thao tác Git. **Giới hạn:** Chưa có screenshot source/giao diện chạy thực hoặc báo cáo DOCX; nhóm bổ sung khi lập trình.

## P-REQ-007 — Lưu prompt của người dùng

**Loại:** Yêu cầu lưu vết AI. **Nội dung nguyên văn:**

```text
bạn nhớ note lại các prompt mình gửi bạn luôn nha note vào md
```

**Kết quả:** Tạo chính file nhật ký này; mỗi module có AI_LOG riêng; phân biệt prompt mẫu và đã dùng.

## P-REQ-008 — Hướng dẫn công việc và cấu hình trên từng nhánh

**Loại:** Bổ sung yêu cầu đồng bộ đặc tả. **Nội dung nguyên văn:**

```text
bạn nhớ mỗi nhánh làm file md giải thích công việc mỗi nhánh và cấu hình như nào sẵn cho các nhánh đó nha để đừng làm lệch d:\WEBCUOIKI\PROJECT_PLAN.md d:\WEBCUOIKI\DATABASE_DESIGN.md d:\WEBCUOIKI\USE_CASES.mdd:\WEBCUOIKI\docs\README.md
```

**Diễn giải đường dẫn phục vụ công việc:** Có bốn file PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md và docs/README.md; tin nhắn gốc thiếu dấu phân cách giữa hai đường dẫn cuối. **Kết quả:** Mỗi nhánh có file hướng dẫn riêng, bảng công việc/UC, schema/owner, config/contract, setup và minh chứng cần ghi; baseline chung giữ trên main.

## 2. Liên kết kết quả và kiểm chứng

| Yêu cầu | Tài liệu thực hiện | Nội dung cần nhóm tiếp tục |
| --- | --- | --- |
| P-REQ-001–005 | Ba đặc tả ở gốc repository | Hiện thực và kiểm thử nghiệp vụ |
| P-REQ-006/008 | README, BRANCH_WORKFLOW, COMMON_BASELINE, SETUP, hồ sơ ba module | Hoàn thành B0, điền người/commit, lập trình, bổ sung hình và setup thực |
| P-REQ-007 | AI_PROMPT_HISTORY và AI_LOG của module | Ghi thêm prompt AI đã sử dụng thật và phần nhóm đánh giá/chỉnh sửa |

Sau khi tạo nhánh, commit Git là bằng chứng file tồn tại, không phải bằng chứng tính năng đã chạy. Người tổng hợp điền hash commit cuối và kết quả kiểm tra vào hồ sơ tích hợp, không thay prompt nguyên văn.
