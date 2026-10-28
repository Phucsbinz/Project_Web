<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Đơn giao của tôi · UTETra</title></head><body>
<jsp:include page="nav.jsp"/>
<section class="shipper-page" aria-labelledby="shipper-title">
<span class="eyebrow">UTETRA DELIVERY</span><h1 class="h3" id="shipper-title">Đơn giao của tôi</h1>
<p class="text-secondary">Chỉ hiển thị đơn đang được phân công cho bạn và đơn bạn đã hoàn thành. Đơn bị hủy hoặc chuyển cho người khác sẽ được gỡ khỏi danh sách.</p>
<div id="order-message" hidden role="alert"></div>
<form method="get" class="d-flex flex-wrap gap-2 mb-4"><label for="delivery-filter">Trạng thái đơn<select name="status" id="delivery-filter" class="form-select"><option value="">Tất cả</option><c:forTokens items="NEW,CONFIRMED,PICKED_UP,DELIVERING,DELIVERED,RETURN_REQUESTED,RETURN_APPROVED,RETURNED,REFUNDED" delims="," var="s"><option data-status="${s}" value="${s}" ${status == s ? 'selected' : ''}><c:out value="${s}"/></option></c:forTokens></select></label><button class="btn btn-primary align-self-end">Lọc đơn giao</button><a class="btn btn-outline-secondary align-self-end" href="<c:url value='/shipper/reports'/>">Thống kê của tôi</a></form>
<c:if test="${empty orders}"><p class="alert alert-info">Chưa có đơn giao phù hợp. Bạn có thể đổi bộ lọc hoặc đợi cửa hàng phân công.</p></c:if>
<div class="row g-3"><c:forEach items="${orders}" var="o"><div class="col-12 col-xl-6"><article class="card h-100 delivery-card"><div class="card-body d-flex flex-column gap-2">
<div class="d-flex justify-content-between flex-wrap gap-2"><h2 class="h5 mb-0 text-break mw-100"><a href="<c:url value='/shipper/orders/${o.id}'/>"><c:out value="${o.code}"/></a></h2><span class="status-pill" data-status="${o.status}"><c:out value="${o.status}"/></span></div>
<p class="mb-0">Lấy hàng tại: <strong><c:out value="${o.storeName}"/></strong></p>
<p class="mb-0 text-break">Địa chỉ lấy: <c:out value="${o.pickupAddress}"/> · <c:out value="${o.pickupPhone}"/></p>
<p class="mb-0">Người nhận: <c:out value="${o.recipientName}"/> · <c:out value="${o.recipientPhone}"/></p>
<p class="mb-0 text-break">Giao đến: <c:out value="${o.deliveryAddress}"/></p>
<p class="mb-0">${o.codDue > 0 ? 'COD cần thu:' : 'COD còn phải thu:'} <strong data-money="${o.codDue}"><c:out value="${o.codDue}"/></strong> · <span class="status-pill" data-status="${o.paymentStatus}"><c:out value="${o.paymentStatus}"/></span></p>
<a class="btn btn-primary align-self-start mt-auto" href="<c:url value='/shipper/orders/${o.id}'/>">Xem đơn / cập nhật giao hàng</a>
</div></article></div></c:forEach></div>
<nav class="d-flex flex-wrap gap-2 mt-4" aria-label="Phân trang đơn giao"><c:if test="${page > 0}"><c:url var="previous" value="/shipper/orders"><c:param name="page" value="${page-1}"/><c:param name="status" value="${status}"/></c:url><a class="btn btn-outline-secondary" href="${previous}">Trang trước</a></c:if><c:if test="${orders.size() == 20}"><c:url var="next" value="/shipper/orders"><c:param name="page" value="${page+1}"/><c:param name="status" value="${status}"/></c:url><a class="btn btn-outline-secondary" href="${next}">Trang sau</a></c:if></nav>
</section><script src="<c:url value='/assets/js/orders.js'/>"></script></body></html>
