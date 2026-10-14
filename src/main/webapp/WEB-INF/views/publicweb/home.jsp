<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><title>UTETra — Trà sữa theo cách của bạn</title></head><body>
<section class="hero p-4 p-md-5 mb-4 rounded-4">
  <span class="badge text-bg-light mb-3">UTETra</span>
  <h1 class="display-5 fw-bold">Trà sữa theo cách của bạn</h1>
  <p class="lead mb-0">Chọn chi nhánh, chọn món và tùy biến ly trà sữa yêu thích.</p>
</section>
<div class="alert alert-info">Đây là trang kiểm chứng nền tảng dùng chung. Thực đơn và các chức năng bán hàng sẽ được triển khai trên các nhánh chức năng.</div>
<section aria-labelledby="stack-heading"><h2 id="stack-heading" class="h4 mb-3">Nền tảng UTETra</h2>
<div class="row row-cols-1 row-cols-sm-2 row-cols-lg-4 g-3">
<c:forEach items="${stack}" var="item"><div class="col"><div class="card h-100 border-0 shadow-sm"><div class="card-body"><h3 class="h6 mb-0"><c:out value="${item}"/></h3></div></div></div></c:forEach>
</div></section>
</body></html>
