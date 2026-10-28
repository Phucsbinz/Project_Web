'use strict';
window.utetra = Object.freeze({
  uuid() {
    if(crypto.randomUUID)return crypto.randomUUID();
    const bytes=crypto.getRandomValues(new Uint8Array(16));bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
    const hex=Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');return hex.slice(0,8)+'-'+hex.slice(8,12)+'-'+hex.slice(12,16)+'-'+hex.slice(16,20)+'-'+hex.slice(20);
  },
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
