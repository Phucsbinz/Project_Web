<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Thống kê giao hàng</title></head><body>
<jsp:include page="nav.jsp"/>
<h1 class="h3">Thống kê giao hàng của tôi</h1><form method="get" class="d-flex gap-3 mb-3"><label>Từ ngày<input class="form-control" name="from" type="date" value="${report.from}"></label><label>Đến ngày<input class="form-control" name="to" type="date" value="${report.to}"></label><button class="btn btn-primary align-self-end">Xem</button></form><dl><dt>Đã phân công</dt><dd><c:out value="${report.assigned}"/></dd><dt>Đã giao</dt><dd><c:out value="${report.completed}"/></dd><dt>Đang được giao</dt><dd><c:out value="${report.active}"/></dd><dt>Đơn đã báo giao thất bại</dt><dd><c:out value="${report.failed}"/></dd></dl><a href="<c:url value='/shipper/orders'/>">Xem đơn được phân công</a></body></html>
