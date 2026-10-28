'use strict';
(() => {
 const root=document.getElementById('shipper-scopes');if(!root)return;
 const form=document.getElementById('scope-form'),error=document.getElementById('scope-error');let busy=false;
 async function save(value){if(busy)return;busy=true;root.querySelectorAll('button').forEach(b=>b.disabled=true);error.classList.add('d-none');try{
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 const response=await fetch(context+'/admin/shipper-scopes',{method:'POST',headers:{'Content-Type':'application/json',Accept:'application/json',...utetra.csrfHeaders()},body:JSON.stringify(value)});
 if(response.redirected||response.status===401)throw new Error('Phiên đăng nhập đã hết hạn.');
 const json=await response.json();if(!response.ok)throw new Error(json.error||json.message||'Không thể cập nhật phạm vi.');location.reload();
 }catch(e){error.textContent=e.message;error.classList.remove('d-none');}finally{busy=false;root.querySelectorAll('button').forEach(b=>b.disabled=false);}}
 form.addEventListener('submit',e=>{e.preventDefault();if(!form.reportValidity())return;const shipperId=Number(document.getElementById('scope-shipper').value),storeId=Number(document.getElementById('scope-store').value);const row=[...root.querySelectorAll('tr[data-shipper]')].find(r=>Number(r.dataset.shipper)===shipperId&&Number(r.dataset.store)===storeId);save({shipperId,storeId,active:true,version:Number(row?.dataset.version||0),reason:document.getElementById('scope-reason').value});});
 root.querySelectorAll('[data-revoke]').forEach(button=>button.addEventListener('click',()=>{const row=button.closest('tr'),input=row.querySelector('input');if(!input.reportValidity())return;save({shipperId:Number(row.dataset.shipper),storeId:Number(row.dataset.store),active:false,version:Number(row.dataset.version),reason:input.value});}));
})();
