# UTETra — Đối chiếu yêu cầu của giảng viên

> Trạng thái mới nhất sau lượt kiểm yêu cầu đầy đủ: [FINAL_REQUIREMENTS_STATUS](FINAL_REQUIREMENTS_STATUS.md). VNPAY đã có adapter/test nhưng chưa merchant thật; carrier Manager theo shop đã bổ sung. Các kết luận theo snapshot/lượt kiểm cũ bên dưới giữ để tra lịch sử.

Ngày kiểm tra: **24/10/2026**. Nhánh: `integration/utetra`. Mã nguồn được đánh giá: **8369b67**.

> Sau audit này, người dùng đã yêu cầu bổ sung QR ngân hàng và sửa các phần có thể xử lý. Xem [REMEDIATION_20261024](REMEDIATION_20261024.md) để biết thay đổi mới và việc bàn giao. Các nhận xét/số liệu dưới đây giữ nguyên tại snapshot ban đầu; không dùng chúng để phủ nhận source đã bổ sung sau đó.

## 1. Kết luận

**Chưa thể báo cáo project đã hoàn chỉnh 100% hoặc đã sẵn sàng nghiệm thu toàn bộ yêu cầu.** Nền công nghệ, giao diện chung và phần lớn nghiệp vụ đã có code; bộ kiểm thử tích hợp gần nhất đạt 137 test. Tuy nhiên vẫn còn thiếu thanh toán trực tuyến, dữ liệu để demo nghiệp vụ trên CSDL hiện tại, cấu hình gửi OTP trên máy này và một phần lớn hồ sơ báo cáo cuối kỳ. Kiến trúc ba tầng và quyền quản lý nhà vận chuyển của Manager cũng còn điểm cần chỉnh hoặc làm rõ.

Đây là đánh giá kỹ thuật dựa trên source, tài liệu và bằng chứng kiểm thử; **không phải điểm chấm hay xác nhận nghiệm thu của giảng viên**. Không tự quy đổi số dòng checklist thành phần trăm điểm, vì các yêu cầu có trọng số và độ quan trọng khác nhau.

Lượt này chỉ kiểm tra và lập tài liệu: không thêm dữ liệu demo, không sửa cấu hình/nghiệp vụ, không gửi email, không tạo giao dịch, không bật Shipper và không push nhánh.

## 2. Nguồn quy định và giới hạn đối chiếu

1. **Quy định cuối kỳ người dùng gửi nguyên văn**, được lưu trong [AI_PROMPT_HISTORY](AI_PROMPT_HISTORY.md), mục `P-REQ-009`: chức năng theo vai trò, một template Bootstrap, phân công, báo cáo và thang điểm.
2. **Yêu cầu báo cáo khi dùng AI** trong hội thoại và prompt history: DOCX hướng dẫn từng chức năng, hình kiến trúc, sơ đồ hoạt động, hình diễn giải source/UI, setup và các prompt đã sử dụng.
3. [KhaoSat_PhanTichHeThong.docx](../KhaoSat_PhanTichHeThong.docx): tài liệu tham khảo cách khảo sát/phân tích, không phải kết quả khảo sát thực tế của UTETra.
4. [Quy định và cấu hình chung](QUY_DINH_VA_CAU_HINH_CHUNG.md), [PROJECT_PLAN](../PROJECT_PLAN.md), [DATABASE_DESIGN](../DATABASE_DESIGN.md), [USE_CASES](../USE_CASES.md): bản cụ thể hóa của nhóm; có cả quyết định riêng, không coi mọi chi tiết trong đó là nguyên văn yêu cầu thầy.
5. Source, test, hình và tài liệu của ba module trong [docs/features](features/).

**Không áp nhầm đề khác:** `final-report.pdf` trong thư mục tài liệu cấu hình là báo cáo BOOKROOM giữa kỳ; `Đề thi Quá trình_01_2627.pdf` là đề BOOKROOM giữa kỳ. Các giới hạn của bài đó như Servlet, hai role, đúng năm bảng, sáu sách/trang, footer thông tin bài thi hoặc tên lớp theo MSSV không tự động trở thành quy định cuối kỳ UTETra. Không dùng ngày thi giữa kỳ làm hạn nộp cuối kỳ.

**Cách đọc trạng thái:**

- **Có code + test:** đã tìm thấy triển khai và kiểm thử liên quan; chưa đồng nghĩa đã demo toàn bộ trên TiDB hoặc provider thật.
- **Có kiểm chứng runtime:** có bằng chứng chạy WAR/Tomcat/provider thật trong phạm vi ghi rõ.
- **Một phần:** có triển khai/hồ sơ nhưng còn thiếu điều kiện hoặc khác phạm vi yêu cầu.
- **Thiếu:** chưa thấy triển khai hoặc sản phẩm bàn giao cần thiết trong snapshot được kiểm tra.
- **Cần xác nhận:** câu chữ hoặc cách phân quyền cần thầy đồng ý; không tự kết luận là vi phạm chắc chắn.
- **Phần thêm:** không tính là thiếu chức năng bắt buộc nếu chưa bật.

## 3. Bằng chứng hiện có và khả năng demo

### 3.1. Kiểm thử và chạy ứng dụng

