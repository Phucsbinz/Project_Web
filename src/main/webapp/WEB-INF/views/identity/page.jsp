<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Tài khoản và thành viên · UTETra</title>
<link rel="stylesheet" href="<c:url value='/assets/css/identity-profile.css?v=20261023c'/>">
<script defer src="<c:url value='/assets/js/identity.js'/>"></script></head><body>
<c:choose><c:when test="${identityPage.startsWith('/auth/')}"><%@ include file="auth.jspf" %></c:when><c:otherwise>
<div id="identity" data-page="<c:out value='${identityPage}'/>">

<c:if test="${not empty identityRole}"><div class="identity-hero"><div><span class="eyebrow">${workspace ? 'PEOPLE & ACCESS' : 'YOUR UTETRA CORNER'}</span><h1>${workspace ? 'Người dùng & đội ngũ.' : 'Góc nhỏ của bạn.'}</h1><p>${workspace ? 'Quản lý tài khoản và phân quyền trong phạm vi của bạn.' : 'Lưu những điều thân quen. Thêm niềm vui mỗi lần trở lại.'}</p></div><span class="member-mark" aria-hidden="true"><svg class="icon"><use href="#i-leaf"/></svg></span></div><nav class="identity-nav d-flex flex-wrap gap-2 mb-4" aria-label="Tài khoản">
<a class="btn btn-outline-success btn-sm" href="<c:url value='/account/profile'/>">Hồ sơ</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/account/security'/>">Mật khẩu và bảo mật</a>
<c:if test="${buyer}">
<a class="btn btn-outline-success btn-sm" href="<c:url value='/account/addresses'/>">Địa chỉ</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/account/loyalty'/>">Điểm thành viên</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/favorites'/>">Yêu thích</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/viewed'/>">Đã xem</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/reviews'/>">Đánh giá</a>
<a class="btn btn-outline-success btn-sm" href="<c:url value='/comments'/>">Bình luận</a>
</c:if>
<c:if test="${identityRole == 'ROLE_MANAGER'}"><a class="btn btn-outline-success btn-sm" href="<c:url value='/manager/users'/>">Quản lý người dùng</a></c:if>
<c:if test="${identityRole == 'ROLE_ADMIN'}"><a class="btn btn-outline-success btn-sm" href="<c:url value='/admin/users'/>">Quản trị người dùng</a></c:if>
</nav></c:if>
<div id="identity-message" class="alert d-none" role="status" aria-live="polite"></div>
<c:if test="${identityPage == '/account/profile'}"><section class="profile-banner"><div class="profile-banner-heading"><span class="eyebrow">HỒ SƠ CÁ NHÂN</span><h2 id="profile-display-name">Tài khoản của bạn</h2><p id="profile-display-email"></p></div><div class="avatar-preview-wrap"><img id="avatar-preview" alt="Ảnh đại diện hiện tại" hidden><span id="avatar-placeholder" aria-label="Chưa có ảnh đại diện">UT</span><p id="avatar-preview-note">Ảnh đại diện hiện tại. Chọn ảnh để xem trước.</p></div>
</section></c:if>
<div class="row g-4"><section class="col-12 col-lg-5 identity-editor"><div class="card shadow-sm border-0"><div class="card-body p-4">
<c:choose>
<c:when test="${identityPage == '/account/profile'}">
<h1 class="h3">Hồ sơ của bạn</h1>
<form data-endpoint="/account/profile/save" data-multipart id="profile-form"><input name="version" type="hidden" data-number>
<label class="form-label" for="fullName">Họ tên</label><input class="form-control mb-3" id="fullName" name="fullName" required maxlength="100" autocomplete="name">
<label class="form-label" for="phone">Điện thoại</label><input class="form-control mb-3" id="phone" name="phone" maxlength="20" autocomplete="tel"><label class="form-label" for="avatar">Ảnh đại diện mới</label><input class="form-control mb-2" id="avatar" name="file" type="file" accept="image/jpeg,image/png"><p class="text-secondary small">JPEG/PNG, tối đa 5MB. Ảnh chỉ được cập nhật khi lưu hồ sơ.</p><button class="btn btn-success">Lưu hồ sơ</button></form>
<a class="security-setting security-page-link" href="<c:url value='/account/security'/>"><span><strong>Mật khẩu và bảo mật</strong><small>Đổi mật khẩu và hỗ trợ khôi phục tài khoản</small></span><span aria-hidden="true">›</span></a>
<div class="security-session"><div><strong>Phiên đăng nhập</strong><p class="small text-secondary mb-0">Đăng xuất tài khoản khỏi trình duyệt này.</p></div><button class="btn btn-outline-danger" id="logout">Đăng xuất</button></div>
</c:when>
<c:when test="${identityPage == '/account/security'}"><section class="profile-security" aria-labelledby="security-title"><h1 id="security-title" class="h3">Mật khẩu và bảo mật</h1><p class="text-secondary small">Quản lý cách đăng nhập và bảo vệ tài khoản của bạn.</p>
<details class="security-setting" open><summary><span><strong>Đổi mật khẩu</strong><small>Cập nhật mật khẩu để bảo vệ tài khoản</small></span><span aria-hidden="true">›</span></summary>
<form data-endpoint="/account/password" id="password-settings-form" class="pt-3">
<label class="form-label" for="currentPassword">Mật khẩu hiện tại</label><input class="form-control mb-3" id="currentPassword" name="currentPassword" type="password" required autocomplete="current-password">
<a class="d-inline-block mb-3 small" href="<c:url value='/auth/forgot'/>">Quên mật khẩu hiện tại?</a><br><label class="form-label" for="password">Mật khẩu mới</label><input class="form-control mb-3" id="password" name="password" type="password" required minlength="8" maxlength="72" autocomplete="new-password">
<label class="form-label" for="confirmPassword">Nhập lại mật khẩu mới</label><input class="form-control mb-3" id="confirmPassword" name="confirmPassword" type="password" required autocomplete="new-password">
<label class="d-flex gap-2 align-items-center mb-3 small"><input type="checkbox" id="show-profile-password">Hiện mật khẩu</label>
<p class="small text-secondary">Dùng ít nhất 8 ký tự, tối đa 72 byte UTF-8. Sau khi đổi, các phiên đăng nhập sẽ kết thúc và bạn cần đăng nhập lại.</p>
<button class="btn btn-success">Lưu mật khẩu mới</button></form></details>

