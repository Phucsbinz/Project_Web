'use strict';
window.utetra = Object.freeze({
  csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    return token && header ? {[header]: token} : {};
  },
  websocketUrl() {
    const context = document.querySelector('meta[name="app-context"]')?.content || '';
    return `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}${context}/ws`;
  }
});