- [Biên bản tích hợp 24/10](integration/VERIFICATION_20261024.md): Maven `clean verify` đạt **137 tests, 0 failures, 0 errors, 0 skipped**, build WAR thành công trên Java 21.
- Lượt audit đọc lại `target/surefire-reports/TEST-*.xml`, tổng vẫn là 137/0/0/0. Không chạy lại toàn bộ test chỉ để lập báo cáo khi source không đổi.
- WAR đã chạy với `utetra_db`, Hibernate validate đạt và Flyway xác nhận **6 migration**. [16 HTTP checks](integration/HTTP_CHECKS_20261024.json) đạt, gồm trang công khai, tài nguyên, redirect bảo vệ, API 401 và checkout thiếu CSRF trả 403.
- Lượt audit chạy lại `py -3 tools/check_shared_ui.py --compare main`: **đạt**, một layout SiteMesh và 16 file shared UI giống main.
- Các HTTP check trên **không phải** nghiệm thu mua hàng, gửi OTP, upload video, thanh toán ngân hàng và hoàn tiền end-to-end trên TiDB. Kiểm thử nghiệp vụ tích hợp chủ yếu sử dụng H2 và fixture riêng.
- Lịch sử [kiểm chứng order](features/order-payment/VERIFICATION.md) có upload/xóa PNG thật qua Cloudinary ngày 14/10. Bằng chứng đó không thay kiểm toàn bộ luồng trả hàng/bình luận video trên bản tích hợp hiện tại.
- [Hồ sơ OTP 20/10](features/identity-membership/OTP_MAIL_20261020.md) ghi nhận kiểm tra email thật ở máy thành viên. Không suy diễn rằng nhóm chưa bao giờ gửi được mail.

### 3.2. CSDL đang dùng — truy vấn chỉ đọc ngày 24/10

| Dữ liệu/điều kiện | Kết quả |
| --- | ---: |
| Database được kiểm tra | `utetra_db` |
| Cửa hàng đang hoạt động | 0 |
| Sản phẩm đang hoạt động | 0 |
| Sản phẩm đang hoạt động và có thể bán | 0 |
| Đơn đã giao và đã thanh toán | 0 |
| Sản phẩm có số lượng đã bán > 10 | 0 |
| Nhà vận chuyển đang hoạt động | 0 |
| Biểu phí giao đang hoạt động | 0 |
| Chính sách chiết khấu đang hoạt động | 0 |
| Tài khoản đang hoạt động, đã xác minh | 9 |
| Migration thành công | 6 |
| `tidb_enable_check_constraint` | **OFF** |

Kết quả chi tiết cục bộ: `.local/teacher-audit/database-summary.txt` (không chứa credential). Đây là số liệu ở thời điểm kiểm tra, không cam kết dữ liệu sẽ giữ nguyên sau đó. Không truy cập/sửa database `project_idv`.

**Hệ quả:** chưa đủ dữ liệu hiện tại để demo chọn shop/món, vận chuyển, checkout, doanh thu, top bán chạy hoặc bình luận sau mua. Không phải kết luận các chức năng này không có code. Nhóm cần chuẩn bị dữ liệu demo có nhãn rõ ràng, đủ điều kiện nghiệp vụ; không trình bày đơn fixture hoặc số bán giả lập như giao dịch khách hàng thật.

**Ràng buộc CSDL:** migration chạy thành công không chứng minh CHECK đang được TiDB thực thi. Cần rà soát từng CHECK đang dựa vào DB, chứng minh validation tương ứng ở ứng dụng và kiểm lại trên DB đích. Không bật thiết lập GLOBAL của cluster dùng chung chỉ để làm đẹp kết quả audit.

### 3.3. Cấu hình phục vụ demo

- SMTP trên máy này: không có `.local/mail.properties`; không có các biến `UTETRA_MAIL_ENABLED`, `UTETRA_MAIL_USERNAME`, `UTETRA_MAIL_APP_PASSWORD` ở môi trường tiến trình kiểm tra. Cấu hình mặc định tắt gửi mail. **Có code email OTP, nhưng bản chạy mặc định ở máy này chưa sẵn sàng demo nhận OTP thật.** Xem [MAIL_SETUP](features/identity-membership/MAIL_SETUP.md).
- `cloudinary.enabled=true`; đã có adapter và bằng chứng provider ảnh thật trước đó. Cần kiểm lại qua UI nghiệp vụ khi có dữ liệu demo; chưa coi video thật đã nghiệm thu.
- `payment.vnpay.enabled=false`; không có implementation cổng VNPAY/MOMO hoàn chỉnh. Đây là thiếu code nghiệp vụ, không chỉ thiếu API key.
- `features.shipper.enabled=false`; code/test đã có nhưng đang tắt. Shipper là phần thêm theo đề.
- Server đã dừng sau kiểm chứng trước đó; lượt audit không khởi động lại.

### 3.4. Stack đã chốt cho UTETra

Đây là stack đề tài/nhóm đã chọn theo tài liệu cấu hình, không khẳng định mọi số phiên bản là điều kiện chấm điểm nguyên văn của thầy.

| Công nghệ | Tình trạng thực tế |
| --- | --- |
| Java 21, Spring Boot 3.5.13 | Khóa trong `pom.xml`, đã build/chạy WAR. |
| JSP/JSTL | Có dependency Jakarta, JSP dưới WEB-INF và render Tomcat thực tế. |
| Decorator SiteMesh 3.2.2 | Có cấu hình filter và một layout chung; đã kiểm runtime. |
| Bootstrap 5.3.3 | Một bộ assets/theme chung, có kiểm cấu trúc và tải tài nguyên. |
| JPA | Có entity/repository, transaction và validate schema; việc còn dùng SQL/JDBC không tự phủ định JPA, nhưng cần sửa trách nhiệm tầng như mục 10. |
| MySQL | Driver MySQL, Flyway MySQL; DB thực là TiDB Cloud tương thích MySQL, cần xác nhận chấp nhận nếu thầy đòi đúng MySQL Server. |
| Spring Security + JWT | Có filter, BCrypt, xác minh JWT/phiên và kiểm quyền; không chỉ khai báo dependency. |
| WebSocket | Có Spring STOMP `/ws`, kiểm phiên/scope và sự kiện sau commit; chưa demo đầy đủ trên dataset TiDB hiện tại. |
| Cloudinary | Có adapter upload/xóa ảnh và kiểm provider trước đó; cần bổ sung demo nghiệp vụ/media thật trên bản cuối. |

