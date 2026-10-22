<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title><c:out value="${store.name}"/> · UTETra</title></head><body>
<c:if test="${not empty store.bannerUrl}"><img class="w-100 rounded-4 mb-4 catalog-banner" src="<c:out value='${store.bannerUrl}'/>" alt="Banner shop"></c:if>
<h1><c:out value="${store.name}"/></h1><p><c:out value="${store.description}"/></p><p><c:out value="${store.address}"/> · <c:out value="${store.phone}"/></p><p>Mở cửa ${store.openTime} – ${store.closeTime} · ${store.acceptingOrders ? 'Đang nhận đơn' : 'Tạm ngừng nhận đơn'}</p>
<a class="btn btn-dark mb-4" href="<c:url value='/menu'/>?storeId=${store.id}">Chọn shop này và lọc thực đơn</a>
<h2 class="h4">Khu vực phục vụ</h2><ul><c:forEach items="${store.areas}" var="a"><c:if test="${a.active}"><li><c:out value="${a.name}"/></li></c:if></c:forEach></ul><p class="small text-secondary">Phí giao được xác định theo khu vực và nhà vận chuyển khi đặt hàng.</p>
<h2 class="h4">Ưu đãi đang diễn ra</h2><c:forEach items="${promotions}" var="p"><p class="alert alert-success"><strong><c:out value="${p.name}"/></strong> · Giảm ${p.percent}% · Tối đa ${p.maxDiscount} đ · Đơn từ ${p.minOrder} đ <c:if test="${not empty p.code}">· Mã <c:out value="${p.code}"/></c:if></p></c:forEach><c:if test="${empty promotions}"><p>Chưa có ưu đãi đang áp dụng.</p></c:if>
<c:if test="${not empty deliveryRates}"><div class="table-responsive mb-4"><table class="table"><caption>Phí giao theo khu vực và nhà vận chuyển</caption><thead><tr><th scope="col">Khu vực</th><th scope="col">Nhà vận chuyển</th><th scope="col">Phí giao</th></tr></thead><tbody><c:forEach items="${deliveryRates}" var="r"><tr><td><c:out value="${r.areaName}"/></td><td><c:out value="${r.carrierName}"/></td><td>${r.fee} đ</td></tr></c:forEach></tbody></table></div></c:if>
<h2 class="h4">Thực đơn shop</h2><%@ include file="product-list.jspf" %></body></html>
