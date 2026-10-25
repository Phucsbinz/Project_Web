# UTETra đối chiếu yêu cầu cuối kỳ sau khi bổ sung

Kiểm thử chính ngày 24/10/2026, đối chiếu và cập nhật ngày 25/10/2026. Nhánh `integration/utetra`, working tree phát triển tiếp từ `8369b67`, **chưa commit/push tại thời điểm audit**; người dùng đã yêu cầu phát hành ngày 25/10, SHA/trạng thái remote xem lịch sử Git. Đây là trạng thái mới nhất, thay việc dùng nguyên kết luận audit snapshot cũ cho source hiện tại.

## 1. Kết luận

**Phần lớn chức năng bắt buộc đã có source và kiểm thử; chưa đủ cơ sở nói đã hoàn chỉnh 100% để nộp.** Các điểm code thiếu rõ trước đó đã được bổ sung: QR bank, JWT DB, finite stock/realtime checkout, VNPAY adapter và quyền Manager quản lý carrier theo shop. Còn phải cấu hình/kiểm provider thật, dữ liệu demo, thông tin phân công và báo cáo DOCX cuối.

Không dự đoán điểm của thầy hoặc coi số testcase là tỷ lệ hoàn thiện. “Có code + test” không có nghĩa đã nghiệm thu mọi tổ hợp UI, DB/provider production hay hồ sơ cá nhân.

## 2. Những phần đã bổ sung ở lượt kiểm tra này

1. **VNPAY:** ký URL 2.1.0/HMAC SHA512, kiểm chủ đơn/hạn dùng, verify IPN/merchant/ref/amount/status, chống callback/receipt trùng, return không ghi PAID, lưu callback hash/outcome, failure/REVIEW, tiền đến sau hủy và ghi hoàn thủ công bởi Admin. Main config vẫn disabled vì chưa có merchant thật. Xem [VNPAY](features/order-payment/VNPAY.md).
2. **Manager carrier:** tạo/sửa tên và phí carrier riêng trong shop được giao; không sửa carrier global hoặc shop khác. Checkout/biểu phí không dùng carrier riêng sai shop. Migration `2026102403`, UI tools và regression test đã có.
3. **Setup:** viết lại [SETUP](SETUP.md) vì bản baseline cũ còn nói chỉ có ba bảng và chưa có auth/OTP. Hướng dẫn hiện phản ánh source tích hợp, JWT DB, carrier, bank/VNPAY và provider cần cấu hình.
4. **Khảo sát:** đọc lại mẫu DOCX; thực hiện khảo sát bàn hai hệ thống đồ uống công khai, lưu ảnh thật/URL/HTTP/thời điểm và bài học. [Kết quả khảo sát](SURVEY_FINDINGS_20261024.md). Chưa phỏng vấn hoặc thử giao dịch trên các website đó.
5. **Hình báo cáo:** thêm kiến trúc tổng thể, activity VNPAY và 5 ảnh source có dòng/chú giải trong [report-assets](report-assets/manifest.json); có script `tools/build_report_figures.py` dựng lại từ source. Hình source không thay screenshot ứng dụng chạy.
6. **Nhật ký/đặc tả:** cập nhật prompt, UC VNPAY/Manager và chỉ mục catalog. Giữ bản đặc tả cũ để tra lịch sử nhưng đánh dấu trạng thái hiện tại, không đưa câu “chưa có source” cũ vào báo cáo như tình trạng mới.

## 3. Khảo sát phân tích thiết kế và phân công

- **Khảo sát hiện trạng:** đã có nghiên cứu công khai có nguồn và ảnh; cần nhóm đọc, xác nhận người thực hiện và bổ sung phỏng vấn nếu thầy yêu cầu. Không sao chép nhận xét Tiki/Shopee cũ trong mẫu thành sự thật 2026.
- **Phân tích hệ thống:** có actor, use case, business contract, thiết kế DB và luồng; cần rà ERD/sequence/từ điển dữ liệu theo migration cuối rồi đưa vào DOCX.
- **Phân công:** đã có ba nhóm UC gắn nhánh catalog, identity và order, làm đủ tầng. Thiếu bảng tên/MSSV/owner/reviewer được nhóm xác nhận cho bản nộp; không tự suy ra thành viên từ tên tác giả Git.
- **GitHub:** có lịch sử nhánh/commit, nhưng các bổ sung mới hiện chưa commit/push; cần chốt release và minh chứng từng thành viên theo phạm vi người dùng cho phép.
- **Một template Bootstrap:** dùng một SiteMesh `layouts/app.jsp`, Bootstrap 5.3.3 và theme chung; Shared UI checker so main đạt. Có UI thật responsive, cần diễn tập bản cuối cho mọi vai trò.

