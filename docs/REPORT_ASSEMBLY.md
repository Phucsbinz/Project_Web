# UTETra — Tổng hợp hồ sơ Markdown thành báo cáo Word

## 1. Ánh xạ yêu cầu giảng viên

| Yêu cầu | Nhóm chuẩn bị tại đâu | Điều kiện đưa vào báo cáo |
| --- | --- | --- |
| Kiến trúc ứng dụng | PROJECT_PLAN + baseline + sơ đồ thực của từng module | Nguồn sơ đồ và ảnh xuất, giải thích thành phần/luồng |
| Sơ đồ hoạt động chức năng | Hồ sơ từng UC | Có luồng chính/lỗi/quyền; phù hợp code thực |
| Hình diễn giải source code | Hồ sơ UC + assets code | Code thật, callout/caption, path/symbol/commit và diễn giải |
| Hình diễn giải giao diện | Hồ sơ UC + assets ui | Screenshot chạy thật theo vai trò/trạng thái |
| Setup ứng dụng | SETUP và phần riêng từng module | Version/profile/config/lệnh đã kiểm chứng, không secret |
| Prompt sinh code/sinh hình/... | Nhật ký yêu cầu chung và AI_LOG từng module | Nguyên văn, công cụ/model nếu biết, kết quả, nhóm sửa/kiểm chứng |

## 2. Cấu trúc báo cáo đề xuất

1. Thông tin đề tài/thành viên, mục tiêu, bảng phân công use case/role/nhánh và minh chứng GitHub.
2. Khảo sát hiện trạng thực theo SURVEY_SYSTEM_ANALYSIS, phân tích actor/use case/sequence/ERD; mẫu DOCX là tham khảo.
3. Kiến trúc toàn ứng dụng, công nghệ, dữ liệu và cấu hình chung.
4. Hướng dẫn setup từ môi trường sạch, seed demo và cách chạy.
5. Chức năng danh mục và chi nhánh: từng UC của TV1.
6. Chức năng tài khoản/thành viên: từng UC của TV2.
7. Chức năng giỏ/đặt/thanh toán/vận hành/realtime/báo cáo: từng UC của TV3.
8. Ghép nhánh và kiểm thử xuyên suốt, kết quả và hạn chế thực tế.
9. Phụ lục nhật ký prompt AI và kiểm kê hình/code/commit.

Mỗi UC trình bày mục tiêu → kiến trúc → activity → hướng dẫn code từng bước → hình source có diễn giải → hình UI → setup/test → vai trò AI. Không chỉ gom ảnh hoặc danh sách prompt mà thiếu hướng dẫn xây dựng.

## 3. Quy ước lưu minh chứng

```text
docs/features/<module>/
├── IMPLEMENTATION.md
├── AI_LOG.md
├── use-cases/<UC-ID>.md
├── ai-responses/<PROMPT-ID>.md
└── assets/<UC-ID>/
    ├── architecture.mmd
    ├── architecture.png
    ├── activity.mmd
    ├── activity.png
    ├── code-01.png
    ├── ui-01.png
    └── test-01.png
```

Các thư mục minh chứng được tạo khi có file thật. ID hình dùng prefix CAT/IDN/ORD + UC + loại + số, chẳng hạn ORD-UC-ORD-03-CODE-01. Không đặt tất cả file `image1.png`. Giữ nguồn hình có thể sửa và file ảnh đủ rõ để xuất Word. Không dùng hình chứa token/secret/dữ liệu cá nhân thật.

## 4. Bảng kiểm kê hình chung

| ID hình | UC/owner | File ảnh | File nguồn | Caption | Prompt | Commit | Kiểm chứng |
| --- | --- | --- | --- | --- | --- | --- | --- |
| [điền] | [điền] | [điền] | [điền] | [điền] | [điền/nếu có] | [điền] | [điền] |

Sơ đồ do AI gợi ý bằng Mermaid vẫn cần ghi prompt thật nếu được dùng. Sơ đồ mẫu của bộ tài liệu này ghi nguồn “thiết kế ban đầu trong hồ sơ nhánh”; không biến thành prompt sinh hình đã sử dụng. Chụp code/UI là thao tác minh chứng, không tự coi là hình AI sinh.

## 5. Quy trình chuyển đổi

1. Chốt commit tích hợp và yêu cầu mỗi thành viên bàn giao hồ sơ UC/prompt/ảnh thực.
2. Nhóm trưởng rà placeholders, kiểm link, số hình, nguồn code và kết quả test.
3. Tạo một bản Markdown tổng hợp theo mục lục đã đánh số lại và nhóm theo vai trò ở trên, với đường dẫn ảnh hợp lệ; chuyển các Mermaid thành PNG/SVG trước khi chuyển Word.
4. Chọn công cụ chuyển Markdown → DOCX hoặc dàn trong Word. Nếu dùng Pandoc, cần cài thực và tạo file tổng hợp trước; lệnh mẫu dưới đây chưa được chạy trong lượt soạn hồ sơ này.
5. Mở DOCX hoặc render để kiểm mọi trang: ảnh/code rõ, caption đúng, bảng không bị cắt, mục lục/số trang hợp lệ, font tiếng Việt chuẩn.
6. Sửa bản nguồn và xuất lại đến khi đạt yêu cầu; giao cả DOCX và nguồn/hình cho nhóm.

```powershell
# Ví dụ sau khi đã cài Pandoc và tạo report/UTETra_REPORT.md:
pandoc report/UTETra_REPORT.md --from=gfm --to=docx --toc --output=report/UTETra_REPORT.docx
```

Không kỳ vọng Pandoc tự render Mermaid. Không dùng lệnh mẫu làm chứng cứ đã xuất DOCX. Bản Word cần kiểm trực quan trước khi nộp.

## 6. Checklist nghiệm thu hồ sơ

- [ ] Đủ 55 UC P0 theo USE_CASES hiện tại, cộng 3 UC Shipper nếu triển khai phần thêm; phân công và báo cáo theo actor/role.
- [ ] Mỗi UC có đủ sáu phần giảng viên yêu cầu, gồm setup và prompt thật.
- [ ] Các hình thiết kế/mockup và ảnh thực được ghi nhãn, không lẫn bằng chứng.
- [ ] Báo cáo giữ các chức năng đặc tả và chọn chi nhánh thủ công, không tính khoảng cách.
- [ ] Version/code/branch/commit và kết quả test có thể tra lại.
- [ ] Không còn placeholder trong phần nộp; phần chưa làm ghi rõ hạn chế.
- [ ] DOCX được mở/render và kiểm layout, hình, bảng, caption và tiếng Việt.

## Bổ sung theo quy định cuối kỳ

Đối chiếu từng chức năng Guest/User/Vendor/Manager/Admin và Shipper phần thêm với QUY_DINH_VA_CAU_HINH_CHUNG. Chú ý OTP email, top20, cart database, COD+VNPAY, like/view/review/comment50ký tự/ảnh/video, promo/commission/carrier và trả–hoàn. Phần đã làm phải có code/test/hình/commit, phần chưa làm ghi rõ. Thang điểm 10 tương đương100: quản lý1, ba tầng0.5, logic/security6, UI1, sáng tạo1, báo cáo0.5; chưa có deadline/link nộp. Một template Bootstrap, config properties main, MySQL riêng.
