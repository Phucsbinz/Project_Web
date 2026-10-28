<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Phạm vi Shipper · UTETra</title></head><body>
<section class="order-page" id="shipper-scopes">
<h1 class="h3">Phạm vi phục vụ Shipper</h1>
<p>Cấp quyền phục vụ chi nhánh trước khi phân công từng đơn. Thu hồi phạm vi chặn quyền xem và cập nhật đơn; lịch sử phân công vẫn được lưu.</p>
<div class="alert alert-danger d-none" role="alert" id="scope-error"></div>
<form class="card card-body mb-4" id="scope-form">
<div class="row g-3"><div class="col-md-6"><label for="scope-shipper" class="form-label">Shipper</label><select id="scope-shipper" class="form-select" required><c:forEach items="${shippers}" var="s"><option value="${s.id}"><c:out value="${s.full_name}"/></option></c:forEach></select></div>
<div class="col-md-6"><label for="scope-store" class="form-label">Chi nhánh</label><select id="scope-store" class="form-select" required><c:forEach items="${stores}" var="s"><option value="${s.id}"><c:out value="${s.name}"/></option></c:forEach></select></div>
<div class="col-12"><label for="scope-reason" class="form-label">Lý do cấp quyền</label><input id="scope-reason" class="form-control" required maxlength="500"></div></div><button class="btn btn-primary mt-3 align-self-start" type="submit">Cấp phạm vi phục vụ</button></form>
<div class="table-responsive"><table class="table"><thead><tr><th>Shipper</th><th>Chi nhánh</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody><c:forEach items="${scopes}" var="s"><tr data-shipper="${s.shipper_id}" data-store="${s.store_id}" data-version="${s.version}"><td><c:out value="${s.full_name}"/></td><td><c:out value="${s.store_name}"/></td><td><c:out value="${s.is_active ? 'Đang phục vụ' : 'Đã thu hồi'}"/></td><td><c:if test="${s.is_active}"><label class="visually-hidden" for="revoke-${s.id}">Lý do thu hồi</label><input id="revoke-${s.id}" class="form-control mb-2" maxlength="500" placeholder="Lý do thu hồi" required><button class="btn btn-outline-danger" type="button" data-revoke>Thu hồi</button></c:if></td></tr></c:forEach></tbody></table></div>
</section><script src="<c:url value='/assets/js/shipper-scopes.js'/>"></script></body></html>