## 4. Chức năng chung

1. Tìm kiếm sản phẩm: có query tham số, từ khóa, pagination và test.
2. Lọc theo shop/danh mục/giá, sort: có whitelist/validation và test.
3. Đăng ký tài khoản: có User chờ xác minh và OTP email.
4. Kích hoạt email bằng OTP: có TTL/giới hạn thử/gửi lại/dùng một lần. Máy hiện tại vẫn cần SMTP để nhận mail thật.
5. Đăng nhập: Spring Security, BCrypt, kiểm active/verified, phát JWT và đăng ký JTI/hash/hạn vào DB.
6. Đăng xuất: thu hồi phiên/cookie và guard socket; JWT access cũ không còn được sử dụng khi phiên bị thu hồi.
7. Quên mật khẩu: OTP theo purpose, reset một lần và thu hồi phiên; cần test nhận email thật trên máy báo cáo.
8. Bảo vệ mật khẩu: BCrypt, không lưu mật khẩu rõ. Đây là cơ chế băm phù hợp mục tiêu yêu cầu “mã hóa”.

Source/test identity đã có; không lấy mail mock làm minh chứng SMTP thật. Thành viên identity từng có biên bản Gmail thật trước đó, nhưng máy hiện tại chưa có cấu hình local cần thiết.

## 5. Guest và User

- **Guest homepage:** có chọn sản phẩm số ly đã bán >10 từ đơn DELIVERED/PAID, sắp số bán giảm dần; vẫn cần xác nhận cách diễn giải câu thầy. DB cần dữ liệu đủ ngưỡng để demo.
- **Home User, theo danh mục, top 20:** đủ nhóm mới/bán chạy/đánh giá/yêu thích và phân trang. Cần dataset đủ để nhìn thấy giới hạn/top/thứ tự thực.
- **Profile và nhiều địa chỉ:** có code/test; địa chỉ checkout phải thuộc người mua.
- **Chi tiết món:** có size/đường/đá/topping và quote server, kiểm tùy chọn, shop/category/variant đang bán và giờ nhận đơn.
- **Giỏ DB:** có cart người đăng nhập, chỉnh/xóa/hợp nhất guest, scope và idempotency.
- **COD:** có tạo đơn và xác nhận thu đủ tiền khi giao, không tự coi tạo đơn là PAID.
- **VNPAY hoặc MOMO:** chọn VNPAY, đã có adapter/test giao thức; **chưa chạy qua merchant sandbox thật**, vẫn disabled. Không phải cả hai cổng đều bắt buộc. QR thủ công có riêng, không gọi là VNPAY.
- **Lịch sử theo trạng thái:** có mới/xác nhận/đang giao/đã giao/hủy/trả–hoàn, quyền buyer và workflow. Bước đã lấy hàng phục vụ Vendor.
- **Hủy đơn:** có điều kiện/trạng thái/version, hoàn điểm/quota/tồn theo thời điểm; paid bank/VNPAY chờ hoàn, REVIEW chờ đối soát cổng.
- **Trả và hoàn:** có yêu cầu/ảnh/duyệt/từ chối/nhận hoặc miễn trả vật lý, ledger/đảo điểm/settlement một lần; rule nhóm là toàn đơn trong 3 giờ. Bank/VNPAY hoàn thực tế ngoài app rồi Admin xác nhận, chưa có API refund cổng tự động.
- **Thích sản phẩm:** có lưu DB và danh sách.
- **Sản phẩm đã xem:** có lịch sử và danh sách.
- **Đánh giá món đã mua:** có proof item đã giao/đã trả tiền và validation số sao.
- **Bình luận đã mua tối thiểu 50 ký tự:** có proof và kiểm độ dài Unicode/quyền sửa/xóa.
- **Ảnh/video bình luận:** có code kiểm định dạng/size/media/provider/cleanup và test. Ảnh Cloudinary có biên bản provider thật trước đó; video thật và luồng comment trên dataset cloud chưa nghiệm thu đầy đủ.
- **Chọn mã giảm giá:** có điều kiện/thời gian/shop/global/quota tổng và mỗi người, reserve/use/release, snapshot; kiểm voucher cuối giữa hai người và hai shop.

## 6. Vendor Seller

