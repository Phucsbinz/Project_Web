# UTETra — Đối chiếu hướng dẫn của thầy với cấu hình thực

## 1. Tài liệu đã đọc và phạm vi sử dụng

Đã đọc hướng dẫn tổng hợp trong TAI_LIEU_CAU_HINH_PROJECT_CUOI_KI, trích text 17 PDF và đối chiếu các phần liên quan. Một số slide/code nằm trong ảnh nên text extraction không thể thay đọc code nguồn trong guide hoặc kiểm build thật. Không tuyên bố đã kiểm trực quan mọi trang/hình PDF.

| Nguồn | Phần đối chiếu | Áp dụng/điều chỉnh UTETra |
| --- | --- | --- |
| HUONG_DAN_CAU_HINH_TONG_HOP.md mục2–7 | Boot3.5.13/Java21/WAR, JSP/JSTL/SiteMesh/Bootstrap/JPA/MySQL | pom, UtetraApplication, properties, layout/filter, Flyway |
| 03_springsecurity6.pdf | Authentication/Authorization/Provider/FilterChain và BCrypt | SecurityConfig, AuthenticationConfig, role/CSRF, request trước Service |
| 04_JWT.pdf + guide mục8 | Claims/chữ ký HS256/exp/Bearer/filter; slide dùng JJWT khác ví dụ Nimbus mới | Chọn Nimbus10.10 trong guide, cookie JSP + header API, session current/revoke; không chép cả 2 thư viện JWT |
| 06_MVC_3tier.pdf | Presentation MVC, Business Logic, Data Access | Controller → Service → Repository/JPA, View JSP riêng |
| 01_JAVASERVLET.pdf và hướng dẫn AJAX/CRUD API Boot3 | JSP/taglib/forward/REST DTO/filter | Jakarta JSTL, c:out/c:url, controller vs rest, /api excluded SiteMesh |
| vd1.pdf/vd3.pdf + guide mục10 | Cloudinary, owner, validation, URL/publicId và transaction | HTTP5 2.4.0 theo guide, adapter image5MB, cleanup contract after commit; không dùng SQL Server/Thymeleaf từ mẫu |
| guide mục9 + Spring official | STOMP bổ sung; ví dụ project nguồn là Socket.IO | Native /ws cùng origin, Spring Security message, CSRF CONNECT, own/shop topics; không thêm Node/Mongo |
| GraphQL/Thymeleaf/Swagger/Mapper Boot4 | Tài liệu bổ trợ có phiên bản/stack khác | Không đưa framework/version khác vào stack JSP/Boot3; DTO mapping có thể học ý tưởng |
| KhaoSat_PhanTichHeThong.docx | Survey/UC/sequence/ERD/dictionary | Chỉ bố cục tham khảo, không copy nhận xét cũ/ObjectId/schema marketplace đầy đủ |

## 2. File cấu hình/mã thật

| Công cụ | File cần đọc | Cách xác minh |
| --- | --- | --- |
| Maven/Boot/WAR | pom.xml, .mvn/wrapper/maven-wrapper.properties, UtetraApplication | mvnw verify, target/utetra.war |
| JSP/JSTL/Decorator | application.properties, SiteMeshConfig, WEB-INF/views/layouts và publicweb/home.jsp | HTTP home, một navbar, title/body ghép đúng, tag JSTL rendered |
| Bootstrap/template | assets/vendor/bootstrap, docs/template reference + LICENSE, utetra.css | Assets local, một bộ5.3.3, responsive cần kiểm ảnh thêm |
| JPA/MySQL/TiDB | application-mysql.properties, V1 migration, Role/User/AuthSession/repos | TLS connection, Flyway V1, Hibernate validate trong utetra_db |
| Security/JWT | SecurityConfig, AuthenticationConfig, JwtService/filter, SessionIdentityService | Test signature/expiry/issuer/audience/role/CSRF/revoked; không đăng ký filter servlet lần2 |
| STOMP | WebSocketConfig/WebSocketSecurityConfig, StoreAccessPolicy | Broker startup, guest handshake401; authorized subscriptions/business events chờ feature |
| Cloudinary | CloudinaryConfig, MediaService/adapters | Conditional disabled khi chưa có credential, validation ảnh; upload thật chưa kiểm |
| Email/OTP/VNPAY | app.mail/app.otp/payment.vnpay properties | Có config slots; chưa SMTP/merchant hoặc nghiệp vụ, không báo success giả |

## 3. Nguồn chính thức bổ sung

- [Spring Boot JSP limitations](https://docs.spring.io/spring-boot/3.3/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations): WAR/JSP.
- [SiteMesh config](https://github.com/sitemesh/sitemesh3/blob/master/CONFIGURATION.md): filter và decorator.
- [Spring WebSocket Security](https://docs.spring.io/spring-security/reference/6.5/servlet/integrations/websocket.html): message authorization/CSRF.
- [TiDB Java JDBC](https://docs.pingcap.com/developer/dev-guide-sample-application-java-jdbc/): MySQL-compatible/TLS identity.
- [Shop Homepage template](https://github.com/StartBootstrap/startbootstrap-shop-homepage): source và MIT license.
- [Cloudinary Java SDK](https://cloudinary.com/documentation/java_integration): server-side upload/publicId.
- [VNPAY pay/IPN](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html): callback server xác nhận, không tin ReturnUrl.

Cấu hình trực tiếp properties là lựa chọn mới của người dùng, khác mẫu guide dùng .env. MySQL/TiDB thay SQL Server của PDF. Cookie JWT + session DB + CSRF là lựa chọn project khác mẫu JSP session/Bearer API hai-chain của guide; đã ghi rõ và phải kiểm chức năng auth/logout/revoke khi feature làm.