## 4. Khảo sát, phân công, kiến trúc và giao diện

| ID | Yêu cầu của thầy | Đánh giá hiện tại | Minh chứng / phần còn phải làm |
| --- | --- | --- | --- |
| A01 | Khảo sát hiện trạng | **Một phần — thiếu khảo sát thực** | [SURVEY_SYSTEM_ANALYSIS](SURVEY_SYSTEM_ANALYSIS.md) còn `[CẦN BỔ SUNG]`. Cần hệ thống khảo sát, thời điểm, ảnh, nhận xét và kết luận áp dụng cho UTETra; mẫu DOCX không thay khảo sát nhóm. |
| A02 | Phân tích và thiết kế hệ thống | **Một phần** | Có kế hoạch, đặc tả UC, schema/contract và Mermaid; cần đối chiếu lại với code đã ghép, hoàn thiện sơ đồ/từ điển dữ liệu và đưa vào báo cáo. |
| A03 | Phân công thành viên theo chức năng/vai trò | **Một phần** | Ba nhóm use case đã chia hợp lý nhưng bảng chung vẫn dùng TV1/TV2/TV3. Điền tên, MSSV, UC/role, reviewer và commit; không cần ép một người chỉ làm đúng một role. |
| A04 | Quá trình commit GitHub | **Có lịch sử, chưa chốt hồ sơ nộp** | Có lịch sử nhiều tác giả/nhánh; tên Git không tự chứng minh danh tính thành viên. Bổ sung mapping người–commit–UC. Bản tích hợp mới đang local, chưa push trong lượt tích hợp trước. |
| A05 | Mô hình ba tầng | **Một phần — còn phụ thuộc sai tầng** | Phần lớn Controller → Service → Repository. `StoreService`, `CatalogStatistics` truy vấn JDBC trực tiếp; `CatalogQueryRepository` lại phụ thuộc `CatalogStatistics` ở package service. Xem mục 10. |
| A06 | Spring Boot phải sử dụng Spring Security | **Có code + test** | [SecurityConfig](../src/main/java/com/utetra/config/SecurityConfig.java), JWT filter và service kiểm quyền/scope. Không dùng việc ẩn menu thay authorization. |
| A07 | Sử dụng 01 template Bootstrap | **Có kiểm chứng cấu trúc/runtime** | Start Bootstrap Shop Homepage, Bootstrap 5.3.3, `utetra.css`, duy nhất `layouts/app.jsp`, một decorator. Shared checker đạt, bản chạy đã kiểm header/footer và tải Bootstrap một lần. |
| A08 | UI đẹp, dễ dùng, responsive | **Một phần kiểm chứng** | Có ảnh nhiều kích thước trong các module và các cải tiến UI; bản tích hợp mới chưa chạy lại toàn bộ visual QA/mobile/tablet/desktop của mọi UC. “Đẹp/dễ dùng” cần người dùng/thầy đánh giá. |
| A09 | Logic, validation, security đầy đủ từng role | **Có nền và test; chưa nghiệm thu toàn bộ** | 137 test gồm scope, phiên/quyền, CSRF, transaction và cross-module; còn các gap payment, DB CHECK, dữ liệu và demo provider. Không tuyên bố đã kiểm hết mọi lỗ hổng. |

## 5. Chức năng chung và Guest

| ID | Yêu cầu | Đánh giá | Minh chứng / giới hạn |
| --- | --- | --- | --- |
| C01 | Tìm kiếm sản phẩm | **Có code + test** | `CatalogService`, `CatalogQueryRepository`, `CatalogIntegrationTest`; có tìm tên và phân trang. DB hiện chưa có món để demo. |
| C02 | Lọc sản phẩm | **Có code + test** | Lọc shop/danh mục/giá, whitelist sort, giới hạn kích thước trang; cần dữ liệu kiểm các tổ hợp UI. |
| C03 | Đăng ký và gửi OTP kích hoạt qua email | **Có code + test; máy demo chưa sẵn sàng** | `AuthService`, `OtpService`, `MailOtpDelivery`, `IdentityIntegrationTest`; SMTP hiện chưa cấu hình ở máy này. Không gọi mock OTP là email thật. |
| C04 | Đăng nhập | **Có code + test** | Kiểm tài khoản active/verified, BCrypt, JWT/phiên. Cần demo tài khoản mỗi role với dữ liệu đúng scope. |
| C05 | Đăng xuất | **Có code + test** | Thu hồi phiên/token, xử lý cookie; có kiểm lifecycle phiên và WebSocket. |
| C06 | Quên mật khẩu, OTP email, đặt lại mật khẩu | **Có code + test; phụ thuộc SMTP** | OTP theo purpose, reset grant, dùng một lần và thu hồi phiên cũ. Cần thao tác nhận OTP thật trên máy báo cáo. |
| C07 | Mật khẩu được bảo vệ | **Có code + test** | BCrypt là băm mật khẩu, đáp ứng mục đích bảo vệ mật khẩu của yêu cầu “mã hóa”; không lưu mật khẩu rõ. |
| G01 | Trang chủ Guest theo điều kiện bán trên 10 sản phẩm của các shop | **Có code; cần xác nhận diễn giải và dữ liệu** | Query đang lấy sản phẩm có tổng số lượng bán >10 từ đơn DELIVERED/PAID. Thầy cần xác nhận cách hiểu; hiện có 0 món đạt ngưỡng. |
| G02 | Sắp xếp từ lớn đến nhỏ | **Có code + test** | Guest sort số bán giảm dần. Cần ít nhất nhiều món đạt ngưỡng với số bán khác nhau để demo đúng thứ tự. |

