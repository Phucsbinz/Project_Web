# Tích hợp website thật — 20/10/2026

Cập nhật sau IDN-AI-004: đã có 8 tài khoản khởi tạo thủ công theo yêu cầu người dùng và kiểm tra đăng nhập thực 8/8; xem [ACCOUNTS_20261020](ACCOUNTS_20261020.md). Các ghi nhận DB chưa có tài khoản bên dưới là thời điểm trước khi người dùng cho phép bổ sung tài khoản.

Nhánh làm việc: `feature/identity-membership`. Đã fetch GitHub và merge `origin/feature/catalog-sales` (`812e26e`) cùng `origin/feature/order-payment` (`daf5979`); `origin/main` đã có trong lịch sử. Merge catalog: `745b309`; merge order: `2f5e3e2`. Nhánh integration không có nghiệp vụ/UI mới hơn hai feature nên không merge thêm lịch sử trùng. Không push. Stash `identity-before-ui-integration-20261020` được giữ làm bản dự phòng; identity đã được khôi phục đầy đủ và xung đột đã giải quyết.

## Thay đổi

- Layout/navigation Bootstrap chung cho khách, đăng nhập, hồ sơ và các vai trò; bỏ nội dung giới thiệu kỹ thuật trong footer, sửa UTF-8 của JSP fragment, footer theo chiều cao màn hình.
- Trang món gọi quote của catalog rồi thêm đúng variant/đường/đá/topping/số lượng vào giỏ thật. Đánh giá/bình luận công khai, nút yêu thích và lượt xem nối identity qua CSRF; nội dung khách hàng render bằng textContent.
- Đăng nhập dùng users/password_hash trong database và BCrypt; quyền/session kiểm lại từ DB. Không có tài khoản hoặc mật khẩu cố định trong luồng chạy thật.
- Một StoreAccessPolicy chung kiểm phiên hiện tại và owner/membership. Giữ CATALOG_ADMIN do catalog cung cấp, đồng bộ allowlist membership với identity. Metadata membership/audit dùng entity catalog hiện có, không tạo bảng thứ hai.
- OrderLoyaltyBridge chỉ gọi LoyaltyService của identity trong cùng transaction; flush trạng thái order trước ledger. Hoàn tiền cập nhật điều kiện payment trước REFUND/REVERSAL. Native identity update không clear persistence context của module gọi, tránh mất thay đổi order/settlement/return.
- Adapter thời gian order hỗ trợ OffsetDateTime mà schema JPA hợp nhất trả về. Test fixtures dùng schema thật đã hợp nhất, không dựng lại bảng catalog/order rút gọn của identity.
- Đã dừng demo H2 cổng 8087, gỡ banner demo, launcher và tài khoản demo trong runnable fixtures, OrderSmokeHarness, seed catalog H2, script browser demo và trang home nền không còn dùng. Giữ unit/integration fixtures trong test để kiểm thử isolated; chúng không nằm trong WAR. Ảnh/báo cáo QA cũ là lịch sử, không phải bằng chứng chạy TiDB.

## Database đã kiểm chứng

Chỉ kết nối `utetra_db`, TLS/datasource/credential kế thừa main. Trước tích hợp DB chỉ có V1 và không có user. Flyway hiện có sáu migration thành công: V1, V2, V3, V2026101701, V2026102001, V2026102002. JPA `ddl-auto=validate` khởi động thành công; không dùng update/create cho DB thật. V3 chỉ lấy bảng catalog sở hữu, V2 sở hữu commission/order, identity sở hữu membership/audit/loyalty/community; migration cuối nối các FK liên module. Không seed user, sản phẩm, doanh số hay đơn vào DB thật.

TiDB không hỗ trợ lệnh SET TRANSACTION READ ONLY trong môi trường này. Hikari đặt Connector/J `readOnlyPropagatesToServer=false`, giữ transaction readOnly tại tầng ứng dụng và không đổi biến server.

**Giới hạn CHECK:** cụm TiDB hiện báo `tidb_enable_check_constraint is off`. Các CHECK trong migration không được xem là đã bảo vệ dữ liệu trên cụm này; Bean Validation và kiểm tra service vẫn áp dụng. Không đổi GLOBAL vì có database project khác trên cùng cụm. Nếu chủ cụm bật CHECK sau này, cần migration bổ sung tạo/kiểm chứng lại CHECK trên `utetra_db`, không sửa checksum migration đã chạy. Tham khảo [TiDB system variables](https://docs.pingcap.com/tidb/stable/system-variables/#tidb_enable_check_constraint-new-in-v720).

## Kiểm chứng và giới hạn nghiệp vụ

- Toàn bộ 110 test: 0 failure, 0 error; gồm identity, catalog, order, security, JSP/Tomcat, socket, media và migration hợp nhất.
- Sau bổ sung FK: IntegratedMigrationTest chạy lại và package thành công. Sau sửa encoding: CatalogViewTest 3/3 và package thành công.
- Edge headless kiểm tra website thực với mysql: 13/13 check, desktop 1440 và mobile 390, không seed dữ liệu. Đã kiểm tra tiếng Việt, không tràn ngang, không lỗi JavaScript; parse bốn script bằng Chromium; tài khoản không tồn tại trả 401/không cấp cookie đăng nhập và thiếu CSRF trả 403. Bằng chứng hiện tại: [verification.json](assets/integration-20261020/verification.json), ảnh trong cùng thư mục.
- Database chưa có tài khoản/shop/sản phẩm; UI hiện trạng thái rỗng thật. Đăng nhập BCrypt/role/session đã kiểm thử trên database test isolated; chưa có credential người dùng thật để nghiệm thu đăng nhập TiDB.
- SMTP đang tắt; đã yêu cầu người dùng cấu hình SMTP thật để đăng ký/kích hoạt/quên mật khẩu. Không trả OTP vào UI/log để bỏ qua bước xác thực. Cần xác định tài khoản quản trị ban đầu sau khi có người dùng thật.
- VNPAY vẫn tắt theo quyết định của nhóm; checkout COD và hoàn COD dùng xác nhận thu/trả tiền thực. Cloudinary được cấu hình nhưng chưa upload nghiệm thu provider thật trong lượt tích hợp này.

## Chạy trên Windows

```powershell
python tools/run_verify.py
python tools/start_web.py
# http://localhost:8080
python tools/web_browser_check.py
python tools/database_inventory.py
python tools/stop_web.py
```

Launcher chạy WAR production bằng JDK21, profile mysql, bind localhost và ghi PID vào `.local/web-process.json`. Stopper xác minh command line đúng WAR workspace trước khi dừng. Không khởi chạy main class test, không bật test profile. Config giữ trong properties theo yêu cầu chung; không ghi credential vào tài liệu hoặc báo cáo.
