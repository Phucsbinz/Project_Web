<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Đăng ký shop · UTETra</title></head><body><div class="mx-auto" style="max-width:760px"><h1>Đăng ký shop</h1><p>Gửi hồ sơ để quản trị xét duyệt trước khi bắt đầu bán hàng.</p><form id="registration-form" class="vstack gap-3">
<%@ include file="store-fields.jspf" %>
<button class="btn btn-dark">Gửi hồ sơ đăng ký</button><output class="form-result" aria-live="polite"></output></form></div><script src="<c:url value='/assets/js/catalog.js'/>" defer></script></body></html>
