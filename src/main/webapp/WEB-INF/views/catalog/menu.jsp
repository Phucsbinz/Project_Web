<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Thực đơn · UTETra</title></head><body>
<section class="p-4 p-md-5 rounded-4 catalog-hero mb-4"><p class="text-uppercase small fw-bold">UTETra · Trà ngon mỗi ngày</p><h1>Chọn vị trà, chọn niềm vui.</h1><p>Tự chọn chi nhánh và tùy biến ly trà theo sở thích của bạn.</p><a class="btn btn-dark" href="<c:url value='/stores'/>">Khám phá các shop</a></section>
<form class="row g-3 mb-4" method="get" action="<c:url value='/menu'/>">
<c:if test="${param.top == 'true'}"><input type="hidden" name="top" value="true"></c:if>
<div class="col-md-4"><label class="form-label" for="keyword">Tên sản phẩm</label><input class="form-control" id="keyword" name="keyword" maxlength="150" value="<c:out value='${param.keyword}'/>"></div>
<div class="col-md-4"><label class="form-label" for="categoryId">Danh mục</label><select class="form-select" id="categoryId" name="categoryId"><option value="">Tất cả danh mục</option><c:forEach items="${categories}" var="c"><option value="${c.id}" ${param.categoryId == c.id ? 'selected' : ''}><c:out value="${c.name}"/></option></c:forEach></select></div>
<div class="col-md-4"><label class="form-label" for="storeId">Shop</label><select class="form-select" id="storeId" name="storeId"><option value="">Tất cả shop</option><c:forEach items="${stores}" var="s"><option value="${s.id}" ${param.storeId == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option></c:forEach></select></div>
<div class="col-md-3"><label class="form-label" for="min">Giá từ (đ)</label><input class="form-control" type="number" min="0" id="min" name="min" value="<c:out value='${param.min}'/>"></div>
<div class="col-md-3"><label class="form-label" for="max">Đến (đ)</label><input class="form-control" type="number" min="0" id="max" name="max" value="<c:out value='${param.max}'/>"></div>
<div class="col-md-4"><label class="form-label" for="sort">Sắp xếp</label><select class="form-select" id="sort" name="sort"><c:forTokens items="newest,price_asc,price_desc,sold,rating,favorites" delims="," var="s"><option value="${s}" ${param.sort == s ? 'selected' : ''}><c:choose><c:when test="${s == 'newest'}">Mới nhất</c:when><c:when test="${s == 'price_asc'}">Giá tăng dần</c:when><c:when test="${s == 'price_desc'}">Giá giảm dần</c:when><c:when test="${s == 'sold'}">Bán chạy</c:when><c:when test="${s == 'rating'}">Đánh giá cao</c:when><c:otherwise>Yêu thích nhiều</c:otherwise></c:choose></option></c:forTokens></select></div>
<div class="col-md-2 d-flex align-items-end"><button class="btn btn-dark w-100">Tìm món</button></div></form>
<c:if test="${empty groups}"><h2 class="h4">Thực đơn</h2><c:if test="${pageContext.request.servletPath == '/'}"><p>Món bán trên 10 ly, xếp theo số lượng đã bán.</p></c:if></c:if>
<%@ include file="product-list.jspf" %>
<nav class="d-flex gap-3 align-items-center my-4" aria-label="Phân trang">
<c:if test="${result.page > 0}"><button type="button" class="btn btn-outline-dark catalog-page" data-page="${result.page - 1}">Trang trước</button></c:if>
<span>Trang ${result.page + 1} · ${result.total} món</span>
<c:if test="${result.page + 1 < result.pages}"><button type="button" class="btn btn-outline-dark catalog-page" data-page="${result.page + 1}">Trang sau</button></c:if></nav>
<c:forEach items="${groups}" var="group"><section class="my-5"><h2 class="h4"><c:choose><c:when test="${group.key == 'newest'}">20 món mới nhất</c:when><c:when test="${group.key == 'sold'}">20 món bán chạy</c:when><c:when test="${group.key == 'rating'}">20 món đánh giá cao</c:when><c:otherwise>20 món được yêu thích</c:otherwise></c:choose></h2><c:set var="result" value="${group.value}"/><%@ include file="product-list.jspf" %><a href="<c:url value='/menu'/>?top=true&amp;sort=${group.key}">Xem theo trang</a></section></c:forEach>
<script src="<c:url value='/assets/js/catalog.js'/>" defer></script>
</body></html>
