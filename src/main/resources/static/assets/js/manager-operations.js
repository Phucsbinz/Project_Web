(() => {
 'use strict';
 const app=document.getElementById('manager-operations');if(!app)return;
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 const select=document.getElementById('operations-store'),message=document.getElementById('operations-message'),content=document.getElementById('operations-content');
 let stores=[],current=null,sequence=0;
 const node=(tag,text,css)=>{const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(css)n.className=css;return n;};
 const say=(text,error=false)=>{message.textContent=text;message.className='workspace-status alert '+(error?'alert-danger':'alert-info');};
 async function request(path,method='GET',body){
  const headers={Accept:'application/json'};if(body!==undefined)headers['Content-Type']='application/json';
  const token=document.querySelector('meta[name="_csrf"]')?.content,key=document.querySelector('meta[name="_csrf_header"]')?.content;if(token&&key)headers[key]=token;
  const res=await fetch(context+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),credentials:'same-origin',cache:'no-store'});
  if(res.ok && method!=='GET' && !res.redirected && !res.headers.get('Content-Type'))return {};
  if(res.redirected||!res.headers.get('Content-Type')?.includes('application/json'))throw new Error('Phiên đăng nhập hoặc phân công đã thay đổi. Hãy tải lại trang.');
  const data=await res.json();if(!res.ok)throw new Error(data.message||'Không thể cập nhật. Hãy tải lại dữ liệu và thử lại.');return data;
 }
 async function action(button,work){button.disabled=true;try{await work();await load();say('Đã cập nhật dữ liệu chi nhánh.');}catch(e){say(e.message,true);}finally{button.disabled=false;}}
 async function load(){
  const ticket=++sequence;content.hidden=true;
  const list=await request('/api/manager/operations/stores');if(ticket!==sequence)return;
  stores=list;select.replaceChildren();
  if(stores.length!==1)select.append(new Option('Chọn chi nhánh',''));
  stores.forEach(s=>select.append(new Option(s.name,String(s.id))));
  const requested=new URLSearchParams(location.search).get('store');
  const desired=stores.find(s=>String(s.id)===(current?String(current.id):requested));
  select.value=desired?String(desired.id):stores.length===1?String(stores[0].id):'';
  document.getElementById('operations-picker').hidden=!stores.length;
  if(!stores.length){current=null;say('Bạn chưa được phân công chi nhánh. Liên hệ quản trị viên để được phân công.');return;}
  if(!select.value){current=null;say('Bạn được phân công nhiều chi nhánh. Chọn chi nhánh để bắt đầu vận hành.');return;}
  current=stores.find(s=>String(s.id)===select.value);
  await render(ticket);
 }
 async function render(ticket){
  const branch=current,id=branch.id;
  document.getElementById('operations-title').textContent='Quản lý '+branch.name;
  document.getElementById('operations-branch').textContent=branch.name;
  const sidebar=document.querySelector('.workspace-sidebar h2');if(sidebar)sidebar.textContent='Quản lý '+branch.name;
  document.getElementById('operations-orders').href=context+'/manager/orders?store='+id;
  document.getElementById('operations-report').href=context+'/manager/reports?store='+id;
  document.getElementById('operations-accepting').textContent=branch.acceptingOrders?'Chi nhánh đang nhận đơn':'Chi nhánh đang tạm ngưng nhận đơn';
  const toggle=document.getElementById('operations-toggle');toggle.textContent=branch.acceptingOrders?'Tạm ngưng nhận đơn':'Mở nhận đơn';toggle.onclick=()=>action(toggle,()=>request('/api/manager/operations/stores/'+id+'/accepting','PUT',{acceptingOrders:!branch.acceptingOrders,version:branch.version}));
  const products=await request('/api/manager/catalog/products?storeId='+id);if(ticket!==sequence)return;
  const tbody=document.getElementById('operations-products');tbody.replaceChildren();
  if(!products.length){const tr=node('tr'),td=node('td','Chi nhánh chưa có sản phẩm.');td.colSpan=4;tr.append(td);tbody.append(tr);}
  for(const product of products){
   const tr=node('tr'),toggleProduct=node('button',product.available?'Tạm ngưng bán':'Cho phép bán','btn btn-outline-primary btn-sm');toggleProduct.type='button';
   toggleProduct.addEventListener('click',()=>action(toggleProduct,()=>request('/api/manager/operations/products/'+product.id+'/availability','PUT',{available:!product.available,version:product.version})));
   const actions=node('td');actions.append(toggleProduct);tr.append(node('td',product.name),node('td',product.available?'Sẵn bán':'Tạm ngưng bán'),node('td',''),actions);tbody.append(tr);
   for(const variant of product.variants||[]){
    const row=node('tr'),stock=node('td'),saveCell=node('td');row.append(node('td','↳ '+variant.sizeName),node('td',variant.active?'Đang phục vụ':'Ngừng phục vụ'));
    if(variant.stockQuantity==null){stock.textContent='Chưa theo dõi tồn';saveCell.textContent='Chủ cửa hàng thiết lập theo dõi';}
    else {const input=node('input');input.type='number';input.min='0';input.max='1000000';input.step='1';input.required=true;input.value=variant.stockQuantity;input.className='form-control';input.setAttribute('aria-label','Tồn kho '+product.name+' '+variant.sizeName);stock.append(input);
     const save=node('button','Lưu tồn kho','btn btn-outline-primary btn-sm');save.type='button';save.addEventListener('click',()=>{if(!input.reportValidity())return;action(save,()=>request('/api/manager/operations/variants/'+variant.id+'/stock','PUT',{quantity:Number(input.value),version:variant.version}));});saveCell.append(save);}
    row.append(stock,saveCell);tbody.append(row);
   }
  }
  const toppings=await request('/api/manager/catalog/stores/'+id+'/toppings');if(ticket!==sequence)return;
  const toppingBody=document.getElementById('operations-toppings');toppingBody.replaceChildren();
  for(const topping of toppings){
   const row=node('tr'),cell=node('td'),toggle=node('button',topping.available?'Tạm ngưng bán':'Cho phép bán','btn btn-outline-primary btn-sm');toggle.type='button';toggle.disabled=!topping.active;
   toggle.addEventListener('click',()=>action(toggle,()=>request('/api/manager/catalog/stores/'+id+'/toppings/'+topping.id,'PUT',{available:!topping.available})));
   cell.append(toggle);row.append(node('td',topping.name),node('td',!topping.active?'Đã ngừng hoạt động toàn hệ thống':topping.available?'Sẵn bán':'Tạm ngưng bán'),cell);toppingBody.append(row);
  }
  if(!toppings.length){const row=node('tr'),cell=node('td','Chưa có topping.');cell.colSpan=3;row.append(cell);toppingBody.append(row);}
  content.hidden=false;say('Đang vận hành '+branch.name+'.');
 }
 select.addEventListener('change',()=>{current=stores.find(s=>String(s.id)===select.value)||null;const url=new URL(location.href);if(current)url.searchParams.set('store',current.id);else url.searchParams.delete('store');history.replaceState(null,'',url);load().catch(e=>say(e.message,true));});
 load().catch(e=>say(e.message,true));
})();