1. Có quyền mua hàng của User: source/test giữ được giỏ khi chuyển role, Vendor vẫn checkout được.
2. Đăng ký shop: hồ sơ chờ duyệt, gán policy/role và thu hồi phiên cũ; không tự duyệt shop của mình.
3. Trang chủ shop: sửa profile/banner/giờ/nhận đơn trong scope.
4. Sản phẩm của mình: CRUD/size/topping/availability, ảnh, stock/version; không sửa shop khác, không xóa snapshot lịch sử.
5. Đơn shop: mới/xác nhận/đã lấy/đang giao/đã giao/hủy/trả–hoàn, state machine và quyền.
6. Chương trình khuyến mãi: có chương trình shop, quota và tính giá đúng scope.
7. Doanh thu: có hàng/phí/hoa hồng/hoàn/net và lọc thời gian/shop; cần dữ liệu để demo số liệu.

## 7. Manager và Admin

- Tìm kiếm/quản lý user: đã có code/test. Manager theo quan hệ shop được phân công; Admin toàn hệ thống, kiểm khóa/role/thu hồi phiên và Admin cuối.
- Sản phẩm từng shop: có role/scope, validation/audit/version.
- Danh mục/topping: Admin có; Manager cần phân công `CATALOG_ADMIN` cho danh mục dùng chung. Cần cấp đúng tài khoản demo và chốt thầy chấp nhận delegation này; không tự mở quyền toàn cục cho mọi Manager.
- Chiết khấu app cho shop: có policy và snapshot tỷ lệ/tiền trên đơn; không lẫn với voucher giảm giá người mua.
- Khuyến mãi giảm % món và giảm phí giao: có code/test, Manager trong scope, Admin toàn app.
- Tên/phí nhà vận chuyển: **đã bổ sung Manager CRUD carrier riêng shop**, kèm biểu phí khu vực; Admin CRUD global và theo shop. Không áp carrier riêng sang shop khác; không cho đổi scope bằng request.

Chức năng quản lý carrier không còn chỉ Admin làm được. Chính sách danh mục dùng chung vẫn cần được nhóm trình bày rõ trong phân quyền và báo cáo.

## 8. Shipper và điều khoản đề tài khác

- Shipper xem/quản lý đơn được giao và thống kê: có code/test, cấu hình thật tắt. Thầy ghi phần thêm nên chưa bật không phải thiếu bắt buộc.
- Điều khoản đề tài không bán hàng không áp dụng UTETra; không dùng điều đó để bỏ Vendor.

## 9. Kiến trúc giao diện và sáng tạo

- Controller/View → Service → Repository; SQL shop/thống kê đã chuyển về tầng dữ liệu và bỏ Repository phụ thuộc Service. JPA có thật; native SQL tại Repository không tự là sai mô hình.
- Spring Security/JWT/CSRF, kiểm role/scope ở backend; ẩn menu không thay authorization.
- JSP server render kết hợp JSON/AJAX trong cùng app hợp lệ với yêu cầu đã gửi, không bắt buộc làm frontend framework khác.
- Một layout/template/theme, module JS/CSS theo phạm vi; đã kiểm Tomcat/SiteMesh thực và responsive các luồng được ghi trong biên bản.
- Sáng tạo có JWT DB, voucher đồng thời, finite stock, WebSocket/polling, ledger điểm, Cloudinary và bank/VNPAY adapter. DB xử lý tranh chấp, socket báo sau commit; không hứa tự được trọn 1 điểm sáng tạo.
- TiDB là MySQL-compatible; cần chốt nếu thầy đòi đúng MySQL Server. CHECK trên cluster OFF vẫn là việc cần rà/kiểm trên DB đích, không tự bật GLOBAL của cluster dùng chung.

## 10. Báo cáo DOCX và AI

1. **DOCX cuối:** chưa có bản UTETra hoàn chỉnh được nghiệm thu trong repo. Markdown, mẫu DOCX và hình chưa thay bản Word nộp.
2. **Hướng dẫn từng chức năng:** có hồ sơ module/UC, đã bổ sung VNPAY và carrier; catalog có chỉ mục source/test cập nhật, nhưng các bảng kết quả riêng/reviewer còn phải điền đúng thực tế.
3. **Hình kiến trúc:** đã thêm hình tổng thể hiện tại; có diagram module. Cần caption, đánh số và đặt đúng phần trong Word.
4. **Hình hoạt động:** identity có 16 bộ; catalog/order có nguồn Mermaid và thêm activity VNPAY, chưa thể coi toàn bộ UC đều đã có hình hoạt động cuối cùng được review.
5. **Hình source có giải thích:** identity có bộ riêng, thêm 5 ảnh source core. Còn phải bổ sung/duyệt theo từng phần được phân công, không dùng một hình chung thay mọi chức năng.
6. **Hình UI có diễn giải:** có ảnh thật nhiều module/kích thước, thêm Manager carrier và VNPAY H2. Ảnh fixture/mô phỏng phải ghi đúng, không gọi là provider thật.
7. **Setup:** đã viết lại theo bản mới, cần thành viên khác làm lại từ clone đến demo trên máy báo cáo.
8. **Prompt AI code/hình:** giữ AI_PROMPT_HISTORY và AI_LOG các module; thêm prompt hiện tại/kết quả. Hình vẽ từ source/chụp browser ghi đúng provenance, không bịa prompt sinh ảnh cho ảnh không dùng ImageGen.
9. **Phần cá nhân và trả lời:** nhóm điền danh tính/UC/commit, viết giải thích mình hiểu và diễn tập; không thể nghiệm thu khả năng trả lời chỉ qua source.