Không tự đổi G01 thành yêu cầu chắc chắn “hiển thị trên 10 thẻ sản phẩm”. Câu thầy gửi còn có thể được hiểu khác; nhóm cần hỏi rõ điều kiện lựa chọn sản phẩm.

## 6. Chức năng User

| ID | Yêu cầu | Đánh giá | Minh chứng / giới hạn |
| --- | --- | --- | --- |
| U01 | Trang chủ User | **Có code + test** | `CatalogController.menu`, JSP catalog dùng layout chung; thiếu dữ liệu demo. |
| U02 | Sản phẩm theo danh mục | **Có code + test** | Query/JSP lọc danh mục đang hoạt động và phân trang. |
| U03 | Top 20 sản phẩm mới nhất | **Có code + test** | Nhóm newest với giới hạn top 20; cần dữ liệu đủ để kiểm giới hạn. |
| U04 | Top 20 bán chạy | **Có code + test** | Nhóm sold dựa số lượng bán; DB hiện không có đơn trả tiền/đã giao để minh họa. |
| U05 | Top 20 đánh giá | **Có code + test** | Nhóm rating dựa đánh giá hiển thị; cần dữ liệu người mua hợp lệ. |
| U06 | Top 20 yêu thích | **Có code + test** | Nhóm favorites dựa dữ liệu lượt thích. |
| U07 | Phân trang hoặc lazy loading các danh sách trên | **Có code + test** | Chọn phân trang; không bắt buộc triển khai đồng thời lazy loading. Đã giữ `top=true` khi lọc ở bản ghép. |
| U08 | Profile người dùng | **Có code + test** | Identity profile và view/test liên quan; có giao diện thực đã chụp. |
| U09 | Quản lý nhiều địa chỉ nhận hàng | **Có code + test** | Identity address, địa chỉ mặc định, kiểm chủ sở hữu; checkout lấy địa chỉ thuộc người mua. |
| U10 | Chi tiết sản phẩm | **Có code + test** | Catalog detail, thông tin món/tùy chọn và community; chưa demo được với DB hiện tại. |
| U11 | Giỏ hàng lưu database | **Có code + test** | `CartService`, guest merge và test xuyên module giữ giỏ sau chuyển User thành Vendor. Giỏ khách tạm là bổ sung, không thay giỏ DB người đăng nhập. |
| U12 | Thanh toán COD | **Có code + test** | `CheckoutService`, `OrderWorkflowService`; snapshot giá/phí, tạo đơn và xác nhận thu tiền theo quyền. Chưa checkout thật trên TiDB trong lượt kiểm chứng gần nhất. |
| U13 | Thanh toán VNPAY hoặc MOMO | **Thiếu implementation** | Checkout đang gán `paymentMethod="COD"`. Chưa có luồng tạo thanh toán, kiểm chữ ký/IPN, đối chiếu tiền/đơn và chống callback lặp. Chỉ cần một trong VNPAY/MOMO nếu yêu cầu cổng trực tuyến; không đòi cả hai. Xem mục 10 về câu chữ đề. |
| U14 | Lịch sử đơn mới | **Có code + test** | Order list/detail theo buyer và trạng thái NEW. |
| U15 | Lịch sử đã xác nhận | **Có code + test** | Trạng thái CONFIRMED và chuyển trạng thái có quyền. |
| U16 | Lịch sử đang giao | **Có code + test** | Trạng thái DELIVERING; không cho client tự gán trạng thái tùy ý. |
| U17 | Lịch sử đã giao | **Có code + test** | DELIVERED và điều kiện thanh toán/thu tiền. |
| U18 | Hủy đơn | **Có code + test** | Kiểm trạng thái, phạm vi actor và rollback/hoàn lại tài nguyên liên quan. |
| U19 | Trả hàng – hoàn tiền | **Có code + test; provider/e2e cần bổ sung** | Luồng yêu cầu, bằng chứng, duyệt/từ chối và ledger/điểm; chính sách nhóm là toàn đơn trong 3 giờ. Chưa có hoàn tiền VNPAY/MOMO vì cổng chưa triển khai. |
| U20 | Thích sản phẩm | **Có code + test** | `CommunityService`, lưu DB và danh sách yêu thích. |
| U21 | Sản phẩm đã xem | **Có code + test** | Community ghi lịch sử xem và danh sách tương ứng. |
| U22 | Đánh giá sản phẩm đã mua | **Có code + test** | Kiểm item thuộc đơn đã giao/đã trả tiền, điểm 1–5; cần demo mua thành công trước đánh giá. |
| U23 | Bình luận sản phẩm đã mua, tối thiểu 50 ký tự | **Có code + test** | Community kiểm bằng chứng mua, độ dài 50–2000 theo code point và quyền sửa/xóa. |
| U24 | Bình luận hình ảnh | **Có code + test; cần demo UI/provider** | Upload server, kiểm MIME/nội dung/kích thước, Cloudinary và cleanup. Provider ảnh có kiểm trước đó; chưa nghiệm thu comment xuyên module trên TiDB. |
| U25 | Bình luận video | **Có code + test; chưa xác nhận provider thật trên máy này** | `CommunityMediaService` hỗ trợ MP4, kiểm media/codec/thời lượng và giới hạn; không dùng kiểm upload PNG làm bằng chứng video thành công. |
| U26 | Chọn mã giảm giá | **Có code + test** | Checkout tính lại giá phía server, kiểm điều kiện/quota/scope/snapshot; cần dữ liệu khuyến mãi demo. |

