package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import java.util.Map;
import org.springframework.stereotype.Repository;

/** Reads catalog/order-owned tables using DATABASE_DESIGN; never invents purchase proof. */
@Repository
public class CommerceIdentityAdapter {
    private final IdentityRepository db;
    public CommerceIdentityAdapter(IdentityRepository db) { this.db=db; }
    public void product(long id) {
        IdentityValidation.id(id);
        db.one("select p.id from products p join stores s on s.id=p.store_id where p.id=? and p.is_active=true and s.status='ACTIVE'",id);
    }
    public void purchase(long userId,long productId,long itemId) {
        IdentityValidation.id(userId); IdentityValidation.id(productId); IdentityValidation.id(itemId);
        db.one("select oi.id from order_items oi join product_variants v on v.id=oi.variant_id join orders o on o.id=oi.order_id where oi.id=? and v.product_id=? and o.user_id=? and o.status='DELIVERED' and o.payment_status='PAID'",itemId,productId,userId);
    }
    public Map<String,Object> order(long id) { IdentityValidation.id(id); return db.one("select id,user_id,status,payment_status,payment_method,points_used,points_earned,subtotal,product_discount,points_discount from orders where id=? for update",id); }
    public boolean canManageUser(long manager,long target) {
        return db.count("select count(*) from store_memberships m where m.user_id=? and m.is_active=true and (exists(select 1 from stores s where s.id=m.store_id and s.owner_user_id=?) or exists(select 1 from orders o where o.store_id=m.store_id and o.user_id=?))",manager,target,target)>0;
    }
}
