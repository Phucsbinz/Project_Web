'use strict';
window.orders = {
  async send(path, value) {
    const context = document.querySelector('meta[name="app-context"]')?.content || '';
    const multipart = value instanceof FormData;
    const response = await fetch(context + path, {method: 'POST', headers: {...(multipart ? {} : {'Content-Type': 'application/json'}), ...utetra.csrfHeaders()}, body: multipart ? value : JSON.stringify(value)});
    const result = response.headers.get('Content-Type')?.includes('application/json') ? await response.json() : {};
    if (!response.ok) throw new Error(result.error || 'Thao tác thất bại. Hãy tải lại dữ liệu.');
    return result;
  },
  message(text, error = false) {
    const node = document.getElementById('order-message');
    if (node) { node.className = 'alert ' + (error ? 'alert-danger' : 'alert-success'); node.textContent = text; node.hidden = false; }
  },
  async action(path, value, reload = true) {
    try { const result = await this.send(path, value); this.message('Đã cập nhật thành công.'); if (reload) location.reload(); return result; }
    catch (error) { this.message(error.message, true); }
  }
};

// Native STOMP over the shared /ws endpoint. SQL-backed pages and polling remain
// authoritative; websocket events only tell the user to refresh committed data.
document.addEventListener('DOMContentLoaded', () => {
  if (!document.querySelector('meta[name="_csrf"]') || !document.querySelector('a[href$="/notifications"]')) return;
  let socket, retry = 1000, timer, stopped = false, buffer = '';
  const context = document.querySelector('meta[name="app-context"]')?.content || '';
  function connect() {
    if (stopped) return;
    buffer = '';
    socket = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + context + '/ws');
    socket.onopen = () => {
      const header = document.querySelector('meta[name="_csrf_header"]').content;
      const token = document.querySelector('meta[name="_csrf"]').content;
      socket.send('CONNECT\naccept-version:1.2\nhost:' + location.host + '\nheart-beat:0,0\n' + header + ':' + token + '\n\n\0');
    };
    socket.onmessage = event => {
      buffer += event.data;
      let end;
      while ((end = buffer.indexOf('\0')) >= 0) {
        const frame = buffer.slice(0, end).replace(/^\n+/, ''); buffer = buffer.slice(end + 1);
        if (frame.startsWith('CONNECTED\n')) {
          retry = 1000;
          ['orders', 'notifications'].forEach(name => socket.send('SUBSCRIBE\nid:order-ui-' + name + '\ndestination:/user/queue/' + name + '\nack:auto\n\n\0'));
        } else if (frame.startsWith('MESSAGE\n')) {
          orders.message('Có cập nhật mới về đơn hàng hoặc thông báo. Tải lại trang để xem dữ liệu mới nhất.');
        } else if (frame.startsWith('ERROR\n')) socket.close();
      }
    };
    socket.onclose = event => {
      if (event.code === 1008) stopped = true;
      if (!stopped) { timer = setTimeout(connect, retry); retry = Math.min(retry * 2, 30000); }
    };
    socket.onerror = () => socket.close();
  }
  window.addEventListener('pagehide', () => { stopped = true; clearTimeout(timer); socket?.close(); });
  connect();
});