## 7. Chức năng Vendor/Seller

| ID | Yêu cầu | Đánh giá | Minh chứng / giới hạn |
| --- | --- | --- | --- |
| V01 | Có các quyền của User | **Có code + test** | Vendor mua hàng bằng cùng nghiệp vụ User; test xuyên module kiểm chuyển role/thu hồi phiên/giữ giỏ. Cần demo Vendor mua hàng trong UI. |
| V02 | Đăng ký shop | **Có code + test** | `StoreService`: đăng ký, duyệt, gán policy, chuyển quyền Vendor và thu hồi phiên cũ. |
| V03 | Quản lý trang chủ shop | **Có code + test** | Hồ sơ shop/banner, giờ mở cửa/nhận đơn và trang shop; thiếu shop hoạt động trong DB hiện tại. |
| V04 | Quản lý sản phẩm của mình | **Có code + test** | Catalog guard theo owner/shop, CRUD/trạng thái/ảnh/tùy chọn; cần demo không sửa được shop khác. |
| V05 | Quản lý đơn mới và đã xác nhận | **Có code + test** | OrderWorkflow theo owner/scope, state machine. |
| V06 | Bước đã lấy hàng | **Có code + test** | Có PICKED_UP trong workflow Vendor; phân biệt với lịch sử User chỉ cần các trạng thái đề liệt kê. |
| V07 | Đang giao và đã giao | **Có code + test** | Kiểm trạng thái/quyền và dữ liệu thanh toán. |
| V08 | Hủy, trả hàng – hoàn tiền | **Có code + test; thiếu online refund** | Phạm vi shop và ledger COD đã có; chưa có giao dịch hoàn qua cổng trực tuyến. |
| V09 | Tạo chương trình khuyến mãi | **Có code + test** | Catalog promotion và order tính quyền/điều kiện áp dụng; cần dữ liệu demo. |
| V10 | Quản lý doanh thu shop | **Có code + test** | `OperationsService.revenue`, tính theo đơn/thanh toán/hoàn và scope; DB hiện chưa có doanh thu. |

## 8. Chức năng Manager và Admin

Hai role đều có các nhóm chức năng thầy liệt kê; Manager giới hạn cửa hàng được phân công, Admin toàn hệ thống. Bảng này không dùng “Admin làm được” thay cho “Manager làm được”.

| ID | Yêu cầu | Manager | Admin | Minh chứng / giới hạn |
| --- | --- | --- | --- | --- |
| M01 | Tìm kiếm user | **Có code + test** | **Có code + test** | `UserManagementService`; Manager theo user có liên hệ shop được giao, Admin toàn cục. |
| M02 | Quản lý user | **Có code + test theo scope** | **Có code + test** | Khóa/mở, vai trò/phân công trong quyền; thu hồi phiên khi quyền đổi, bảo vệ Admin cuối. Cần demo ranh giới quyền rõ. |
| M03 | Quản lý sản phẩm từng shop | **Có code + test** | **Có code + test** | `CatalogGuard.operates`, membership active và shop scope. |
| M04 | Quản lý danh mục | **Một phần/cần xác nhận phân quyền** | **Có code + test** | Manager cần membership `CATALOG_ADMIN` để sửa danh mục/topping dùng chung. MANAGER thông thường không tự có quyền này. Cần khai báo phân công và demo đúng tài khoản hoặc chỉnh theo thầy. |
| M05 | Quản lý chiết khấu app cho shop | **Có code + test theo scope** | **Có code + test** | Chính sách và gán shop, snapshot hoa hồng trong đơn; đây là khoản app thu từ shop, không đồng nhất voucher khách. |
| M06 | Khuyến mãi giảm % sản phẩm | **Có code + test theo scope** | **Có code + test** | Catalog promotion và checkout áp dụng điều kiện. |
| M07 | Khuyến mãi giảm phí vận chuyển | **Có code + test theo scope** | **Có code + test** | Promotion shipping kết hợp biểu phí và kiểm quota/scope. |
| M08 | Quản lý tên nhà vận chuyển | **Chưa đáp ứng đầy đủ quyền theo đề** | **Có code + test** | `OperationsService.carrier()` gọi `admin(actor)`; Manager không sửa tên nhà vận chuyển. Cần thầy chấp nhận Admin quản lý danh mục chung hoặc bổ sung cơ chế Manager phù hợp. |
| M09 | Quản lý phí nhà vận chuyển | **Một phần** | **Có code + test** | Manager sửa biểu phí theo khu vực trong scope qua `rate()`, nhưng phí cơ bản của carrier vẫn Admin-only. Không ghi Manager đã có đầy đủ tên/phí như Admin. |

