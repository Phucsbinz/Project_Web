# UTETRA-FIX-20261028 — Prompt sửa ba lỗi và kiểm rủi ro khóa

- Ngày/giờ, người gửi: 28/10/2026, người dùng; không rõ thời điểm gửi chính xác.
- Nhánh, UC, commit trước: feature/order-payment; UC-ORD-03/07, UC-AUTH-06/USER-01/05/06, merge cart/tồn; HEAD269ea2c8c6d434078ca96846527dcd3d47a6873e và working tree có sẵn.
- Công cụ/model: Codex; không suy đoán mã model cụ thể.
- Loại: sửa lỗi, kiểm thử và review.
- Mục tiêu/ngữ cảnh/file đã cấp: D:\WEBCUOIKI, AGENTS.md, báo cáo ALL_ROLE_AUDIT_20261028.md; phạm vi chi tiết nguyên văn dưới đây.
- Trạng thái: đã dùng để sửa; kết quả thực và giới hạn ở SCOPED_FIXES_20261028.md.

## Prompt nguyên văn

```text
Bạn là Senior Java/Spring Boot Engineer chịu trách nhiệm sửa lỗi UTETra tại D:\WEBCUOIKI.

NHIỆM VỤ
Điều tra và sửa đúng 3 lỗi đã được báo cáo dưới đây, đồng thời đánh giá một rủi ro deadlock. Ưu tiên giữ nguyên hành vi đúng của các chức năng hiện có. Không mở rộng tính năng hoặc refactor ngoài phạm vi.

Các lỗi được báo cáo mức Trung bình:
1. Đơn nhóm: host không chọn món vẫn đặt được, nhưng sau đó bị chặn trang thanh toán/theo dõi nhóm.
2. Đổi mật khẩu: OTP/grant quên mật khẩu cấp trước đó vẫn dùng được sau khi đổi mật khẩu trong tài khoản.
3. Đánh giá/bình luận: giao diện chỉ chọn được 100 dòng mua gần nhất, không truy cập được các dòng cũ hơn.

Rủi ro cần xác minh:
4. Checkout và hủy đơn có thể khóa tồn kho theo thứ tự khác nhau, dẫn tới deadlock. Chưa tái hiện trên MySQL/TiDB; không được ghi thành lỗi đã xác nhận.

SKILL VÀ CÁCH LÀM
- Đọc và áp dụng .agents/skills/using-superpowers/SKILL.md.
- Dùng systematic-debugging để xác định nguyên nhân trước khi sửa.
- Dùng test-driven-development cho bản sửa lỗi nghiệp vụ: có test tái hiện thất bại đúng nguyên nhân trước, rồi sửa để test đạt.
- Dùng verification-before-completion trước khi kết luận.
- Làm từng lỗi theo thứ tự; không cho nhiều agent cùng sửa file dùng chung.
- Được thực hiện việc điều tra, sửa và kiểm tra trong phạm vi này; không dừng ở đề xuất kế hoạch.
- Chỉ hỏi khi thiếu quyết định nghiệp vụ thực sự ảnh hưởng cách sửa hoặc cần thao tác ngoài phạm vi được giao.

A. KIỂM TRA BAN ĐẦU VÀ GIỚI HẠN THAY ĐỔI

1. Đọc AGENTS.md, PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md và tài liệu docs/features/ liên quan.
   Đọc docs/SHARED_UI.md trước mọi sửa giao diện.
   Xác định contract mới nhất áp dụng cho từng luồng; nếu tài liệu mâu thuẫn, nêu rõ, không tự chọn theo cách dễ làm test đạt.

2. Kiểm tra branch, git status và diff hiện tại.
   Workspace có thể đang chứa thay đổi chưa commit của người dùng hoặc agent khác.
   Không reset, restore, stash, xóa, ghi đè hoặc hoàn tác thay đổi có sẵn.
   Không tự chuyển nhánh, merge, commit hoặc push.
   Khi cần sửa cùng file đã thay đổi, đọc kỹ diff và giữ nguyên phần không thuộc nhiệm vụ.

3. Xác định baseline kiểm thử bằng môi trường test của dự án.
   Ghi rõ lỗi nào đã có trước khi sửa, lệnh nào chạy được, lệnh nào bị chặn.
   Không dùng kết quả kiểm thử trong tài liệu cũ làm bằng chứng cho code hiện tại.

4. Với mỗi lỗi, trước khi sửa phải nêu ngắn gọn:
   - Bước tái hiện và hành vi mong đợi theo contract.
   - Nguyên nhân gốc, kèm file/hàm và bằng chứng.
   - File dự kiến sửa.
   - Các caller, endpoint, role và chức năng khác dùng chung code.
   - Những hành vi phải giữ nguyên và test hồi quy tương ứng.

5. Giữ Java 21, Spring Boot 3, JSP/JSTL, SiteMesh, Bootstrap, JPA và MySQL-compatible.
   Không đổi datasource, provider, cơ chế JWT hoặc cấu hình nghiệp vụ chỉ để làm test đạt.
   Không vô hiệu hóa CSRF, validation, ownership hoặc phân quyền.
   Không đổi API/schema nếu chưa chứng minh cần thiết.
   Nếu cần migration, tạo migration tiến mới; không sửa migration đã áp dụng.
   Không chạy migration hoặc thử cạnh tranh trên dữ liệu thật khi chưa được cho phép.

B. LỖI 1  HOST KHÔNG CÓ MÓN BỊ CHẶN THANH TOÁN/THEO DÕI

1. Tái hiện đơn nhóm có host không chọn món nhưng thành viên có món.
2. Truy vết từ tạo nhóm, tham gia, chốt đơn tới quyền đọc nhóm,
   xem đơn, xem thông tin thanh toán và thực hiện thanh toán.
3. Kiểm tra có đang nhầm quyền điều phối của host với việc sở hữu
   một dòng món hoặc một phần tiền phải trả hay không.
4. Xác định contract có cho phép host không mua món hay không:
   - Nếu cho phép: bảo đảm host truy cập được các màn hình và thao tác
     điều phối mà contract quy định, kể cả khi không có phần tiền riêng.
   - Nếu không cho phép: chặn nhất quán trước khi tạo đơn, thông báo rõ;
     không tạo thành công rồi mới chặn bước tiếp theo.
   Không tự thêm món giả, phần tiền giả hoặc participant giả để vượt kiểm tra.
5. Không mở quyền đọc/chi trả cho người ngoài nhóm hoặc thay đổi
   quyền xem phần thanh toán của từng thành viên.

Kiểm thử bắt buộc:
- Host có món và host không có món.
- Nhóm hoàn toàn không có món.
- Thành viên hợp lệ và người ngoài nhóm.
- Host trả toàn bộ và chia phần theo các phương thức contract đang hỗ trợ.
- Thanh toán/theo dõi sau tạo đơn, tải lại trang và truy cập URL trực tiếp.
- Luồng đơn cá nhân vẫn hoạt động.
- Tổng tiền, phần tiền, trạng thái và quyền thực hiện thao tác không bị thay đổi sai.

C. LỖI 2  OTP/GRANT QUÊN MẬT KHẨU CŨ VẪN CÒN HIỆU LỰC

1. Phân biệt OTP, grant/token đã đổi từ OTP và các bước reset password hiện có.
2. Tái hiện riêng:
   - OTP được cấp trước khi đổi mật khẩu trong tài khoản.
   - Grant reset đã được cấp trước khi đổi mật khẩu trong tài khoản.
3. Xác định vì sao các bằng chứng reset cũ vẫn vượt qua kiểm tra.
4. Sau khi đổi mật khẩu thành công, vô hiệu hóa các OTP/grant reset cũ
   của đúng tài khoản, theo thiết kế phù hợp với cơ chế lưu trữ hiện có.
5. Kiểm tra tính nguyên tử và tình huống cạnh tranh giữa đổi mật khẩu,
   xác minh OTP và sử dụng grant; tránh khoảng hở dùng lại bằng chứng cũ.
6. Không vô hiệu hóa OTP khác mục đích hoặc dữ liệu của tài khoản khác.
   Không thay chính sách session/JWT ngoài yêu cầu nếu chưa có căn cứ.
   Không ghi mật khẩu, OTP, token hoặc grant vào log.

Kiểm thử bắt buộc:
- OTP reset cũ bị từ chối sau đổi mật khẩu thành công.
- Grant reset cũ bị từ chối sau đổi mật khẩu thành công.
- Yêu cầu đổi mật khẩu thất bại không làm sai trạng thái credential.
- Luồng quên mật khẩu mới tạo sau đó vẫn hoạt động.
- Grant đã sử dụng không thể replay; grant hết hạn vẫn bị từ chối.
- Tài khoản khác và OTP khác mục đích không bị ảnh hưởng.
- Đăng nhập bằng mật khẩu mới hoạt động; mật khẩu cũ không còn dùng được.
- Có kiểm tra tình huống cạnh tranh phù hợp nếu nguyên nhân liên quan transaction.

D. LỖI 3  KHÔNG CHỌN ĐƯỢC DÒNG MUA CŨ HƠN 100 DÒNG

1. Xác định giới hạn nằm ở query, service, API, controller hay giao diện.
2. Đọc contract xác định dòng mua nào đủ điều kiện đánh giá/bình luận.
3. Triển khai cách truy cập các dòng hợp lệ cũ hơn bằng phân trang,
   tải thêm hoặc cơ chế sẵn có phù hợp trong dự án.
   Không chỉ đổi giới hạn 100 thành một số lớn hơn.
   Không tải toàn bộ lịch sử mua không giới hạn vào bộ nhớ hoặc trang.
4. Giữ thứ tự ổn định với tiêu chí phụ duy nhất khi cần, tránh lặp hoặc
   bỏ sót dòng giữa các trang trong điều kiện dữ liệu không thay đổi.
5. Backend vẫn kiểm ownership và điều kiện mua hợp lệ khi gửi đánh giá,
   không tin ID do giao diện gửi lên.

Kiểm thử bắt buộc:
- Tài khoản có hơn 100 dòng mua đủ điều kiện vẫn chọn được dòng cũ.
- Các mốc 0, 1, 100, 101 dòng và trang cuối.
- Không lặp hoặc mất dòng khi chuyển trang với dữ liệu cố định.
- Dòng mua của tài khoản khác hoặc không đủ điều kiện bị từ chối.
- Quy định đã đánh giá, nội dung bình luận và media hiện có được giữ nguyên.
- Trải nghiệm tải, rỗng và lỗi rõ ràng; giao diện mobile vẫn sử dụng được.

E. RỦI RO DEADLOCK  CHECKOUT VÀ HỦY ĐƠN

1. Lập bảng thứ tự lấy khóa thực tế của checkout, hủy đơn và những
   luồng liên quan cùng thay đổi tồn kho.
   Bao gồm thứ tự khóa nhiều dòng tồn và các khóa khác nếu liên quan.
2. Chỉ ra cặp đường thực thi có thể hình thành chu kỳ chờ,
   kèm bằng chứng source/query; phân biệt nguy cơ lý thuyết với lỗi tái hiện được.
3. Nếu xác nhận các luồng lấy khóa không nhất quán, sửa ở phạm vi nhỏ nhất
   để thống nhất thứ tự dựa trên khóa định danh ổn định.
   Không giả định sort danh sách trong Java tự động bảo đảm thứ tự khóa SQL;
   phải kiểm tra cách repository/database thực hiện truy vấn.
4. Không dùng synchronized trong một JVM để thay bảo đảm transaction DB.
   Không thêm retry vô hạn, nuốt lỗi hoặc làm mất tính nguyên tử.
5. Giữ nguyên chống oversell, hoàn tồn đúng một lần, rollback và idempotency.
6. Nếu có MySQL/TiDB test riêng được phép sử dụng, chạy kiểm thử cạnh tranh
   có điều phối, timeout và lặp phù hợp với nhiều sản phẩm theo thứ tự đảo nhau.
   Kiểm tra kết quả dữ liệu cuối, không chỉ kiểm tra không có exception.
7. Nếu không có môi trường phù hợp, ghi rõ chưa xác minh trên MySQL/TiDB.
   Test H2 không phải bằng chứng loại trừ deadlock trên MySQL/TiDB.
   Không tuyên bố đã loại bỏ mọi deadlock chỉ vì một kịch bản đã đạt.

F. NGĂN ẢNH HƯỞNG CHỨC NĂNG KHÁC

- Mỗi bản sửa phải gắn với nguyên nhân và tiêu chí nghiệm thu của một lỗi.
- Trước khi sửa helper/service/repository dùng chung, tìm tất cả nơi gọi;
  kiểm thử các caller có hành vi bị tác động.
- Giữ contract của trường hợp đang đúng; không dùng điều kiện đặc biệt
  theo tài khoản demo, ID cố định hoặc dữ liệu test.
- Không refactor lan rộng, đổi tên hàng loạt, format toàn repository
  hoặc nâng dependency trong lượt sửa lỗi này.
- Không xóa/skip test, giảm assertion hoặc sửa kỳ vọng chỉ để xanh.
  Chỉ sửa test sai khi có bằng chứng contract, và giải thích rõ.
- Nếu bản sửa gây regression, xử lý regression trước khi chuyển lỗi tiếp theo.
- Nếu phát hiện lỗi độc lập ngoài phạm vi, ghi nhận riêng; không tự sửa lan.
- Nếu ba lần sửa cùng vấn đề vẫn thất bại, dừng sửa thử, tổng hợp bằng chứng
  và đánh giá lại nguyên nhân/thiết kế trước khi tiếp tục.

G. KIỂM CHỨNG VÀ BÀN GIAO

1. Với từng lỗi: chạy test tái hiện trước sửa, test sau sửa và test hồi quy
   của các chức năng dùng chung; lưu bằng chứng thực.
2. Sau các bản sửa, chạy bộ test/build của repository theo hướng dẫn setup
   và Maven Wrapper, không bỏ test. Báo rõ test fail, error và skipped.
3. Nếu sửa giao diện:
   - Giữ duy nhất layouts/app.jsp, Bootstrap 5.3.3 và utetra.css dùng chung.
   - CSS/JS riêng phải scope theo module.
   - Chạy py -3 tools/check_shared_ui.py.
   - Chỉ thêm --compare main khi main đã được xác nhận cập nhật.
   - JSP/layout phải kiểm render Tomcat/SiteMesh thực tế.
   - Kiểm tra màn hình bị tác động ở 390px, 768px và desktop;
     kiểm lỗi console/network và thao tác người dùng liên quan.
4. Đọc lại diff cuối để phát hiện thay đổi ngoài phạm vi và lỗi phân quyền.
5. Ghi prompt thực đã nhận và kết quả thực vào Markdown theo
   docs/templates/PROMPT_ENTRY.md; không ghi secret hoặc nhận đã test
   những bước chưa chạy. Không ghi đè nhật ký có sẵn.
6. Không commit/push/merge.
```

