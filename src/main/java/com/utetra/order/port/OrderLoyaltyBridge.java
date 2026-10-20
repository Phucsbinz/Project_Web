package com.utetra.order.port;
import com.utetra.identity.LoyaltyService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Delegates to the single identity-owned ledger inside the order transaction. */
@Service
public class OrderLoyaltyBridge {
 private final LoyaltyService loyalty; private final EntityManager em;
 public OrderLoyaltyBridge(LoyaltyService loyalty,EntityManager em) {this.loyalty=loyalty;this.em=em;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void record(long user,long order,String type,int points) {
  if(points<0) throw new IllegalArgumentException("Negative points");
  if(points==0) return;
  em.flush();
  try {loyalty.apply(order,type);}
  catch(com.utetra.identity.IdentityException ex) {throw new com.utetra.order.service.OrderProblem(ex.status(),ex.getMessage());}
 }
}
