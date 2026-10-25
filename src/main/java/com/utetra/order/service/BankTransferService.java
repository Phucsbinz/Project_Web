package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class BankTransferService {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final OrderSupport support;
  private final Clock clock;

  public BankTransferService(OrderDataRepository data, CommerceReferenceRepository refs,
      OrderSupport support, Clock clock) {
    this.data = data;
    this.refs = refs;
    this.support = support;
    this.clock = clock;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Map<String, Object> confirm(Actor actor, long id, BankConfirmation request) {
    refs.requireCurrent(actor, false);
    // One central receiving account: only the account's authorized administrator
    // reconciles receipts; shop operators and buyers cannot assert money arrived.
    if (!actor.is("ADMIN")) throw OrderProblem.forbidden();
    Order o = data.require(Order.class, id, true);
    if (!"BANK_TRANSFER".equals(o.paymentMethod))
      throw OrderProblem.conflict("Đơn không dùng chuyển khoản ngân hàng.");
    String reference = OrderSupport.required(request.reference(), 100).toUpperCase(Locale.ROOT);
    if (!reference.matches("[A-Z0-9][A-Z0-9._/-]{2,99}"))
      throw OrderProblem.invalid("Mã giao dịch ngân hàng không hợp lệ.");
    var amount = OrderSupport.amount(request.amount());
    if (amount.signum() <= 0 || amount.compareTo(o.finalAmount) != 0)
      throw OrderProblem.conflict("Số tiền nhận phải khớp chính xác tổng đơn.");
    PaymentTransaction p = data.query(PaymentTransaction.class,
        "where e.orderId=:order and e.provider='BANK_TRANSFER'", Map.of("order", id), true, 1)
        .stream().findFirst().orElseThrow(OrderProblem::missing);
    if ("SUCCEEDED".equals(p.status)) {
      if (reference.equals(p.providerTxnId) && amount.compareTo(p.amount) == 0)
        return support.view(o); // Safe retry, no duplicate audit/event/loyalty.
      throw OrderProblem.conflict("Đơn đã đối soát với giao dịch khác.");
    }
    if (o.version != request.version()) throw OrderProblem.conflict("Đơn đã thay đổi; tải lại trước khi xác nhận.");
    if (!Set.of("NEW", "CANCELLED").contains(o.status)
        || !Set.of("UNPAID", "CANCELLED").contains(o.paymentStatus))
      throw OrderProblem.conflict("Trạng thái đơn không cho phép xác nhận chuyển khoản.");
    if (!data.query(PaymentTransaction.class,
        "where e.provider='BANK_TRANSFER' and e.providerTxnId=:reference",
        Map.of("reference", reference), false, 1).isEmpty())
      throw OrderProblem.conflict("Mã giao dịch đã dùng cho đơn khác.");
    p.providerTxnId = reference;
    p.amount = amount;
    p.status = "SUCCEEDED";
    p.paidAt = clock.instant();
    p.updatedAt = clock.instant();
    o.paidAt = p.paidAt;
    o.paymentConfirmedBy = actor.userId();
    // A late transfer must never reopen a cancelled order; keep it for refund.
    o.paymentStatus = "CANCELLED".equals(o.status) ? "REFUND_PENDING" : "PAID";
    o.updatedAt = clock.instant();
    support.audit(actor, "BANK_TRANSFER_CONFIRMED", id);
    support.event(o, actor, o.status, "BANK_TRANSFER_CONFIRMED", reference);
    return support.view(o);
  }
}
