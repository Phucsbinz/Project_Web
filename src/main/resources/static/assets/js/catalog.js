'use strict';
(() => {
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  async function send(path, data, method = 'POST') {
    const headers = {'Content-Type': 'application/json'};
    const csrfResponse = await fetch(context + '/products/csrf', {cache:'no-store'});
    if (!csrfResponse.ok) throw new Error('Không thể xác minh phiên. Hãy tải lại trang.');
    const csrf = await csrfResponse.json(); headers[csrf.headerName] = csrf.token;
    const response = await fetch(context + path, {method, headers, body: JSON.stringify(data)});
    const value = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(value.message || value.detail || (response.status === 403 ? 'Không có quyền hoặc phiên đã hết hạn. Hãy tải lại trang.' : 'Không thể xử lý yêu cầu. Kiểm tra dữ liệu và thử lại.'));
    return value;
  }
  document.querySelectorAll('.catalog-page').forEach(button => button.addEventListener('click', () => {
    const url = new URL(location.href); url.searchParams.set('page', button.dataset.page); location.href = url;
  }));
  const drink = document.querySelector('#drink-form');
  if (drink) drink.addEventListener('submit', async event => {
    event.preventDefault(); const button = drink.querySelector('button'); const result = document.querySelector('#quote-result');
    const data = Object.fromEntries(new FormData(drink));
    ['variantId','quantity','sugarLevel','iceLevel'].forEach(key => data[key] = Number(data[key]));
    data.toppingIds = new FormData(drink).getAll('toppingIds').map(Number);
    button.disabled = true; result.textContent = 'Đang tính giá…';
    try { const quote = await send('/products/' + drink.dataset.product + '/quote', data); await send('/cart/items', {variantId:data.variantId,sugar:data.sugarLevel,ice:data.iceLevel,toppingIds:data.toppingIds,quantity:data.quantity,note:''}); result.textContent = 'Đã thêm vào giỏ hàng · ' + new Intl.NumberFormat('vi-VN').format(quote.total) + ' đ'; }
    catch(error) { result.textContent = error.message; } finally { button.disabled = false; }
  });
  const registration = document.querySelector('#registration-form');
  if (registration) registration.addEventListener('submit', async event => {
    event.preventDefault(); const button = registration.querySelector('button'); const result = registration.querySelector('output');
    button.disabled = true; result.textContent = 'Đang gửi hồ sơ…';
    try { const data = Object.fromEntries(new FormData(registration)); data.version = 0; data.acceptingOrders = false;
      await send('/account/shop-registration', data); registration.reset(); result.textContent = 'Hồ sơ đã được gửi và đang chờ duyệt.';
    } catch(error) { result.textContent = error.message; } finally { button.disabled = false; }
  });
})();
