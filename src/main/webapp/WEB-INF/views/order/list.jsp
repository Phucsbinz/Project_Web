<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Danh sách đơn</title></head><body>
<jsp:include page="nav.jsp"/>
<h1 class="h3">Danh sách đơn hàng</h1><form method="get" class="d-flex gap-2 mb-3"><label>Trạng thái<select name="status" class="form-select"><option value="">Tất cả</option><c:forTokens items="NEW,CONFIRMED,PICKED_UP,DELIVERING,DELIVERED,CANCELLED,RETURN_REQUESTED,RETURN_APPROVED,RETURNED,REFUNDED" delims="," var="s"><option value="${s}" ${status == s ? 'selected' : ''}><c:out value="${s}"/></option></c:forTokens></select></label><button class="btn btn-primary align-self-end">Lọc</button></form>
<c:if test="${empty orders}"><div class="alert alert-info">Chưa có đơn phù hợp.</div></c:if>
<div class="table-responsive"><table class="table"><thead><tr><th>Đơn</th><th>Cửa hàng</th><th>Trạng thái</th><th>Thanh toán</th><th>Tổng</th></tr></thead><tbody><c:forEach items="${orders}" var="o"><tr><td><a href="<c:url value='${base}/${o.id}'/>"><c:out value="${o.code}"/></a></td><td><c:out value="${o.storeName}"/></td><td><c:out value="${o.status}"/></td><td><c:out value="${o.paymentStatus}"/></td><td><c:out value="${o.total}"/>đ</td></tr></c:forEach></tbody></table></div>
<c:if test="${page > 0}"><c:url var="previous" value="${base}"><c:param name="page" value="${page-1}"/><c:param name="status" value="${status}"/></c:url><a class="btn btn-outline-secondary" href="${previous}">Trang trước</a></c:if>
<c:url var="next" value="${base}"><c:param name="page" value="${page+1}"/><c:param name="status" value="${status}"/></c:url><a class="btn btn-outline-secondary" href="${next}">Trang sau</a>
</body></html>
