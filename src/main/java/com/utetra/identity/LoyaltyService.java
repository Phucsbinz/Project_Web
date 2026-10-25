package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.utetra.security.UtetraPrincipal;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class LoyaltyService {
    private final IdentityRepository db; private final AccountService account; private final CommerceIdentityAdapter commerce; private final Clock clock;
    public LoyaltyService(IdentityRepository db,AccountService account,CommerceIdentityAdapter commerce,Clock clock) { this.db=db; this.account=account; this.commerce=commerce; this.clock=clock; }
    @Transactional(readOnly=true)
    public Map<String,Object> summary(UtetraPrincipal p,int page,int size) {
        account.require(p,true); int offset=IdentityValidation.offset(page,size);
        var a=db.optional("select id,balance,debt,lifetime_earned from loyalty_accounts where user_id=?",p.userId());
        var result=new LinkedHashMap<String,Object>();
        result.put("balance",a.map(v -> number(v,"balance")).orElse(0L)); result.put("debt",a.map(v -> number(v,"debt")).orElse(0L));
        long earned=a.map(v -> number(v,"lifetime_earned")).orElse(0L);
        result.put("lifetime_earned",earned); result.put("tier",earned>=500?"Vàng":earned>=100?"Bạc":"Đồng");
        result.put("transactions",a.isEmpty()?List.of():db.list("select order_id,type,points_delta,balance_after,debt_after,created_at from loyalty_transactions where account_id=? order by id desc limit ? offset ?",number(a.get(),"id"),size,offset));
        return result;
    }
    /** Internal contract only. Caller must hold order lock and include order/ledger in one transaction. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void apply(long orderId,String type) {
        if(type==null || !Set.of("REDEEM","REFUND","EARN","REVERSAL").contains(type)) throw IdentityException.invalid("Loại giao dịch điểm không hợp lệ.");
        var order=commerce.order(orderId); long userId=number(order,"user_id");
        BigDecimal subtotal=money(order,"subtotal"),productDiscount=money(order,"product_discount"),pointsDiscount=money(order,"points_discount");
        long pointsUsed=number(order,"points_used");
        if(pointsUsed<0 || productDiscount.compareTo(subtotal)>0 || pointsDiscount.compareTo(subtotal.subtract(productDiscount))>0 || pointsDiscount.compareTo(BigDecimal.valueOf(pointsUsed).multiply(BigDecimal.valueOf(1000)))!=0)
            throw IdentityException.invalid("Snapshot số tiền và điểm sử dụng của đơn không nhất quán.");
        db.user(userId,true);
        if(db.count("select count(*) from loyalty_accounts where user_id=?",userId)==0)
            db.update("insert into loyalty_accounts(user_id,balance,debt,lifetime_earned,updated_at,version) values(?,0,0,0,?,0)",userId,time(clock.instant()));
        var a=db.one("select * from loyalty_accounts where user_id=? for update",userId); long accountId=number(a,"id");
        if(db.count("select count(*) from loyalty_transactions where order_id=? and type=?",orderId,type)>0) return;
        long balance=number(a,"balance"),debt=number(a,"debt"),earned=number(a,"lifetime_earned"),delta=0;
        String status=string(order,"status"),payment=string(order,"payment_status");
        if(type.equals("REDEEM")) {
            long points=number(order,"points_used");
            var eligible=new BigDecimal(order.get("subtotal").toString()).subtract(new BigDecimal(order.get("product_discount").toString()));
            long max=eligible.multiply(new BigDecimal("0.20")).divideToIntegralValue(new BigDecimal("1000")).longValueExact();
            if(!status.equals("NEW") || points<0 || points>max || points>balance || debt>0) throw IdentityException.invalid("Không đủ điểm hoặc vượt giới hạn 20%.");
            delta=-points;
        } else if(type.equals("EARN")) {
            if(!status.equals("DELIVERED") || !payment.equals("PAID")) throw IdentityException.invalid("Chỉ cộng điểm khi đã giao và trả tiền.");
            var net=new BigDecimal(order.get("subtotal").toString()).subtract(new BigDecimal(order.get("product_discount").toString())).subtract(new BigDecimal(order.get("points_discount").toString()));
            delta=net.divideToIntegralValue(new BigDecimal("10000")).longValueExact();
            if(delta<0) throw IdentityException.invalid("Giá trị đơn không hợp lệ.");
            earned=Math.addExact(earned,delta);
        } else {
            boolean refunded=payment.equals("REFUNDED");
            boolean cancelledUnpaid=status.equals("CANCELLED") && Set.of("UNPAID","PENDING","FAILED","CANCELLED").contains(payment);
            boolean cancelledBank=status.equals("CANCELLED") && payment.equals("REFUND_PENDING")
                && Set.of("BANK_TRANSFER","VNPAY").contains(order.get("payment_method"));
            if(!(refunded || type.equals("REFUND") && (cancelledUnpaid || cancelledBank))) throw IdentityException.invalid("Chưa đủ điều kiện hoàn điểm.");
            String previous=type.equals("REFUND")?"REDEEM":"EARN";
            var origin=db.optional("select points_delta from loyalty_transactions where order_id=? and type=? and account_id=?",orderId,previous,accountId);
            if(origin.isEmpty()) return;
            delta=-number(origin.get(),"points_delta");
            if(type.equals("REVERSAL")) earned=Math.addExact(earned,delta);
        }
        long net=Math.addExact(Math.subtractExact(balance,debt),delta);
        balance=Math.max(0,net); debt=Math.max(0,-net);
        if(earned<0 || balance>Integer.MAX_VALUE || debt>Integer.MAX_VALUE || earned>Integer.MAX_VALUE) throw IdentityException.invalid("Số điểm vượt giới hạn.");
        db.update("update loyalty_accounts set balance=?,debt=?,lifetime_earned=?,updated_at=?,version=version+1 where id=?",balance,debt,earned,time(clock.instant()),accountId);
        db.update("insert into loyalty_transactions(account_id,order_id,type,points_delta,balance_after,debt_after,created_at) values(?,?,?,?,?,?,?)",accountId,orderId,type,delta,balance,debt,time(clock.instant()));
    }
    private BigDecimal money(Map<String,Object> order,String key) {
        Object value=order.get(key);
        if(!(value instanceof Number)) throw IdentityException.invalid("Snapshot đơn thiếu số tiền hợp lệ.");
        var amount=new BigDecimal(value.toString());
        if(amount.signum()<0 || amount.stripTrailingZeros().scale()>0) throw IdentityException.invalid("Số tiền đơn phải là VND nguyên đồng và không âm.");
        return amount;
    }
}
