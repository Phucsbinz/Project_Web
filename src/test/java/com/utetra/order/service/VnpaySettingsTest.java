package com.utetra.order.service;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class VnpaySettingsTest {
  private VnpaySettings settings(boolean enabled,String gateway) {
    return new VnpaySettings(enabled,"TESTCODE","test-only-merchant-secret-1234567890",gateway,
        "https://merchant.example.invalid/payments/vnpay/return","https://merchant.example.invalid/payments/vnpay/ipn");
  }
  @Test void signingMatchesIndependentKnownVectorAndEncodesFieldsInSortedOrder() {
    var fields=Map.of("vnp_TmnCode","TESTCODE","vnp_OrderInfo","UTETra order 1","vnp_Amount","1000000");
    assertThat(VnpaySettings.canonical(fields)).isEqualTo("vnp_Amount=1000000&vnp_OrderInfo=UTETra+order+1&vnp_TmnCode=TESTCODE");
    assertThat(settings(true,"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html").sign(fields))
        .isEqualTo("4189e3aa72ae3c423f352d8a6cc8173f95b227c2e95f8cc0388164200120f39111c0bcc8c7191a4bbaadbd611928f284a0342ccb04750e2007ebac465ff12b1c");
  }
  @Test void disabledOrUntrustedGatewayCannotBeOffered() {
    assertThat(settings(false,"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html").available()).isFalse();
    assertThat(settings(true,"https://example.invalid/phishing").available()).isFalse();
    assertThat(settings(true,"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html").available()).isTrue();
  }
}
