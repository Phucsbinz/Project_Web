'use strict';
(() => {
  const app = document.querySelector('#catalog-manager');
  if (!app) return;
  const context = document.querySelector('meta[name="app-context"]').content;
  const base = context + '/api/' + app.dataset.role + '/catalog';
  const select = document.querySelector('#managed-store');
  const form = document.querySelector('#management-form');
  const list = document.querySelector('#management-list');
  const message = document.querySelector('#management-message');
  const tabs = app.dataset.role === 'manager' ? ['categories','toppings'] : app.dataset.role === 'vendor' ? ['stores','products','promotions','areas','stock'] : ['stores','products','categories','toppings','promotions','areas','stock',...(app.dataset.role === 'admin' ? ['memberships'] : [])];
  const section = tabs.includes(app.dataset.section) ? app.dataset.section : 'stores';
  let editing = null, shopList = [], categories = [], toppings = [], productList = [], loading = false;
  let productLookupReady = false, productLookupRevision = 0;
  const labels = {stores:'Chi nhánh',products:'Sản phẩm',categories:'Danh mục',toppings:'Topping',promotions:'Khuyến mãi',areas:'Khu vực giao',stock:'Topping tại shop',memberships:'Phân công Manager'};
  const help={stores:'Xem thông tin chi nhánh, duyệt hồ sơ và cập nhật tình trạng nhận đơn.',products:'Chọn chi nhánh, tìm món trong danh sách rồi bấm Chỉnh sửa; hoặc thêm món mới.',categories:'Quản lý nhóm đồ uống dùng chung. Thứ tự quyết định cách hiển thị trên thực đơn.',toppings:'Quản lý tên, giá và trạng thái topping dùng chung.',promotions:'Kiểm tra phạm vi chi nhánh, thời gian và điều kiện trước khi lưu ưu đãi.',areas:'Chọn chi nhánh để cấu hình những khu vực nhận giao hàng.',stock:'Chọn chi nhánh và topping để cập nhật khả năng phục vụ.',memberships:'Chọn chi nhánh rồi nhập ID Manager cần phân công. Quyền có hiệu lực sau khi lưu.'};
  const states={ACTIVE:'Đang hoạt động',APPROVED:'Đã duyệt',PENDING_APPROVAL:'Chờ duyệt',REJECTED:'Đã từ chối',SUSPENDED:'Tạm khóa',DRAFT:'Bản nháp'};
  document.getElementById('management-title').textContent=app.dataset.role==='admin'&&location.pathname===context+'/admin'?'Tổng quan quản trị':'Quản lý '+labels[section].toLocaleLowerCase('vi');document.getElementById('management-guide').textContent=help[section];
  function filterRecords(){const query=document.getElementById('management-search').value.trim().toLocaleLowerCase('vi');const cards=[...list.querySelectorAll('article')];let count=0;cards.forEach(card=>{card.hidden=!card.dataset.search.includes(query);if(!card.hidden)count++;});document.getElementById('management-result-count').textContent=cards.length?count+'/'+cards.length+' mục':'';}
  document.getElementById('management-search').addEventListener('input',filterRecords);
  document.getElementById('management-create').hidden=section==='stores'&&app.dataset.role!=='vendor';
  document.getElementById('management-create').addEventListener('click',()=>{if(!form.querySelector('input,select,textarea')){select.focus();notice('Hãy chọn chi nhánh trước khi thêm dữ liệu.',true);return;}edit();document.getElementById('management-editor').scrollIntoView({behavior:'smooth',block:'start'});form.querySelector('input:not([type=checkbox]),select,textarea')?.focus({preventScroll:true});});
  const specs = {
    stores:[['name','Tên chi nhánh'],['slug','Đường dẫn'],['address','Địa chỉ'],['phone','Điện thoại'],['description','Giới thiệu','textarea'],['openTime','Mở cửa','time'],['closeTime','Đóng cửa','time'],['acceptingOrders','Nhận đơn','checkbox']],
    categories:[['name','Tên danh mục'],['slug','Đường dẫn'],['description','Mô tả','textarea'],['displayOrder','Thứ tự','number'],['active','Hoạt động','checkbox']],
    toppings:[['name','Tên topping'],['price','Giá (đ)','number'],['active','Hoạt động','checkbox']],
    products:[['name','Tên sản phẩm'],['slug','Đường dẫn'],['categoryId','Danh mục','category'],['description','Mô tả','textarea'],['catalogKey','Mã món chung giữa các chi nhánh (có thể để trống)','text'],['basePrice','Giá gốc (đ)','number'],['active','Hoạt động','checkbox'],['available','Sẵn bán','checkbox'],['featured','Nổi bật','checkbox'],['toppingIds','Topping được phép chọn','toppings']],
    promotions:[['name','Tên chương trình'],['code','Mã (có thể bỏ trống)'],['kind','Loại ưu đãi','kind'],['percent','Giảm (%)','number'],['maxDiscount','Giảm tối đa (đ)','number'],['minOrder','Đơn tối thiểu (đ)','number'],['usageLimit','Tổng hạn mức','number'],['perUserLimit','Hạn mức mỗi người','number'],['startsAt','Bắt đầu','datetime-local'],['endsAt','Kết thúc','datetime-local'],['productIds','Sản phẩm áp dụng (bỏ trống: toàn phạm vi)','products'],['active','Hoạt động','checkbox']],
    areas:[['areaCode','Mã khu vực'],['name','Tên khu vực'],['active','Hoạt động','checkbox']],
    stock:[['toppingId','Topping','topping'],['available','Sẵn bán','checkbox']],
    memberships:[['userId','ID tài khoản Manager','number'],['active','Có hiệu lực','checkbox'],['catalogDelegation','Cho phép quản trị danh mục chung','checkbox']]
  };
  function el(tag, text, cls) { const e = document.createElement(tag); if(text !== undefined) e.textContent = text; if(cls) e.className = cls; return e; }
  function notice(text, error = false) { message.textContent = text; message.className = 'alert ' + (error ? 'alert-danger' : 'alert-success'); }
  function syncPromotionSave() {
    if(section === 'promotions') {
      const save = form.querySelector('button');
      if(save) save.disabled = !productLookupReady;
    }
  }
  async function loadProductOptions() {
    const revision = ++productLookupRevision, store = select.value;
    productLookupReady = false; syncPromotionSave();
    let loaded = [];
    if(store) loaded = await request('/products?storeId=' + store);
    else if(section === 'promotions' && app.dataset.role === 'admin') {
      loaded = (await Promise.all(shopList.map(async shop =>
        (await request('/products?storeId=' + shop.id)).map(product =>
          ({...product,name:product.name + ' · ' + shop.name}))))).flat();
    }
    if(revision !== productLookupRevision) return false;
    productList = loaded; productLookupReady = true; syncPromotionSave();
    return true;
  }
  async function request(path, method = 'GET', data) {
    const headers = {Accept: 'application/json'};
    if(method !== 'GET') {
      const response = await fetch(context + '/products/csrf', {cache:'no-store'});
      if(!response.ok) throw new Error('Không thể xác minh phiên. Hãy tải lại trang.');
      const csrf = await response.json(); headers[csrf.headerName] = csrf.token;
    }
    if(data && !(data instanceof FormData)) headers['Content-Type'] = 'application/json';
    const response = await fetch(base + path, {method,headers,body:data instanceof FormData ? data : data ? JSON.stringify(data) : undefined});
    if(response.redirected || response.status === 401) throw new Error('Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.');
    const result = await response.json().catch(() => ({}));
    if(!response.ok) throw new Error(result.message || result.detail || (response.status === 403 ? 'Bạn chưa được cấp quyền cho thao tác này.' : response.status === 409 ? 'Dữ liệu trùng hoặc đã thay đổi. Hãy tải lại.' : 'Yêu cầu không thành công (' + response.status + '). Kiểm tra dữ liệu hoặc đăng nhập lại.'));
    return result;
  }
  function field(name,label,type = 'text',value) {
    const group = el('div'); const caption = el('label',label,'form-label'); caption.htmlFor = 'manage-' + name; group.append(caption);
    const options = {category:categories,topping:toppings,toppings:toppings,products:productList,kind:[{id:'PRODUCT_PERCENT',name:'Giảm giá sản phẩm'},{id:'SHIPPING_PERCENT',name:'Giảm phí giao'}]};
    const input = el(options[type] ? 'select' : type === 'textarea' ? 'textarea' : 'input'); input.name = name; input.id = caption.htmlFor;
    input.className = type === 'checkbox' ? 'form-check-input d-block' : options[type] ? 'form-select' : 'form-control';
    if(options[type]) {
      input.multiple = ['toppings','products'].includes(type);
      options[type].forEach(o => { const option = el('option',o.name); option.value = o.id; option.selected = input.multiple ? (value || []).includes(o.id) : String(value) === String(o.id); input.append(option); });
      if(type === 'category' && value != null && !options[type].some(o => String(o.id) === String(value))) {
        const option = el('option','Danh mục #' + value + ' (đã ngừng hoạt động)');
        option.value = value; option.selected = true; input.append(option);
        group.append(el('small','Danh mục hiện tại đã ngừng hoạt động. Giữ nguyên hoặc chủ động chọn danh mục thay thế.','field-help'));
      }
      if(type === 'products') {
        const known = new Set(options[type].map(o => String(o.id)));
        for(const id of value || []) if(!known.has(String(id))) {
          const option = el('option','Sản phẩm #' + id + ' (đã chọn)');
          option.value = id; option.selected = true; input.append(option);
        }
      }
    } else if(type === 'checkbox') { input.type = 'checkbox'; input.checked = value === undefined ? ['active','available'].includes(name) : Boolean(value); }
    else { if(type !== 'textarea') input.type = type; input.value = value ?? (type === 'number' ? (name.includes('Limit') ? 1 : 0) : ''); }
    if(type === 'datetime-local' && value) { const date = new Date(value); input.value = new Date(date - date.getTimezoneOffset()*60000).toISOString().slice(0,16); }
    if(type === 'time') input.step = '1';
    if(type === 'number') { input.min = name.includes('Limit') || name.endsWith('Id') ? '1' : '0'; input.step = name === 'percent' ? '0.01' : '1'; if(name === 'percent') input.max = '100'; }
    input.required = !['checkbox','textarea','toppings','products'].includes(type) && name !== 'code' && name !== 'catalogKey';
    if(type === 'text') input.maxLength = name === 'phone' ? 20 : name === 'code' ? 50 : section === 'categories' || section === 'toppings' ? 100 : 150;
    if(type === 'textarea') input.maxLength = 4000;
    group.append(input);if(input.multiple)group.append(el('small','Giữ Ctrl (Windows) hoặc ⌘ (Mac) để chọn nhiều mục; bỏ chọn tất cả nếu không áp dụng.','field-help'));if(name==='slug')group.append(el('small','Đường dẫn không dấu, viết thường, ngăn cách bằng dấu gạch ngang.','field-help'));return group;
  }
  function edit(item = null) {
    document.getElementById('management-editor').hidden=section==='stores'&&app.dataset.role!=='vendor'&&!item;
    editing = item;list.querySelectorAll('article').forEach(card=>card.classList.toggle('record-editing',Boolean(item)&&card.dataset.recordId===String(item.id)));form.replaceChildren(); document.querySelector('#catalog-form-title').textContent = (item ? 'Cập nhật ' : 'Tạo / cấu hình ') + labels[section];
    specs[section].forEach(([name,label,type]) => form.append(field(name,label,type,item?.[name])));
    if(section === 'products') ['S','M','L'].forEach(size => {
      const old = item?.variants.find(v => v.sizeName === size);
      form.append(field('size_' + size,'Bán size ' + size,'checkbox',old ? old.active : size === 'M'));
      form.append(field('extra_' + size,'Phụ thu size ' + size + ' (đ)','number',old?.extraPrice || 0));
      const quantity = field('stock_' + size,'Số ly còn nhận size ' + size + ' (trống: giữ hiện có)','number',old?.stockQuantity ?? '');
      const input = quantity.querySelector('input'); if(input) {input.required=false;input.min='0';input.max='1000000';input.step='1';} form.append(quantity);
    });
    const save = el('button',item ? 'Lưu thay đổi' : 'Lưu '+labels[section].toLocaleLowerCase('vi'),'btn btn-dark'); form.append(save); syncPromotionSave();
    const reset = el('button',item?'Hủy chỉnh sửa / thêm mới':'Làm trống biểu mẫu','btn btn-outline-secondary'); reset.type = 'button'; reset.onclick = () => edit(); form.append(reset);
    if(section === 'stores' && !item) { save.disabled = true; form.append(el('p','Đăng ký shop mới tại trang Đăng ký shop. Chọn một shop để cập nhật.','small text-secondary')); }
    document.querySelector('#image-panel').hidden = !item || !['products','stores'].includes(section);
    if(item) document.querySelector('#image-type').value = section === 'products' ? 'product' : 'store';
  }
  function button(text, action) {
    const b = el('button',text,'btn btn-sm btn-outline-dark'); b.type = 'button';
    b.onclick = async () => { if(loading) return; loading = true; b.disabled = true; try { await action(); } catch(error) { notice(error.message,true); } finally { b.disabled = false; loading = false; } }; return b;
  }
  async function review(item, decision, card) {
    card.querySelector('[data-review-form]')?.remove();
    const reviewForm = el('form',undefined,'vstack gap-2 mt-3'); reviewForm.dataset.reviewForm = '';
    const noteLabel = el('label',decision === 'APPROVED' ? 'Ghi chú duyệt hồ sơ' : 'Lý do từ chối','form-label');
    const note = el('textarea'); note.id = 'review-note-' + item.id; noteLabel.htmlFor = note.id;
    note.required = true; note.maxLength = 1000; note.className = 'form-control'; reviewForm.append(noteLabel,note);
    let policy;
    if(decision === 'APPROVED') {
      const policies = await request('/stores/' + item.id + '/commission-policies');
      if(!policies.length) throw new Error('Chưa có chính sách hoa hồng đang hoạt động. Hãy liên hệ quản trị trước khi duyệt shop.');
      const label = el('label','Chính sách hoa hồng','form-label'); policy = el('select'); policy.id = 'review-policy-' + item.id; label.htmlFor = policy.id;
      policy.className = 'form-select'; policy.required = true;
      const placeholder = el('option','Chọn chính sách'); placeholder.value = ''; policy.append(placeholder);
      policies.forEach(p => { const option = el('option',p.name + ' · ' + p.ratePercent + '%'); option.value = p.id; policy.append(option); });
      reviewForm.append(label,policy);
    }
    const save = el('button',decision === 'APPROVED' ? 'Xác nhận duyệt' : 'Xác nhận từ chối','btn btn-dark');
    const cancel = el('button','Hủy','btn btn-outline-secondary'); cancel.type = 'button'; cancel.onclick = () => reviewForm.remove();
    reviewForm.append(save,cancel);
    reviewForm.onsubmit = async event => {
      event.preventDefault(); if(loading) return;
      if(!note.value.trim()) { note.setCustomValidity('Vui lòng nhập nội dung phản hồi.'); note.reportValidity(); return; }
      loading = true; save.disabled = true;
      try { await request('/stores/' + item.id + '/review','POST',{decision,note:note.value.trim(),version:item.version,commissionPolicyId:policy ? Number(policy.value) : null}); await reload(); notice('Đã xử lý hồ sơ.'); }
      catch(error) { notice(error.message,true); } finally { loading = false; save.disabled = false; }
    };
    note.oninput = () => note.setCustomValidity(''); card.append(reviewForm); note.focus();
  }
  function rememberStore() {
    const url = new URL(location.href);
    if(select.value) url.searchParams.set('storeId',select.value); else url.searchParams.delete('storeId');
    history.replaceState(null,'',url);
    document.querySelectorAll('[data-catalog-tab]').forEach(tab => {
      const target = new URL(context + '/' + app.dataset.role + '/' + tab.dataset.catalogTab,location.origin);
      if(select.value) target.searchParams.set('storeId',select.value); tab.href = target.pathname + target.search;
    });
  }
  async function refresh() {
    document.querySelectorAll('[data-catalog-tab]').forEach(tab => {
      const active = tab.dataset.catalogTab === section;
      tab.classList.toggle('btn-primary', active); tab.classList.toggle('btn-outline-dark', !active);
      if(active) tab.setAttribute('aria-current','page'); else tab.removeAttribute('aria-current');
    });
    list.replaceChildren(el('p','Đang tải…'));
    const storeRequired = ['products','areas','stock','memberships'].includes(section) || (section === 'promotions' && app.dataset.role !== 'admin');
    if (app.dataset.role === 'admin' && select.options[0]?.value === '') select.options[0].textContent = section === 'promotions' ? 'Toàn hệ thống (khuyến mãi)' : 'Chọn cửa hàng để quản lý';
    if (storeRequired && !select.value) {
      editing = null;
      document.querySelector('#catalog-list-title').textContent = labels[section];
      const empty = el('div',undefined,'empty-state');
      empty.append(el('h3','Bắt đầu từ một cửa hàng.'),el('p',shopList.length ? 'Chọn cửa hàng ở phía trên để xem và quản lý dữ liệu.' : 'Chưa có cửa hàng trong phạm vi của bạn. Khi cửa hàng được duyệt hoặc phân công, dữ liệu sẽ xuất hiện tại đây.'));
      if(app.dataset.role === 'vendor') {const link=el('a','Đăng ký cửa hàng','btn btn-primary');link.href=context+'/account/shop-registration';empty.append(link);}
      else {const link=el('a','Xem cửa hàng','btn btn-outline-dark');link.href=context+'/'+app.dataset.role+'/stores';empty.append(link);}
      list.replaceChildren(empty); form.replaceChildren(el('p','Chọn cửa hàng trước khi tạo hoặc cập nhật dữ liệu.','small text-secondary'));
      document.querySelector('#catalog-form-title').textContent = 'Sẵn sàng cho cửa hàng của bạn';
      document.querySelector('#image-panel').hidden = true;
      return;
    }
    try {
      let data = [];
      if(section === 'stores') data = shopList;
      else if(section === 'areas') data = shopList.find(s => String(s.id) === select.value)?.areas || [];
      else if(section === 'stock' || section === 'memberships') { edit(); list.replaceChildren(el('p','Chọn dữ liệu bên phải để cập nhật cấu hình.')); return; }
      else data = await request('/' + section + (['products','promotions'].includes(section) && select.value ? '?storeId=' + select.value : ''));
      list.replaceChildren(); document.querySelector('#catalog-list-title').textContent = labels[section] + ' (' + data.length + ')';
      data.forEach(item => {
        const card = el('article',undefined,'card p-3');card.dataset.recordId=String(item.id);card.dataset.search=((item.name||'')+' '+(item.address||'')+' '+(item.code||'')).toLocaleLowerCase('vi');card.append(el('h3',item.name,'h5'));
        const info = section === 'stores' ? item.address + ' · ' + (states[item.status]||item.status) : section === 'products' ? new Intl.NumberFormat('vi-VN').format(item.basePrice) + ' đ · ' + (item.active ? 'Hoạt động' : 'Ngừng bán') : section === 'promotions' ? item.percent + '% · ' + (item.active ? 'Bật' : 'Tắt') : item.active ? 'Hoạt động' : 'Ngừng hoạt động';
        card.append(el('p',info,'small text-secondary')); const actions = el('div',undefined,'d-flex flex-wrap gap-2');
        actions.append(button('Chỉnh sửa',() => {edit(item);document.getElementById('management-editor').scrollIntoView({behavior:'smooth',block:'start'});form.querySelector('input,select,textarea')?.focus({preventScroll:true});}));
        if(section === 'products') actions.append(button('Ngừng bán',async () => { if(!await window.utetraConfirm('Ngừng bán sản phẩm này?')) return; await request('/products/' + item.id + '?version=' + item.version,'DELETE'); await refresh(); }));
        if(section === 'stores' && app.dataset.role !== 'vendor') {
          if(item.status === 'PENDING_APPROVAL') { actions.append(button('Duyệt',() => review(item,'APPROVED',card)),button('Từ chối',() => review(item,'REJECTED',card))); }
          if(['ACTIVE','SUSPENDED'].includes(item.status)) actions.append(button(item.status === 'ACTIVE' ? 'Tạm khóa' : 'Mở lại', async () => {
            await request('/stores/' + item.id + '/state','PUT',{status:item.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',version:item.version}); await reload();
          }));
        }
        card.append(actions); list.append(card);
      });
      if(!data.length) list.append(el('p','Chưa có dữ liệu trong phạm vi này.','alert alert-light border'));
      filterRecords();edit();
    } catch(error) { list.replaceChildren(el('p',error.message,'alert alert-warning')); edit(); }
  }
  async function reload() {
    productLookupReady = false; syncPromotionSave();
    const previous = select.value || new URL(location.href).searchParams.get('storeId') || new URL(location.href).searchParams.get('store'); shopList = await request('/stores'); select.replaceChildren();
    if(app.dataset.role === 'admin') { const o = el('option','Toàn hệ thống (khuyến mãi)'); o.value = ''; select.append(o); }
    shopList.forEach(s => { const o = el('option',s.name + ' · ' + (states[s.status]||s.status)); o.value = s.id; select.append(o); });
    if(!select.options.length) {const o=el('option','Chưa có cửa hàng được phân công');o.value='';select.append(o);}
    if([...select.options].some(o => o.value === previous)) select.value = previous;
    rememberStore();
    const lookups = await fetch(context + '/products/lookups').then(r => r.ok ? r.json() : {categories:[]}); categories = lookups.categories;
    toppings = await request('/toppings');
    if(await loadProductOptions()) await refresh();
  }
  form.addEventListener('submit', async event => {
    event.preventDefault(); if(loading) return; loading = true; const save = form.querySelector('button'); save.disabled = true;
    try {
      if(section === 'promotions' && !productLookupReady) throw new Error('Chưa tải được danh sách sản phẩm. Hãy tải lại trước khi lưu khuyến mãi.');
      const values = new FormData(form), data = {};
      specs[section].forEach(([name,,type]) => { data[name] = type === 'checkbox' ? form.elements[name].checked : ['toppings','products'].includes(type) ? values.getAll(name).map(Number) : type === 'number' || ['category','topping'].includes(type) ? Number(values.get(name)) : type === 'datetime-local' ? new Date(values.get(name)).toISOString() : values.get(name); });
      let path = '/' + section + (editing ? '/' + editing.id : ''), method = editing ? 'PUT' : 'POST'; data.version = editing?.version || 0;
      if(section === 'products') {
        data.storeId = Number(select.value); data.variants = ['S','M','L'].filter(s => form.elements['size_' + s].checked).map(s => ({sizeName:s,extraPrice:Number(values.get('extra_' + s)),active:true,stockQuantity:values.get('stock_' + s)===''?null:Number(values.get('stock_' + s)),stockVersion:editing?.variants.find(v=>v.sizeName===s)?.version??null}));
      }
      if(section === 'promotions') { data.storeId = select.value ? Number(select.value) : null; if(!data.code) data.code = null; }
      if(['areas','stock','memberships'].includes(section)) {
        if(!select.value) throw new Error('Hãy chọn shop.');
        path = '/stores/' + select.value + (section === 'areas' ? '/areas' + (editing ? '/' + editing.id : '') : section === 'stock' ? '/toppings/' + data.toppingId : '/memberships?catalogDelegation=' + data.catalogDelegation);
        if(section === 'stock') method = 'PUT';
      }
      await request(path,method,data); await reload(); notice('Đã lưu thành công.');
    } catch(error) { notice(error.message,true); } finally { save.disabled = false; loading = false; }
  });
  tabs.forEach(tab => {
    const link = el('a',labels[tab],tab === section ? 'btn btn-primary' : 'btn btn-outline-dark'); link.dataset.catalogTab = tab;
    link.href = context + '/' + app.dataset.role + '/' + tab;
    if(tab === section) link.setAttribute('aria-current','page'); document.querySelector('#catalog-tabs').append(link);
  });
  select.addEventListener('change',async () => { try { rememberStore(); if(await loadProductOptions()) await refresh(); } catch(error) { notice(error.message,true); } });
  document.querySelector('#catalog-reload').onclick = () => reload().catch(error => notice(error.message,true));
  document.querySelector('#catalog-image').addEventListener('submit',async event => {
    event.preventDefault(); if(!editing || loading) return; loading = true;
    try { const type = document.querySelector('#image-type').value;
      if((section === 'products') !== (type === 'product')) throw new Error('Chọn đúng loại ảnh cho mục đang sửa.');
      const data = new FormData(); data.set('file',document.querySelector('#image-file').files[0]); data.set('version',editing.version);
      await request('/media/' + type + '/' + editing.id,'POST',data); await reload(); notice('Đã cập nhật ảnh.');
    } catch(error) { notice(error.message,true); } finally { loading = false; }
  });
  reload().catch(error => notice(error.message,true));
})();
