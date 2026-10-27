'use strict';
document.addEventListener('DOMContentLoaded',()=>{
// Searchable choices retain authoritative IDs; arbitrary delivery/payment values are invalid.
document.querySelectorAll('.checkout-page select').forEach(select=>{
 const input=document.createElement('input'),list=document.createElement('datalist');
 input.className='form-control';input.type='text';input.required=select.required;input.autocomplete='off';
 input.setAttribute('aria-label',select.closest('label').childNodes[0].textContent.trim());
 list.id='checkout-choices-'+select.name;input.setAttribute('list',list.id);input.dataset.choice=select.name;
 const requiredChoice=select.required;
 const label=option=>(option.dataset.label||option.textContent).trim();
 function refreshChoices(){list.replaceChildren();[...select.options].filter(option=>(!requiredChoice||option.value!=='')&&!option.disabled).forEach(option=>{const item=document.createElement('option');item.value=label(option);list.append(item);});if(select.value&&select.selectedOptions[0].disabled){select.value='';input.value='';}const match=[...select.options].some(option=>!option.disabled&&(!requiredChoice||option.value!=='')&&label(option).toLocaleLowerCase('vi')===input.value.trim().toLocaleLowerCase('vi'));if(input.value.trim()&&!match)input.setCustomValidity('Vui lòng chọn một gợi ý hợp lệ.');}
 refreshChoices();new MutationObserver(refreshChoices).observe(select,{subtree:true,childList:true,attributes:true,attributeFilter:['disabled'],characterData:true});
 const empty=[...select.options].find(option=>!option.value);input.placeholder=empty?.textContent.trim()||'Nhập để tìm hoặc chọn';
 function sync(){input.value=select.value?label(select.selectedOptions[0]):'';input.setCustomValidity('');}
 select.hidden=true;select.required=false;select.after(input,list);sync();
 select.addEventListener('change',sync);
 input.addEventListener('input',()=>{const value=input.value.trim().toLocaleLowerCase('vi'),match=[...select.options].find(option=>!option.disabled&&(!requiredChoice||option.value!=='')&&label(option).toLocaleLowerCase('vi')===value);select.value=match?match.value:'';input.setCustomValidity(value&&!match?'Vui lòng chọn một gợi ý hợp lệ.':'');});
 input.addEventListener('change',()=>{if(!input.value.trim()&&empty)input.setCustomValidity('');});
 document.querySelectorAll('[data-payment-choice]').forEach(button=>{if(select.name!=='paymentMethod')return;button.addEventListener('click',()=>{select.value=button.dataset.paymentChoice;sync();input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));});});
});

});
