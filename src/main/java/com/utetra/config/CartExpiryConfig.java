package com.utetra.config;
import com.utetra.order.service.CartService;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
@Configuration @Profile("!test") @EnableScheduling
public class CartExpiryConfig {
 private final CartService carts;private final com.utetra.order.service.GroupOrderService groups;
 public CartExpiryConfig(CartService carts,com.utetra.order.service.GroupOrderService groups){this.carts=carts;this.groups=groups;}
 @Scheduled(fixedDelay=60000,initialDelay=60000) public void expire(){carts.expireInactive();groups.expireGroups();}
}
