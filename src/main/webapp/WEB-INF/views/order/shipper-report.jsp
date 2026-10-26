<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Thống kê giao hàng</title></head><body>
<jsp:include page="nav.jsp"/>
<section class="shipper-report" aria-labelledby="shipper-report-title">
<div class="page-heading"><div><span class="eyebrow">YOUR DELIVERY JOURNEY</span><h1 id="shipper-report-title" class="h3">Thống kê giao hàng của tôi</h1><p class="text-secondary">Theo dõi các đơn được phân công trong khoảng thời gian bạn chọn.</p></div></div>
<form method="get" class="shipper-report-filters d-flex flex-wrap gap-3 mb-4"><label for="shipper-from">Từ ngày<input id="shipper-from" class="form-control" name="from" type="date" value="${report.from}"></label><label for="shipper-to">Đến ngày<input id="shipper-to" class="form-control" name="to" type="date" value="${report.to}"></label><button class="btn btn-primary align-self-end">Xem thống kê</button></form>
<div class="report-metrics" aria-label="Kết quả giao hàng"><article><span>Đã phân công</span><strong><c:out value="${report.assigned}"/></strong></article><article><span>Đã giao</span><strong><c:out value="${report.completed}"/></strong></article><article><span>Đang được giao</span><strong><c:out value="${report.active}"/></strong></article><article><span>Đã báo giao thất bại</span><strong><c:out value="${report.failed}"/></strong></article></div>
<a class="btn btn-outline-success" href="<c:url value='/shipper/orders'/>">Xem đơn được phân công →</a></section></body></html>
