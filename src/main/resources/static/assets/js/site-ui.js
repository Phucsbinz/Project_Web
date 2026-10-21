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
