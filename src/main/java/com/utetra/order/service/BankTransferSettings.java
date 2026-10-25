package com.utetra.order.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Public receiving details only; no banking login or authorization credential. */
@Component
public class BankTransferSettings {
  private final boolean enabled;
  private final String bank, account, owner;

  public BankTransferSettings(
      @Value("${payment.bank.enabled:false}") boolean enabled,
      @Value("${payment.bank.bank-name:}") String bank,
      @Value("${payment.bank.account-number:}") String account,
      @Value("${payment.bank.account-name:}") String owner) {
    this.enabled = enabled;
    this.bank = bank;
    this.account = account;
    this.owner = owner;
  }

  public boolean available() {
    return enabled && !bank.isBlank() && account.matches("[0-9]{6,30}") && !owner.isBlank();
  }

  public String method(String requested) {
    String method = requested == null ? "COD" : requested;
    if (!"COD".equals(method) && !"BANK_TRANSFER".equals(method))
      throw OrderProblem.invalid("Phương thức thanh toán không hợp lệ.");
    if ("BANK_TRANSFER".equals(method) && !available())
      throw OrderProblem.conflict("Chuyển khoản ngân hàng hiện chưa sẵn sàng.");
    return method;
  }

  public Map<String, String> instructions(long orderId) {
    return Map.of("bank", bank, "account", account, "owner", owner,
        "memo", "UT" + orderId, "qrPath", "/assets/images/payment/techcombank-utetra.jpg");
  }
}
