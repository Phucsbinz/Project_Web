package com.utetra.order.service;
import java.net.URI;
public final class QrPublicOrigin {
 private QrPublicOrigin(){}
 public static String resolve(String configured,String tunnel,boolean isolatedTest){
  String value=tunnel==null?configured:tunnel;
  try{var uri=URI.create(value);String host=uri.getHost();boolean loopback="localhost".equalsIgnoreCase(host)||"127.0.0.1".equals(host)||"::1".equals(host)||"[::1]".equals(host);
   if(host!=null&&uri.getUserInfo()==null&&uri.getRawQuery()==null&&uri.getRawFragment()==null&&(uri.getPath()==null||uri.getPath().isEmpty())&&(("https".equals(uri.getScheme())&&!loopback)||isolatedTest&&loopback&&"http".equals(uri.getScheme())))return value;
  }catch(IllegalArgumentException ignored){}
  throw OrderProblem.conflict("QR Cloudflare chưa sẵn sàng. Hãy khởi động tunnel; hệ thống không tạo mã QR localhost.");
 }
}
