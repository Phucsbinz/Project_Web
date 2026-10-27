/* Private CSKH messages use HTTP services; STOMP only signals committed changes. */
'use strict';
(() => {
 const app=document.getElementById('support-app');if(!app)return;
 const base=document.querySelector('meta[name="app-context"]')?.content||'';
 const customer=app.dataset.customer==='true',me=Number(app.dataset.user);
 const threads=document.getElementById('support-threads'),messages=document.getElementById('support-messages');
 const compose=document.getElementById('support-compose'),body=document.getElementById('support-body'),send=compose.querySelector('button');
 const older=document.getElementById('support-older'),alert=document.getElementById('support-alert');
 const drafts=new Map(),attempts=new Map();let selected=null,page=0,generation=0,rows=new Map(),sending=false,loading=false,hasOlder=false;
 function notice(text){alert.textContent=text||'';alert.hidden=!text;}
 function active(enabled){body.disabled=!enabled;send.disabled=!enabled||sending;}
 function clear(){selected=null;generation++;rows.clear();messages.replaceChildren(node('p','Quyền truy cập đã thay đổi. Chọn lại hội thoại hoặc đăng nhập lại.','support-empty'));active(false);}
 function node(tag,text,cls){const e=document.createElement(tag);if(text!==undefined)e.textContent=text;if(cls)e.className=cls;return e;}
 async function request(path,method='GET',data){
  const headers={Accept:'application/json'};
  if(method!=='GET'){
   const csrfResponse=await fetch(base+'/products/csrf',{credentials:'same-origin',cache:'no-store'});
   if(!csrfResponse.ok)throw new Error('Không lấy được phiên gửi an toàn. Vui lòng tải lại.');
   const csrf=await csrfResponse.json();headers[csrf.headerName]=csrf.token;headers['Content-Type']='application/json';
  }
  const response=await fetch(base+'/api/support'+path,{method,headers,credentials:'same-origin',cache:'no-store',...(data?{body:JSON.stringify(data)}:{})});
  const value=response.headers.get('Content-Type')?.includes('application/json')?await response.json():{};
  if(!response.ok){const error=new Error(value.message||'Chưa thể tải CSKH. Vui lòng thử lại.');error.status=response.status;if([401,403,404].includes(response.status))clear();throw error;}
  return value;
 }
 function draw(mode='latest'){
  const nearBottom=messages.scrollHeight-messages.scrollTop-messages.clientHeight<90,oldHeight=messages.scrollHeight,oldTop=messages.scrollTop;
  const items=[...rows.values()].sort((a,b)=>a.id-b.id),fragment=document.createDocumentFragment();
  if(!items.length)fragment.append(node('p','Chưa có tin nhắn. Bạn có thể bắt đầu trao đổi tại đây.','support-empty'));
  for(const m of items){const mine=Number(m.sender_id)===me;const article=node('article',undefined,'support-message'+(mine?' mine':''));article.dataset.messageId=m.id;
   article.append(node('strong',(mine?'Bạn':m.sender_name)+' · '+({'ROLE_USER':'Khách hàng','ROLE_MANAGER':'Quản lý chi nhánh','ROLE_ADMIN':'Admin'}[m.sender_role]||'Hỗ trợ')));
   article.append(node('p',m.body));const date=new Date(m.created_at);const time=node('time',Number.isNaN(date.getTime())?'':date.toLocaleString('vi-VN'));if(!Number.isNaN(date.getTime()))time.dateTime=date.toISOString();article.append(time);fragment.append(article);
  }
  messages.replaceChildren(fragment);older.hidden=!hasOlder;
  messages.scrollTop=mode==='older'?messages.scrollHeight-oldHeight+oldTop:(mode==='initial'||nearBottom?messages.scrollHeight:oldTop);
 }
 async function select(id){if(selected)drafts.set(selected,body.value);selected=id;const ticket=++generation;rows.clear();hasOlder=false;body.value=drafts.get(id)||'';active(false);messages.replaceChildren(node('p','Đang tải tin nhắn…','support-empty'));
  try{const data=await request('/conversations/'+encodeURIComponent(id)+'/messages');if(ticket!==generation)return;
   data.items.forEach(m=>rows.set(m.id,m));hasOlder=data.hasMore;document.getElementById('support-title').textContent=customer?data.conversation.title:data.conversation.customer_name;
   document.getElementById('support-context').textContent=data.conversation.channel==='ADMIN'?'Hỗ trợ từ đội ngũ Admin UTETra':'Quản lý chi nhánh · '+data.conversation.title;
   draw('initial');active(true);threads.querySelectorAll('button[data-thread]').forEach(b=>b.setAttribute('aria-current',String(b.dataset.thread===id)));notice('');
  }catch(e){notice(e.message);}
 }
 async function list(){const data=await request('/conversations?page='+page),fragment=document.createDocumentFragment();
  if(!data.items.length)fragment.append(node('p',customer?'Bạn chưa có hội thoại. Chọn một kênh hỗ trợ ở trên.':'Chưa có khách hàng liên hệ trong phạm vi của bạn.','support-empty'));
  for(const t of data.items){const button=node('button',undefined,'support-thread');button.type='button';button.dataset.thread=t.id;button.setAttribute('aria-current',String(t.id===selected));button.append(node('strong',customer?t.title:t.customer_name));button.append(node('small',(customer?'':t.title+' · ')+(t.last_body||'Chưa có tin nhắn')));button.addEventListener('click',()=>select(t.id));fragment.append(button);}
  threads.replaceChildren(fragment);document.getElementById('support-page').textContent='Trang '+(page+1);document.getElementById('support-prev').disabled=page===0;document.getElementById('support-next').disabled=!data.hasMore;
  if(!selected&&data.items.length)await select(data.items[0].id);
 }
 async function fresh(){if(!selected||loading)return;loading=true;const id=selected,ticket=generation;
  try{let more=true,iterations=0;while(more&&ticket===generation&&iterations++<10){const last=Math.max(0,...rows.keys());const data=await request('/conversations/'+encodeURIComponent(id)+'/messages?after='+last);if(ticket!==generation)return;data.items.forEach(m=>rows.set(m.id,m));if(data.items.length)draw();more=data.hasMore;}}
  catch(e){notice(e.message);}finally{loading=false;}
 }
 older.addEventListener('click',async()=>{if(!selected||!rows.size)return;const id=selected,ticket=generation;older.disabled=true;try{const data=await request('/conversations/'+id+'/messages?before='+Math.min(...rows.keys()));if(ticket!==generation)return;data.items.forEach(m=>rows.set(m.id,m));hasOlder=data.hasMore;draw('older');}catch(e){notice(e.message);}finally{older.disabled=false;}});
 compose.addEventListener('submit',async event=>{event.preventDefault();if(!selected||sending)return;const text=body.value.trim();if([...text].length<1||[...text].length>2000){notice('Tin nhắn cần từ 1 đến 2000 ký tự.');return;}const id=selected;
  let attempt=attempts.get(id);if(!attempt||attempt.body!==text){attempt={body:text,clientMessageId:utetra.uuid()};attempts.set(id,attempt);}sending=true;send.disabled=true;
  try{await request('/conversations/'+id+'/messages','POST',attempt);attempts.delete(id);drafts.delete(id);if(selected===id){if(body.value.trim()===text)body.value='';await fresh();}await list();notice('');}
  catch(e){notice(e.message);}finally{sending=false;send.disabled=!selected;}
 });
 body.addEventListener('input',()=>{if(selected)drafts.set(selected,body.value);});
 body.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='Enter'&&!e.isComposing){e.preventDefault();compose.requestSubmit();}});
 document.getElementById('support-refresh').addEventListener('click',()=>Promise.all([list(),fresh()]).catch(e=>notice(e.message)));
 for(const [id,step] of [['support-prev',-1],['support-next',1]])document.getElementById(id).addEventListener('click',()=>{page+=step;list().catch(e=>notice(e.message));});
 async function targets(){if(!customer)return;const data=await request('/targets'),select=document.getElementById('support-target');select.replaceChildren(node('option','Chọn Admin hoặc chi nhánh…'));select.firstElementChild.value='';
  if(data.adminAvailable){const option=node('option','Admin UTETra');option.value='ADMIN';select.append(option);}for(const s of data.stores){const option=node('option','Chi nhánh · '+s.name);option.value=String(s.id);select.append(option);}
 }
 document.getElementById('support-start')?.addEventListener('submit',async event=>{event.preventDefault();const form=event.target,button=form.querySelector('button');button.disabled=true;try{const target=document.getElementById('support-target').value;if(!target)return;const thread=await request('/conversations','POST',{storeId:target==='ADMIN'?null:Number(target)});page=0;await list();await select(thread.id);}catch(e){notice(e.message);}finally{button.disabled=false;}});
 let socket,stopped=false,retry=1000,buffer='',refreshTimer;
 const connection=document.getElementById('support-connection');
 async function connect(){if(stopped)return;buffer='';try{const csrf=await (await fetch(base+'/products/csrf',{credentials:'same-origin',cache:'no-store'})).json();if(stopped)return;socket=new WebSocket(utetra.websocketUrl());
  socket.onopen=()=>socket.send('CONNECT\naccept-version:1.2\nhost:'+location.host+'\nheart-beat:0,0\n'+csrf.headerName+':'+csrf.token+'\n\n\0');
  socket.onmessage=event=>{buffer+=event.data;if(buffer.length>131072){socket.close();return;}let end;while((end=buffer.indexOf('\0'))>=0){const frame=buffer.slice(0,end).replace(/^\n+/,'');buffer=buffer.slice(end+1);
   if(frame.startsWith('CONNECTED\n')){retry=1000;connection.textContent='Đã kết nối realtime';socket.send('SUBSCRIBE\nid:support-inbox\ndestination:/user/queue/support\nack:auto\n\n\0');}
   else if(frame.startsWith('MESSAGE\n')){clearTimeout(refreshTimer);refreshTimer=setTimeout(()=>Promise.all([list(),fresh()]).catch(e=>notice(e.message)),80);}
   else if(frame.startsWith('ERROR\n'))socket.close();
  }};socket.onerror=()=>{};socket.onclose=()=>{connection.textContent='Đang dùng tải lại định kỳ; sẽ thử kết nối realtime.';if(!stopped){setTimeout(connect,retry);retry=Math.min(retry*2,30000);}};
 }catch(_){connection.textContent='Đang dùng tải lại định kỳ.';if(!stopped)setTimeout(connect,5000);}}
 const poll=setInterval(()=>{if(!document.hidden)Promise.all([list(),fresh()]).catch(e=>notice(e.message));},7000);
 window.addEventListener('beforeunload',()=>{stopped=true;clearInterval(poll);socket?.close();});
 Promise.all([targets(),list()]).catch(e=>notice(e.message));connect();
})();
