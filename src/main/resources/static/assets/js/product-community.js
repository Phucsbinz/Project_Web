'use strict';
(() => {
 const node=document.getElementById('product-community'); if(!node)return;
 const context=document.querySelector('meta[name="app-context"]')?.content||'';
 const id=node.dataset.product, message=node.querySelector('[role="status"]');
 async function mutation(path) {
  const csrf=await fetch(context+'/products/csrf',{cache:'no-store'}).then(r=>r.json());
  const r=await fetch(context+path,{method:'POST',headers:{[csrf.headerName]:csrf.token}});
  if(!r.ok)throw new Error('Vui lòng đăng nhập bằng tài khoản mua hàng và thử lại.');
 }
 async function load() {
  const r=await fetch(context+'/products/'+id+'/community');
  if(!r.ok)throw new Error('Chưa tải được đánh giá. Vui lòng thử lại.');
  const data=await r.json();
  message.textContent='Đã bán '+data.soldUnits+' ly · ★ '+Number(data.rating.average_stars).toFixed(1)+'/5 · '+data.rating.review_count+' đánh giá · '+data.commentCount+' bình luận · '+data.favorites+' lượt yêu thích';
  for(const type of ['reviews','comments']) {
   const list=node.querySelector('[data-list="'+type+'"]'); list.replaceChildren();
   if(!data[type].length) {const p=document.createElement('p');p.className='text-secondary';p.textContent=type==='reviews'?'Chưa có đánh giá.':'Chưa có bình luận.';list.append(p);}
   data[type].forEach(item=>{
    const article=document.createElement('article');article.className='card card-body mb-3';
    const author=document.createElement('strong');author.textContent=item.author_name+(item.stars?' · '+item.stars+'/5':'');
    const text=document.createElement('p');text.textContent=item.content;article.append(author,text);
    for(const media of item.media||[]) {
     const url=media.secure_url||media.url;if(!url||!url.startsWith('https://'))continue;
     const element=document.createElement(media.resource_type==='video'?'video':'img');element.src=url;element.className='img-fluid rounded mt-2';element.style.maxHeight='280px';
     if(element.tagName==='VIDEO')element.controls=true;else {element.alt='Ảnh khách hàng';element.loading='lazy';}
     article.append(element);
    }list.append(article);
   });
  }
 }
 node.querySelector('[data-favorite]')?.addEventListener('click',async e=>{
  const button=e.currentTarget;button.disabled=true;
  try {await mutation('/favorites/'+id);await load();}catch(error){message.textContent=error.message;}finally{button.disabled=false;}
 });
 load().catch(error=>{message.textContent=error.message;});
 if(node.dataset.buyer==='true')mutation('/viewed/'+id).catch(()=>{});
})();
