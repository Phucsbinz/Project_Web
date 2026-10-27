/* Identity forms use the shared JWT cookie and the CSRF token rendered by SiteMesh. */
(() => {
  'use strict';
  const root = document.getElementById('identity');
  if (!root) return;
  const page = root.dataset.page;
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  const output = document.getElementById('identity-content');
  const message = document.getElementById('identity-message');
  let currentPage = 0, query = '', rows = [];
  // Keep the email within this tab; never put it or a reset grant in a URL.
  const pendingKey = purpose => `utetra.otp.${purpose}`;
  function rememberEmail(purpose, email) {
    try { sessionStorage.setItem(pendingKey(purpose), email); } catch (_) { /* The OTP page still accepts email manually. */ }
  }
  let avatarObjectUrl;
  const avatarInput = document.getElementById('avatar');
  avatarInput?.addEventListener('change', () => {
    if (avatarObjectUrl) URL.revokeObjectURL(avatarObjectUrl);
    const file = avatarInput.files[0];
    if (!file) { load(); return; }
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      avatarInput.value = ''; say('Chọn ảnh JPEG/PNG tối đa 5MB.', true); load(); return;
    }
    const img = document.getElementById('avatar-preview');
    avatarObjectUrl = URL.createObjectURL(file); img.src = avatarObjectUrl; img.hidden = false;
    document.getElementById('avatar-placeholder').hidden = true;
    document.getElementById('avatar-preview-note').textContent = 'Ảnh xem trước — bấm Cập nhật ảnh để lưu.';
    img.onerror = () => { img.hidden = true; document.getElementById('avatar-placeholder').hidden = false; avatarInput.value = ''; say('Không đọc được ảnh. Vui lòng chọn ảnh khác.', true); };
  });
  document.getElementById('show-profile-password')?.addEventListener('change', event => {
    document.querySelectorAll('#password-settings-form input[autocomplete$="password"]').forEach(input => { input.type = event.target.checked ? 'text' : 'password'; });
  });
  const otpForm = document.getElementById('otp-form');
  if (otpForm) {
    try { otpForm.elements.email.value = sessionStorage.getItem(pendingKey(otpForm.elements.purpose.value)) || ''; } catch (_) { /* Storage may be disabled. */ }
    // Mail clients can include invisible characters or spacing when copying the code.
    // Handle paste before maxlength truncates it, preserving leading zeroes.
    otpForm.elements.otp.addEventListener('paste', event => {
      const pasted = event.clipboardData?.getData('text');
      if (pasted === undefined) return;
      const normalized = pasted.replace(/[\s\u200B-\u200D\uFEFF]/g, '');
      if (/^[0-9]{6}$/.test(normalized)) {
        event.preventDefault(); otpForm.elements.otp.value = normalized;
        otpForm.elements.otp.dispatchEvent(new Event('input', {bubbles: true}));
      }
    });
  }
  function completeAuth() {
    document.getElementById('otp-form')?.classList.add('d-none');
    document.getElementById('reset-form')?.classList.add('d-none');
    document.getElementById('auth-back')?.classList.add('d-none');
    document.getElementById('auth-complete')?.classList.remove('d-none');
    document.getElementById('auth-flow-title').textContent = page === '/auth/otp' ? 'Tài khoản đã sẵn sàng!' : 'Đã đổi mật khẩu';
    document.getElementById('auth-flow-intro').textContent = 'Đăng nhập để tiếp tục trải nghiệm UTETra.';
    document.querySelectorAll('.auth-steps li').forEach(step => { step.className = 'is-complete'; step.removeAttribute('aria-current'); });
    try { sessionStorage.removeItem(pendingKey(otpForm.elements.purpose.value)); } catch (_) { /* No persisted secret. */ }
    document.getElementById('identity-message').focus();
  }
  const say = (text, error = false) => {
    message.className = `alert ${error ? 'alert-danger' : 'alert-success'}`;
    message.textContent = text;
  };
  async function request(path, method = 'GET', data) {
    const headers = {Accept: 'application/json'};
    const csrf = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (csrf && csrfHeader) headers[csrfHeader] = csrf;
    let body;
    if (data instanceof FormData) body = data;
    else if (data !== undefined) { body = JSON.stringify(data); headers['Content-Type'] = 'application/json'; }
    let res;
    try {
      res = await fetch(context + path, {method, headers, body, credentials: 'same-origin', cache: 'no-store'});
    } catch (_) {
      throw new Error('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối và thử lại sau.');
    }
    if (path === '/auth/login' && res.status === 401) {
      throw new Error('Email hoặc mật khẩu không đúng. Vui lòng thử lại hoặc chọn Quên mật khẩu.');
    }
    if (res.redirected || !res.headers.get('Content-Type')?.includes('application/json')) {
      throw new Error('Phiên đăng nhập đã hết hạn hoặc quyền truy cập không hợp lệ. Vui lòng đăng nhập lại.');
    }
    const result = await res.json();
    if (!res.ok) throw new Error(result.message || (res.status === 403 ? 'Yêu cầu không được phép. Hãy tải lại trang và thử lại.' : 'Không thể xử lý yêu cầu.'));
    return result;
  }
  const run = async action => { try { await action(); } catch (error) { say(error.message, true); } };
  function fieldData(form) {
    const values = Object.fromEntries(new FormData(form));
    form.querySelectorAll('[data-number]').forEach(e => { values[e.name] = Number(e.value); });
    form.querySelectorAll('input[type="checkbox"]').forEach(e => { values[e.name] = e.checked; });
    for (const name of ['username', 'email', 'fullName', 'phone', 'recipientName', 'addressDetail', 'otp']) {
      if (typeof values[name] === 'string') values[name] = values[name].trim();
    }
    delete values.confirmPassword;
    return values;
  }
  function validateForm(form) {
    form.querySelectorAll('input,textarea,select').forEach(input => input.setCustomValidity(''));
    for (const [name, max] of [['fullName',100],['recipientName',100],['addressDetail',500]]) {
      const input = form.querySelector(`[name="${name}"]`);
      if (input && (![...input.value.trim()].length || [...input.value.trim()].length > max)) input.setCustomValidity(`Nội dung cần 1–${max} ký tự sau khi bỏ khoảng trắng đầu/cuối.`);
    }
    for (const input of form.querySelectorAll('[name="password"],[name="currentPassword"]')) {
      const size = new TextEncoder().encode(input.value).length;
      if (!input.value.trim() || size > 72 || /[\x00-\x1f\x7f]/.test(input.value) || input.name === 'password' && input.minLength >= 8 && [...input.value].length < 8) {
        input.setCustomValidity('Mật khẩu mới cần ít nhất 8 ký tự; tối đa 72 byte UTF-8, không chứa ký tự điều khiển.');
      }
    }
    const confirmation = form.querySelector('[name="confirmPassword"]');
    if (confirmation && confirmation.value !== form.elements.password.value) confirmation.setCustomValidity('Mật khẩu nhập lại chưa khớp.');
    const phone = form.querySelector('[name="phone"]');
    if (phone && phone.value.trim()) {
      const value = phone.value.trim(), digits = value.replace(/\D/g, '').length;
      if (!/^\+?[0-9 ()-]+$/.test(value) || value.length > 20 || digits < 8 || digits > 15) phone.setCustomValidity('Số điện thoại cần 8–15 chữ số và tối đa 20 ký tự.');
    }
    const content = form.querySelector('[name="content"]');
    if (content) {
      const length = [...content.value.trim()].length, min = page === '/comments' ? 50 : 0;
      if (length < min || length > 2000) content.setCustomValidity(`Nội dung cần từ ${min} đến 2000 ký tự sau khi bỏ khoảng trắng đầu/cuối.`);
    }
    for (const input of form.querySelectorAll('input[type="file"]')) {
      const files = [...input.files].filter(f => f.name);
      if (files.length > 5 || files.reduce((sum, f) => sum + f.size, 0) > 25 * 1024 * 1024) input.setCustomValidity('Tối đa 5 tệp và tổng dung lượng request tối đa 25MB.');
      for (const file of files) {
        const image = ['image/jpeg', 'image/png'].includes(file.type), video = file.type === 'video/mp4' && input.name === 'files';
        if (!file.size || !image && !video || file.size > (image ? 5 : 20) * 1024 * 1024) input.setCustomValidity('Chỉ nhận ảnh JPEG/PNG tối đa 5MB hoặc MP4 tối đa 20MB; tệp không được rỗng.');
      }
    }
    return form.reportValidity();
  }
  function text(tag, content, css) {
    const e = document.createElement(tag); e.textContent = content ?? ''; if (css) e.className = css; return e;
  }
  function button(label, action, css = 'btn btn-outline-success btn-sm') {
    const b = text('button', label, css); b.type = 'button';
    b.addEventListener('click', () => run(async () => { b.disabled = true; try { await action(); } finally { b.disabled = false; } }));
    return b;
  }
  function card(title) {
    const article = text('article', '', 'card border-0 shadow-sm');
    const body = text('div', '', 'card-body p-4'); body.append(text('h2', title, 'h5')); article.append(body); output.append(article); return body;
  }
  function fill(form, data) {
    for (const [key, value] of Object.entries(data)) {
      const name = key.replace(/_([a-z])/g, (_, x) => x.toUpperCase());
      const input = form.elements.namedItem(name);
      if (input) { if (input.type === 'checkbox') input.checked = Boolean(value); else input.value = value ?? ''; }
    }
  }
  document.querySelectorAll('form[data-endpoint]').forEach(form => {
    // Recompute custom rules on every submission, including after correcting an earlier invalid value.
    form.noValidate = true;
    for (const [name,max] of [['content',2000],['fullName',100],['recipientName',100],['addressDetail',500]]) {
      const input = form.querySelector(`[name="${name}"]`); if (input) input.maxLength = max * 2;
    }
    form.querySelectorAll('input,textarea,select').forEach(input => input.addEventListener('input', () => input.setCustomValidity('')));
    form.addEventListener('submit', event => {
      event.preventDefault();
      if (!validateForm(form)) return;
      run(async () => {
        const submit = form.querySelector('button[type="submit"],button:not([type])'); submit.disabled = true;
        try {
          const result = await request(form.dataset.endpoint, form.dataset.method || 'POST', form.hasAttribute('data-multipart') ? new FormData(form) : fieldData(form));
          if (form.id === 'profile-form') avatarInput.value = '';
          say(result.message || 'Đã lưu.');
          if (['/auth/register', '/auth/forgot'].includes(form.dataset.endpoint)) {
            const register = form.dataset.endpoint === '/auth/register';
            rememberEmail(register ? 'REGISTER' : 'PASSWORD_RESET', form.elements.email.value.trim());
            window.location.assign(context + (register ? '/auth/otp' : '/auth/reset')); return;
          }
          if (result.grant) {
            const reset = document.getElementById('reset-form'); reset.classList.remove('d-none'); reset.elements.grant.value = result.grant;
            reset.querySelector('[type="password"]').focus(); form.classList.add('d-none');
            document.getElementById('auth-flow-title').textContent = 'Tạo mật khẩu mới';
            document.getElementById('auth-flow-intro').textContent = 'Email đã được xác minh. Chọn mật khẩu mới để bảo vệ tài khoản của bạn.';
            const steps = document.querySelectorAll('.auth-steps li');
            steps[1].className = 'is-complete'; steps[1].removeAttribute('aria-current');
            steps[2].className = 'is-current'; steps[2].setAttribute('aria-current', 'step');
          }
          if ((form.id === 'otp-form' && !result.grant) || form.dataset.endpoint === '/auth/reset') completeAuth();
          if (result.redirect) {
            const next=new URLSearchParams(location.search).get('next');
            const invite=form.dataset.endpoint==='/auth/login' && /^\/group-orders\/(new|[0-9a-fA-F-]{36})$/.test(next||'');
            window.location.assign(invite?context+next:result.redirect); return;
          }
          if (form.dataset.endpoint === '/auth/reset') { form.reset(); form.elements.grant.value = ''; form.classList.add('d-none'); }
          if (!page.startsWith('/auth/')) await load();
          if (form.id === 'address-form') { form.reset(); delete form.dataset.method; form.dataset.endpoint = '/account/addresses'; }
          if (form.id === 'community-form') form.elements.content.value = '';
        } finally { submit.disabled = false; }
      });
    });
  });
  document.getElementById('address-form')?.addEventListener('reset', event => {
    event.target.dataset.endpoint = '/account/addresses'; delete event.target.dataset.method;
  });
  document.getElementById('resend-otp')?.addEventListener('click', event => run(async () => {
    const f = document.getElementById('otp-form');
    if (!f.elements.email.reportValidity()) return;
    const button = event.currentTarget; button.disabled = true;
    try {
      const result = await request('/auth/otp/send', 'POST', {email: f.elements.email.value.trim(), purpose: f.elements.purpose.value}); say(result.message);
      rememberEmail(f.elements.purpose.value, f.elements.email.value.trim());
      let seconds = 60; button.textContent = `Gửi lại sau ${seconds}s`;
      const timer = setInterval(() => {
        seconds--; button.textContent = seconds ? `Gửi lại sau ${seconds}s` : 'Gửi lại OTP';
        if (!seconds) { clearInterval(timer); button.disabled = false; }
      }, 1000);
    } catch (error) { button.disabled = false; throw error; }
  }));
  document.getElementById('logout')?.addEventListener('click', () => run(async () => {
    const result = await request('/auth/logout', 'POST'); window.location.assign(result.redirect);
  }));
  document.getElementById('refresh-session')?.addEventListener('click', () => run(async () => { say((await request('/auth/refresh', 'POST')).message); }));
  document.getElementById('user-search')?.addEventListener('submit', e => {
    e.preventDefault(); query = e.target.elements.q.value; currentPage = 0; run(load);
  });
  document.getElementById('assignment-form')?.addEventListener('submit', e => {
    e.preventDefault(); run(async () => {
      const f = e.target.elements;
      say((await request(`/admin/users/${Number(f.managerId.value)}/assignments`, 'POST', {storeId: Number(f.storeId.value), active: f.active.checked})).message);
    });
  });
  document.getElementById('prev-page')?.addEventListener('click', () => { currentPage = Math.max(0, currentPage - 1); run(load); });
  document.getElementById('next-page')?.addEventListener('click', () => { currentPage++; run(load); });
  function address(row) {
    const body = card(row.recipient_name + (row.is_default ? ' · Mặc định' : ''));
    body.append(text('p', row.phone), text('p', row.address_detail));
    body.append(button('Sửa địa chỉ', async () => {
      const f = document.getElementById('address-form'); fill(f, row); f.dataset.endpoint = `/account/addresses/${row.id}`; f.dataset.method = 'PUT'; f.scrollIntoView({behavior: 'smooth'});
    }), button('Xóa', async () => { if (await window.utetraConfirm('Xóa địa chỉ này?')) { await request(`/account/addresses/${row.id}`, 'DELETE'); await load(); } }, 'btn btn-outline-danger btn-sm ms-2'));
  }
  function product(row) {
    const body = card(row.name);
    body.append(text('p', `${Number(row.base_price).toLocaleString('vi-VN')} ₫`));
    const link = text('a', 'Xem sản phẩm', 'btn btn-outline-success btn-sm'); link.href = context + `/products/${Number(row.id)}`; body.append(link);
    if (page === '/favorites') body.append(button('Bỏ thích', async () => { await request(`/favorites/${row.id}`, 'DELETE'); await load(); }, 'btn btn-outline-secondary btn-sm ms-2'));
  }
  function entry(row) {
    const body = card(page === '/reviews' ? `${row.stars} / 5 sao` : 'Bình luận của bạn');
    body.append(text('p', row.content), text('p', row.status === 'VISIBLE' ? 'Đang hiển thị' : 'Đã ẩn', 'text-secondary small'));
    for (const asset of row.media || []) {
      if (!asset.secure_url?.startsWith('https://')) continue;
      const element = document.createElement(asset.resource_type === 'video' ? 'video' : 'img');
      element.src = asset.secure_url; element.className = 'img-fluid rounded mb-2'; element.style.maxHeight = '240px';
      if (element.tagName === 'VIDEO') element.controls = true; else { element.alt = 'Ảnh trải nghiệm sản phẩm'; element.loading = 'lazy'; }
      body.append(element);
    }
    if (row.status !== 'VISIBLE') return;
    const edit = document.createElement('textarea'); edit.value = row.content; edit.className = 'form-control my-2'; edit.maxLength = 4000; edit.setAttribute('aria-label', 'Sửa nội dung'); body.append(edit);
    let rating;
    if (page === '/reviews') {
      rating = document.createElement('select'); rating.className = 'form-select mb-2'; rating.setAttribute('aria-label', 'Số sao');
      for (let i = 1; i <= 5; i++) { const o = text('option', `${i} sao`); o.value = i; rating.append(o); } rating.value = row.stars; body.append(rating);
    }
    body.append(button('Lưu chỉnh sửa', async () => {
      const length = [...edit.value.trim()].length;
      if (length < (page === '/comments' ? 50 : 0) || length > 2000) throw new Error('Nội dung bình luận cần 50–2000 ký tự; đánh giá tối đa 2000 ký tự.');
      say((await request(`${page}/${row.id}`, 'PUT', {content: edit.value, stars: Number(rating?.value || 0), version: row.version})).message); await load();
    }),
      button('Gỡ nội dung', async () => { if (await window.utetraConfirm('Gỡ nội dung này?')) { await request(`${page}/${row.id}`, 'DELETE'); await load(); } }, 'btn btn-outline-danger btn-sm ms-2'));
  }
  function user(row) {
    const body = card(`${row.full_name} · #${row.id}`); body.closest('.card').classList.add('user-list-card'); body.prepend(text('span',row.full_name?.trim().split(/\s+/).map(v=>v[0]).slice(-2).join('')||'U','user-avatar')); body.append(text('p', `${row.email} · ${row.username}`));
    const active = document.createElement('input'); active.type = 'checkbox'; active.checked = row.is_active; active.className = 'form-check-input me-2';
    const label = text('label', '', 'form-check-label d-block mb-3'); label.append(active, document.createTextNode('Tài khoản hoạt động')); body.append(label);
    const role = document.createElement('select'); role.className = 'form-select mb-3'; role.setAttribute('aria-label', 'Vai trò');
    for (const r of ['USER', 'VENDOR', 'MANAGER', 'ADMIN', 'SHIPPER']) { const o = text('option', ({USER:'Khách hàng',VENDOR:'Chủ cửa hàng',MANAGER:'Quản lý chi nhánh',ADMIN:'Quản trị viên',SHIPPER:'Nhân viên giao hàng'})[r]); o.value = 'ROLE_' + r; role.append(o); }
    role.value = row.role_name; role.disabled = !page.startsWith('/admin/'); body.append(role);
    body.append(button('Lưu tài khoản', async () => {
      say((await request(`${page}/${row.id}`, 'PUT', {active: active.checked, role: role.value, version: row.version})).message); await load();
    }));
  }
  async function load() {
    if (!output || page.startsWith('/auth/') || page === '/account/security') return;
    output.replaceChildren(text('p', 'Đang tải…', 'text-secondary'));
    try {
      const suffix = `?page=${currentPage}&size=20&q=${encodeURIComponent(query)}`;
      const data = await request(page + '/data' + suffix); output.replaceChildren();
      if (page === '/account/profile') {
        fill(document.getElementById('profile-form'), data);
        const body = card('Thông tin tài khoản');
        body.closest('.card').classList.add('account-facts');
        document.getElementById('profile-display-name').textContent = data.full_name || data.username;
        document.getElementById('profile-display-email').textContent = data.email;
        body.querySelector('h2')?.classList.add('member-name');
        body.prepend(text('span', 'THÔNG TIN TÀI KHOẢN', 'eyebrow'));
        const details = document.createElement('dl'); details.className = 'profile-details';
        for (const [label, value] of [['Tên đăng nhập', data.username], ['Email', data.email], ['Điện thoại', data.phone || 'Chưa cập nhật'], ['Xác minh email', data.email_verified_at ? 'Đã xác minh' : 'Chưa xác minh'], ['Ngày tham gia', data.created_at ? new Date(data.created_at).toLocaleDateString('vi-VN') : 'Chưa có thông tin']]) {
          details.append(text('dt', label), text('dd', value));
        }
        body.append(details);
        const preview = document.getElementById('avatar-preview');
        const placeholder = document.getElementById('avatar-placeholder');
        const imageUrl = data.avatar_url?.startsWith('https://') ? data.avatar_url : null;
        preview.hidden = !imageUrl; placeholder.hidden = !!imageUrl;
        document.getElementById('avatar-preview-note').textContent = 'Ảnh đại diện hiện tại. Chọn ảnh để xem trước.';
        if (imageUrl) preview.src = imageUrl; else preview.removeAttribute('src');
        placeholder.textContent = (data.full_name || data.username || 'UT').trim().split(/\s+/).slice(-2).map(x => x[0]).join('').toUpperCase();
        return;
      }
      if (page === '/account/loyalty') {
        document.getElementById('identity-title').textContent = 'Điểm và hạng thành viên';
        const body = card(`Hạng ${data.tier}`); body.closest('.card').classList.add('loyalty-summary'); body.append(text('p', `${data.balance} điểm khả dụng`, 'fs-4 text-success'), text('p', `${data.debt} điểm cần bù · ${data.lifetime_earned} điểm tích lũy hợp lệ`));
        rows = data.transactions; rows.forEach(t => { const b = card(`${t.type} · Đơn #${t.order_id}`); b.append(text('p', `${t.points_delta > 0 ? '+' : ''}${t.points_delta} điểm · Số dư ${t.balance_after} · Nợ ${t.debt_after}`)); });
      } else {
        rows = data;
        const render = page === '/account/addresses' ? address : page.endsWith('/users') ? user : ['/reviews', '/comments'].includes(page) ? entry : product;
        rows.forEach(render);
        if (page === '/favorites') document.getElementById('identity-title').textContent = 'Sản phẩm yêu thích';
        if (page === '/viewed') document.getElementById('identity-title').textContent = 'Sản phẩm đã xem';
      }
      if (!rows.length) output.append(text('p', 'Chưa có dữ liệu trong danh sách này.', 'alert alert-light border'));
      if (page !== '/account/addresses') {
        document.getElementById('identity-paging').classList.remove('d-none');
        document.getElementById('prev-page').disabled = currentPage === 0; document.getElementById('next-page').disabled = rows.length < 20;
        document.getElementById('page-label').textContent = `Trang ${currentPage + 1}`;
      }
    } catch (error) { output.replaceChildren(text('p', error.message, 'alert alert-warning')); }
  }
  if (['/reviews', '/comments'].includes(page)) run(async () => {
    const purchases = await request('/reviews/purchases');
    const select = document.getElementById('orderItemId'); select.replaceChildren(text('option', 'Chọn sản phẩm từ đơn đã mua')); select.firstChild.value = '';
    purchases.forEach(p => { const o = text('option', `${p.product_name} · dòng đơn #${p.id}`); o.value = p.id; o.dataset.product = p.product_id; select.append(o); });
    select.addEventListener('change', () => { document.querySelector('#community-form [name="productId"]').value = select.selectedOptions[0]?.dataset.product || ''; });
    if (!purchases.length) { select.replaceChildren(text('option', 'Chưa có sản phẩm đủ điều kiện')); select.firstChild.value = ''; }
  });
  run(load);
})();
