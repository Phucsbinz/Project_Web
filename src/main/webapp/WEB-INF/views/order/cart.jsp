<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Giỏ hàng UTETra</title></head><body>
<jsp:include page="nav.jsp"/>
<div class="order-page-head"><span class="eyebrow">A BAG FULL OF LITTLE JOYS</span><h1>Giỏ hàng của bạn.</h1><p class="text-secondary small">Những lựa chọn ngon lành cho một ngày vui hơn.</p></div><div id="order-message" hidden role="alert"></div>
<c:choose><c:when test="${empty cart.items}"><div class="empty-state cart-empty"><svg class="icon" aria-hidden="true"><use href="#i-bag"/></svg><h2>Giỏ nhỏ, đợi niềm vui lớn.</h2><p>Giỏ hàng đang trống. Chọn món tại cửa hàng bạn muốn mua, thêm đường đá theo ý thích và hẹn một ly thật ngon.</p><a class="btn btn-primary" href="<c:url value='/menu'/>">Chọn ly trà đầu tiên <svg class="icon" aria-hidden="true"><use href="#i-arrow"/></svg></a></div></c:when><c:otherwise>
<div class="table-responsive"><table class="table align-middle"><thead><tr><th>Món</th><th>Đường / đá</th><th>Số ly</th><th>Ghi chú</th><th></th></tr></thead><tbody>
<c:forEach items="${cart.items}" var="item"><tr><td><c:out value="${item.name}"/> · <c:out value="${item.size}"/><small class="d-block"><c:forEach items="${item.toppings}" var="top"><span class="me-2"><c:out value="${top}"/></span></c:forEach></small></td><td><c:out value="${item.sugar}"/>% / <c:out value="${item.ice}"/>%</td><td><input aria-label="Số ly" class="form-control quantity" type="number" min="1" max="20" value="${item.quantity}" data-id="${item.id}"></td><td><c:out value="${item.note}"/></td><td><button class="btn btn-outline-danger delete-item" data-id="${item.id}">Xóa</button></td></tr></c:forEach>
</tbody></table></div><button class="btn btn-outline-danger" id="clear-cart">Xóa giỏ / đổi cửa hàng</button>
<c:choose><c:when test="${signedIn}"><a class="btn btn-primary" href="<c:url value='/checkout'/>">Tiến hành thanh toán</a></c:when><c:otherwise><p class="mt-3">Đăng nhập để lưu các món đã chọn và tiếp tục đặt hàng.</p><a href="<c:url value='/auth/login'/>">Đăng nhập</a></c:otherwise></c:choose>
</c:otherwise></c:choose>
<c:if test="${signedIn}"><button class="btn btn-outline-secondary mt-3" id="merge-cart">Giữ các món đã chọn trước đăng nhập</button></c:if>
<script src="<c:url value='/assets/js/orders.js'/>"></script><script>
const cartVersion = ${cart.version};
document.querySelectorAll('.quantity').forEach(node => node.addEventListener('change', () => orders.action('/cart/items/' + node.dataset.id, {quantity: Number(node.value), version: cartVersion})));
document.querySelectorAll('.delete-item').forEach(node => node.addEventListener('click', () => orders.action('/cart/items/' + node.dataset.id + '/delete', {version: cartVersion})));
document.querySelector('#clear-cart')?.addEventListener('click', () => {if (confirm('Xóa giỏ hiện tại để đổi cửa hàng?')) orders.action('/cart/clear', {});});
document.querySelector('#merge-cart')?.addEventListener('click', () => orders.action('/cart/merge', {}));
</script></body></html>
