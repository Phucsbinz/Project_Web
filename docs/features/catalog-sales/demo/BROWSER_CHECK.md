# Kiểm tra UI catalog trên H2 disposable

Điều kiện: JDK21, Maven, Windows/Edge và Python có module `websocket` (package `websocket-client`). Script chỉ dùng test JWT với key test đã có trong repository, không thêm endpoint đăng nhập hoặc tài khoản thật. Fixture chính sách/Manager chỉ để mô phỏng prerequisite trong H2; không triển khai chức năng của nhánh khác.

Sau `mvn verify` thành công, chạy server từ thư mục gốc repo:

```powershell
mvn -B -ntp spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=test' '-Dspring-boot.run.arguments=--spring.config.additional-location=file:./src/test/resources/ --server.port=8087 --server.address=127.0.0.1 --spring.jpa.defer-datasource-initialization=true --spring.sql.init.mode=always --spring.sql.init.data-locations=file:docs/features/catalog-sales/demo/catalog-browser-h2.sql'
```

Mở PowerShell thứ hai, cùng thư mục gốc:

```powershell
python docs/features/catalog-sales/demo/browser_check.py
```

Script mở Edge headless với profile riêng trong tmp, kiểm tra rồi đóng browser. JSON/ảnh tại `assets/update-20261024`. Trong fixture có chủ shop riêng, Admin và Manager được gán shop; script từ chối hồ sơ fixture để kiểm phản hồi và escape XSS. Chạy lại phải khởi động lại server để tái tạo H2 mới. Dừng server bằng Ctrl+C sau kiểm tra. Không dùng fixture này để seed MySQL/TiDB.

Kết quả ngày 24/10/2026: 7 checks đạt, không exception JavaScript. Các nhãn form đăng ký shop đã được kiểm đúng tiếng Việt sau khi thêm pageEncoding cho JSP fragment. Đây là kiểm UI/catalog, không phải nghiệm thu đăng nhập thật, checkout, email, Cloudinary hoặc cloud.
