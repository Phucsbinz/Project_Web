# UTETra — Hướng dẫn cho AI làm việc trên nhánh chức năng

## 1. Prompt mở phiên cho thành viên

Mẫu dưới đây **chưa được xem là prompt đã dùng**. Thành viên điền thông tin, gửi cho AI của mình, sau đó lưu nguyên văn bản đã gửi và phản hồi tại nhật ký module.

```text
Bạn đang hỗ trợ dự án UTETra tại repository này, nhánh <tên nhánh>.
Người phụ trách: <họ tên>, use case hiện làm: <mã UC>.
Trước khi sửa code, đọc PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md,
docs/README.md, docs/COMMON_BASELINE.md, docs/SETUP.md,
file BRANCH_<MODULE>.md và docs/features/<module>/IMPLEMENTATION.md.
Kiểm tra baseline/commit main và cấu hình thực trong repository.
Thực hiện use case từ dữ liệu đến giao diện theo file hướng dẫn nhánh.
Không tự đổi stack, schema, profile DB, contract hoặc phạm vi chức năng.
Chi nhánh do khách chọn; không tính khoảng cách/tự chọn cửa hàng gần nhất.
Nếu thiếu dependency giữa nhánh, ghi contract/mock test và việc cần phối hợp.
Ghi nguyên văn prompt tôi gửi, mục đích, AI/model (nếu biết), kết quả AI,
file/commit thay đổi, cách tôi sửa và kiểm chứng vào AI_LOG.md của module.
Mẫu prompt chỉ đánh dấu gợi ý, không ghi như đã thực hiện.
Mỗi UC cập nhật hồ sơ: kiến trúc, activity diagram, hướng dẫn từng bước,
code có diễn giải, ảnh giao diện thật, setup, testcase và kết quả thực tế.
Sơ đồ có file nguồn và PNG/SVG xuất ra; mockup phải ghi nhãn rõ.
Không ghi xong hoặc test pass nếu chưa có minh chứng thực.
Hãy kiểm tra hiện trạng và triển khai phần đã được giao.
```

## 2. Quy trình cho mỗi công việc

1. Đọc nhánh, use case, baseline và dữ liệu hiện tại; xác định owner/phụ thuộc.
2. Ghi prompt thực tế trước khi chạy công việc hoặc ngay sau khi nhận được prompt.
3. Xác định đầu vào/đầu ra, tiền điều kiện, luồng lỗi, quyền và transaction.
4. Vẽ kiến trúc chức năng và sơ đồ hoạt động; ghi nhãn thiết kế dự kiến nếu chưa có code.
5. Làm từng bước Entity/migration → Repository/DTO → Service → Controller → JSP/JS → kiểm thử. Không sinh khối code lớn mà không kiểm tra tích hợp.
6. Chọn đoạn code trọng tâm, thêm chú giải về dữ liệu/quyền/transaction; ghi path, symbol và commit. Chụp đoạn code thực tế sau khi hoàn thiện.
7. Chạy và chụp giao diện ở trạng thái bình thường, lỗi/rỗng và vai trò liên quan.
8. Ghi cách setup/seed và kết quả test thật; liệt kê hạn chế/lỗi còn lại.
9. Cập nhật bảng hình, prompt, tiến độ và checklist bàn giao; review rồi commit.

## 3. Dùng AI để tạo hình và diễn giải

Sơ đồ kiến trúc/hoạt động nên sinh nguồn Mermaid hoặc PlantUML rồi kiểm tra với code và xuất ảnh; báo cáo Word cần ảnh thật, không chỉ khối Mermaid. Nếu AI tạo hình raster minh họa, ghi công cụ/model, prompt, ảnh gốc và cách chỉnh; kiểm tra lại chữ và luồng trước khi dùng.

Ảnh source code là code thực của nhóm, kèm callout đánh số hoặc chú giải dưới hình. Ảnh giao diện là ảnh ứng dụng đang chạy; mockup AI chỉ dùng để trình bày ý tưởng và ghi rõ “mockup, chưa triển khai”. Không dùng hình AI như bằng chứng ứng dụng đã chạy hoặc test đã qua.

Hình cần có tên file, caption, mục đích, liên kết nguồn, mã UC, prompt liên quan, commit và trạng thái dự kiến/đã kiểm chứng. Nội dung nhìn rõ khi chèn vào Word, không chụp cả màn hình IDE khiến code quá nhỏ. Che token, password, cookie, địa chỉ/điện thoại thật.

## 4. Ghi prompt và vai trò của nhóm

Nhật ký ghi nguyên văn prompt, không chỉ tóm tắt. Nếu prompt chứa secret, lưu bản che và ghi rõ vị trí đã che; không lưu secret nguyên văn. Lưu toàn văn phản hồi AI trong file riêng khi dài; nhật ký có đường dẫn và phần nhóm sử dụng/bỏ/sửa.

Không tự gán tên model hoặc thời gian chính xác nếu không có dữ liệu. Không suy ra prompt sinh code/sinh hình trong cuộc trò chuyện trước khi chúng thực sự diễn ra. Nếu AI hoặc thành viên phát hiện một quyết định đã được sửa, thêm bản ghi đính chính, giữ bản ghi cũ để phản ánh lịch sử.

Mỗi phiên mới đọc AI_LOG và hồ sơ UC để tiếp tục từ phần đã làm. Kiểm tra nhánh bằng git trước sửa; không tạo lại toàn bộ cấu hình hoặc xóa tài liệu gốc. Cập nhật file tại chỗ.
