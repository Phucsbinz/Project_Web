(() => {
  'use strict';
  const root = document.getElementById('moderation'); if (!root) return;
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  const form = document.getElementById('moderation-search'), list = document.getElementById('moderation-list'), message = document.getElementById('moderation-message');
  const prev = document.getElementById('moderation-prev'), next = document.getElementById('moderation-next');
  let page = 0, filter = {kind:'reviews', status:'', q:''}, busy = false;
  const node = (tag, value, css) => { const item = document.createElement(tag); item.textContent = value; if (css) item.className = css; return item; };
  const say = (value, error=false) => { message.textContent=value; message.className='alert '+(error?'alert-danger':'alert-info'); };
  async function request(path, method='GET', body) {
    const headers = {Accept:'application/json'}, token=document.querySelector('meta[name="_csrf"]')?.content, key=document.querySelector('meta[name="_csrf_header"]')?.content;
    if(token && key) headers[key]=token; if(body) headers['Content-Type']='application/json';
    const response = await fetch(context+path,{method,headers,body:body?JSON.stringify(body):undefined,credentials:'same-origin',cache:'no-store'});
    if(response.redirected || !response.headers.get('Content-Type')?.includes('application/json')) throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    const result=await response.json(); if(!response.ok) throw new Error(result.message || 'Không thể xử lý yêu cầu.'); return result;
  }
  async function load() {
    if(busy) return; busy=true; prev.disabled=true; next.disabled=true;
    try {
      const rows=await request('/admin/moderation/data?'+new URLSearchParams({...filter,page,size:20})); list.replaceChildren();
      for(const row of rows) {
        const kind=filter.kind;
        const card=node('article','','card card-body'), title=node('h2',`${row.product_name} · #${row.id}`,'h5');
        card.append(title,node('p',`${row.author_name} · Đơn #${row.order_id} · ${row.status==='VISIBLE'?'Đang hiển thị':'Đã ẩn'}${row.stars ? ' · '+row.stars+'/5 sao':''}`,'small text-secondary'),node('p',row.content || 'Nội dung đã bị tác giả gỡ.'));
        const action=node('button',row.status==='VISIBLE'?'Ẩn nội dung':'Khôi phục hiển thị','btn btn-outline-success align-self-start'); action.type='button';
        action.disabled=row.status==='HIDDEN' && !row.content?.trim();
        if(action.disabled) card.append(node('p','Không thể khôi phục nội dung đã bị tác giả gỡ.','small text-secondary'));
        action.onclick=async()=>{action.disabled=true;try {
          const visible=row.status==='HIDDEN';
          if(!await window.utetraConfirm(`${visible?'Khôi phục hiển thị':'Ẩn'} ${kind==='reviews'?'đánh giá':'bình luận'} #${row.id} của ${row.author_name}?`)) return;
          await request(`/admin/moderation/${kind}/${row.id}`,'PUT',{visible,version:row.version}); await load(); say('Đã cập nhật hiển thị nội dung.');
        }catch(error){say(error.message,true);}finally{action.disabled=false;}};
        card.append(action); list.append(card);
      }
      if(!rows.length) list.append(node('p','Không có nội dung phù hợp.','text-secondary'));
      document.getElementById('moderation-page').textContent='Trang '+(page+1); prev.disabled=page===0; next.disabled=rows.length<20; say(rows.length+' nội dung trên trang này.');
    } catch(error) {say(error.message,true);} finally {busy=false;}
  }
  form.onsubmit=e=>{e.preventDefault();if(busy)return;filter=Object.fromEntries(new FormData(form));page=0;load();};
  prev.onclick=()=>{if(!busy && page>0){page--;load();}}; next.onclick=()=>{if(!busy){page++;load();}};load();
})();
