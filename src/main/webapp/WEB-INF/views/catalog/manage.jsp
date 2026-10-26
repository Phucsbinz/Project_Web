<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Quản lý bán hàng · UTETra</title></head><body>
<div id="catalog-manager" data-role="${rolePath}" data-section="${catalogSection}"><span class="eyebrow">YOUR BUSINESS, THOUGHTFULLY MANAGED</span><h1>Quản lý bán hàng</h1><p>Quản lý dữ liệu theo quyền và phạm vi shop được giao.</p>
<nav id="catalog-tabs" class="d-flex flex-wrap gap-2 mb-4" aria-label="Chức năng quản lý"></nav>
<div class="catalog-scope-picker d-flex flex-wrap gap-3 align-items-center mb-4"><label for="managed-store" class="form-label mb-0">Shop</label><select id="managed-store" class="form-select" style="max-width:420px" aria-label="Shop quản lý"></select><button id="catalog-reload" type="button" class="btn btn-outline-dark">Tải lại</button></div>
<div id="management-message" role="status" aria-live="polite" class="mb-3"></div>
<div class="catalog-workspace"><section class="catalog-records"><h2 id="catalog-list-title" class="h4">Danh sách</h2><div id="management-list" class="vstack gap-3"></div></section>
<section class="card p-4"><h2 id="catalog-form-title" class="h4">Tạo mới</h2><form id="management-form" class="vstack gap-3"></form></section></div>
<section id="image-panel" class="card p-4 mt-4" hidden><h2 class="h5">Ảnh sản phẩm / shop</h2><form id="catalog-image" class="d-flex gap-3 flex-wrap align-items-end"><div><label for="image-type" class="form-label">Vị trí</label><select id="image-type" class="form-select"><option value="product">Ảnh sản phẩm</option><option value="store">Ảnh shop</option><option value="banner">Banner shop</option></select></div><div><label for="image-file" class="form-label">Ảnh JPEG/PNG, tối đa 5 MB</label><input class="form-control" id="image-file" type="file" accept="image/jpeg,image/png" required></div><button class="btn btn-outline-dark">Tải ảnh cho mục đang sửa</button></form></section>
</div><script src="<c:url value='/assets/js/catalog-management.js'/>" defer></script></body></html>
