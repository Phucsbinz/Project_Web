package com.utetra.order.service;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VnpaySettings {
  final boolean enabled;
  final String terminal,secret,paymentUrl,returnUrl,ipnUrl;
  public VnpaySettings(@Value("${payment.vnpay.enabled:false}") boolean enabled,
      @Value("${payment.vnpay.tmn-code:}") String terminal,
      @Value("${payment.vnpay.hash-secret:}") String secret,
      @Value("${payment.vnpay.payment-url:}") String paymentUrl,
      @Value("${payment.vnpay.return-url:}") String returnUrl,
      @Value("${payment.vnpay.ipn-url:}") String ipnUrl) {
    this.enabled=enabled;this.terminal=terminal;this.secret=secret;this.paymentUrl=paymentUrl;this.returnUrl=returnUrl;this.ipnUrl=ipnUrl;
  }
  public boolean available() {
    if(!enabled || !terminal.matches("[A-Za-z0-9]{3,20}") || secret.length()<16) return false;
    try {
      URI gateway=URI.create(paymentUrl),callback=URI.create(returnUrl),ipn=URI.create(ipnUrl);
      return "https".equals(gateway.getScheme()) && Set.of("sandbox.vnpayment.vn","pay.vnpay.vn").contains(gateway.getHost())
          && gateway.getRawQuery()==null && gateway.getUserInfo()==null && gateway.getFragment()==null
          && Set.of("http","https").contains(callback.getScheme()) && callback.getHost()!=null && callback.getUserInfo()==null
          && "https".equals(ipn.getScheme()) && ipn.getHost()!=null && ipn.getUserInfo()==null;
    } catch(RuntimeException e) {return false;}
  }
  public Duration timeout() { return Duration.ofMinutes(15); }
  public static String canonical(Map<String,String> fields) {
    return new TreeMap<>(fields).entrySet().stream()
        .filter(e->e.getKey().startsWith("vnp_") && !Set.of("vnp_SecureHash","vnp_SecureHashType").contains(e.getKey()))
        .filter(e->e.getValue()!=null && !e.getValue().isEmpty())
        .map(e->URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8))
        .collect(java.util.stream.Collectors.joining("&"));
  }
  public String sign(Map<String,String> fields) {
    try {
      Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
      return HexFormat.of().formatHex(mac.doFinal(canonical(fields).getBytes(StandardCharsets.UTF_8)));
    } catch(java.security.GeneralSecurityException e) {throw new IllegalStateException(e);}
  }
  public boolean valid(Map<String,String> fields) {
    String hash=fields.get("vnp_SecureHash");
    return available() && hash!=null && hash.matches("[a-fA-F0-9]{128}")
        && terminal.equals(fields.get("vnp_TmnCode"))
        && MessageDigest.isEqual(sign(fields).getBytes(StandardCharsets.US_ASCII),hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
  }
}
