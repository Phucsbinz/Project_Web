'use strict';
(() => {
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  async function send(path, data, method = 'POST') {
    const headers = {'Content-Type': 'application/json', Accept: 'application/json'};
    const csrfResponse = await fetch(context + '/products/csrf', {cache:'no-store'});
    if (!csrfResponse.ok) throw new Error('Không thể xác minh phiên. Hãy tải lại trang.');
    const csrf = await csrfResponse.json(); headers[csrf.headerName] = csrf.token;
    const response = await fetch(context + path, {method, headers, body: JSON.stringify(data)});
    if (response.status === 401 || response.redirected) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    const value = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(value.error || value.message || value.detail || (response.status === 403 ? 'Không có quyền hoặc phiên đã hết hạn. Hãy tải lại trang.' : 'Không thể xử lý yêu cầu. Kiểm tra dữ liệu và thử lại.'));
    return value;
  }
  document.querySelectorAll('.catalog-page').forEach(button => button.addEventListener('click', () => {
    const url = new URL(location.href); url.searchParams.set('page', button.dataset.page); location.href = url;
  }));
  const drink = document.querySelector('#drink-form');
  if(drink) {
    for(const select of drink.querySelectorAll('select[name="variantId"],select[name="sugarLevel"],select[name="iceLevel"]')) {
      const group=document.createElement('div');group.className='drink-choices';group.setAttribute('role','group');
      group.setAttribute('aria-label',document.querySelector('label[for="'+select.id+'"]')?.textContent||select.name);
      for(const option of select.options){
        const choice=document.createElement('button');choice.type='button';choice.textContent=select.name==='variantId'?option.textContent.split(' (')[0]:option.textContent;choice.dataset.value=option.value;
        choice.addEventListener('click',()=>{select.value=option.value;select.dispatchEvent(new Event('change',{bubbles:true}));});group.append(choice);
      }
      const sync=()=>group.querySelectorAll('button').forEach(button=>{const active=button.dataset.value===select.value;button.classList.toggle('selected',active);button.setAttribute('aria-pressed',String(active));});
      select.addEventListener('change',sync);select.classList.add('visually-hidden');select.after(group);sync();
    }
    const quantity=drink.querySelector('#quantity');
    if(quantity){const row=document.createElement('div');row.className='drink-quantity';quantity.before(row);for(const [label,delta] of [['Giảm số lượng',-1],['Tăng số lượng',1]]){const button=document.createElement('button');button.type='button';button.textContent=delta<0?'−':'+';button.setAttribute('aria-label',label);button.addEventListener('click',()=>{quantity.value=String(Math.max(1,Math.min(20,Number(quantity.value||1)+delta)));quantity.dispatchEvent(new Event('input',{bubbles:true}));});if(delta<0)row.append(button,quantity);else row.append(button);}}
    const estimate=()=>{const data=new FormData(drink);let unit=Number(drink.dataset.basePrice||0)+Number(drink.querySelector('select[name=variantId]')?.selectedOptions[0]?.dataset.extraPrice||0);drink.querySelectorAll('[name=toppingIds]:checked').forEach(top=>unit+=Number(top.dataset.price||0));document.getElementById('quote-result').textContent='Tạm tính: '+new Intl.NumberFormat('vi-VN').format(unit*Number(data.get('quantity')||1))+' đ · Chọn chi nhánh và xác nhận giá khi lên đơn.';};
    drink.addEventListener('change',estimate);drink.addEventListener('input',estimate);estimate();
  }
  if (drink) drink.addEventListener('submit', async event => {
    event.preventDefault(); const button = drink.querySelector('button[type=submit],button:not([type])'); const result = document.querySelector('#quote-result');
    const data = Object.fromEntries(new FormData(drink));
    ['variantId','quantity','sugarLevel','iceLevel'].forEach(key => data[key] = Number(data[key]));
    data.toppingIds = new FormData(drink).getAll('toppingIds').map(Number);
    button.disabled = true; result.textContent = 'Đang tính giá…';
    try {
      const item={variantId:data.variantId,sugar:data.sugarLevel,ice:data.iceLevel,toppingIds:data.toppingIds,quantity:data.quantity,note:''},group=drink.dataset.group;
      if(group){const response=await fetch(context+'/group-orders/'+encodeURIComponent(group)+'/data',{headers:{Accept:'application/json'},cache:'no-store'});if(!response.ok)throw new Error('Không thể mở nhóm. Hãy quay lại nhóm và thử lại.');const state=await response.json();await send('/group-orders/'+encodeURIComponent(group)+'/items',{version:state.version,item});result.textContent='Đã thêm món vào nhóm. Quay lại nhóm để xem các món đã chọn.';if(window.parent!==window)window.parent.postMessage({type:'utetra:group-item-added'},location.origin);}
      else{await send('/cart/draft/items',item);result.textContent='Đã thêm vào giỏ hàng.';window.dispatchEvent(new Event('utetra:cart-changed'));window.dispatchEvent(new Event('utetra:cart-added'));if(window.parent!==window){window.parent.postMessage({type:'utetra:cart-changed'},location.origin);window.parent.postMessage({type:'utetra:cart-added'},location.origin);}}
    }
    catch(error) { result.textContent = error.message; } finally { button.disabled = false; }
  });
  if(drink){const submit=drink.querySelector('[data-product-submit]');if(submit)submit.disabled=submit.dataset.available!=='true';drink.dataset.ready='true';}
  const registration = document.querySelector('#registration-form');
  if (registration) registration.addEventListener('submit', async event => {
    event.preventDefault(); const button = registration.querySelector('button'); const result = registration.querySelector('output');
    button.disabled = true; result.textContent = 'Đang gửi hồ sơ…';
    try { const data = Object.fromEntries(new FormData(registration)); data.version = 0; data.acceptingOrders = false;
      await send('/account/shop-registration', data); location.reload();
    } catch(error) { result.textContent = error.message; } finally { button.disabled = false; }
  });
})();

