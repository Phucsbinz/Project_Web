<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<nav aria-label="Chức năng đặt hàng" class="nav nav-pills flex-wrap gap-2 mb-4">
<c:if test="${orderRole == 'GUEST' || orderRole == 'ROLE_USER' || orderRole == 'ROLE_VENDOR'}"><a class="nav-link" href="<c:url value='/cart'/>">Giỏ hàng</a></c:if>
<c:if test="${orderRole == 'ROLE_USER' || orderRole == 'ROLE_VENDOR'}"><a class="nav-link" href="<c:url value='/orders'/>">Đơn mua của tôi</a></c:if>
<c:if test="${orderRole != 'GUEST'}"><a class="nav-link" href="<c:url value='/notifications'/>">Thông báo</a></c:if>
<c:if test="${orderRole == 'ROLE_VENDOR'}"><a class="nav-link" href="<c:url value='/vendor/orders'/>">Đơn của cửa hàng</a><a class="nav-link" href="<c:url value='/vendor/revenue'/>">Doanh thu cửa hàng</a></c:if>
<c:if test="${orderRole == 'ROLE_MANAGER'}"><a class="nav-link" href="<c:url value='/manager/orders'/>">Vận hành đơn</a><a class="nav-link" href="<c:url value='/manager/order-tools'/>">Phí giao / hoa hồng</a><a class="nav-link" href="<c:url value='/manager/reports'/>">Báo cáo</a></c:if>
<c:if test="${orderRole == 'ROLE_ADMIN'}"><a class="nav-link" href="<c:url value='/admin/orders'/>">Đơn toàn hệ thống</a><a class="nav-link" href="<c:url value='/admin/order-tools'/>">Vận chuyển / hoa hồng</a><a class="nav-link" href="<c:url value='/admin/reports'/>">Báo cáo hệ thống</a></c:if>
<c:if test="${orderRole == 'ROLE_SHIPPER'}"><a class="nav-link" href="<c:url value='/shipper/orders'/>">Đơn giao của tôi</a><a class="nav-link" href="<c:url value='/shipper/reports'/>">Thống kê giao hàng</a></c:if>
</nav>
