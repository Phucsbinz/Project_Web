(() => {
 'use strict';
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 document.querySelectorAll('[data-branch-managers]').forEach(button=>button.addEventListener('click',async()=>{
  const id=button.dataset.branchManagers,output=document.querySelector('[data-manager-list="'+id+'"]');button.disabled=true;output.textContent='Đang tải…';
  try{const res=await fetch(context+'/api/vendor/operations/stores/'+Number(id)+'/managers',{headers:{Accept:'application/json'},credentials:'same-origin',cache:'no-store'});if(res.redirected||!res.headers.get('Content-Type')?.includes('application/json'))throw new Error('Phiên đăng nhập đã hết hạn.');const data=await res.json();if(!res.ok)throw new Error(data.message||'Không thể tải phân công.');output.replaceChildren();if(!data.length){output.textContent='Chưa có Manager được phân công.';return;}const list=document.createElement('ul');list.className='ps-3';data.forEach(manager=>{const item=document.createElement('li');item.textContent=manager.name+(manager.active?' · Đang phân công':' · Đã thu hồi');list.append(item);});output.append(list);}catch(e){output.textContent=e.message;}finally{button.disabled=false;}
 }));
})();
