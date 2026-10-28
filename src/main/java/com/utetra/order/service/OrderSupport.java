package com.utetra.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.math.*;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class OrderSupport {
  private final OrderDataRepository data;
  private final CommerceReferenceRepository refs;
  private final SimpMessagingTemplate broker;
  private final Clock clock;
  private final ObjectMapper json;

  public OrderSupport(
      OrderDataRepository data,
      CommerceReferenceRepository refs,
      SimpMessagingTemplate broker,
      Clock clock,
      ObjectMapper json) {
    this.data = data;
    this.refs = refs;
    this.broker = broker;
    this.clock = clock;
    this.json = json;
  }

  public static BigDecimal amount(BigDecimal value) {
    if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("999999999999")) > 0)
      throw OrderProblem.invalid("Số tiền không hợp lệ.");
    try {
      return value.setScale(0, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw OrderProblem.invalid("Tiền VND phải là số nguyên đồng.");
    }
  }

  public String hash(Object value) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(value)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String required(String v, int max) {
    if (v == null || v.trim().isEmpty() || v.trim().length() > max)
      throw OrderProblem.invalid("Nội dung bắt buộc không hợp lệ.");
    return v.trim();
  }

  public static String note(String v, int max) {
    return v == null ? "" : (v.length() <= max ? v.trim() : required(v, max));
  }

  public void buyer(Actor actor) {
    refs.requireCurrent(actor, false);
    if (!actor.buyer()) throw OrderProblem.forbidden();
  }

  public void audit(Actor actor, String action, long id) {
    refs.write(
        "insert into audit_logs(actor_id,action,entity_type,entity_id,summary,created_at)"
            + " values(?,?,?,?,?,?)",
        actor.userId(),
        action,
        "ORDER",
        id,
        action,
        clock.instant());
  }

  public void event(Order order, Actor actor, String from, String type, String reason) {
    if (!TransactionSynchronizationManager.isSynchronizationActive())
      throw new IllegalStateException("Order events require a transaction");
    data.flush(); // @Version is the persisted version used by history and websocket consumers.
    OrderStatusHistory h = new OrderStatusHistory();
    h.orderId = order.id;
    h.fromStatus = from;
    h.toStatus = order.status;
    h.eventType = type;
    h.changedBy = actor == null ? null : actor.userId();
    h.reason = note(reason, 500);
    h.orderVersion = order.version;
    h.createdAt = clock.instant();
    data.save(h);
    LinkedHashSet<Long> recipients = new LinkedHashSet<>(refs.recipients(order.storeId));
    recipients.add(order.userId);
    if(order.groupOrderId!=null)for(var member:data.query(GroupOrderMember.class,"where e.orderId=:order and e.groupOrderId=:group",Map.of("order",order.id,"group",order.groupOrderId),false,51))recipients.add(member.userId);
    for (var assignment :
        data.query(
            DeliveryAssignment.class,
            "where e.orderId=:order and e.status in ('ACTIVE','COMPLETED')",
            Map.of("order", order.id),
            false,
            1)) recipients.add(assignment.shipperId);
    for (Long recipient : recipients) {
      Notification n = new Notification();
      n.userId = recipient;
      n.orderId = order.id;
      n.eventKey = "order:" + order.id + ":" + order.version;
      n.type = type;
      n.title = "Đơn " + order.orderCode;
      n.content = "Trạng thái: " + order.status;
      n.createdAt = clock.instant();
      data.save(n);
    }
    Map<String, Object> payload =
        Map.of(
            "orderId",
            order.id,
            "version",
            order.version,
            "status",
            order.status,
            "eventId",
            "order:" + order.id + ":" + order.version);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              for (Long recipient : recipients) {
                broker.convertAndSendToUser(recipient.toString(), "/queue/orders", payload);
                broker.convertAndSendToUser(recipient.toString(), "/queue/notifications", payload);
              }
              broker.convertAndSend("/topic/stores/" + order.storeId + "/orders", payload);
              broker.convertAndSend("/topic/admin/orders", payload);
            } catch (org.springframework.messaging.MessagingException unavailable) {
              // The order and notifications are already committed. SQL polling and
              // reconnect recover them; a broker outage must not report a failed purchase.
              org.slf4j.LoggerFactory.getLogger(OrderSupport.class)
                  .warn("Realtime delivery unavailable for committed order {}", order.id);
            }
          }
        });
  }

  public Map<String, Object> view(Order o) {
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("id", o.id);
    v.put("code", o.orderCode);v.put("groupOrder",o.groupOrderId!=null);
    v.put("storeId", o.storeId);
    v.put("storeName", o.storeName);
    v.put("status", o.status);
    v.put("paymentStatus", o.paymentStatus);
    v.put("paymentMethod", o.paymentMethod);
    v.put("subtotal", amount(o.subtotal));
    v.put("productDiscount", amount(o.productDiscount));
    v.put("groupDiscount",amount(o.groupDiscount));v.put("groupParticipants",o.groupParticipants);
    v.put("pointsDiscount", amount(o.pointsDiscount));
    v.put("shippingGross", amount(o.shippingGross));
    v.put("shippingDiscount", amount(o.shippingDiscount));
    v.put("total", amount(o.finalAmount));
    v.put("createdAt", o.createdAt.toString());
    v.put("version", o.version);
    return v;
  }
}
