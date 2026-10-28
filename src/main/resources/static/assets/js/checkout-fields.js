/* Searchable choices keep server IDs and expose a visible, keyboard-accessible menu. */
'use strict';
document.addEventListener('DOMContentLoaded',()=>{
 document.querySelectorAll('.checkout-page select:not([data-voucher-kind])').forEach(select=>{
  const required=select.required,label=select.getAttribute('aria-label')||select.closest('label')?.childNodes[0].textContent.trim()||'Lựa chọn';
  const wrap=document.createElement('div'),input=document.createElement('input'),toggle=document.createElement('button'),menu=document.createElement('div');
  wrap.className='choice-control';input.className='form-control';input.type='text';input.required=required;input.autocomplete='off';input.dataset.choice=select.name;input.setAttribute('aria-label',label);input.setAttribute('role','combobox');input.setAttribute('aria-autocomplete','list');input.setAttribute('aria-expanded','false');
  menu.id='checkout-choices-'+select.name;menu.className='choice-options';menu.role='listbox';menu.hidden=true;input.setAttribute('aria-controls',menu.id);
  toggle.type='button';toggle.className='choice-toggle';toggle.textContent='⌄';toggle.setAttribute('aria-label','Mở danh sách '+label);toggle.setAttribute('aria-controls',menu.id);toggle.setAttribute('aria-expanded','false');toggle.setAttribute('aria-haspopup','listbox');
  const title=option=>(option.dataset.label||option.textContent).trim(),options=()=>[...select.options].filter(o=>!o.disabled&&(!required||o.value!==''));
  function close(){menu.hidden=true;input.setAttribute('aria-expanded','false');toggle.setAttribute('aria-expanded','false');}
  function render(all=false){menu.replaceChildren();const search=all?'':input.value.trim().toLocaleLowerCase('vi');const found=options().filter(o=>title(o).toLocaleLowerCase('vi').includes(search));for(const option of found){const button=document.createElement('button');button.type='button';button.role='option';button.textContent=title(option);button.setAttribute('aria-selected',String(select.value===option.value));button.addEventListener('click',event=>{event.preventDefault();input.value=title(option);input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));close();input.focus();});menu.append(button);}if(!found.length){const text=document.createElement('p');text.className='small text-secondary p-2 mb-0';text.textContent='Không tìm thấy lựa chọn phù hợp.';menu.append(text);}menu.hidden=false;input.setAttribute('aria-expanded','true');toggle.setAttribute('aria-expanded','true');}
  function sync(){input.value=select.value?title(select.selectedOptions[0]):'';input.setCustomValidity('');close();}
  const empty=[...select.options].find(o=>!o.value);input.placeholder=empty?.textContent.trim()||'Nhập để tìm hoặc bấm mũi tên';
  select.hidden=true;select.required=false;select.after(wrap);wrap.append(input,toggle,menu);sync();
  toggle.addEventListener('click',event=>{event.preventDefault();if(menu.hidden){render(true);input.focus();}else close();});
  input.addEventListener('input',()=>{const value=input.value.trim().toLocaleLowerCase('vi'),match=options().find(o=>title(o).toLocaleLowerCase('vi')===value);select.value=match?match.value:'';input.setCustomValidity(value&&!match?'Vui lòng chọn một gợi ý hợp lệ.':'');if(match)close();else render();});
  input.addEventListener('keydown',event=>{if(event.key==='ArrowDown'){event.preventDefault();if(menu.hidden)render(true);menu.querySelector('button')?.focus();}if(event.key==='Escape')close();});
  menu.addEventListener('keydown',event=>{const buttons=[...menu.querySelectorAll('button')],index=buttons.indexOf(document.activeElement);if(['ArrowDown','ArrowUp'].includes(event.key)){event.preventDefault();buttons[(index+(event.key==='ArrowDown'?1:-1)+buttons.length)%buttons.length]?.focus();}if(event.key==='Escape'){close();input.focus();}});
  document.addEventListener('click',event=>{if(!wrap.contains(event.target))close();});wrap.addEventListener('focusout',()=>setTimeout(()=>{if(!wrap.contains(document.activeElement))close();},0));select.addEventListener('change',sync);
  new MutationObserver(()=>{if(select.value&&select.selectedOptions[0]?.disabled){select.value='';input.value='';}const match=options().some(o=>title(o).toLocaleLowerCase('vi')===input.value.trim().toLocaleLowerCase('vi'));input.setCustomValidity(input.value.trim()&&!match?'Vui lòng chọn một gợi ý hợp lệ.':'');if(!menu.hidden)render(true);}).observe(select,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled'],characterData:true});
 });
});
