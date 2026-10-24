<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Đăng ký shop · UTETra</title></head><body><div class="mx-auto" style="max-width:760px"><h1>Đăng ký shop</h1><p>Gửi hồ sơ để quản trị xét duyệt trước khi bắt đầu bán hàng.</p>
<section class="mb-4" aria-labelledby="registration-history"><h2 id="registration-history" class="h4">Hồ sơ của bạn</h2>
<c:forEach items="${registrations}" var="r"><article class="card p-3 mb-3"><h3 class="h5"><c:out value="${r.store.name}"/></h3>
<p><c:choose><c:when test="${r.status == 'PENDING'}">Đang chờ xét duyệt</c:when><c:when test="${r.status == 'APPROVED'}">Đã được duyệt</c:when><c:otherwise>Hồ sơ bị từ chối</c:otherwise></c:choose></p>
<c:if test="${not empty r.note}"><p>Phản hồi: <c:out value="${r.note}"/></p></c:if>
<c:if test="${r.status == 'APPROVED'}"><p class="small">Nếu vừa được duyệt, hãy đăng nhập lại để cập nhật quyền bán hàng.</p><a href="<c:url value='/vendor/shop'/>">Quản lý shop</a></c:if></article></c:forEach>
<c:if test="${empty registrations}"><p>Bạn chưa gửi hồ sơ đăng ký shop.</p></c:if></section>
<h2 class="h4">Gửi hồ sơ mới</h2><form id="registration-form" class="vstack gap-3">
<%@ include file="store-fields.jspf" %>
<button class="btn btn-dark">Gửi hồ sơ đăng ký</button><output class="form-result" aria-live="polite"></output></form></div><script src="<c:url value='/assets/js/catalog.js'/>" defer></script></body></html>