</section><a class="d-inline-block mt-4 small" href="<c:url value='/account/profile'/>">← Quay lại hồ sơ</a></c:when>
<c:when test="${identityPage == '/account/addresses'}">
<h1 class="h3">Địa chỉ nhận hàng</h1><p class="text-secondary">Thêm nhiều địa chỉ và chọn địa chỉ mặc định.</p>
<form data-endpoint="/account/addresses" id="address-form"><label class="form-label" for="recipientName">Người nhận</label><input class="form-control mb-3" id="recipientName" name="recipientName" required maxlength="100" autocomplete="name">
<label class="form-label" for="phone">Điện thoại</label><input class="form-control mb-3" id="phone" name="phone" required pattern="\+?[0-9 \(\)\-]{8,20}" maxlength="20" inputmode="tel" autocomplete="tel">
<label class="form-label" for="addressDetail">Địa chỉ chi tiết</label><textarea class="form-control mb-3" id="addressDetail" name="addressDetail" required maxlength="500" autocomplete="street-address"></textarea>
<div class="form-check mb-3"><input class="form-check-input" id="isDefault" name="isDefault" type="checkbox"><label class="form-check-label" for="isDefault">Địa chỉ mặc định</label></div><button class="btn btn-success">Lưu địa chỉ</button><button class="btn btn-link" type="reset">Thêm mới</button></form>
</c:when>
<c:when test="${identityPage == '/reviews' || identityPage == '/comments'}">
<h1 class="h3">${identityPage == '/reviews' ? 'Đánh giá sản phẩm' : 'Bình luận sản phẩm'}</h1><p class="text-secondary">Chia sẻ trải nghiệm từ đơn đã giao và đã thanh toán.</p>
<form data-endpoint="${identityPage}" data-multipart id="community-form"><input name="productId" type="hidden">
<label class="form-label" for="orderItemId">Sản phẩm đã mua</label><select class="form-select mb-3" id="orderItemId" name="orderItemId" required><option value="">Đang tải sản phẩm…</option></select>
<div class="d-flex flex-wrap align-items-center gap-2 mb-2" aria-label="Trang sản phẩm đã mua"><button type="button" class="btn btn-outline-secondary" id="purchase-prev" disabled>Trang mua trước</button><span id="purchase-page-label">Trang 1</span><button type="button" class="btn btn-outline-secondary" id="purchase-next" disabled>Trang mua sau</button><button type="button" class="btn btn-outline-primary" id="purchase-retry" hidden>Thử tải lại</button></div><p id="purchase-status" class="small text-secondary" role="status" aria-live="polite">Đang tải sản phẩm đã mua…</p>
<c:if test="${identityPage == '/reviews'}"><label class="form-label" for="stars">Số sao</label><select class="form-select mb-3" id="stars" name="stars"><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select></c:if>
<label class="form-label" for="content">Nội dung ${identityPage == '/comments' ? '(50–2000 ký tự)' : '(tối đa 2000 ký tự)'}</label><textarea class="form-control mb-3" id="content" name="content" maxlength="2000" minlength="${identityPage == '/comments' ? '50' : '0'}" ${identityPage == '/comments' ? 'required' : ''} rows="5"></textarea>
<label class="form-label" for="files">Ảnh/video (tối đa 5 tệp; ảnh 5MB, MP4 20MB)</label><input class="form-control mb-3" id="files" name="files" type="file" multiple accept="image/jpeg,image/png,video/mp4"><button type="submit" class="btn btn-success">Gửi trải nghiệm</button></form>
</c:when>
<c:when test="${identityPage == '/admin/users' || identityPage == '/manager/users'}">
<h1 class="h3">Quản lý người dùng</h1><p class="text-secondary">Tìm tài khoản và cập nhật trạng thái theo phạm vi quản lý.</p>
<form id="user-search"><label class="form-label" for="q">Tên hoặc email</label><input class="form-control mb-3" id="q" name="q" maxlength="100"><button class="btn btn-success">Tìm kiếm</button></form>
<c:if test="${identityPage == '/admin/users'}"><hr><h2 class="h5">Phân công Manager</h2><form id="assignment-form"><label class="form-label" for="managerId">Mã Manager</label><input class="form-control mb-2" name="managerId" id="managerId" type="number" min="1" required><label class="form-label" for="storeId">Mã cửa hàng</label><input class="form-control mb-2" name="storeId" id="storeId" type="number" min="1" required><div class="form-check mb-3"><input class="form-check-input" type="checkbox" name="active" id="assignmentActive" checked><label class="form-check-label" for="assignmentActive">Phân công có hiệu lực</label></div><button class="btn btn-outline-success">Lưu phân công</button></form></c:if>
</c:when>
<c:otherwise><h1 id="identity-title" class="h3">Thành viên UTETra</h1><p class="text-secondary">Dành riêng cho hành trình thưởng thức trà của bạn.</p></c:otherwise>
</c:choose>
<c:if test="${empty identityRole}"><a class="d-block mt-4 small" href="<c:url value='/auth/login'/>">Đã có tài khoản? Đăng nhập</a></c:if>
</div></div></section><section class="col-12 col-lg-7 identity-results" aria-label="Thông tin của bạn"><div id="identity-content" class="d-grid gap-3" aria-live="polite"></div>
<div id="identity-paging" class="d-none d-flex gap-2 mt-3"><button class="btn btn-outline-secondary" id="prev-page">Trang trước</button><span id="page-label" class="align-self-center"></span><button class="btn btn-outline-secondary" id="next-page">Trang sau</button></div>
</section></div></div></c:otherwise></c:choose></body></html>