Các giới hạn scope nhằm tránh sửa dữ liệu ngoài chi nhánh là hợp lý về an toàn. Tuy nhiên, nếu giới hạn làm thiếu chức năng thầy yêu cầu cho Manager thì vẫn cần điều chỉnh hoặc được chấp nhận; không tự coi thiết kế riêng là thay thế quy định.

## 9. Phần thêm và điều khoản không áp dụng

| ID | Nội dung | Đánh giá |
| --- | --- | --- |
| S01 | Shipper quản lý đơn được giao | **Phần thêm, có code + test, production đang tắt**. Bật cờ và demo khi nhóm chọn báo cáo phần này. |
| S02 | Shipper thống kê đơn được giao | **Phần thêm, có code + test, production đang tắt**. Không tính thiếu vào chức năng bắt buộc. |
| X01 | Các đề tài không bán hàng chỉ có Guest/User/Manager/Admin | **Không áp dụng**: UTETra là đề tài bán hàng, không được dùng điều này để bỏ Vendor. |
| X02 | Sáng tạo: ví điện tử/JWT/voucher/WebSocket… | Có JWT, voucher, WebSocket và upload media; cần trình bày demo/giải thích thật. Các ví dụ sáng tạo không có nghĩa phải làm mọi công nghệ liệt kê hoặc tự động được trọn điểm. |

## 10. Những điểm cần sửa hoặc xác nhận trước khi tuyên bố đúng đề

### 10.1. Thanh toán trực tuyến chưa hoàn thành

Đề viết `thanh toán(COD, VNPAY hoặc MOMO)`. Tài liệu phạm vi nhóm đang diễn giải là **COD + một cổng VNPAY hoặc MOMO**. Với cách hiểu này, project chưa đủ yêu cầu. Nếu thầy cho phép chỉ COD thì cần ghi nhận xác nhận đó và cập nhật phạm vi; không tự suy ra việc người dùng cho hoãn VNPAY đồng nghĩa thầy miễn yêu cầu.

Chỉ bật cờ hoặc điền merchant/key không tạo ra tích hợp. Cần đủ luồng redirect/thanh toán, xác minh callback/IPN và số tiền/mã đơn, chống xử lý lặp, trạng thái thất bại/hết hạn, đối soát và xử lý hoàn theo phạm vi đã thống nhất. Sandbox thật phải được ghi đúng là sandbox, không phải thanh toán ngân hàng production.

### 10.2. Ba tầng chưa sạch

Nguồn cụ thể:

- [StoreService](../src/main/java/com/utetra/service/StoreService.java) inject `JdbcTemplate` và đọc/ghi trực tiếp một số dữ liệu chính sách, phí giao, user/phiên.
- [CatalogStatistics](../src/main/java/com/utetra/service/CatalogStatistics.java) là `@Service` nhưng trực tiếp truy vấn SQL.
- [CatalogQueryRepository](../src/main/java/com/utetra/repository/catalog/CatalogQueryRepository.java) inject `CatalogStatistics`, tạo chiều phụ thuộc Repository → Service.

Native SQL/JDBC không tự động là sai khi truy vấn nằm trong tầng dữ liệu. Vấn đề ở đây là trách nhiệm truy cập DB và chiều phụ thuộc bị trộn. Nên chuyển truy vấn sang Repository/DAO, tách phần metadata kiểm bảng thành tầng dữ liệu, để Service điều phối nghiệp vụ và transaction. Kiểm lại test xuyên identity/catalog/order sau refactor. Đây là rủi ro thực với tiêu chí ba tầng 0,5 điểm, không tự ấn định thầy sẽ trừ bao nhiêu.

### 10.3. Quyền Manager chưa khớp hoàn toàn câu chữ

Tên/phí cơ bản nhà vận chuyển chỉ Admin sửa; Manager mới sửa biểu phí khu vực. Danh mục dùng chung đòi thêm quyền phân công `CATALOG_ADMIN`. Cần trình bày rõ với thầy hoặc điều chỉnh triển khai để Manager thực hiện đủ chức năng trong phạm vi được giao. Không giải quyết bằng cách mở quyền toàn hệ thống cho mọi Manager.

### 10.4. MySQL/TiDB và CHECK

Nhóm đang dùng TiDB Cloud qua giao thức/driver MySQL. Cần xác nhận nếu thầy yêu cầu **đúng MySQL Server** thay vì chấp nhận hệ tương thích MySQL; chưa có cơ sở khẳng định TiDB là vi phạm chắc chắn. Cũng không coi MySQL-compatible đồng nghĩa mọi ràng buộc MySQL đều hoạt động giống hệt: CHECK trên cluster đang OFF, phải kiểm ràng buộc thực tế ở DB đích.

### 10.5. Không phải vi phạm tự thân

- MVC server render JSP kết hợp API JSON/AJAX trong cùng ứng dụng không trái yêu cầu đã gửi. JSP vẫn là view; không có yêu cầu cấm API hay bắt buộc làm SPA.
- Một `app.jsp` SiteMesh dùng chung cho nhiều role đáp ứng định hướng một template; không bắt buộc tạo layout riêng từng role.
- SQL Server/MySQL/PostgreSQL là lựa chọn công nghệ, nhóm chọn MySQL; không phải bắt buộc tích hợp cả ba DB.
- Chọn chi nhánh thủ công, không tính khoảng cách là quyết định người dùng đã chốt; thầy không yêu cầu định vị/khoảng cách trong văn bản được cung cấp.
- Trả hàng trong 3 giờ, điểm/hạng, guest merge là quy tắc/bổ sung của nhóm; cần giải thích và test, không giả định thầy đã quy định đúng các con số này.
- 55 UC bắt buộc + 3 UC Shipper là cách nhóm phân rã đặc tả, không phải số lượng UC thầy ấn định. Chia ba nhánh theo nhóm UC vẫn được nếu mapping đủ vai trò và từng cá nhân báo cáo phần được giao.

