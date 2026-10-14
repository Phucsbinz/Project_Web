# Hướng dẫn cấu hình project cuối kỳ từ các bài tập trong D:\java

Ngày tổng hợp: 13/10/2026. Cơ sở dữ liệu sử dụng: **MySQL**, theo lựa chọn của bạn.

Tài liệu này tổng hợp cấu hình đã đọc trong các project bài tập, chia theo Spring Boot, JSP/JSTL, SiteMesh, Bootstrap, JPA, MySQL, JWT, WebSocket và Cloudinary. Những ví dụ điều chỉnh để ghép vào project cuối kỳ được ghi là **mẫu tích hợp**; phần WebSocket Spring là **bổ sung từ tài liệu chính thức**, vì mã nguồn realtime tìm thấy tại đây dùng Node.js/Socket.IO.

Đây là hướng dẫn cấu hình, không phải một project cuối kỳ đã được build và chạy thử. Phiên bản ghi ở bảng dưới là phiên bản trong `pom.xml` hiện có, không có nghĩa mọi project dùng chung một bộ thư viện.

## Mục lục

1. [Project nguồn và cách chọn nền](#1-project-nguồn-và-cách-chọn-nền)
2. [Spring Boot và Maven](#2-spring-boot-và-maven)
3. [JSP và JSTL](#3-jsp-và-jstl)
4. [Decorator SiteMesh](#4-decorator-sitemesh)
5. [Bootstrap](#5-bootstrap)
6. [JPA và Hibernate](#6-jpa-và-hibernate)
7. [MySQL](#7-mysql)
8. [JWT và Spring Security](#8-jwt-và-spring-security)
9. [WebSocket](#9-websocket)
10. [Cloudinary](#10-cloudinary)
11. [REST API, AJAX và DTO](#11-rest-api-ajax-và-dto)
12. [Ghép các phần vào project cuối kỳ](#12-ghép-các-phần-vào-project-cuối-kỳ)
13. [Lỗi thường gặp](#13-lỗi-thường-gặp)
14. [Danh mục PDF và kết quả rà soát](#14-danh-mục-pdf-và-kết-quả-rà-soát)

## 1. Project nguồn và cách chọn nền

Các đường dẫn project bên dưới tính từ `D:\java`. Các PDF nằm **trực tiếp cạnh file Markdown này**, không chia thư mục con.

| Project nguồn | Phiên bản đọc từ project | Phần nên tham khảo |
| --- | --- | --- |
| `24133045_BuiThanhPhuc_BT05` | Boot 3.3.5, Java 17, WAR | Nền gần yêu cầu nhất: JSP/JSTL, SiteMesh 3.2.2, Bootstrap 3.4.1, JPA, MySQL; SQL Server là cấu hình được comment |
| `24133045_BuiThanhPhuc_BTVN_API` | Boot 3.5.13, Java 21, WAR | JSP, REST API, AJAX, JPA, multipart; springdoc 2.8.17 |
| `TUAN10/24133045_BuiThanhPhuc_JWT_Nimbus` | Boot 4.1.1, Java 26, JAR | Nimbus 10.10, Bearer JWT, BCrypt, filter, lỗi API, MySQL; giao diện hiện có dùng Thymeleaf |
| `BTAP09/24133045_BuiThanhPhuc_Security_VD12` | Boot 4.1.1, Java 26, JAR | Form login, role, nhiều SecurityFilterChain, Cloudinary HTTP5 2.4.0, transaction khi thay/xóa ảnh |
| `24133045_BuiThanhPhuc_MapperDTO_VD12` và `..._VD3` | Boot 4.0.8, Java 21, JAR | DTO, mapper, quan hệ entity, JPA/MySQL; giao diện Thymeleaf |
| `24133045_BuiThanhPhuc_Thymeleaf` | Boot 3.3.13, Java 21, JAR | JPA/MySQL, cấu hình `validate`, H2 cho test, cách tách giao diện; tài liệu bổ trợ |
| `BTAPTUAN7/24133045_BuiThanhPhuc_GraphQL` | Boot 3.5.13, Java 21, JAR | JPA/MySQL, schema khởi tạo và GraphQL; phần đọc thêm |
| `24133045_01` | Servlet, Java 17, WAR | JSP/JSTL, SiteMesh, JPA thuần, MySQL, tách cấu hình ngoài WAR |
| `BaiTap02`, `24133045_BuiThanhPhuc_BTap03`, `Bai2_CRUD_Category` | Servlet/Jakarta, WAR | `persistence.xml`, EntityManager, CRUD, JSP/JSTL; không có cơ chế tự cấu hình Spring Boot |
| `Bai1_Login_Cookie_Session`, các bản trong `24133045_BuiThanhPhuc`, `BANHANG`, `BTJAVA` | Servlet/Jakarta, WAR | JDBC, session/cookie, MVC ba tầng, Bootstrap/JSP |
| `booking-travel-api` | Node.js | JWT + Socket.IO thực tế; không phải Spring WebSocket |

**Cách dùng cho bài cuối kỳ:** tham khảo layout và view của BT05; tham khảo cấu trúc WAR Boot 3.5.13/Java 21 của BTVN_API nếu tạo project mới theo bộ ví dụ dưới. Lấy logic JWT và Cloudinary từ TUAN10/BTAP09 nhưng điều chỉnh theo phiên bản nền. Không chép toàn bộ các `pom.xml` vào nhau, không đưa một `@SpringBootApplication` từ project khác vào cùng ứng dụng.

Các file nguồn chính:

- [BT05/pom.xml](../24133045_BuiThanhPhuc_BT05/pom.xml)
- [BT05/application.properties](../24133045_BuiThanhPhuc_BT05/src/main/resources/application.properties)
- [BT05/Application.java](../24133045_BuiThanhPhuc_BT05/src/main/java/vn/iotstar/Application.java)
- [BT05/CustomSiteMeshFilter.java](../24133045_BuiThanhPhuc_BT05/src/main/java/vn/iotstar/config/CustomSiteMeshFilter.java)
- [BTVN_API/pom.xml](../24133045_BuiThanhPhuc_BTVN_API/pom.xml)
- [JWT/JwtService.java](../TUAN10/24133045_BuiThanhPhuc_JWT_Nimbus/src/main/java/vn/iotstar/services/JwtService.java)
- [JWT/SecurityConfiguration.java](../TUAN10/24133045_BuiThanhPhuc_JWT_Nimbus/src/main/java/vn/iotstar/configs/SecurityConfiguration.java)
- [BTAP09/CloudinaryConfig.java](../BTAP09/24133045_BuiThanhPhuc_Security_VD12/src/main/java/vn/iotstar/config/CloudinaryConfig.java)
- [BTAP09/CloudinaryServiceImpl.java](../BTAP09/24133045_BuiThanhPhuc_Security_VD12/src/main/java/vn/iotstar/service/impl/CloudinaryServiceImpl.java)
- [BTAP09/ProductServiceImpl.java](../BTAP09/24133045_BuiThanhPhuc_Security_VD12/src/main/java/vn/iotstar/service/impl/ProductServiceImpl.java)

## 2. Spring Boot và Maven

### 2.1. Cấu trúc project dùng JSP

```text
project-cuoi-ki/
  pom.xml
  .env                     # cấu hình riêng của máy, không commit
  .env.example             # tên biến và giá trị mẫu
  src/main/java/vn/iotstar/
    Application.java
    config/
    controller/
    entity/
    repository/
    service/
    dto/
    filter/
  src/main/resources/
    application.properties
    application-mysql.properties
    static/css/
    static/js/
    static/images/
  src/main/webapp/
    WEB-INF/views/
    WEB-INF/decorators/web.jsp
    WEB-INF/decorators/admin.jsp
```

Đặt `Application` ở package cha của controller, entity, repository và service để Spring quét được các thành phần. Ví dụ `vn.iotstar.Application` bao trùm `vn.iotstar.*`.

### 2.2. Maven mẫu tích hợp

Mẫu này chọn **Boot 3.5.13 + Java 21 + WAR**, dựa trên BTVN_API; bổ sung SiteMesh từ BT05, JWT từ TUAN10 và Cloudinary từ BTAP09. Các số phiên bản là mốc để đối chiếu bài tập, không phải khẳng định phiên bản mới nhất.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.13</version>
    <relativePath/>
  </parent>
  <groupId>vn.iotstar</groupId>
  <artifactId>project-cuoi-ki</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>war</packaging>
  <properties>
    <java.version>21</java.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.tomcat.embed</groupId>
      <artifactId>tomcat-embed-jasper</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>jakarta.servlet.jsp.jstl</groupId>
      <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
    </dependency>
    <dependency>
      <groupId>org.glassfish.web</groupId>
      <artifactId>jakarta.servlet.jsp.jstl</artifactId>
    </dependency>
    <dependency>
      <groupId>org.sitemesh</groupId>
      <artifactId>sitemesh</artifactId>
      <version>3.2.2</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.nimbusds</groupId>
      <artifactId>nimbus-jose-jwt</artifactId>
      <version>10.10</version>
    </dependency>
    <dependency>
      <groupId>com.cloudinary</groupId>
      <artifactId>cloudinary-http5</artifactId>
      <version>2.4.0</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Giữ Spring, Hibernate, Tomcat và JDBC driver theo dependency management của Boot. Khi tạo lại BT05 nguyên bản, dùng POM riêng của BT05; mẫu trên là phương án ghép mới, không phải bản sao nguyên xi.

### 2.3. Main class và WAR

```java
package vn.iotstar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

WAR dùng JSP có thể chạy bằng `java -jar` hoặc deploy vào container phù hợp. JSP không được hỗ trợ với executable JAR; vì vậy `src/main/webapp` và `packaging=war` phải đi cùng nhau. Xem [giới hạn JSP của Spring Boot](https://docs.spring.io/spring-boot/3.3/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations).

Với nền Boot 3.x này, chọn Tomcat 10.1 khi deploy ngoài. Không đổi sang Tomcat 11 chỉ vì máy đang có sẵn; phải đối chiếu Servlet/JSP và phiên bản nền. BT05 đang khai báo riêng JSP API 4.0.0 trong khi nền Boot 3.3.x dùng Tomcat 10.1; khi ghép mới hãy bỏ override đó và để nền quản lý API/container.

### 2.4. Cấu hình chung

`src/main/resources/application.properties`:

```properties
spring.application.name=project-cuoi-ki
server.port=${SERVER_PORT:8080}
spring.profiles.active=${SPRING_PROFILES_ACTIVE:mysql}
spring.config.import=optional:file:.env[.properties]

spring.mvc.view.prefix=/WEB-INF/views/
spring.mvc.view.suffix=.jsp
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

security.jwt.secret-key=${JWT_SECRET}
security.jwt.expiration-time=${JWT_EXPIRATION:3600000}
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}
```

Trong mẫu này, static resource dùng URL mặc định `/css/...`, `/js/...`, `/images/...`. BT05 dùng `spring.mvc.static-path-pattern=/static/**`; nếu giữ thiết lập đó thì URL phải có `/static/` và các filter cũng phải loại trừ `/static/*`.

## 3. JSP và JSTL

**Nguồn:** BT05, BTVN_API, BaiTap02, BTJAVA. **PDF:** JavaServlet, MVC ba tầng và hướng dẫn AJAX/CRUD API.

### 3.1. View resolver

Với prefix/suffix ở mục 2, controller trả `"admin/categories/list"` sẽ mở:

```text
src/main/webapp/WEB-INF/views/admin/categories/list.jsp
```

Ví dụ độc lập để kiểm tra JSP:

```java
package vn.iotstar.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("names", List.of("Spring Boot", "JSP", "MySQL"));
        return "home";
    }
}
```

`src/main/webapp/WEB-INF/views/home.jsp`:

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head><title>Trang chủ</title></head>
<body>
  <h1>Công cụ của project</h1>
  <ul>
    <c:forEach items="${names}" var="name">
      <li><c:out value="${name}"/></li>
    </c:forEach>
  </ul>
  <a href="<c:url value='/admin/categories'/>">Quản lý danh mục</a>
</body>
</html>
```

### 3.2. Quy tắc khi chuyển từ bài Servlet

- Dùng `jakarta.servlet.*`, `jakarta.persistence.*` và JSTL Jakarta; không trộn dependency Servlet/JSTL `javax` cũ.
- JSP cần cả JSTL API và implementation. Chỉ thêm API có thể gây lỗi không tìm thấy taglib.
- URL nội bộ dùng `<c:url>` hoặc context path để WAR chạy được ở `/project-cuoi-ki`.
- `@Controller` trả tên view; `@RestController` trả JSON. Trả tên JSP từ `@RestController` sẽ hiển thị chuỗi thay vì render trang.
- Đặt JSP dưới `WEB-INF` và đi qua controller. Không truy cập trực tiếp `/WEB-INF/views/home.jsp` từ trình duyệt.
- `c:out` phù hợp để hiển thị dữ liệu người dùng; tránh đưa nội dung nhập vào trực tiếp bằng `innerHTML`.
- Khi thêm Spring Security cho JSP, cho phép dispatcher `FORWARD` để bước forward tới JSP không bị chặn.

Form multipart:

```jsp
<form method="post" action="<c:url value='/admin/products/save'/>"
      enctype="multipart/form-data">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
  <input name="name" required>
  <input type="file" name="image" accept="image/jpeg,image/png">
  <button type="submit">Lưu</button>
</form>
```

Đoạn CSRF dành cho form login/session có bật CSRF. API Bearer JWT ở mục 8 có chính sách riêng.

## 4. Decorator SiteMesh

**Nguồn thực tế:** BT05 dùng `CustomSiteMeshFilter` và đăng ký bằng `FilterRegistrationBean` trong `Application.java`; `24133045_01` dùng filter trong `web.xml`; BaiTap02 dùng `WEB-INF/sitemesh3.xml`.

SiteMesh ghép phần `title`, `head`, `body` của trang con vào layout. Nó không xử lý quyền admin: `/admin/*` vẫn cần Spring Security kiểm tra quyền.

### 4.1. Filter mẫu tích hợp

`src/main/java/vn/iotstar/config/CustomSiteMeshFilter.java`:

```java
package vn.iotstar.config;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;

public class CustomSiteMeshFilter extends ConfigurableSiteMeshFilter {
    @Override
    protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
        builder.addDecoratorPath("/*", "/WEB-INF/decorators/web.jsp")
            .addDecoratorPath("/admin/*", "/WEB-INF/decorators/admin.jsp")
            .addExcludedPath("/api/*")
            .addExcludedPath("/auth/*")
            .addExcludedPath("/users/*")
            .addExcludedPath("/users")
            .addExcludedPath("/ws/*")
            .addExcludedPath("/ws")
            .addExcludedPath("/css/*")
            .addExcludedPath("/js/*")
            .addExcludedPath("/images/*")
            .addExcludedPath("/image")
            .addExcludedPath("/error");
    }
}
```

`src/main/java/vn/iotstar/config/SiteMeshConfig.java` **là file mới của mẫu**, không phải tên file có sẵn trong BT05:

```java
package vn.iotstar.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteMeshConfig {
    @Bean
    public FilterRegistrationBean<CustomSiteMeshFilter> siteMeshFilter() {
        var registration = new FilterRegistrationBean<CustomSiteMeshFilter>();
        registration.setFilter(new CustomSiteMeshFilter());
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(1);
        return registration;
    }
}
```

BT05 gốc map tới `/web.jsp` và `/admin.jsp`, đồng thời có các file ở webapp root và `WEB-INF/decorators`. Mẫu trên chọn duy nhất các file trong `WEB-INF/decorators` để không nhầm bản layout đang được dùng. Khi sao chép layout hãy chỉnh mapping và vị trí file đồng thời.

### 4.2. Layout

`WEB-INF/decorators/web.jsp`:

```jsp
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title><sitemesh:write property="title"/></title>
  <link rel="stylesheet"
        href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
  <sitemesh:write property="head"/>
</head>
<body>
  <header class="container py-3">
    <a href="<c:url value='/'/>">Project cuối kỳ</a>
  </header>
  <main class="container"><sitemesh:write property="body"/></main>
  <footer class="container py-3">Lập trình Web</footer>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

`admin.jsp` có cùng các điểm chèn, thêm menu quản trị. Với SiteMesh 3, `sitemesh:write` do bộ phân tích SiteMesh xử lý; không tự thêm URI taglib SiteMesh cũ nếu chưa kiểm tra cơ chế đó.

### 4.3. Kiểm tra

Mở trang chủ và trang admin: mỗi trang có đúng một header/footer, title thay đổi theo trang con. Gọi API và tải CSS/JS: trả JSON/file nguyên vẹn. WebSocket phải được loại trừ. Chỉ đăng ký filter một lần, không vừa khai báo `web.xml` vừa tạo bean cho cùng filter trong Boot.

Có thể đối chiếu [cấu hình SiteMesh chính thức](https://github.com/sitemesh/sitemesh3/blob/master/CONFIGURATION.md); cú pháp starter của phiên bản khác không được thay thế trực tiếp cho cách đăng ký filter thủ công trong bài tập.

## 5. Bootstrap

**Nguồn:** BT05 layout dùng Bootstrap **3.4.1** và jQuery **3.7.1**; giao diện JWT TUAN10 dùng Bootstrap **5.3.3**. Đây là hai hệ class và plugin khác nhau.

| Khi giữ layout BT05 | Khi viết layout mới bằng Bootstrap 5 |
| --- | --- |
| CSS/JS 3.4.1 và jQuery tải trước Bootstrap JS | CSS 5.3.3 và `bootstrap.bundle.min.js` cùng phiên bản |
| `data-toggle`, `data-target` | `data-bs-toggle`, `data-bs-target` |
| `pull-right`, `btn-default`, `panel` | `float-end`, `btn-secondary`, `card` |
| Plugin JS cần jQuery | Bootstrap không cần jQuery; AJAX bài tập vẫn có thể dùng jQuery riêng |

Mẫu layout ở mục 4 dùng Bootstrap 5.3.3 giống mốc của bài JWT. Nếu chép nguyên layout BT05, giữ Bootstrap 3 trước rồi chuyển class có chủ đích. Không nạp cả Bootstrap 3 và 5 vào cùng một trang.

Đặt CSS/JS chung ở decorator. File riêng của trang có thể đưa trong `<head>` của trang con để SiteMesh chèn qua `property="head"`. Với máy học offline, lưu bản CSS/JS tại `src/main/resources/static` và dùng URL có context path.

Tài liệu để đối chiếu component và cách nạp script: [Bootstrap 5.3](https://getbootstrap.com/docs/5.3/getting-started/introduction/).

## 6. JPA và Hibernate

### 6.1. Phân biệt hai kiểu cấu hình trong bài tập

| Servlet/JPA thuần: BaiTap02, 24133045_01 | Spring Boot: BT05, BTVN_API, TUAN10, BTAP09 |
| --- | --- |
| `src/main/resources/META-INF/persistence.xml` | `application.properties` + `spring.datasource.*` + `spring.jpa.*` |
| Tự tạo `EntityManagerFactory`, `EntityManager` | Spring quản lý EntityManager và transaction |
| DAO tự `begin/commit/rollback` | `JpaRepository` và service `@Transactional` |
| `hibernate.hbm2ddl.auto` | `spring.jpa.hibernate.ddl-auto` |

Project cuối kỳ dùng Boot: chọn nhánh Boot, không chép `JpaConfig` tạo factory thủ công để chạy song song với datasource của Spring.

### 6.2. Entity và repository mẫu

Mẫu dùng tên thuộc tính tương ứng ý tưởng CRUD danh mục trong BT05, với accessor viết tay để không cần thêm Lombok:

```java
package vn.iotstar.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "categoryname", nullable = false, length = 255)
    private String categoryname;

    protected Category() {}
    public Category(String categoryname) { this.categoryname = categoryname; }
    public Integer getCategoryId() { return categoryId; }
    public String getCategoryname() { return categoryname; }
    public void setCategoryname(String value) { categoryname = value; }
}
```

```java
package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.iotstar.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Page<Category> findByCategorynameContaining(String name, Pageable pageable);
}
```

Tên `findByCategorynameContaining` phải khớp property Java `categoryname`, không phải tên cột tùy ý. ID generic của repository phải khớp entity: `Integer` khác `Long`.

Service đặt `@Transactional` trên thao tác thay đổi nhiều bảng; `@Transactional(readOnly=true)` trên đọc và map DTO có quan hệ lazy. Với `open-in-view=false`, hãy map dữ liệu cần cho JSP/JSON trong service hoặc truy vấn fetch phù hợp trước khi transaction đóng.

BT05 đã có tìm kiếm + phân trang: UI dùng trang bắt đầu từ 1, `PageRequest.of` bắt đầu từ 0. Khi viết lại cần chặn `page<1`, giới hạn `size`, tránh truyền `currentPage-1` âm.

### 6.3. Quan hệ và kiểu dữ liệu

- Nhiều sản phẩm thuộc một danh mục: `@ManyToOne` + `@JoinColumn`; phía danh mục `@OneToMany(mappedBy="category")`, tên `mappedBy` khớp field phía sản phẩm.
- Tiền dùng `BigDecimal` và cột DECIMAL; không dùng số thực cho phép tính tiền.
- MySQL dùng VARCHAR/TEXT + `utf8mb4`. Những `columnDefinition="NVARCHAR(MAX)"` trong mẫu SQL Server cần chuyển khi dùng MySQL.
- Không trả entity chứa `password` và quan hệ vòng trực tiếp ra API. Dùng DTO; mục 11 chỉ cách lấy từ project MapperDTO.
- `javax.imageio.ImageIO` trong Cloudinary vẫn là API Java SE hợp lệ. Không đổi mọi package `javax.*` một cách máy móc sang `jakarta.*`.

### 6.4. DDL

BT05/BTVN_API có `update`; TUAN10 và BTAP09 ưu tiên `validate`. Với database mới để học có thể dùng `update` tạm thời; khi có schema ổn định chọn `validate`. `validate` kiểm tra schema và không tạo database/bảng. `create`/`create-drop` chỉ dùng cho dữ liệu test có thể bỏ đi.

Các script SQL cần dùng đúng project: JWT có `database/schema.sql`; BaiTap02 có SQL của `servletjpa`; `24133045_01` có schema bookstore. Không nhập tất cả script vào một database vì các bảng `users`, `categories`, `products` có thể khác cột và kiểu ID.

## 7. MySQL

**Phần cấu hình chính theo yêu cầu của bạn.** Tài liệu PDF có chỗ trình bày SQL Server; hãy lấy logic Java rồi thay datasource và kiểu dữ liệu theo phần này. Không cần thêm SQL Server/PostgreSQL driver vào project cuối kỳ dùng MySQL.

### 7.1. Tạo database

Chạy trong MySQL Workbench hoặc MySQL CLI với tài khoản có quyền tạo database/user:

```sql
CREATE DATABASE project_cuoi_ki
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'project_user'@'localhost' IDENTIFIED BY 'THAY_MAT_KHAU_CUA_BAN';
GRANT ALL PRIVILEGES ON project_cuoi_ki.* TO 'project_user'@'localhost';
```

Đây là tài khoản phục vụ học và tự tạo schema trong database của project. Nếu user đã tồn tại thì dùng user đó hoặc đổi mật khẩu qua `ALTER USER`, không chạy lại `CREATE USER` nguyên xi. Khi schema ổn định có thể tách quyền tạo bảng khỏi tài khoản chạy ứng dụng.

### 7.2. Profile MySQL

`src/main/resources/application-mysql.properties`:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/project_cuoi_ki?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8}
spring.datasource.username=${DB_USERNAME:project_user}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}
```

URL này dành cho MySQL cục bộ khi học. Nếu dùng database từ xa, cấu hình TLS theo server; không tự mang `useSSL=false` từ cấu hình localhost lên môi trường đó. Hibernate thường tự chọn dialect theo JDBC; nếu cần khai báo thì dùng `org.hibernate.dialect.MySQLDialect`.

`.env` ở thư mục chứa `pom.xml`:

```properties
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=mysql
DB_URL=jdbc:mysql://localhost:3306/project_cuoi_ki?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
DB_USERNAME=project_user
DB_PASSWORD=THAY_MAT_KHAU_CUA_BAN
DDL_AUTO=update
JWT_SECRET=THAY_BANG_CHUOI_BASE64_NGAU_NHIEN
JWT_EXPIRATION=3600000
CLOUDINARY_CLOUD_NAME=THAY_CLOUD_NAME
CLOUDINARY_API_KEY=THAY_API_KEY
CLOUDINARY_API_SECRET=THAY_API_SECRET
```

Đặt `DDL_AUTO=update` cho lần phát triển đầu tiên nếu chưa có schema. Sau khi kiểm tra bảng và có script tái tạo, đổi `validate`. `.env` ở đây được Spring import như **Java properties**, không dùng `export DB_PASSWORD=...`, không thêm dấu ngoặc kép theo cú pháp shell. Nếu chạy IDE, working directory phải là thư mục project để tìm được `.env`.

`.gitignore` tối thiểu:

```gitignore
.env
.env.*
!.env.example
target/
uploads/
```

### 7.3. Kiểm tra kết nối

```powershell
mysql -h localhost -P 3306 -u project_user -p project_cuoi_ki
```

Trong MySQL:

```sql
SELECT DATABASE(), CURRENT_USER(), VERSION();
SHOW TABLES;
```

Tên database đang dùng trong bài tập:

| Project | Database/port mặc định đọc được |
| --- | --- |
| BT05 | `webst2`, port web 8090 |
| BTVN_API | `btvn_api`, port web 8101 |
| Thymeleaf | `thymeleaf_category`, port web 8088 |
| JWT TUAN10 | `tuan10_jwt_nimbus` trong schema, port web 8005 |
| BTAP09 | `btap09_vd12`, port web 8091 |
| GraphQL | `btaptuan7_graphql`, port web 8107 |
| BaiTap02 | `servletjpa` |
| 24133045_01 | `bookstore_24133045_01` |

### 7.4. Nếu cần đọc lại project JPA thuần

`META-INF/persistence.xml` khai báo `jakarta.persistence.jdbc.driver/url/user/password`; dấu `&` trong URL XML phải viết `&amp;`. Ví dụ:

```xml
<property name="jakarta.persistence.jdbc.driver" value="com.mysql.cj.jdbc.Driver"/>
<property name="jakarta.persistence.jdbc.url"
          value="jdbc:mysql://localhost:3306/project_cuoi_ki?serverTimezone=UTC&amp;characterEncoding=UTF-8"/>
<property name="jakarta.persistence.jdbc.user" value="project_user"/>
<property name="jakarta.persistence.jdbc.password" value="THAY_MAT_KHAU_CUA_BAN"/>
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

Đoạn này chỉ giải thích cấu hình bài Servlet; không cần dùng nó khi đã cấu hình datasource Boot ở trên.

## 8. JWT và Spring Security

**Nguồn:** `TUAN10/04_JWT.pdf` và project Nimbus. **Nguồn bổ trợ:** `BTAP09/03_springsecurity6.pdf`, các VD1/2/3 và `SecurityConfig.java` của BTAP09.

### 8.1. Những gì project JWT đang làm

Luồng thực tế: `/auth/signup` tạo user với BCrypt; `/auth/login` xác thực email/password và trả `{token, expiresIn}`; client gửi `Authorization: Bearer ...` tới `/users/me`, `/users`.

`JwtService` dùng Nimbus HS256, secret được decode Base64 và phải dài ít nhất 32 byte. Khi đọc token, mã nguồn kiểm tra thuật toán, chữ ký, hạn dùng, `notBefore` nếu có và subject. `JWT_EXPIRATION=3600000` tính bằng **milliseconds**, tức 1 giờ.

`JwtAuthenticationFilter` đọc token, tải `UserDetails` từ DB, kiểm tra enabled/locked, đặt `Authentication` vào `SecurityContextHolder`. Sai token được chuyển tới `HandlerExceptionResolver`. `FilterRegistrationBean` của filter JWT bị disable để filter chỉ chạy trong SecurityFilterChain, tránh chạy hai lần.

### 8.2. Các file cần mang logic sang

| File/package từ JWT TUAN10 | Công việc khi ghép |
| --- | --- |
| `services/JwtService.java` | Có thể giữ logic Nimbus và hai property `security.jwt.*` |
| `filter/JwtAuthenticationFilter.java` | Đổi package/import theo project; giữ kiểm tra tài khoản và resolver |
| `configs/ApplicationConfiguration.java` | Đăng ký UserDetailsService, PasswordEncoder, provider, AuthenticationManager |
| `services/AuthenticationService.java` | Xác thực và đăng ký user; map lại DTO/entity của bài cuối kỳ |
| `controllers/AuthenticationController.java` | Quyết định giữ `/auth/*` hoặc đổi `/api/auth/*` |
| `controllers/UserController.java` | Bảo vệ endpoint hồ sơ/danh sách theo role của bài cuối kỳ |
| `controllers/GlobalExceptionHandler.java` | Giữ cấu trúc lỗi, tránh token sai dẫn tới 500 |
| `entity/User.java`, `repository/UserRepository.java` | Hợp nhất vào một User entity/repository của project |
| `models/*` | DTO login/register/response; không trả password hash ra client |

Không mang `Application.java` và các trang Thymeleaf của TUAN10 vào ứng dụng JSP nếu không có nhu cầu dùng Thymeleaf.

Sinh secret trong PowerShell:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKeyBytes)
$jwtRng.Dispose()
[Convert]::ToBase64String($jwtKeyBytes)
```

Dán kết quả vào `.env` của máy. Không dùng secret test toàn số 0 hoặc secret mẫu để chạy project có dữ liệu thực.

### 8.3. Ghép JWT API với JSP session

JSP được mở bằng điều hướng trình duyệt và form HTML; các request đó không tự gắn Bearer header lấy từ JavaScript. Mẫu tích hợp dùng **hai chuỗi bảo mật**: API dùng JWT stateless; trang JSP dùng form login/session và CSRF. Hai chuỗi dùng cùng DB user và PasswordEncoder.

Để dùng mẫu, đổi các route JWT gốc thành `/api/auth/signup`, `/api/auth/login`, `/api/users/me` và `/api/users`. Cập nhật controller, client, CORS và bộ Postman cùng lúc. Nếu muốn giữ URL gốc, đổi `securityMatcher` và permit rules cho đủ `/auth/**` và `/users/**`.

```java
package vn.iotstar.config;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.filter.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            DaoAuthenticationProvider provider) throws Exception {
        http.securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable())
            .authenticationProvider(provider)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                .accessDeniedHandler((req, res, ex) -> res.sendError(403)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain pageSecurity(HttpSecurity http,
            DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
            .authorizeHttpRequests(a -> a
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true).permitAll())
            .logout(l -> l.logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(
            JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
```

Mẫu cần các bean provider/UserDetailsService và JwtAuthenticationFilter đã liệt kê; cần controller GET `/login` trả JSP `login`. Form login gửi `email`, `password` và CSRF token. `hasRole("ADMIN")` đòi authority `ROLE_ADMIN`; user JWT gốc phải được bổ sung quyền nếu bài cuối kỳ có admin/user.

API CSRF bị tắt trong mẫu vì nó chỉ nhận Bearer header; không áp dụng chính sách đó cho toàn bộ JSP/session. Nếu dùng JWT trong cookie phải đánh giá lại CSRF. Trường hợp frontend khác origin, bổ sung CorsConfigurationSource với origin cụ thể, method cần dùng và header `Authorization`, `Content-Type`; project JWT gốc chỉ cho GET/POST/OPTIONS nên CRUD PUT/DELETE cần bổ sung.

Provider dùng `UserDetailsService` và `PasswordEncoder`; đối chiếu API theo nền bạn chọn tại [Spring Security DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/dao-authentication-provider.html). Project Boot 4 và Boot 3 không phải lúc nào cũng có cùng constructor/import.

### 8.4. Kiểm tra JWT

1. Đăng ký user, xem DB để chắc password đã hash BCrypt.
2. Đăng nhập đúng, nhận token và `expiresIn`; sai password trả 401.
3. Gọi hồ sơ với Bearer token, kiểm tra không có field password.
4. Bỏ token, sửa một ký tự token, dùng token hết hạn: nhận 401.
5. Khóa tài khoản: token cũ không tiếp tục được sử dụng.
6. Role user truy cập endpoint admin: bị từ chối 403.

Bộ test tham khảo: [JwtFlowIntegrationTest.java](../TUAN10/24133045_BuiThanhPhuc_JWT_Nimbus/src/test/java/vn/iotstar/JwtFlowIntegrationTest.java). Test của project gốc dùng database MySQL riêng `tuan10_jwt_nimbus_test` và `create-drop`; không trỏ test đó vào database cuối kỳ.

## 9. WebSocket

### 9.1. Mã nguồn tìm thấy tại đây

[booking-travel-api/src/services/socketService.js](../booking-travel-api/src/services/socketService.js) dùng **Socket.IO** và JWT middleware. Nó kiểm tra user đang active, gắn identity từ server vào socket, có chat công cộng và phòng hỗ trợ riêng; tin nhắn chat được lưu MongoDB và user lấy từ MySQL.

Đó là nguồn để học cách xác thực và phân quyền phòng chat. Client Socket.IO không kết nối trực tiếp với endpoint Spring STOMP vì giao thức khác nhau. Không cần thêm MongoDB vào project Java nếu bạn chọn lưu tin nhắn bằng JPA/MySQL.

Không tìm thấy bài Spring STOMP/WebSocket hoặc PDF WebSocket riêng trong các tài liệu đã kiểm kê. Các ví dụ tiếp theo là bổ sung theo [Spring STOMP](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/enable.html), không được trình bày như mã nguồn đã có ở BT05/TUAN10.

### 9.2. Mẫu chat dùng session của JSP

POM ở mục 2 đã có `spring-boot-starter-websocket`. Tạo `WebSocketConfig.java`:

```java
package vn.iotstar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
```

Mẫu này dùng **WebSocket native, không SockJS**. Endpoint mặc định kiểm tra cùng origin; trang JSP đã đăng nhập sẽ truyền session cookie khi handshake, Spring dùng user từ HTTP request. SiteMesh phải loại trừ `/ws`. Không `permitAll` endpoint chat trong cấu hình session nếu muốn yêu cầu đăng nhập.

`ChatController.java`:

```java
package vn.iotstar.controller;

import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {
    public record ChatInput(String content) {}
    public record ChatOutput(String sender, String content) {}

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatOutput send(ChatInput input, Principal principal) {
        if (principal == null) throw new IllegalArgumentException("Cần đăng nhập");
        if (input == null || input.content() == null)
            throw new IllegalArgumentException("Thiếu nội dung");
        String content = input.content().trim();
        if (content.isEmpty() || content.length() > 2000)
            throw new IllegalArgumentException("Tin nhắn phải từ 1 đến 2000 ký tự");
        return new ChatOutput(principal.getName(), content);
    }
}
```

Tạo `chat.jsp` và controller GET `/chat` trả view `chat`. Trên JSP, tải bản UMD của `@stomp/stompjs` bạn đã chọn về `static/js/vendor/stomp.umd.min.js` để tránh lệ thuộc CDN khi demo. Ví dụ sau chạy khi trang đã đăng nhập và nút Send chỉ được bật sau `onConnect`:

```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<div id="messages"></div>
<input id="content" maxlength="2000">
<button id="send" disabled>Gửi</button>
<script src="<c:url value='/js/vendor/stomp.umd.min.js'/>"></script>
<script>
const wsPath = '<c:url value="/ws"/>';
const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
const client = new StompJs.Client({
  brokerURL: scheme + '//' + location.host + wsPath,
  reconnectDelay: 5000,
  onConnect: () => {
    document.querySelector('#send').disabled = false;
    client.subscribe('/topic/chat', frame => {
      const message = JSON.parse(frame.body);
      const row = document.createElement('p');
      row.textContent = message.sender + ': ' + message.content;
      document.querySelector('#messages').append(row);
    });
  },
  onWebSocketClose: () => {
    document.querySelector('#send').disabled = true;
  }
});
document.querySelector('#send').onclick = () => {
  if (!client.connected) return;
  client.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({content: document.querySelector('#content').value})
  });
};
client.activate();
</script>
```

`/ws` là URL handshake; `/app/chat.send` chạy `@MessageMapping`; `/topic/chat` là kênh nhận. Simple broker giữ subscription trong bộ nhớ, không lưu lịch sử và không tự biến `/queue` thành phòng riêng bảo mật. Muốn lịch sử: thêm `ChatMessage` entity, repository MySQL và service; lấy sender từ Principal, không tin user ID client gửi lên.

### 9.3. Nếu bắt buộc chat dùng JWT

Mẫu session ở trên là cách ghép trực tiếp với JSP. Nếu chat phải dùng JWT stateless:

1. Client gửi Bearer trong **STOMP CONNECT headers**, không phải header của WebSocket handshake trình duyệt.
2. Một `ChannelInterceptor` đọc header, dùng JwtService xác minh, tải user còn active và gọi `accessor.setUser(authentication)`.
3. Interceptor từ chối mọi message không có user và kiểm soát SEND/SUBSCRIBE theo destination; client chỉ được SEND tới endpoint `/app/...` được phép, không tự SEND tới broker `/topic/...`.
4. Cho phép handshake HTTP `/ws` khi chọn cơ chế này, nhưng không mở quyền STOMP. Đặt interceptor xác thực trước interceptor phân quyền.
5. Phòng hỗ trợ riêng phải kiểm tra owner/admin ở server. Dùng user destination hoặc mapping có kiểm tra quyền; đừng cho subscribe phòng người khác chỉ bằng cách biết ID.
6. Có chính sách đóng kết nối khi JWT hết hạn hoặc tài khoản bị khóa; chỉ kiểm tra CONNECT không tự kiểm tra lại hạn dùng cho mọi message sau đó.

Không nhét token vào URL để tránh ghi token trong log. Hướng dẫn cơ chế CONNECT và ChannelInterceptor: [Spring token authentication](https://docs.spring.io/spring-framework/reference/6.2/web/websocket/stomp/authentication-token-based.html). Đây là các bước bổ sung cần triển khai/kiểm thử nếu bạn chọn nhánh JWT cho chat; không phải một module chat JWT hoàn chỉnh đã chạy thử.

## 10. Cloudinary

Tên đúng là **Cloudinary** (phần bạn ghi “Cloudairy”). **Nguồn:** project BTAP09, `CloudinaryConfig`, `CloudinaryServiceImpl`, `ProductServiceImpl`, entity Product; PDF `vd1.pdf` và `vd3.pdf` có nhắc Cloudinary.

### 10.1. Dependency và credentials

Project nguồn dùng `com.cloudinary:cloudinary-http5:2.4.0`, đã có trong POM mẫu. Giữ `cloud_name`, `api_key`, `api_secret` ở phía server qua `.env`; không đưa api_secret vào JSP/JavaScript.

`CloudinaryConfig.java` mẫu:

```java
package vn.iotstar.config;

import java.util.Map;
import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class CloudinaryConfig {
    @Bean
    Cloudinary cloudinary(
            @Value("${cloudinary.cloud-name}") String name,
            @Value("${cloudinary.api-key}") String key,
            @Value("${cloudinary.api-secret}") String secret) {
        return new Cloudinary(Map.of(
            "cloud_name", name, "api_key", key,
            "api_secret", secret, "secure", true));
    }
}
```

Ba key của SDK dùng dấu gạch dưới; ba property của Spring trong bài dùng dấu gạch ngang. Phải nối đúng hai bộ tên. Đối chiếu [Cloudinary Java SDK](https://cloudinary.com/documentation/java_integration).

### 10.2. Service upload lấy từ project

Chuyển ba file này sang project cuối kỳ và chỉnh package; nếu không giữ Lombok thì thay `@RequiredArgsConstructor` bằng constructor Java:

```text
service/CloudinaryService.java
service/CloudinaryUploadResult.java
service/impl/CloudinaryServiceImpl.java
```

Interface có `upload(MultipartFile)` và `delete(String publicId)`. Record kết quả có `url`, `publicId`. Mã nguồn thực tế:

- Từ chối file rỗng hoặc quá 5 MB.
- Chỉ chấp nhận MIME JPEG/PNG và dùng `ImageIO.read` để kiểm tra nội dung ảnh.
- Upload byte[] bằng `cloudinary.uploader().upload(...)`, dùng folder `btap09/products`, `resource_type=image`.
- Lấy **`secure_url`** và **`public_id`** từ kết quả; xóa qua `uploader().destroy(publicId, ...)`.

Đổi folder Cloudinary thành `project-cuoi-ki/products` nếu muốn phân nhóm ảnh. Trong entity MySQL lưu ít nhất:

```java
@Column(name = "image_url", length = 1000)
private String imageUrl;

@Column(name = "cloudinary_public_id", length = 255)
private String cloudinaryPublicId;
```

JSP hiển thị ảnh bằng URL HTTPS, có thể đặt trong biến đã escape bằng JSTL. Không truyền URL ảnh vào hàm delete vì hàm đó cần public ID.

### 10.3. Cập nhật/xóa ảnh và transaction

`ProductServiceImpl` của BTAP09 có cách xử lý nên giữ khi ghép:

| Thao tác | Cách xử lý |
| --- | --- |
| Thêm sản phẩm | Upload ảnh, lưu URL/public ID vào DB; nếu transaction rollback thì xóa ảnh mới |
| Đổi ảnh | Upload mới và cập nhật DB; sau commit mới xóa ảnh cũ |
| Sửa nhưng không chọn ảnh mới | Giữ URL/public ID cũ |
| Xóa sản phẩm | Xóa record; sau commit xóa ảnh Cloudinary |
| Xóa ảnh Cloudinary thất bại | Ghi nhận public ID để retry, không giả định rollback MySQL sẽ phục hồi ảnh |

Project đăng ký `TransactionSynchronization` với `afterCommit` và `afterCompletion`. Transaction MySQL không bao phủ API Cloudinary, nên không xóa ảnh cũ trước khi DB commit.

Điểm cần sửa nếu ghép từ Java 26 xuống Java 21: nguồn có `Math.clamp(size, 1, 50)`; thay bằng `Math.max(1, Math.min(size, 50))` để tương thích nền mẫu. Kiểm tra owner/admin trên service giống nguồn BTAP09, không chỉ ẩn nút Sửa/Xóa trong JSP.

### 10.4. Kiểm tra

Upload PNG/JPEG nhỏ, xem DB có đủ URL/public ID; thử file không phải ảnh và file quá giới hạn; sửa sản phẩm không thay ảnh; thay ảnh mới; xóa sản phẩm và kiểm tra ảnh cũ đã được dọn. Khi thông tin Cloudinary sai, hiển thị lỗi phù hợp thay vì lưu sản phẩm với URL giả.

## 11. REST API, AJAX và DTO

**Nguồn:** BTVN_API; các PDF RESTful/AJAX/CRUD API/Swagger; project MapperDTO VD12/VD3 và BTAP09 MapStruct.

REST controller nên có namespace `/api/...` để Security JWT và SiteMesh loại trừ dễ kiểm soát. MVC controller trả JSP dùng namespace `/admin/...`, `/products`, `/login`. AJAX gửi Bearer tới API; form JSP gửi CSRF cho endpoint session.

Ví dụ AJAX lấy hồ sơ, giả sử `token` vừa nhận sau đăng nhập và còn được giữ trong bộ nhớ trang:

```javascript
async function loadProfile(apiUrl, token) {
  const response = await fetch(apiUrl, {
    headers: {Authorization: 'Bearer ' + token}
  });
  if (response.status === 401) throw new Error('Cần đăng nhập lại');
  if (!response.ok) throw new Error('Không tải được hồ sơ');
  return response.json();
}
```

Tạo `apiUrl` bằng `<c:url value='/api/users/me'/>` trên JSP để chạy dưới context path. Với JSON POST thêm `Content-Type: application/json`; với multipart `FormData` để trình duyệt tự đặt Content-Type và boundary.

MapperDTO VD12/VD3 giúp tách dữ liệu view/API khỏi entity, kể cả sản phẩm và nhiều ảnh. Nếu dùng mapper thủ công thì không cần MapStruct. Nếu dùng MapStruct như BTAP09, thêm `mapstruct:1.6.3`, annotation processor, và `lombok-mapstruct-binding:0.2.0` khi phối hợp Lombok; POM nguồn là nơi đối chiếu cấu hình processor.

BTVN_API dùng `springdoc-openapi-starter-webmvc-ui:2.8.17`. Đây là phần tùy chọn cho nền Boot 3.x; không chép dependency Swagger2/Springfox cũ từ PDF vào Boot mới. Nếu thêm Swagger vào mẫu JWT, quyết định cho phép `/swagger-ui/**` và `/v3/api-docs/**` để dùng khi học; cấu hình Swagger gửi Bearer cho các API được bảo vệ.

## 12. Ghép các phần vào project cuối kỳ

### 12.1. Trình tự thực hiện

1. Tạo nền Boot/Java/WAR thống nhất. Kiểm tra `java -version`, `mvn -version` và JDK IDE cùng nền.
2. Tạo MySQL database; viết `.env`, profile MySQL; dùng schema của project cuối kỳ.
3. Tạo entity/repository/service và thử CRUD trước khi thêm giao diện.
4. Tạo JSP `home`, view resolver và controller. Kiểm tra mở được JSP.
5. Đăng ký SiteMesh một lần, thêm layout web/admin và Bootstrap cùng phiên bản.
6. Thêm session/form login cho JSP, role admin/user, CSRF form.
7. Ghép JwtService/filter vào namespace `/api`, kiểm tra đăng ký/đăng nhập/hồ sơ.
8. Thay upload ổ đĩa bằng Cloudinary tại service sản phẩm; lưu URL/public ID.
9. Thêm chat WebSocket session; nếu cần JWT chat thì triển khai nhánh CONNECT/interceptor riêng.
10. Chốt schema, đổi `ddl-auto=validate`, build WAR và kiểm tra lại đúng cách chạy khi nộp.

### 12.2. Lệnh chạy

Chạy từ thư mục chứa `pom.xml` và `.env`:

```powershell
java -version
mvn -version
mvn clean package
java -jar .\target\project-cuoi-ki-0.0.1-SNAPSHOT.war
```

Khi phát triển có thể dùng `mvn spring-boot:run`. Nếu không có Maven trên PATH, máy này có bản Maven dưới `D:\java\apache-maven-3.9.16-bin`; tìm `bin\mvn.cmd` trong thư mục đó và gọi đường dẫn thực tế. Không đổi môi trường sang Java 26 khi POM mẫu chọn 21 chỉ vì JDK 26 đang có trên máy.

Deploy ngoài Tomcat: cần main class kế thừa SpringBootServletInitializer, dependency Tomcat `provided`, container phù hợp. URL lúc chạy WAR ngoài thường có context `/project-cuoi-ki-0.0.1-SNAPSHOT`; dùng `<c:url>` để tránh link hỏng. Khi chạy executable WAR thường truy cập `http://localhost:8080/` theo port cấu hình.

### 12.3. Kiểm tra trước khi nộp

- [ ] Chỉ có một main application, một bộ entity User và một cấu hình datasource MySQL.
- [ ] Không chứa password DB, JWT secret hoặc Cloudinary secret trong repo.
- [ ] JSP render đúng, tiếng Việt đúng UTF-8, asset tải được cả khi có context path.
- [ ] Layout admin/web chọn đúng; API/ảnh/WebSocket không bị decorator bọc.
- [ ] CRUD có validation, phân trang không nhận page âm, form POST có CSRF khi dùng session.
- [ ] User thường không thao tác được tài nguyên admin hoặc sản phẩm của người khác.
- [ ] JWT sai/hết hạn bị từ chối; DTO không trả password.
- [ ] Upload/thay/xóa ảnh giữ DB và Cloudinary nhất quán theo khả năng retry.
- [ ] Hai cửa sổ chat nhận tin; giả mạo sender/phòng chat bị chặn; tin nhắn được lưu MySQL nếu cần lịch sử.
- [ ] Script SQL khởi tạo đúng schema; test không dùng chung DB cuối kỳ.
- [ ] WAR đã được kiểm tra bằng cách chạy thực tế dùng để demo/nộp.

## 13. Lỗi thường gặp

| Hiện tượng | Chỗ kiểm tra/cách sửa |
| --- | --- |
| `release version 21/26 not supported` | JDK của Maven/IDE thấp hơn POM; chạy `mvn -version` rồi chọn đúng JDK |
| Lỗi `Math.clamp` khi chuyển BTAP09 sang Java 21 | Thay bằng `Math.max(1, Math.min(size, 50))` |
| JSP 404 hoặc in ra tên view | Kiểm tra WAR, file dưới `WEB-INF/views`, prefix/suffix, dùng `@Controller` |
| `Unable to compile class for JSP`, thiếu taglib | Kiểm tra Jasper và cả JSTL API/implementation; bỏ JSP API/container override sai nền |
| Mất CSS hoặc JS sau khi deploy WAR | URL asset có context path; nếu giữ `/static/**` của BT05 thì URL cũng phải đổi theo |
| Layout không áp dụng hoặc áp dụng lặp | Mapping tới đúng file decorator, chỉ đăng ký filter một lần, chỉ REQUEST |
| API nhận HTML | Security redirect sang login hoặc SiteMesh chưa loại trừ API; tách chuỗi JWT API |
| Login/form save trả 403 | CSRF thiếu/sai; tên field email/password không khớp login config; forward JSP bị chặn |
| JDBC `Access denied` | Username/password/host grant và `.env` đúng; file properties không thêm quotes kiểu shell |
| `Unknown database` | Tạo database trước; JDBC driver và Hibernate validate không tự tạo DB |
| `Schema-validation: missing table/column` | Import schema đúng project hoặc dùng update khi phát triển; không trỏ nhầm database bài khác |
| `Communications link failure` | MySQL service đang chạy, port 3306 và URL đúng |
| Không resolve `${DB_PASSWORD}`/`${JWT_SECRET}` | `.env` ở working directory và có dòng `spring.config.import` |
| `PropertyReferenceException` | Tên derived query khớp property entity; generic ID đúng kiểu |
| `LazyInitializationException` | Map DTO/fetch dữ liệu cần dùng trong service transaction; không để JSP tự tải quan hệ lazy |
| JWT key quá ngắn/sai Base64 | Sinh key ngẫu nhiên 32 byte trở lên rồi encode Base64; không dán chuỗi tùy ý |
| JWT filter chạy hai lần | Disable servlet registration; chỉ thêm vào SecurityFilterChain |
| API CRUD PUT/DELETE bị CORS | Bổ sung method và Authorization header vào CORS, đúng origin frontend |
| WebSocket handshake 401/403 | Đăng nhập session trước với mẫu session; nếu JWT CONNECT thì tách quyền handshake và message |
| Socket.IO client không nối được Spring | Dùng STOMP client phù hợp; không dùng Socket.IO protocol cho endpoint `/ws` |
| WebSocket nối được nhưng không thấy tin | SUBSCRIBE `/topic/chat`, SEND `/app/chat.send`, mapping đúng, loại trừ SiteMesh |
| Cloudinary báo sai credentials | Tên biến/env đúng; Spring `cloud-name` nối SDK `cloud_name` |
| Ảnh cũ bị mất sau khi cập nhật DB thất bại | Chỉ xóa ảnh cũ sau commit; dọn ảnh mới nếu rollback |
| Xóa ảnh Cloudinary không được | Truyền `public_id`, không truyền URL HTTPS |

## 14. Danh mục PDF và kết quả rà soát

Đã kiểm kê đệ quy **23.636 file** tại `D:\java` (gồm file ẩn và thư mục con), không gặp lỗi truy cập khi kiểm kê. Đã trích text của toàn bộ **23 PDF**, rà soát cấu hình/mã nguồn bài tập và đọc nội dung liên quan trong **9 ZIP project**. Thư mục công cụ/IDE và bản build được kiểm kê, nhưng không dùng làm project nguồn hướng dẫn. Không có `AGENTS.md` trong danh sách file được kiểm kê.

Thư mục chung cuối cùng có **17 PDF + 1 Markdown**. Bỏ 5 bản sao trùng nội dung; với báo cáo Bookroom, giữ `final-report.pdf` và bỏ bản báo cáo trước đó chỉ thay thứ tự các hình. Các file gốc ở vị trí ban đầu vẫn được giữ nguyên. Mỗi PDF được kiểm tra SHA-256 sau khi sao chép.

### 14.1. PDF đã giữ

| PDF trong thư mục chung | Trang | Nội dung nhận diện | Nguồn gốc |
| --- | ---: | --- | --- |
| [01_JAVASERVLET.pdf](01_JAVASERVLET.pdf) | 105 | JSP/JSTL, MySQL | `01_JAVASERVLET.pdf` |
| [06_MVC_3tier.pdf](06_MVC_3tier.pdf) | 35 | JSP/JSTL | `06_MVC_3tier.pdf` |
| [11_Thymeleaf.pdf](11_Thymeleaf.pdf) | 35 | Spring Boot, JSP/JSTL, Bootstrap, JPA, MySQL | `11_Thymeleaf.pdf` |
| [14_HD_Servlet_JDBC_CRUD.pdf](14_HD_Servlet_JDBC_CRUD.pdf) | 36 | JSP/JSTL, SQL Server, MySQL | `14_HD_Servlet_JDBC_CRUD.pdf` |
| [HƯỚNG DẪN MAPPER DTO TRONG SPRING BOOT 4.pdf](H%C6%AF%E1%BB%9ANG%20D%E1%BA%AAN%20MAPPER%20DTO%20TRONG%20SPRING%20BOOT%204.pdf) | 75 | Spring Boot, Bootstrap, JPA, SQL Server | `HƯỚNG DẪN MAPPER DTO TRONG SPRING BOOT 4.pdf` |
| [Đề thi Quá trình_01_2627.pdf](%C4%90%E1%BB%81%20thi%20Qu%C3%A1%20tr%C3%ACnh_01_2627.pdf) | 4 | JSP/JSTL, SiteMesh, JPA | `Đề thi Quá trình_01_2627.pdf` |
| [final-report.pdf](final-report.pdf) | 36 | JSP/JSTL, SiteMesh, JPA, MySQL | `24133045_backup_tools/.local/report-render/final-report.pdf` |
| [03_springsecurity6.pdf](03_springsecurity6.pdf) | 58 | Spring Boot, JSP/JSTL, JPA, SQL Server, MySQL, JWT | `BTAP09/03_springsecurity6.pdf` |
| [vd1.pdf](vd1.pdf) | 41 | Spring Boot, JPA, SQL Server, JWT, Cloudinary | `BTAP09/vd1.pdf` |
| [vd2.pdf](vd2.pdf) | 19 | Spring Boot, JPA, SQL Server | `BTAP09/vd2.pdf` |
| [vd3.pdf](vd3.pdf) | 55 | Spring Boot, JPA, SQL Server, JWT, Cloudinary | `BTAP09/vd3.pdf` |
| [05_GraphQL.pdf](05_GraphQL.pdf) | 15 | Spring Boot | `BTAPTUAN7/05_GraphQL.pdf` |
| [16_API RestFul.pdf](16_API%20RestFul.pdf) | 58 | Spring Boot, SQL Server, MySQL, JWT | `BTVN/16_API RestFul.pdf` |
| [CẤU HÌNH SWAGGER2 VÀ SWAGGER 3 TRÊN SPRING BOOT.pdf](C%E1%BA%A4U%20H%C3%8CNH%20SWAGGER2%20V%C3%80%20SWAGGER%203%20TR%C3%8AN%20SPRING%20BOOT.pdf) | 2 | Spring Boot | `BTVN/CẤU HÌNH SWAGGER2 VÀ SWAGGER 3 TRÊN SPRING BOOT.pdf` |
| [HƯỚNG DẪN AJAX VỚI RESTFUL API TRONG SPRING BOOT.pdf](H%C6%AF%E1%BB%9ANG%20D%E1%BA%AAN%20AJAX%20V%E1%BB%9AI%20RESTFUL%20API%20TRONG%20SPRING%20BOOT.pdf) | 11 | Spring Boot, JSP/JSTL, SiteMesh, Bootstrap | `BTVN/HƯỚNG DẪN AJAX VỚI RESTFUL API TRONG SPRING BOOT.pdf` |
| [HƯỚNG DẪN CRUD API CATEGORY TRÊN SPRING BOOT 3.pdf](H%C6%AF%E1%BB%9ANG%20D%E1%BA%AAN%20CRUD%20API%20CATEGORY%20TR%C3%8AN%20SPRING%20BOOT%203.pdf) | 15 | Spring Boot, JSP/JSTL, SiteMesh, JPA | `BTVN/HƯỚNG DẪN CRUD API CATEGORY TRÊN SPRING BOOT 3.pdf` |
| [04_JWT.pdf](04_JWT.pdf) | 34 | Spring Boot, JPA, MySQL, JWT | `TUAN10/04_JWT.pdf` |

Những tài liệu Thymeleaf, Mapper DTO, GraphQL, REST/Swagger được giữ làm phần bổ trợ Spring Boot. Từ khóa trong bảng giúp chọn tài liệu, không khẳng định PDF nào cũng là hướng dẫn độc lập cho mọi công cụ được nhắc. `final-report.pdf` là báo cáo thực hành, không phải slide bài giảng.

### 14.2. Các bản không sao chép lặp

| Bản đã giữ | Bản sao/phiên bản cũ tại nguồn |
| --- | --- |
| `01_JAVASERVLET.pdf` | `BTVN/01_JAVASERVLET (1).pdf` |
| `HƯỚNG DẪN MAPPER DTO TRONG SPRING BOOT 4.pdf` | `HUONG_DAN_MAPPER_DTO_SPRING_BOOT_4.pdf` |
| `final-report.pdf` | `24133045_backup_tools/.local/report-render/report.pdf` |
| `CẤU HÌNH SWAGGER2 VÀ SWAGGER 3 TRÊN SPRING BOOT.pdf` | `BTVN/CẤU HÌNH SWAGGER2 VÀ SWAGGER 3 TRÊN SPRING BOOT (1).pdf` |
| `HƯỚNG DẪN AJAX VỚI RESTFUL API TRONG SPRING BOOT.pdf` | `BTVN/HƯỚNG DẪN AJAX VỚI RESTFUL API TRONG SPRING BOOT (1).pdf` |
| `HƯỚNG DẪN CRUD API CATEGORY TRÊN SPRING BOOT 3.pdf` | `BTVN/HƯỚNG DẪN CRUD API CATEGORY TRÊN SPRING BOOT 3 (1).pdf` |

### 14.3. ZIP project đã đối chiếu

Các ZIP bài tập không chứa PDF bổ sung; đã kiểm tra danh sách thành viên và các file nguồn/cấu hình liên quan:

- `24133045_01.zip` (107 thành viên).
- `24133045_BuiThanhPhuc_BaiTap01.zip` (99 thành viên).
- `24133045_BuiThanhPhuc_MapperDTO_All.zip` (82 thành viên).
- `24133045_BuiThanhPhuc_MapperDTO_VD12.zip` (39 thành viên).
- `24133045_BuiThanhPhuc_MapperDTO_VD3.zip` (43 thành viên).
- `BANHANG.zip` (98 thành viên).
- `BTAP09/24133045_BuiThanhPhuc_BTap09_VD123_Source.zip` (135 thành viên).
- `BTAPTUAN7/24133045_BuiThanhPhuc_GraphQL.zip` (39 thành viên).
- `TUAN10/24133045_BuiThanhPhuc_JWT_Nimbus.zip` (66 thành viên).

ZIP Maven/Tomcat/JDK/STS là bộ công cụ, không được sao chép vào thư mục tài liệu. Project nguồn vẫn nằm tại thư mục cũ; các link ở mục 1 dẫn tới vị trí đó.

### 14.4. Tài liệu còn thiếu và phạm vi bổ sung

- Đã tìm thấy project Java cho Spring Boot, JSP/JSTL, SiteMesh, Bootstrap, JPA/MySQL, JWT và Cloudinary.
- Không tìm thấy PDF WebSocket riêng hoặc implementation Spring STOMP trong các project Java bài tập. Đã đọc Socket.IO trong `booking-travel-api` và bổ sung hướng dẫn Spring WebSocket ở mục 9, ghi rõ nguồn.
- Theo yêu cầu cập nhật, cấu hình database của hướng dẫn tập trung vào **MySQL**, không mở rộng tìm/cấu hình SQL Server hoặc PostgreSQL. PDF có nội dung SQL Server vẫn được giữ để học phần Spring/JPA liên quan.
- PDF được trích text để nhận diện nội dung; không chỉnh sửa PDF và không kiểm định tính đúng đắn của từng ảnh/chụp mã nguồn trong tài liệu gốc.
- Các mẫu tích hợp trong Markdown được đối chiếu API/tài liệu; chưa được build và chạy như một ứng dụng hoàn chỉnh trong lượt tổng hợp này.

### 14.5. Thứ tự học đề xuất

1. JavaServlet và MVC ba tầng để nắm request/controller/service/repository/view.
2. Đọc BT05 cùng mục 2-7 để dựng Spring Boot, JSP, layout và MySQL.
3. Đọc CRUD API/AJAX và MapperDTO để tách dữ liệu giao diện/API.
4. Đọc Spring Security và JWT, làm đăng nhập/phân quyền trước.
5. Đọc VD3 và mã Cloudinary để upload/thay/xóa ảnh.
6. Làm WebSocket theo mục 9, thêm lịch sử bằng JPA/MySQL khi cần.
7. Dùng mục 12-13 để ghép, chạy thử và xử lý lỗi.
