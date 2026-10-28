/* Shared presentation interactions. Authentication and business rules remain server-owned. */
'use strict';
(() => {
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 const path=location.pathname;
 const money=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0});
 const statuses={NEW:'Mới đặt',CONFIRMED:'Đã xác nhận',PICKED_UP:'Đã lấy hàng',DELIVERING:'Đang giao',DELIVERED:'Đã giao',CANCELLED:'Đã hủy',RETURN_REQUESTED:'Yêu cầu trả hàng',RETURN_APPROVED:'Đã duyệt trả hàng',RETURNED:'Đã nhận hàng trả',REFUNDED:'Đã hoàn tiền',PAID:'Đã thanh toán',UNPAID:'Chưa thanh toán',PENDING:'Đang chờ xử lý',FAILED:'Không thành công',SUCCEEDED:'Thành công',VISIBLE:'Đang hiển thị',HIDDEN:'Đã ẩn'};
 function polish(root=document) {
  root.querySelectorAll('[data-money]').forEach(node=>{
   const value=Number(node.dataset.money);if(!Number.isFinite(value))return;
   const label=money.format(value);if(node.textContent!==label)node.textContent=label;
  });
  root.querySelectorAll('[data-status]').forEach(node=>{
   const label=statuses[node.dataset.status];if(label&&node.textContent!==label)node.textContent=label;
  });
 }
 document.querySelectorAll('.workspace-sidebar>a,.identity-nav>a,.nav-pills>a').forEach(link=>{
  if(new URL(link.href,location.href).pathname===path){link.classList.add('is-active');link.setAttribute('aria-current','page');if(link.closest('.nav-pills'))link.classList.add('active');}
 });
 document.querySelectorAll('[data-back-top]').forEach(button=>button.addEventListener('click',event=>{event.preventDefault();window.scrollTo({top:0,behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});}));
 document.querySelectorAll('[data-toggle-password]').forEach(button=>button.addEventListener('click',()=>{
  const input=document.getElementById(button.dataset.togglePassword);if(!input)return;
  const show=input.type==='password';input.type=show?'text':'password';button.textContent=show?'Ẩn':'Hiện';button.setAttribute('aria-label',show?'Ẩn mật khẩu':'Hiện mật khẩu');button.setAttribute('aria-pressed',String(show));
 }));
 document.querySelectorAll('[data-site-logout]').forEach(button=>button.addEventListener('click',async()=>{
  button.disabled=true;
  try {
   const response=await fetch(context+'/products/csrf',{credentials:'same-origin',cache:'no-store'});if(!response.ok)throw new Error('Vui lòng tải lại trang rồi thử lại.');
   const csrf=await response.json();
   const logout=await fetch(context+'/auth/logout',{method:'POST',credentials:'same-origin',headers:{[csrf.headerName]:csrf.token}});
   if(!logout.ok)throw new Error('Chưa thể đăng xuất. Vui lòng thử lại.');
   location.assign(context+'/auth/login');
  }catch(error){const toast=document.getElementById('site-toast');toast.textContent=error.message;toast.hidden=false;button.disabled=false;}
 }));
 polish();
 const observer=new MutationObserver(records=>{for(const record of records)for(const node of record.addedNodes)if(node.nodeType===Node.ELEMENT_NODE)polish(node.parentElement||document);});
 observer.observe(document.getElementById('main-content')||document.body,{childList:true,subtree:true});
})();

// Cart badge counts configured lines (not checkout orders or cups).
(() => {
 const badges=[...document.querySelectorAll('[data-cart-count]')];if(!badges.length)return;
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 let pending=false;
 async function refresh(){if(pending)return;pending=true;try{const r=await fetch(context+'/cart/count',{headers:{Accept:'application/json'},cache:'no-store'});if(!r.ok)return;const cart=await r.json();for(const badge of badges){badge.hidden=!cart.count;badge.textContent=cart.count>99?'99+':String(cart.count);badge.setAttribute('aria-label',cart.count+' món trong giỏ');}}catch(_){}finally{pending=false;}}
 window.addEventListener('utetra:cart-changed',refresh);
 window.addEventListener('pageshow',refresh);
 window.addEventListener('focus',refresh);
 window.addEventListener('message',event=>{if(event.origin===location.origin&&event.data?.type==='utetra:cart-changed')refresh();});
 refresh();
})();

(() => {let timer;function added(){const node=document.getElementById('site-toast');if(!node)return;clearTimeout(timer);node.replaceChildren();const title=document.createElement('strong'),detail=document.createElement('small');title.textContent='Đã thêm vào giỏ hàng thành công';detail.textContent='Giỏ hàng được giữ trong 1 giờ kể từ lần cập nhật gần nhất. Hãy hoàn tất đặt hàng trước khi hết hạn.';node.append(title,detail);node.hidden=false;timer=setTimeout(()=>{node.hidden=true;},7000);}window.addEventListener('utetra:cart-added',added);window.addEventListener('message',event=>{if(event.origin===location.origin&&event.data?.type==='utetra:cart-added')added();});})();

