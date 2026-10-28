package com.utetra.identity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OfferRewardListener {
 private static final Logger LOG=LoggerFactory.getLogger(OfferRewardListener.class);
 private final OfferWalletService wallet;
 public OfferRewardListener(OfferWalletService wallet){this.wallet=wallet;}
 @TransactionalEventListener
 public void delivered(OfferDeliveryReward event){try{wallet.awardForOrder(event.orderId());}catch(RuntimeException ex){LOG.warn("Reward reconciliation deferred for delivered order {}. Wallet sync will retry.",event.orderId());}}
}