## 11. Hồ sơ báo cáo và việc sử dụng AI

| ID | Yêu cầu bàn giao | Đánh giá hiện tại | Cần hoàn thiện |
| --- | --- | --- | --- |
| R01 | Báo cáo DOCX cuối kỳ | **Chưa thấy bản UTETra hoàn chỉnh được Git theo dõi** | File DOCX được track hiện là mẫu khảo sát. Markdown là nguồn biên soạn, chưa thay file nộp. Không kết luận nhóm không có DOCX ở máy khác. |
| R02 | Mỗi thành viên viết phần chức năng/role được giao | **Một phần** | Có hồ sơ module, nhưng mapping tên/MSSV/UC và xác nhận cá nhân/reviewer còn thiếu trong bảng chung. |
| R03 | Hướng dẫn chi tiết xây dựng từng chức năng | **Một phần** | Identity và order có nhiều ghi chú triển khai; catalog UC còn template/placeholder. Rà từng UC theo code/commit hiện tại, không chỉ mô tả mục tiêu. |
| R04 | Hình kiến trúc ứng dụng | **Một phần** | Có kiến trúc module order dạng SVG và mô tả/Mermaid. Cần hình tổng thể UTETra, boundary browser/Spring/Security/JPA/DB/providers và liên kết với source thực. |
| R05 | Hình sơ đồ hoạt động từng chức năng | **Một phần, không đồng đều** | Identity có 16 bộ nguồn/hình activity. Catalog/order có Mermaid trong MD nhưng chưa có bộ hình xuất riêng tương ứng từng UC. Kiểm sơ đồ phản ánh cả nhánh lỗi/quyền thực. |
| R06 | Hình diễn giải source code | **Một phần, thiếu ở catalog/order** | Identity có 16 file `code*.png`; chưa tìm thấy file ảnh cùng kiểu ở catalog/order. Code path hoặc code block chưa tự thay hình diễn giải có caption/callout trong DOCX. |
| R07 | Hình diễn giải giao diện | **Một phần** | Cả ba module có screenshot thật, gồm ảnh fixture. Cần lựa chọn, chú giải vai trò/thao tác/kết quả, bổ sung luồng thiếu và chụp lại sau tích hợp nếu UI đổi. Không dùng mockup thay ảnh chạy thật. |
| R08 | File hướng dẫn setup ứng dụng | **Có tài liệu; cần nghiệm thu máy sạch** | [SETUP chung](SETUP.md), SETUP module và MAIL_SETUP. Cần người khác làm theo từ clone đến demo và ghi dependency/provider/dữ liệu còn cần. |
| R09 | Danh sách prompt dùng AI sinh code | **Có nhật ký, chưa chứng minh đầy đủ mọi UC** | `AI_PROMPT_HISTORY.md`, `AI_LOG.md` các module. Mapping prompt → UC/file/commit → chỉnh sửa của nhóm → test; mẫu prompt chưa gửi không tính đã dùng. |
| R10 | Prompt dùng AI sinh hình/sơ đồ | **Một phần** | Cần truy nguồn từng hình: prompt/tool/model nếu có dùng AI, nguồn sơ đồ và cách render. Hình do nhóm tự vẽ/chụp thì ghi đúng, không bịa prompt AI cho hình không dùng AI. |
| R11 | Báo cáo và trả lời câu hỏi | **Chưa thể nghiệm thu bằng đọc code** | Hoàn thiện Word, phân công người trình bày và diễn tập: MVC/ba tầng, transaction, JWT/CSRF, scope, OTP, tổng tiền, idempotency, hoàn tiền, socket. |
| R12 | Nộp và báo cáo đúng lịch/link | **Chưa xác định từ dữ liệu được cung cấp** | Tin nhắn thầy chưa có ngày/link cụ thể. Nhóm kiểm LMS/thông báo lớp; không lấy hạn của đề giữa kỳ. |

### Kiểm kê hình trong hồ sơ module

| Module | Ảnh `code*.png/svg` | Nguồn `activity*.mmd/puml` | Hình `activity*.png/svg` | Hình có tên `architecture*.png/svg` |
| --- | ---: | ---: | ---: | ---: |
| identity-membership | 16 | 16 | 16 | 0 |
| catalog-sales | 0 | 0 | 0 | 0 |
| order-payment | 0 | 0 | 0 | 1 |

Đây là kiểm kê theo tên file trong `docs/features/<module>`, **không phải đếm mọi sơ đồ trong project**: Mermaid nhúng MD và hình đặt tên khác không nằm trong các số này. Các hình tồn tại cũng cần đọc caption/nội dung trước khi coi là đủ yêu cầu diễn giải. Order UC đã tự ghi rõ activity xuất riêng, ảnh code và DOCX còn cần hoàn thiện. Một số tài liệu cũ còn nói Cloudinary chưa cấu hình dù biên bản mới hơn đã kiểm provider; phải thống nhất trạng thái theo ngày/commit trước khi nộp.

## 12. Đối chiếu thang điểm thầy đưa

