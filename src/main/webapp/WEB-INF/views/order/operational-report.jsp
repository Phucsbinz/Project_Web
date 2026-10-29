<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Báo cáo vận hành · UTETra</title></head><body><section class="order-page">
<h1 class="h3">Báo cáo doanh thu và vận hành</h1><p>Toàn bộ doanh thu COD và QR trong các chi nhánh được phân công. Tiền COD thu hộ được theo dõi riêng.</p>
<form method="get" class="row g-3 mb-4"><div class="col-md-4"><label class="form-label" for="op-store">Chi nhánh</label><select name="store" id="op-store" class="form-select"><option value="">Tất cả chi nhánh được giao</option><c:forEach items="${stores}" var="s"><option value="${s.id}" ${param.store == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option></c:forEach></select></div><div class="col-md-3"><label for="op-from" class="form-label">Từ ngày</label><input type="date" id="op-from" name="from" value="${report.from}" class="form-control" required></div><div class="col-md-3"><label for="op-to" class="form-label">Đến ngày</label><input type="date" id="op-to" name="to" value="${report.to}" class="form-control" required></div><div class="col-md-2 align-self-end"><button class="btn btn-primary">Xem báo cáo</button></div></form>
<div class="row g-3"><div class="col-md-4"><div class="card card-body"><h2 class="h6">Tổng đơn</h2><strong><c:out value="${report.orders}"/></strong></div></div><div class="col-md-4"><div class="card card-body"><h2 class="h6">Đã giao</h2><strong><c:out value="${report.delivered}"/></strong></div></div><div class="col-md-4"><div class="card card-body"><h2 class="h6">Đã hủy</h2><strong><c:out value="${report.cancelled}"/></strong></div></div><div class="col-md-6"><div class="card card-body"><h2 class="h6">COD đã thu</h2><strong><c:out value="${report.codCollected}"/> ₫</strong></div></div><div class="col-md-6"><div class="card card-body"><h2 class="h6">COD còn cần thu</h2><strong><c:out value="${report.codDue}"/> ₫</strong></div></div></div>
<h2 class="h4 mt-4">Doanh thu bán hàng · COD và QR</h2>
<p>Doanh thu ghi theo ngày giao thành công; tiền hoàn ghi theo ngày hoàn tất. Không tính đơn chưa hoàn tất. Số đơn vận hành phía trên tính theo ngày tạo đơn.</p>
<dl class="row card-body border rounded">
<dt class="col-sm-8">Đơn ghi nhận doanh thu</dt><dd class="col-sm-4">${report.salesOrders}</dd>
<dt class="col-sm-8">Tiền món sau giảm giá</dt><dd class="col-sm-4" data-money="${report.goods}"></dd>
<dt class="col-sm-8">Phí giao</dt><dd class="col-sm-4" data-money="${report.shipping}"></dd>
<dt class="col-sm-8">Hoa hồng nền tảng</dt><dd class="col-sm-4" data-money="${report.commission}"></dd>
<dt class="col-sm-8">Tiền đã hoàn</dt><dd class="col-sm-4" data-money="${report.refund}"></dd>
<dt class="col-sm-8">Hoa hồng được thu hồi</dt><dd class="col-sm-4" data-money="${report.commissionReversed}"></dd>
<dt class="col-sm-8">Doanh thu hàng hóa ròng sau hoa hồng và hoàn</dt><dd class="col-sm-4" data-money="${report.storeNet}"></dd>
</dl>
</section></body></html>
