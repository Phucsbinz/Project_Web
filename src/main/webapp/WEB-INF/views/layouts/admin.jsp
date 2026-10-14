<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="_csrf" content="<c:out value='${_csrf.token}'/>">
<meta name="_csrf_header" content="<c:out value='${_csrf.headerName}'/>">
<meta name="app-context" content="${pageContext.request.contextPath}">
<title><sitemesh:write property="title"/></title>
<link rel="stylesheet" href="<c:url value='/assets/vendor/bootstrap/bootstrap.min.css'/>">
<link rel="stylesheet" href="<c:url value='/assets/css/utetra.css'/>">
<sitemesh:write property="head"/>
</head><body>
<nav class="navbar navbar-expand-lg bg-white border-bottom mb-4" aria-label="Điều hướng chính"><div class="container">
<a class="navbar-brand" href="<c:url value='/'/>">UTETra</a>
<button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav" aria-controls="mainNav" aria-expanded="false" aria-label="Mở menu"><span class="navbar-toggler-icon"></span></button>
<div class="collapse navbar-collapse" id="mainNav"><span class="navbar-text ms-lg-auto">Quản trị hệ thống</span></div>
</div></nav>
<main class="container pb-5"><sitemesh:write property="body"/></main>
<footer class="site-footer border-top py-4 bg-white"><div class="container small">UTETra · Website chuỗi trà sữa · Spring MVC và kiến trúc 3 tầng</div></footer>
<script src="<c:url value='/assets/vendor/bootstrap/bootstrap.bundle.min.js'/>"></script>
<script src="<c:url value='/assets/js/utetra.js'/>"></script>
</body></html>