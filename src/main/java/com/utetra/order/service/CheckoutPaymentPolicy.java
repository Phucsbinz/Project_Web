package com.utetra.order.service;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Checkout allowlist is independent of archived provider configuration and history. */
@Component
public class CheckoutPaymentPolicy {
 private final Set<String> allowed;
 public CheckoutPaymentPolicy(@Value("${order.payment-methods:COD,LOCAL_QR}") String configured){allowed=Set.copyOf(Arrays.stream(configured.split(",")).map(String::trim).filter(v->!v.isEmpty()).toList());}
 public void require(String requested){if(!allowed.contains(requested==null?"COD":requested))throw OrderProblem.invalid("Phương thức này không còn dùng cho đơn mới. Vui lòng chọn COD hoặc QR.");}
}
