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
  const routeSection = location.pathname.split('/').filter(Boolean).pop();
  let section = ['stores','products','categories','toppings','promotions'].includes(routeSection) ? routeSection : 'stores', editing = null, shopList = [], categories = [], toppings = [], productList = [], loading = false;
  const labels = {stores:'Shop',products:'Sản phẩm',categories:'Danh mục',toppings:'Topping',promotions:'Khuyến mãi',areas:'Khu vực giao',stock:'Topping tại shop',memberships:'Phân công Manager'};
  const specs = {
    stores:[['name','Tên shop'],['slug','Đường dẫn'],['address','Địa chỉ'],['phone','Điện thoại'],['description','Giới thiệu','textarea'],['openTime','Mở cửa','time'],['closeTime','Đóng cửa','time'],['acceptingOrders','Nhận đơn','checkbox']],
    categories:[['name','Tên danh mục'],['slug','Đường dẫn'],['description','Mô tả','textarea'],['displayOrder','Thứ tự','number'],['active','Hoạt động','checkbox']],
    toppings:[['name','Tên topping'],['price','Giá (đ)','number'],['active','Hoạt động','checkbox']],
    products:[['name','Tên sản phẩm'],['slug','Đường dẫn'],['categoryId','Danh mục','category'],['description','Mô tả','textarea'],['basePrice','Giá gốc (đ)','number'],['active','Hoạt động','checkbox'],['available','Sẵn bán','checkbox'],['featured','Nổi bật','checkbox'],['toppingIds','Topping được phép chọn','toppings']],
    promotions:[['name','Tên chương trình'],['code','Mã (có thể bỏ trống)'],['kind','Loại ưu đãi','kind'],['percent','Giảm (%)','number'],['maxDiscount','Giảm tối đa (đ)','number'],['minOrder','Đơn tối thiểu (đ)','number'],['usageLimit','Tổng hạn mức','number'],['perUserLimit','Hạn mức mỗi người','number'],['startsAt','Bắt đầu','datetime-local'],['endsAt','Kết thúc','datetime-local'],['productIds','Sản phẩm áp dụng (bỏ trống: toàn phạm vi)','products'],['active','Hoạt động','checkbox']],
    areas:[['areaCode','Mã khu vực'],['name','Tên khu vực'],['active','Hoạt động','checkbox']],
    stock:[['toppingId','Topping','topping'],['available','Sẵn bán','checkbox']],
    memberships:[['userId','ID tài khoản Manager','number'],['active','Có hiệu lực','checkbox'],['catalogDelegation','Cho phép quản trị danh mục chung','checkbox']]
  };
  function el(tag, text, cls) { const e = document.createElement(tag); if(text !== undefined) e.textContent = text; if(cls) e.className = cls; return e; }
  function notice(text, error = false) { message.textContent = text; message.className = 'alert ' + (error ? 'alert-danger' : 'alert-success'); }
  async function request(path, method = 'GET', data) {
    const headers = {Accept: 'application/json'};
    if(method !== 'GET') {
      const response = await fetch(context + '/products/csrf', {cache:'no-store'});
      if(!response.ok) throw new Error('Không thể xác minh phiên. Hãy tải lại trang.');
      const csrf = await response.json(); headers[csrf.headerName] = csrf.token;
    }
    if(data && !(data instanceof FormData)) headers['Content-Type'] = 'application/json';
    const response = await fetch(base + path, {method,headers,body:data instanceof FormData ? data : data ? JSON.stringify(data) : undefined});
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
    } else if(type === 'checkbox') { input.type = 'checkbox'; input.checked = value === undefined ? ['active','available'].includes(name) : Boolean(value); }
    else { if(type !== 'textarea') input.type = type; input.value = value ?? (type === 'number' ? (name.includes('Limit') ? 1 : 0) : ''); }
    if(type === 'datetime-local' && value) { const date = new Date(value); input.value = new Date(date - date.getTimezoneOffset()*60000).toISOString().slice(0,16); }
    if(type === 'number') { input.min = name.includes('Limit') || name.endsWith('Id') ? '1' : '0'; input.step = name === 'percent' ? '0.01' : '1'; if(name === 'percent') input.max = '100'; }
    input.required = !['checkbox','textarea','toppings','products'].includes(type) && name !== 'code';
    if(type === 'text') input.maxLength = name === 'phone' ? 20 : name === 'code' ? 50 : section === 'categories' || section === 'toppings' ? 100 : 150;
    if(type === 'textarea') input.maxLength = 4000;
    group.append(input); return group;
  }
  function edit(item = null) {
    editing = item; form.replaceChildren(); document.querySelector('#catalog-form-title').textContent = (item ? 'Cập nhật ' : 'Tạo / cấu hình ') + labels[section];
    specs[section].forEach(([name,label,type]) => form.append(field(name,label,type,item?.[name])));
    if(section === 'products') ['S','M','L'].forEach(size => {
      const old = item?.variants.find(v => v.sizeName === size);
      form.append(field('size_' + size,'Bán size ' + size,'checkbox',old ? old.active : size === 'M'));
      form.append(field('extra_' + size,'Phụ thu size ' + size + ' (đ)','number',old?.extraPrice || 0));
    });
    const save = el('button',item ? 'Lưu thay đổi' : 'Lưu','btn btn-dark'); form.append(save);
    const reset = el('button','Tạo / cấu hình mới','btn btn-outline-secondary'); reset.type = 'button'; reset.onclick = () => edit(); form.append(reset);
    if(section === 'stores' && !item) { save.disabled = true; form.append(el('p','Đăng ký shop mới tại trang Đăng ký shop. Chọn một shop để cập nhật.','small text-secondary')); }
    document.querySelector('#image-panel').hidden = !item || !['products','stores'].includes(section);
    if(item) document.querySelector('#image-type').value = section === 'products' ? 'product' : 'store';
  }
  function button(text, action) {
    const b = el('button',text,'btn btn-sm btn-outline-dark'); b.type = 'button';
    b.onclick = async () => { if(loading) return; loading = true; b.disabled = true; try { await action(); } catch(error) { notice(error.message,true); } finally { b.disabled = false; loading = false; } }; return b;
  }
  async function review(item, decision) {
    const note = prompt(decision === 'APPROVED' ? 'Ghi chú duyệt hồ sơ:' : 'Lý do từ chối:'); if(!note?.trim()) return;
    const commissionPolicyId = decision === 'APPROVED' ? Number(prompt('ID chính sách hoa hồng đang hoạt động:')) : null;
    if(decision === 'APPROVED' && !commissionPolicyId) return;
    await request('/stores/' + item.id + '/review','POST',{decision,note,version:item.version,commissionPolicyId}); await reload(); notice('Đã xử lý hồ sơ.');
  }
  async function refresh() {
    document.querySelectorAll('[data-catalog-tab]').forEach(tab => {
      const active = tab.dataset.catalogTab === section;
      tab.classList.toggle('btn-primary', active); tab.classList.toggle('btn-outline-dark', !active);
      tab.setAttribute('aria-pressed', String(active));
    });
    list.replaceChildren(el('p','Đang tải…'));
    const storeRequired = ['products','areas','stock','memberships'].includes(section) || (section === 'promotions' && app.dataset.role !== 'admin');
    if (app.dataset.role === 'admin' && select.options[0]?.value === '') select.options[0].textContent = section === 'promotions' ? 'Toàn hệ thống (khuyến mãi)' : 'Chọn cửa hàng để quản lý';
    if (storeRequired && !select.value) {
      document.querySelector('#catalog-list-title').textContent = labels[section];
      const empty = el('div',undefined,'empty-state');
      empty.append(el('h3','Bắt đầu từ một cửa hàng.'),el('p',shopList.length ? 'Chọn cửa hàng ở phía trên để xem và quản lý dữ liệu.' : 'Chưa có cửa hàng trong phạm vi của bạn. Khi cửa hàng được duyệt hoặc phân công, dữ liệu sẽ xuất hiện tại đây.'));
      if(app.dataset.role === 'vendor') {const link=el('a','Đăng ký cửa hàng','btn btn-primary');link.href=context+'/account/shop-registration';empty.append(link);}
      else empty.append(button('Xem cửa hàng',async()=>{section='stores';await refresh();}));
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
        const card = el('article',undefined,'card p-3'); card.append(el('h3',item.name,'h5'));
        const info = section === 'stores' ? item.address + ' · ' + item.status : section === 'products' ? item.basePrice + ' đ · ' + (item.active ? 'Hoạt động' : 'Ngừng bán') : section === 'promotions' ? item.percent + '% · ' + (item.active ? 'Bật' : 'Tắt') : item.active ? 'Hoạt động' : 'Ngừng hoạt động';
        card.append(el('p',info,'small text-secondary')); const actions = el('div',undefined,'d-flex flex-wrap gap-2');
        actions.append(button('Sửa',() => edit(item)));
        if(section === 'products') actions.append(button('Ngừng bán',async () => { if(!confirm('Ngừng bán sản phẩm này?')) return; await request('/products/' + item.id + '?version=' + item.version,'DELETE'); await refresh(); }));
        if(section === 'stores' && app.dataset.role !== 'vendor') {
          if(item.status === 'PENDING_APPROVAL') { actions.append(button('Duyệt',() => review(item,'APPROVED')),button('Từ chối',() => review(item,'REJECTED'))); }
          if(['ACTIVE','SUSPENDED'].includes(item.status)) actions.append(button(item.status === 'ACTIVE' ? 'Tạm khóa' : 'Mở lại', async () => {
            await request('/stores/' + item.id + '/state','PUT',{status:item.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',version:item.version}); await reload();
          }));
        }
        card.append(actions); list.append(card);
      });
      if(!data.length) list.append(el('p','Chưa có dữ liệu trong phạm vi này.','alert alert-light border'));
      edit();
    } catch(error) { list.replaceChildren(el('p',error.message,'alert alert-warning')); edit(); }
  }
  async function reload() {
    const previous = select.value; shopList = await request('/stores'); select.replaceChildren();
    if(app.dataset.role === 'admin') { const o = el('option','Toàn hệ thống (khuyến mãi)'); o.value = ''; select.append(o); }
    shopList.forEach(s => { const o = el('option',s.name + ' · ' + s.status); o.value = s.id; select.append(o); });
    if(!select.options.length) {const o=el('option','Chưa có cửa hàng được phân công');o.value='';select.append(o);}
    if([...select.options].some(o => o.value === previous)) select.value = previous;
    const lookups = await fetch(context + '/products/lookups').then(r => r.ok ? r.json() : {categories:[]}); categories = lookups.categories;
    toppings = await request('/toppings');
    productList = select.value ? await request('/products?storeId=' + select.value) : [];
    await refresh();
  }
  form.addEventListener('submit', async event => {
    event.preventDefault(); if(loading) return; loading = true; const save = form.querySelector('button'); save.disabled = true;
    try {
      const values = new FormData(form), data = {};
      specs[section].forEach(([name,,type]) => { data[name] = type === 'checkbox' ? form.elements[name].checked : ['toppings','products'].includes(type) ? values.getAll(name).map(Number) : type === 'number' || ['category','topping'].includes(type) ? Number(values.get(name)) : type === 'datetime-local' ? new Date(values.get(name)).toISOString() : values.get(name); });
      let path = '/' + section + (editing ? '/' + editing.id : ''), method = editing ? 'PUT' : 'POST'; data.version = editing?.version || 0;
      if(section === 'products') {
        data.storeId = Number(select.value); data.variants = ['S','M','L'].filter(s => form.elements['size_' + s].checked).map(s => ({sizeName:s,extraPrice:Number(values.get('extra_' + s)),active:true}));
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
  const tabs = app.dataset.role === 'vendor' ? ['stores','products','promotions','areas','stock'] : ['stores','products','categories','toppings','promotions','areas','stock',...(app.dataset.role === 'admin' ? ['memberships'] : [])];
  tabs.forEach(tab => {const item = button(labels[tab],async () => { section = tab; await refresh(); }); item.dataset.catalogTab = tab; document.querySelector('#catalog-tabs').append(item);});
  select.addEventListener('change',async () => { try { productList = select.value ? await request('/products?storeId=' + select.value) : []; await refresh(); } catch(error) { notice(error.message,true); } });
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