// Shared asynchronous confirmation: accessible Bootstrap modal, no browser-origin dialog.
window.utetraConfirm = function(text, title = 'Xác nhận thao tác', acceptLabel = 'Tiếp tục') {
 return new Promise(resolve => {
  window.utetraDialogBusy=true;
  const node=document.createElement('div');node.className='modal fade';node.tabIndex=-1;
  node.setAttribute('aria-labelledby','utetra-confirm-title');
  node.innerHTML='<div class="modal-dialog modal-dialog-centered"><div class="modal-content"><div class="modal-header"><h2 class="modal-title h5" id="utetra-confirm-title"></h2><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button></div><div class="modal-body"></div><div class="modal-footer"><button type="button" class="btn btn-outline-dark" data-bs-dismiss="modal">Quay lại</button><button type="button" class="btn btn-primary" data-confirm-accept>Tiếp tục</button></div></div></div>';
  node.querySelector('.modal-title').textContent=title;node.querySelector('.modal-body').textContent=text;node.querySelector('.modal-body').style.whiteSpace='pre-line';node.querySelector('[data-confirm-accept]').textContent=acceptLabel;
  document.body.append(node);const previous=document.activeElement;const modal=new bootstrap.Modal(node);let accepted=false;
  node.querySelector('[data-confirm-accept]').addEventListener('click',()=>{accepted=true;modal.hide();});
  node.addEventListener('hidden.bs.modal',()=>{modal.dispose();node.remove();window.utetraDialogBusy=false;previous?.focus();resolve(accepted);},{once:true});
  modal.show();
 });
};

// Member-specific persisted bills are polled on every customer page, independently of host checkout.
(()=>{if(document.querySelector('meta[name="utetra-commerce"]')?.content!=='true')return;if(document.querySelector('.local-qr-page,.local-wallet-page'))return;if(!['USER','VENDOR'].includes((document.body.dataset.currentRole||'').replace(/^ROLE_/,'')))return;let busy=false;const context=document.querySelector('meta[name="app-context"]')?.content||'';async function check(){if(busy||document.hidden||window.utetraDialogBusy||document.querySelector('.modal.show'))return;busy=true;try{const response=await fetch(context+'/group-orders/payment-notices',{headers:{Accept:'application/json'},cache:'no-store'});if(!response.ok)return;for(const notice of await response.json()){const key='utetra.group.notice.'+notice.token+'.'+notice.member_id;if(location.pathname===context+'/group-orders/'+notice.token+'/payment'){localStorage.setItem(key,'shown');continue;}if(localStorage.getItem(key))continue;const amount=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(notice.amount_due);const separate=notice.payment_mode==='INDIVIDUAL',qr=['BANK_TRANSFER','LOCAL_QR'].includes(notice.payment_method);const text=separate?'Chủ nhóm đã đặt đơn. Phần của bạn sau giảm giá và phí giao là '+amount+'. '+(qr?'Mở trang thanh toán để hoàn tất phần của bạn bằng QR.':'Thanh toán khi nhận hàng.'):'Chủ nhóm đã đặt đơn và chọn thanh toán toàn bộ. Bạn không cần thanh toán riêng.';if(window.utetraDialogBusy)return;const open=await window.utetraConfirm(text,'Đặt đơn nhóm thành công');localStorage.setItem(key,'shown');if(open){location.assign(context+'/group-orders/'+notice.token+'/payment');return;}}}catch(_){}finally{busy=false;}}check();const timer=setInterval(check,8000);document.addEventListener('visibilitychange',check);window.addEventListener('pagehide',()=>clearInterval(timer));})();

// Terminal group events remain visible even while a member is on their payment page.
(()=>{if(document.querySelector('meta[name="utetra-commerce"]')?.content!=='true')return;if(!['USER','VENDOR'].includes((document.body.dataset.currentRole||'').replace(/^ROLE_/,'')))return;const context=document.querySelector('meta[name="app-context"]')?.content||'';let busy=false;async function check(){if(busy||document.hidden||window.utetraDialogBusy)return;busy=true;try{const response=await fetch(context+'/group-orders/lifecycle-notices',{headers:{Accept:'application/json'},cache:'no-store'});if(!response.ok)return;const notices=await response.json();if(notices.some(notice=>location.pathname===context+'/group-orders/'+notice.token||new URLSearchParams(location.search).get('group')===notice.token)){location.replace(context+'/group-orders/new');return;}for(const notice of notices){const key='utetra.group.lifecycle.'+notice.noticeKey;if(localStorage.getItem(key))continue;if(window.utetraDialogBusy)return;const expired=notice.status==='EXPIRED',title=expired?'Nhóm đã hết hạn':'Nhóm đã bị hủy',text=notice.title+' ('+notice.code+') '+(expired?'đã hết thời gian đặt món. Bạn có thể tạo hoặc tham gia nhóm mới.':'đã được chủ nhóm hủy. Nhóm không nhận thêm món hoặc thanh toán.');const refundLine=notice.refundPending?'\n\nPhần tiền bạn đã thanh toán dự kiến sẽ được hoàn sau 30 phút kể từ khi hủy nhóm.':notice.refunded?'\n\nPhần thanh toán của bạn đã được hoàn trong hệ thống.':'';const open=await window.utetraConfirm(text+refundLine,title,'Đã hiểu');localStorage.setItem(key,'shown');if(open){location.assign(context+'/group-orders/new');return;}}}catch(_){}finally{busy=false;}}check();const timer=setInterval(check,8000);document.addEventListener('visibilitychange',check);window.addEventListener('pagehide',()=>clearInterval(timer));})();

// Keep the shared workspace navigation compact on small screens.
document.addEventListener('DOMContentLoaded',()=>{const sidebar=document.querySelector('.workspace-sidebar'),toggle=sidebar?.querySelector('.workspace-menu-toggle');toggle?.addEventListener('click',()=>{const open=sidebar.classList.toggle('is-open');toggle.setAttribute('aria-expanded',String(open));});});
