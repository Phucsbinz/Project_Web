package com.utetra.order.service;

import com.utetra.order.dto.OrderRequests.Actor;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class VnpayService {
  private final VnpaySettings settings;
  private final OrderDataRepository data;
  private final OrderSupport support;
  private final Clock clock;
  public VnpayService(VnpaySettings settings,OrderDataRepository data,OrderSupport support,Clock clock) {
    this.settings=settings;this.data=data;this.support=support;this.clock=clock;
  }
  @Transactional(readOnly=true)
  public String start(Actor actor,long id,String ip) {
    support.buyer(actor);
    Order o=data.require(Order.class,id,false);
    if(o.userId!=actor.userId()) throw OrderProblem.forbidden();
    if(!settings.available()) throw OrderProblem.conflict("VNPAY chưa có cấu hình merchant hợp lệ.");
    if(!"VNPAY".equals(o.paymentMethod) || !"NEW".equals(o.status) || !"UNPAID".equals(o.paymentStatus))
      throw OrderProblem.conflict("Đơn không còn chờ thanh toán VNPAY.");
    PaymentTransaction p=data.query(PaymentTransaction.class,"where e.orderId=:order and e.provider='VNPAY'",Map.of("order",id),false,1).getFirst();
    if(!"PENDING".equals(p.status) || p.expiresAt==null || !p.expiresAt.isAfter(clock.instant()))
      throw OrderProblem.conflict("Yêu cầu thanh toán đã hết hạn; hủy đơn chờ trước khi đặt lại.");
    var date=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    Map<String,String> fields=new TreeMap<>();
    fields.put("vnp_Version","2.1.0");fields.put("vnp_Command","pay");fields.put("vnp_TmnCode",settings.terminal);
    fields.put("vnp_Amount",p.amount.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
    fields.put("vnp_CurrCode","VND");fields.put("vnp_TxnRef",p.txnRef);
    fields.put("vnp_OrderInfo","UTETra order "+o.orderCode);fields.put("vnp_OrderType","other");
    fields.put("vnp_Locale","vn");fields.put("vnp_ReturnUrl",settings.returnUrl);
    fields.put("vnp_IpAddr",ip!=null && ip.matches("[0-9a-fA-F:.]{3,45}")?ip:"127.0.0.1");
    fields.put("vnp_CreateDate",date.format(p.createdAt));fields.put("vnp_ExpireDate",date.format(p.expiresAt));
    return settings.paymentUrl+"?"+VnpaySettings.canonical(fields)+"&vnp_SecureHash="+settings.sign(fields);
  }
  private Map<String,String> ack(String code) { return Map.of("RspCode",code,"Message",switch(code) {
    case "00"->"Confirm success";case "02"->"Already updated";case "04"->"Invalid amount";
    case "97"->"Invalid signature";case "01"->"Order not found";default->"Cannot process";}); }
  private void record(PaymentTransaction p,Map<String,String> fields,String outcome) {
    String hash=support.hash(VnpaySettings.canonical(fields));
    if(!data.query(PaymentCallbackEvent.class,"where e.provider='VNPAY' and e.eventKey=:key",Map.of("key",hash),false,1).isEmpty()) return;
    var event=new PaymentCallbackEvent();event.paymentId=p.id;event.provider="VNPAY";event.eventKey=hash;
    event.payloadHash=hash;event.verified=true;event.outcome=outcome;event.receivedAt=clock.instant();event.processedAt=clock.instant();
    data.save(event);
  }

  @Transactional(isolation=Isolation.READ_COMMITTED)
  public Map<String,String> ipn(Map<String,String> fields) {
    if(!settings.valid(fields)) return ack("97");
    String ref=fields.get("vnp_TxnRef"),rawAmount=fields.get("vnp_Amount");
    if(ref==null || !ref.matches("[A-Za-z0-9]{1,100}") || rawAmount==null || !rawAmount.matches("[0-9]{1,12}")) return ack("99");
    var found=data.query(PaymentTransaction.class,"where e.provider='VNPAY' and e.txnRef=:ref",Map.of("ref",ref),false,1);
    if(found.isEmpty()) return ack("01");
    // Lock order first, matching cancel/refund; do not invert the workflow's locks.
    Order o=data.require(Order.class,found.getFirst().orderId,true);
    PaymentTransaction p=data.require(PaymentTransaction.class,found.getFirst().id,true);
    BigDecimal amount=new BigDecimal(rawAmount).divide(BigDecimal.valueOf(100));
    if(amount.compareTo(p.amount)!=0 || amount.compareTo(o.finalAmount)!=0) {record(p,fields,"AMOUNT_MISMATCH");return ack("04");}
    String receipt=fields.get("vnp_TransactionNo");
    if(!Objects.toString(fields.get("vnp_ResponseCode"),"").matches("[0-9]{2}")
        || !Objects.toString(fields.get("vnp_TransactionStatus"),"").matches("[0-9]{2}")) return ack("99");
    boolean paid="00".equals(fields.get("vnp_ResponseCode")) && "00".equals(fields.get("vnp_TransactionStatus"));
    if("SUCCEEDED".equals(p.status)) return ack(Objects.equals(receipt,p.providerTxnId)?"02":"99");
    if(!Set.of("NEW","CANCELLED").contains(o.status) || !"VNPAY".equals(o.paymentMethod)) return ack("99");
    if(!paid) {
      boolean review="07".equals(fields.get("vnp_ResponseCode")) || Set.of("01","04","05","06","07").contains(fields.get("vnp_TransactionStatus"));
      String next=review?"REVIEW":"FAILED";
      if(next.equals(p.status) || !Set.of("PENDING","REVIEW").contains(p.status)) return ack("02");
      p.status=next;p.updatedAt=clock.instant();
      record(p,fields,next+"_"+fields.get("vnp_ResponseCode")+"_"+fields.get("vnp_TransactionStatus"));
      if("NEW".equals(o.status)) {o.paymentStatus=next;o.updatedAt=clock.instant();support.event(o,null,o.status,"VNPAY_"+next,"");}
      return ack("00");
    }
    if(receipt==null || !receipt.matches("[0-9]{1,15}")) return ack("99");
    if(!data.query(PaymentTransaction.class,"where e.provider='VNPAY' and e.providerTxnId=:receipt",Map.of("receipt",receipt),false,1).isEmpty()) return ack("99");
    p.providerTxnId=receipt;p.status="SUCCEEDED";p.paidAt=clock.instant();p.updatedAt=clock.instant();
    record(p,fields,"PAID_00_00");
    o.paidAt=p.paidAt;o.paymentStatus="CANCELLED".equals(o.status)?"REFUND_PENDING":"PAID";o.updatedAt=clock.instant();
    support.event(o,null,o.status,"VNPAY_CONFIRMED",receipt);
    return ack("00");
  }
}