## Prompt tiếp nối nguyên văn

```text
ụa sao đăng xuất ra mà nó vẫn hiện đơn hàng trên giỏ hàng mặt dù đã đăng xuất ra nè
```

## Phản hồi AI và kết quả tạo ra

Báo cáo kết quả thực: [SCOPED_FIXES_20261028.md](../SCOPED_FIXES_20261028.md). Mỗi test mới bảo vệ lỗi/vòng đời/quyền tương ứng; UI có script `tools/check_scoped_fixes_ui.py` với H2 cô lập. Không ghi secret trong hồ sơ này. Không nhận AI sinh toàn bộ project.

## Nhóm kiểm tra và chỉnh sửa

Codex điều tra/sửa theo thứ tự, reviewer độc lập chỉ đọc. Danh sách file, RED/GREEN, lệnh build/UI, hạn chế, bằng chứng và trạng thái chưa commit được cập nhật ở báo cáo; người dùng/nhóm xác nhận khi nghiệm thu. Không có commit sau trong lượt này.

## Đối chiếu quy định cuối kỳ

Giữ mô hình ba tầng/MVC, Spring Security/CSRF, sở hữu user/shop và điều kiện đã mua, single SiteMesh/Bootstrap. Shipper là phần thêm, không đổi assignment. H2 và provider stub không thay kiểm TiDB/MySQL/SMTP/Cloudinary/merchant thật. Không đổi config/provider hoặc viết migration cho các bản sửa này.