| Tiêu chí | Điểm /10 | /100 | Mức bám yêu cầu và rủi ro hiện tại |
| --- | ---: | ---: | --- |
| Quản lý, phân công, quá trình commit GitHub | 1 | 10 | Có nhánh và lịch sử; hoàn thiện danh tính/phân công và công bố bản tích hợp đã kiểm. Không hứa trọn điểm từ số commit. |
| Đúng cấu trúc ba tầng | 0,5 | 5 | Có tổ chức ba tầng, còn SQL trong Service và chiều Repository → Service cần chỉnh. |
| Đủ chức năng, logic, validation, security từng role | 6 | 60 | Phần lớn có code/test; payment trực tuyến thiếu, Manager chưa khớp đầy đủ, SMTP/demo/DB CHECK chưa nghiệm thu. Đây là nhóm rủi ro lớn nhất vì trọng số cao. |
| UI đẹp, dễ dùng, responsive | 1 | 10 | Một theme/layout chung đã kiểm; cần test thao tác và responsive toàn bộ bản tích hợp cuối. |
| Chức năng sáng tạo | 1 | 10 | JWT/voucher/WebSocket/media có nền thực; cần demo và giải thích, không dùng chúng để thay chức năng bắt buộc. |
| Báo cáo, trả lời câu hỏi | 0,5 | 5 | Tài liệu nguồn nhiều nhưng khảo sát/DOCX/hình và mapping cá nhân chưa xong; cần diễn tập. |
| **Tổng** | **10** | **100** | **Không thể dự đoán điểm chính xác hoặc xác nhận đạt tối đa từ audit này.** |

## 13. Thứ tự hoàn thiện đề xuất

1. **Chốt với thầy các điểm diễn giải:** COD có phải đi kèm một cổng online; nghĩa điều kiện Guest “bán trên 10”; chấp nhận TiDB; phạm vi Manager với danh mục và carrier. Ghi câu trả lời/ngày vào tài liệu phạm vi.
2. **Hoàn thiện phần thiếu chắc chắn theo phạm vi đã chốt:** triển khai một cổng online nếu bắt buộc, thay vì chỉ bật config. Kiểm cả lỗi/callback lặp/trạng thái và chính sách hoàn tiền liên quan.
3. **Chuẩn bị demo chạy được:** SMTP trên máy báo cáo; tài khoản đủ vai trò/scope; shop đã duyệt, policy, carrier/rate, sản phẩm/tùy chọn, voucher và địa chỉ hợp lệ. Dùng dataset thử nghiệm có nhãn, không reset DB cloud hoặc giả vờ có giao dịch thật.
4. **Làm sạch kiến trúc và quyền còn lệch:** đưa data access về Repository, bỏ phụ thuộc ngược; chỉnh hoặc ghi nhận thầy chấp nhận quyền Manager. Rà ràng buộc khi CHECK OFF.
5. **Hoàn thiện hồ sơ khảo sát và cá nhân:** tên/MSSV/UC/role/nhánh/commit/reviewer, khảo sát thật, cập nhật ERD/data dictionary và tài liệu trạng thái cũ.
6. **Bổ sung hình và DOCX:** kiến trúc tổng thể; activity từng UC; source có giải thích; UI/caption; setup; prompt thật. Ưu tiên catalog và order đang thiếu bộ hình theo UC.
7. **Nghiệm thu lại bản cuối:** test phù hợp thay đổi, native DB, OTP thật, UI mobile/tablet/desktop, checkout→giao→thu tiền→đánh giá/bình luận→trả/hoàn, quyền chéo shop và socket. Ghi riêng phần mock/H2/provider/sandbox.
8. **Chốt release và báo cáo:** người dùng quyết định nhánh/commit push cuối; clone lại để kiểm setup; xuất và xem DOCX; diễn tập từng người. Lượt audit này không tự push.

## 14. Checklist trước khi nói “đã hoàn chỉnh theo yêu cầu”

- [ ] Không còn gap bắt buộc chưa được thầy chấp nhận; phạm vi online payment/Manager/Guest/DB rõ ràng.
- [ ] Các role bắt buộc demo được đủ chức năng bằng đúng tài khoản/scope, Vendor vẫn mua như User.
- [ ] Có dữ liệu đủ để kiểm top 20/phân trang và điều kiện Guest; nguồn dữ liệu demo được ghi đúng.
- [ ] OTP kích hoạt và quên mật khẩu nhận được qua email thật trên máy báo cáo.
- [ ] COD và cổng trực tuyến được nghiệm thu đúng phạm vi đã chốt; không nhầm callback trình duyệt với xác nhận thanh toán đáng tin cậy.
- [ ] Trả/hoàn, giá/phí/chiết khấu/voucher/điểm và lịch sử trạng thái nhất quán.
- [ ] Validation ứng dụng và ràng buộc DB đích được kiểm, có kiểm quyền ngoài UI.
- [ ] Một layout/theme; giao diện bản cuối đã xem thật trên desktop/tablet/mobile.
- [ ] Tên/MSSV/phân công/commit/UC/report cá nhân đủ, minh chứng GitHub đúng bản đã kiểm.
- [ ] Khảo sát thực, phân tích/thiết kế và bộ hình diễn giải đầy đủ, không còn placeholder trong phần nộp.
- [ ] DOCX UTETra đã xuất, mở và rà nội dung/bố cục; setup được người khác làm lại; prompt có nguồn thật, không chứa secret.
- [ ] Mỗi người giải thích được phần mình làm và các ranh giới tích hợp liên quan.

**Trạng thái tại snapshot này: chưa đánh dấu hoàn tất toàn bộ checklist trên.**
