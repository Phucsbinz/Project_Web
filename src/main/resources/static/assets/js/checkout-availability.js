'use strict';
(() => {
  const form = document.getElementById('checkout-form');
  if (!form) return;
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  let revision, busy = false, timer;
  async function refresh() {
    if (busy || document.hidden) return;
    busy = true;
    try {
      const response = await fetch(context + '/checkout/availability', {headers: {Accept: 'application/json'}, cache: 'no-store'});
      if (!response.ok) return;
      const current = await response.json();
      if (revision && revision !== current.revision) {
        form.dispatchEvent(new Event('change'));
        orders.message('Tồn kho hoặc lượt mã giảm giá đã thay đổi. Vui lòng xem lại báo giá trước khi đặt đơn.');
      }
      revision = current.revision;
      document.getElementById('checkout-availability').textContent = current.variants
        .filter(v => v.stockQuantity !== null)
        .map(v => v.name + ' · ' + v.size + ': còn ' + v.stockQuantity + ' ly, trong giỏ ' + v.requested + ' ly').join('; ');
      for (const select of form.querySelectorAll('select[name$="PromotionId"]')) {
        for (const option of select.options) {
          if (!option.value) continue;
          const promotion = current.promotions.find(p => p.id === Number(option.value));
          if (!option.dataset.label) option.dataset.label = option.textContent;
          option.disabled = !promotion?.available;
          option.textContent = option.dataset.label + (promotion?.available ? ' · còn ' + promotion.remaining + ' lượt' : ' · không còn khả dụng');
          if (option.disabled && option.selected) { select.value = ''; form.dispatchEvent(new Event('change')); }
        }
      }
    } catch (error) {
      // A disconnected socket/network cannot grant stock or voucher quota.
      // Checkout always validates the current database state independently.
    } finally { busy = false; }
  }
  document.addEventListener('utetra:availability-changed', () => { clearTimeout(timer); timer = setTimeout(refresh, 150); });
  document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
  const poll = setInterval(refresh, 15000);
  window.addEventListener('pagehide', () => { clearInterval(poll); clearTimeout(timer); });
  refresh();
})();
