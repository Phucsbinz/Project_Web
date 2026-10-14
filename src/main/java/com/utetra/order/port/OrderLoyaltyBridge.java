package com.utetra.order.port;

import com.utetra.order.repository.CommerceReferenceRepository;
import com.utetra.order.service.OrderProblem;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Shared identity-owned contract used inside the order transaction. No HTTP ledger writes. */
@Service
public class OrderLoyaltyBridge {
  private final CommerceReferenceRepository data;
  private final Clock clock;

  public OrderLoyaltyBridge(CommerceReferenceRepository data, Clock clock) {
    this.data = data;
    this.clock = clock;
  }

  @Transactional
  public void record(long user, long order, String type, int points) {
    if (points < 0) throw OrderProblem.invalid("Số điểm không hợp lệ.");
    if (points == 0) return;
    data.user(user, true);
    var accounts = data.list("select * from loyalty_accounts where user_id=? for update", user);
    if (accounts.isEmpty()) {
      data.write(
          "insert into loyalty_accounts(user_id,balance,debt,lifetime_earned,updated_at,version)"
              + " values(?,0,0,0,?,0)",
          user,
          clock.instant());
      accounts = data.list("select * from loyalty_accounts where user_id=? for update", user);
    }
    Map<String, Object> a = accounts.getFirst();
    long id = CommerceReferenceRepository.number(a, "id");
    if (!data.list("select id from loyalty_transactions where order_id=? and type=?", order, type)
        .isEmpty()) return;
    int balance = ((Number) a.get("balance")).intValue(),
        debt = ((Number) a.get("debt")).intValue(),
        earned = ((Number) a.get("lifetime_earned")).intValue();
    int delta;
    switch (type) {
      case "REDEEM" -> {
        if (debt > 0 || balance < points) throw OrderProblem.conflict("Điểm khả dụng không đủ.");
        balance -= points;
        delta = -points;
      }
      case "EARN", "REFUND" -> {
        int repay = Math.min(debt, points);
        debt -= repay;
        balance = Math.addExact(balance, points - repay);
        if (type.equals("EARN")) earned = Math.addExact(earned, points);
        delta = points;
      }
      case "REVERSAL" -> {
        int deduct = Math.min(balance, points);
        balance -= deduct;
        debt = Math.addExact(debt, points - deduct);
        earned = Math.max(0, earned - points);
        delta = -points;
      }
      default -> throw OrderProblem.invalid("Loại giao dịch điểm không hợp lệ.");
    }
    data.write(
        "update loyalty_accounts set"
            + " balance=?,debt=?,lifetime_earned=?,updated_at=?,version=version+1 where id=?",
        balance,
        debt,
        earned,
        clock.instant(),
        id);
    data.write(
        "insert into"
            + " loyalty_transactions(account_id,order_id,type,points_delta,balance_after,debt_after,created_at)"
            + " values(?,?,?,?,?,?,?)",
        id,
        order,
        type,
        delta,
        balance,
        debt,
        clock.instant());
  }
}