// Preview uses the actual saved cart; branch-specific prices are confirmed at checkout.
(() => {
  const list = document.getElementById('menu-cart-items');
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' đ';
  async function refresh() {
    if(!list)return;
    try {
      const response = await fetch(context + '/cart/data', {headers:{Accept:'application/json'},credentials:'same-origin',cache:'no-store'});
      if(!response.ok || !response.headers.get('Content-Type')?.includes('application/json'))throw new Error();
      const data=await response.json();list.replaceChildren();let subtotal=0;
      for(const item of data.items||[]) {
        const li=document.createElement('li'), content=document.createElement('div'), title=document.createElement('strong'), options=document.createElement('small'), price=document.createElement('b');
        title.textContent=item.name;options.textContent='Size '+item.size+' · '+item.quantity+' ly · '+item.sugar+'% đường';price.textContent=money(item.subtotal||0);subtotal+=Number(item.subtotal||0);
        if(item.imageUrl && (item.imageUrl.startsWith(context+'/assets/') || item.imageUrl.startsWith('https://'))){const img=document.createElement('img');img.src=item.imageUrl;img.alt='';li.append(img);}
        content.append(title,options,price);li.append(content);list.append(li);
      }
      if(!list.childElementCount){const li=document.createElement('li');li.textContent='Giỏ hàng đang trống. Chọn món yêu thích để bắt đầu nhé.';list.append(li);}
      else {const li=document.createElement('li');li.className='mini-cart-subtotal';li.textContent='Tạm tính: '+money(subtotal);list.append(li);}
    }catch(_){list.textContent='Mở giỏ hàng để xem các món bạn đã chọn.';}
  }
  window.addEventListener('utetra:cart-changed',refresh);refresh();
  const modal=document.getElementById('drink-modal'), frame=document.getElementById('drink-modal-frame');
  if(!modal || !frame)return;
  const loading=document.getElementById('drink-modal-loading');
  frame.addEventListener('load',()=>{if(frame.getAttribute('src')!=='about:blank'){if(loading)loading.hidden=true;frame.removeAttribute('aria-busy');}});
  document.addEventListener('click',event=>{
    const link=event.target.closest('.product-card .card-footer a');if(!link || event.ctrlKey || event.metaKey || event.shiftKey || event.button!==0)return;
    const url=new URL(link.href,location.href);if(url.origin!==location.origin || !/\/products\/[0-9]+$/.test(url.pathname))return;
    event.preventDefault();const group=new URLSearchParams(location.search).get('group');if(group)url.searchParams.set('group',group);url.searchParams.set('quick','1');if(loading)loading.hidden=false;frame.setAttribute('aria-busy','true');frame.src=url.href;bootstrap.Modal.getOrCreateInstance(modal).show();
  });
  let closeAfterShow=false,shown=false;
  modal.addEventListener('show.bs.modal',()=>{shown=false;closeAfterShow=false;});
  modal.addEventListener('shown.bs.modal',()=>{shown=true;if(closeAfterShow)bootstrap.Modal.getOrCreateInstance(modal).hide();});
  modal.addEventListener('hidden.bs.modal',()=>{shown=false;closeAfterShow=false;frame.src='about:blank';});
  window.addEventListener('message',event=>{
    if(event.origin!==location.origin || event.source!==frame.contentWindow || !['utetra:cart-changed','utetra:group-item-added'].includes(event.data?.type))return;
    if(event.data.type==='utetra:group-item-added'){const toast=document.getElementById('site-toast');if(toast){toast.textContent='Đã thêm món vào nhóm. Về nhóm để xem các món đã chọn.';toast.hidden=false;setTimeout(()=>toast.hidden=true,6500);}}else {refresh();const toast=document.getElementById('site-toast');if(toast){toast.textContent='Đã thêm vào giỏ hàng. Mở giỏ hàng để kiểm tra và đặt món.';toast.hidden=false;clearTimeout(toast.utetraTimer);toast.utetraTimer=setTimeout(()=>{toast.hidden=true;},6000);}}if(shown)bootstrap.Modal.getOrCreateInstance(modal).hide();else closeAfterShow=true;
  });
})();
