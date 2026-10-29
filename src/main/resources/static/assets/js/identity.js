/* Identity forms use the shared JWT cookie and the CSRF token rendered by SiteMesh. */
(() => {
  'use strict';
  const root = document.getElementById('identity');
  if (!root) return;
  const page = root.dataset.page;
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  const output = document.getElementById('identity-content');
  const message = document.getElementById('identity-message');
  let currentPage = 0, query = '', roleFilter = '', rows = [];
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
    if (path.split('?')[0] === '/auth/login' && res.status === 401) {
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
          const next = new URLSearchParams(location.search).get('next');
          const endpoint = form.dataset.endpoint === '/auth/login' && next ? '/auth/login?next=' + encodeURIComponent(next) : form.dataset.endpoint;
          const result = await request(endpoint, form.dataset.method || 'POST', form.hasAttribute('data-multipart') ? new FormData(form) : fieldData(form));
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
            window.location.assign(result.redirect); return;
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
    e.preventDefault(); query = e.target.elements.q.value; roleFilter = e.target.elements.role.value; currentPage = 0; run(load);
  });
  document.querySelectorAll('#create-account-form input').forEach(input => input.addEventListener('input', () => input.setCustomValidity('')));
  document.getElementById('create-account-form')?.addEventListener('submit', e => {
    e.preventDefault(); const form = e.target, submit = form.querySelector('button');
    if (!validateForm(form)) return;
    run(async () => { submit.disabled = true;
      try { const result = await request('/admin/users', 'POST', fieldData(form));
        form.reset(); query = ''; roleFilter = ''; document.getElementById('user-search').reset(); currentPage = 0; await load(); say(result.message);
      } finally { submit.disabled = false; }
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
    const roles = {ROLE_USER:'Khách hàng', ROLE_VENDOR:'Chủ cửa hàng', ROLE_MANAGER:'Quản lý chi nhánh', ROLE_ADMIN:'Quản trị viên', ROLE_SHIPPER:'Nhân viên giao hàng'};
    body.append(text('p', `${roles[row.role_name] || row.role_name} · ${row.is_active ? 'Đang hoạt động' : 'Đã vô hiệu hóa'} · ${row.email_verified_at ? 'Đã xác minh email' : 'Chờ xác minh email'}`, 'small text-secondary'));
    const role = document.createElement('select'); role.className = 'form-select mb-3'; role.setAttribute('aria-label', 'Vai trò tài khoản ' + row.username);
    for (const [value, name] of Object.entries(roles)) { const option = text('option', name); option.value = value; role.append(option); }
    role.value = row.role_name; role.disabled = !page.startsWith('/admin/'); body.append(role);
    if (page.startsWith('/admin/')) body.append(button('Lưu vai trò', async () => {
      if (role.value === row.role_name) { say('Vai trò chưa thay đổi.'); return; }
      if (!await window.utetraConfirm(`Đổi vai trò của ${row.username} từ ${roles[row.role_name]} sang ${roles[role.value]}? Các phiên đăng nhập sẽ kết thúc; phân công Manager bị thu hồi khi đổi sang vai trò khác.${row.role_name === 'ROLE_SHIPPER' ? ' Đơn đã lấy hàng phải giao xong trước khi đổi vai trò; hãy phân công lại đơn chưa lấy hàng.' : ''}`)) return;
      say((await request(`${page}/${row.id}`, 'PUT', {active: row.is_active, role: role.value, version: row.version})).message); await load();
    }));
    body.append(button(row.is_active ? 'Vô hiệu hóa tài khoản' : 'Kích hoạt lại tài khoản', async () => {
      const question = row.is_active ? `Vô hiệu hóa tài khoản ${row.username}? Tài khoản sẽ không đăng nhập được và các phiên hiện tại sẽ kết thúc. Lịch sử giao dịch được giữ lại.${row.role_name === 'ROLE_SHIPPER' ? ' Đơn đã lấy hàng phải giao xong trước khi vô hiệu hóa; hãy phân công lại đơn chưa lấy hàng.' : ''}` : `Kích hoạt lại tài khoản ${row.username}? Người dùng đã xác minh email có thể đăng nhập trở lại.`;
      if (!await window.utetraConfirm(question)) return;
      say((await request(`${page}/${row.id}`, 'PUT', {active: !row.is_active, role: row.role_name, version: row.version})).message); await load();
    }, 'btn btn-outline-danger btn-sm ms-2'));
  }

  async function load() {
    if (!output || page.startsWith('/auth/') || page === '/account/security') return;
    output.replaceChildren(text('p', 'Đang tải…', 'text-secondary'));
    try {
      const suffix = `?page=${currentPage}&size=20&q=${encodeURIComponent(query)}&role=${encodeURIComponent(roleFilter)}`;
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
  if (['/reviews', '/comments'].includes(page)) {
    const form = document.getElementById('community-form');
    const select = document.getElementById('orderItemId');
    const previous = document.getElementById('purchase-prev'), next = document.getElementById('purchase-next');
    const retry = document.getElementById('purchase-retry'), status = document.getElementById('purchase-status');
    const submit = form.querySelector('button[type="submit"]');
    let purchasePage = 0, attemptedPage = 0, loading = false;
    function choice(label) { const option = text('option', label); option.value = ''; select.replaceChildren(option); }
    async function loadPurchases(target) {
      if (loading) return;
      loading = true; attemptedPage = target;
      select.disabled = submit.disabled = previous.disabled = next.disabled = true;
      select.setAttribute('aria-busy', 'true'); retry.hidden = true;
      form.elements.productId.value = ''; choice('Đang tải sản phẩm…'); status.textContent = 'Đang tải sản phẩm đã mua…';
      try {
        const purchases = await request(`/reviews/purchases?page=${target}&size=20`);
        purchasePage = target; choice('Chọn sản phẩm từ đơn đã mua');
        purchases.forEach(p => { const option = text('option', `${p.product_name} · dòng đơn #${p.id}`); option.value = p.id; option.dataset.product = p.product_id; select.append(option); });
        document.getElementById('purchase-page-label').textContent = `Trang ${target + 1}`;
        previous.disabled = target === 0; next.disabled = purchases.length < 20;
        select.disabled = submit.disabled = !purchases.length;
        status.textContent = purchases.length ? 'Chọn sản phẩm trong trang này; dùng Trang mua sau để tìm dòng mua cũ hơn.' : target === 0 ? 'Chưa có sản phẩm từ đơn đã giao và đã thanh toán.' : 'Đã hết danh sách. Chọn Trang mua trước để quay lại.';
        if (!purchases.length) choice('Không có sản phẩm trong trang này');
      } catch (error) {
        choice('Chưa tải được sản phẩm'); status.textContent = error.message; retry.hidden = false;
      } finally { loading = false; select.setAttribute('aria-busy', 'false'); }
    }
    select.addEventListener('change', () => { form.elements.productId.value = select.selectedOptions[0]?.dataset.product || ''; });
    previous.addEventListener('click', () => loadPurchases(purchasePage - 1));
    next.addEventListener('click', () => loadPurchases(purchasePage + 1));
    retry.addEventListener('click', () => loadPurchases(attemptedPage));
    loadPurchases(0);
  }
  run(load);
})();