## 11. Thang điểm và phần còn phải chốt

- Quản lý/phân công/GitHub: 1/10, tương đương 10/100. Thiếu chốt bảng danh tính và release mới, không chỉ đếm commit.
- Ba tầng: 0,5/10, tương đương 5/100. Có cấu trúc và đã sửa data access lệch tầng; nhóm phải giải thích được luồng.
- Đầy đủ chức năng/logic/validation/security: 6/10, tương đương 60/100. Có test và source, còn nghiệm thu provider/dataset/quyền được phân công và các trường hợp thực.
- UI đẹp/dễ dùng/responsive: 1/10, tương đương 10/100. Có nền chung và ảnh/check; đánh giá đẹp/dễ dùng do thầy/người dùng, chưa test tay mọi màn hình cuối.
- Sáng tạo: 1/10, tương đương 10/100. Có chức năng thực, cần demo/giải thích và không thay phần bắt buộc.
- Báo cáo/trả lời: 0,5/10, tương đương 5/100. Còn DOCX hoàn chỉnh và diễn tập cá nhân.

Các việc chưa thể tự đánh dấu xong:

1. Merchant sandbox, public IPN/Return và giao dịch VNPAY thật; SMTP trên máy báo cáo, MP4 provider và thu/hoàn bank thực tế do chủ tài khoản kiểm.
2. Dataset demo đủ shop/product/carrier/rate/policy/voucher/address/top và trạng thái đơn; không tự bịa doanh thu trên cloud.
3. Xác nhận Guest >10, TiDB/MySQL và quyền catalog dùng chung; bổ sung khảo sát thực/đối tượng nếu thầy yêu cầu hơn khảo sát bàn.
4. Tên/MSSV/owner/reviewer, hoàn thiện hình theo UC, DOCX và kiểm Word; setup máy sạch và trả lời câu hỏi.
5. Commit/push bản release theo nhánh được người dùng yêu cầu; hạn/link nộp lấy từ LMS, chưa được cung cấp cụ thể.

## 12. Bằng chứng kiểm tra mới

- Maven **clean verify 161 tests, 0 failures/errors/skipped**, WAR build thành công. Log `.local/final-requirements-verify.log`, XML trong target.
- [Tổng hợp 21 test suite](report-assets/acceptance/test-summary.json) được trích từ XML thực; đây là lượt build ngày 24/10, không ghi thành một lần chạy Maven mới ngày 25/10.
- Browser H2 riêng: Manager tạo/sửa carrier của shop và VNPAY tạo URL/IPN ký giả lập/duplicate/PAID; không lỗi JavaScript. [Biên bản UI](report-assets/acceptance/verification.json).
- WAR/TiDB validate **9 migration**, áp dụng `2026102403`, Hibernate validate đạt. Không seed commerce hoặc thực hiện giao dịch cổng/bank trên cloud trong lượt này.
- [8 kiểm tra HTTP native](report-assets/acceptance/native-http-checks.json): health/menu/assets, chặn truy cập Manager/checkout khi chưa đăng nhập, IPN thiếu chữ ký bị từ chối và Return chỉ chuyển hướng.
- Đọc DB lần tiếp tục ngày 25/10: **0 shop active, 0 sản phẩm active, 0 carrier active, 0 biểu phí active, 0 policy chiết khấu active và 0 đơn đã giao/đã trả tiền**; 9 tài khoản đã xác minh đang active, 9 migration thành công. Vì vậy hiện chưa thể demo mua hàng thực trên DB cloud chỉ bằng việc chạy ứng dụng. Kết quả đọc tổng hợp: [database-summary](report-assets/acceptance/database-summary.txt). Không ghi dữ liệu hoặc cấu hình cluster trong lần kiểm tra này.
- Shared UI checker so main, Node syntax và git diff whitespace kiểm sau sửa. Kết quả HTTP native và hình đọc cùng biên bản, không dùng ảnh H2 chứng minh provider thật.

Đây là đánh giá có giới hạn và danh sách việc hoàn thiện, **chưa phải xác nhận đã đáp ứng toàn bộ để nộp hoặc dự báo điểm**.
