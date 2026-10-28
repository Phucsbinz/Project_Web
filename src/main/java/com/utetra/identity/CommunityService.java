package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.utetra.security.UtetraPrincipal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommunityService {
    private final IdentityRepository db; private final AccountService account; private final CommerceIdentityAdapter commerce;
    private final Clock clock; private final CommunityMediaService media;
    private final IdentityInputs inputs;
    public CommunityService(IdentityRepository db,AccountService account,CommerceIdentityAdapter commerce,Clock clock,CommunityMediaService media,IdentityInputs inputs) {
        this.db=db; this.account=account; this.commerce=commerce; this.clock=clock; this.media=media;
        this.inputs=inputs;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> purchases(UtetraPrincipal p) {
        return purchases(p,0,100);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> purchases(UtetraPrincipal p,int page,int size) {
        account.require(p,true);int offset=IdentityValidation.offset(page,size);
        return db.list("select oi.id, v.product_id,oi.product_name from order_items oi join orders o on o.id=oi.order_id join product_variants v on v.id=oi.variant_id where o.user_id=? and o.status='DELIVERED' and o.payment_status='PAID' order by oi.id desc limit ? offset ?",p.userId(),size,offset);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> publicContent(long productId,int page,int size) {
        commerce.product(productId); int offset=IdentityValidation.offset(page,size);
        String family="(select p.id from products p join stores s on s.id=p.store_id join products original on original.id=? join stores os on os.id=original.store_id where s.owner_user_id=os.owner_user_id and p.category_id=original.category_id and (p.id=original.id or (original.catalog_key is not null and p.catalog_key=original.catalog_key)))";
        var reviews=db.list("select r.id,r.stars,r.content,r.created_at,u.full_name as author_name from reviews r join users u on u.id=r.author_id where r.product_id in "+family+" and r.status='VISIBLE' order by r.id desc limit ? offset ?",productId,size,offset);
        var comments=db.list("select c.id,c.content,c.created_at,u.full_name as author_name from product_comments c join users u on u.id=c.author_id where c.product_id in "+family+" and c.status='VISIBLE' order by c.id desc limit ? offset ?",productId,size,offset);
        reviews.forEach(r -> r.put("media",media.list("review",number(r,"id"))));
        comments.forEach(c -> c.put("media",media.list("comment",number(c,"id"))));
        var rating=db.one("select count(*) as review_count,coalesce(avg(stars),0) as average_stars from reviews where product_id in "+family+" and status='VISIBLE'",productId);
        return Map.of("reviews",reviews,"comments",comments,"rating",rating,
            "soldUnits",db.count("select coalesce(sum(i.quantity),0) from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id where v.product_id in "+family+" and o.status='DELIVERED' and o.payment_status='PAID'",productId),
            "commentCount",db.count("select count(*) from product_comments where product_id in "+family+" and status='VISIBLE'",productId),
            "favorites",db.count("select count(distinct user_id) from product_favorites where product_id in "+family,productId));
    }
    @Transactional
    public long favorite(UtetraPrincipal p,long productId,boolean enabled) {
        IdentityValidation.id(productId);
        account.require(p,true); db.user(p.userId(),true); if(enabled) commerce.product(productId);
        if(enabled) {
            if(db.count("select count(*) from product_favorites where user_id=? and product_id=?",p.userId(),productId)==0)
                db.update("insert into product_favorites(user_id,product_id,created_at) values(?,?,?)",p.userId(),productId,time(clock.instant()));
        } else db.update("delete from product_favorites where user_id=? and product_id=?",p.userId(),productId);
        return db.count("select count(*) from product_favorites where product_id=?",productId);
    }
    @Transactional
    public void viewed(UtetraPrincipal p,long productId) {
        account.require(p,true); db.user(p.userId(),true); commerce.product(productId);
        if(db.update("update product_views set last_viewed_at=?,view_count=view_count+1 where user_id=? and product_id=?",time(clock.instant()),p.userId(),productId)==0)
            db.update("insert into product_views(user_id,product_id,last_viewed_at,view_count) values(?,?,?,1)",p.userId(),productId,time(clock.instant()));
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> products(UtetraPrincipal p,boolean viewed,int page,int size) {
        account.require(p,true); int offset=IdentityValidation.offset(page,size);
        String table=viewed?"product_views":"product_favorites",sort=viewed?"x.last_viewed_at":"x.created_at";
        return db.list("select p.id,p.name,p.slug,p.image_url,p.base_price from "+table+" x join products p on p.id=x.product_id join stores s on s.id=p.store_id where x.user_id=? and p.is_active=true and s.status='ACTIVE' order by "+sort+" desc,p.id desc limit ? offset ?",p.userId(),size,offset);
    }
    @Transactional
    public long review(UtetraPrincipal p,IdentityForms.Review f,List<MultipartFile> files) {
        inputs.require(f);
        account.require(p,true); db.user(p.userId(),true); commerce.purchase(p.userId(),f.productId(),f.orderItemId());
        if(f.stars()<1 || f.stars()>5) throw IdentityException.invalid("Số sao từ 1 đến 5.");
        if(db.count("select count(*) from reviews where order_item_id=?",f.orderItemId())>0) throw IdentityException.conflict();
        var now=time(clock.instant());
        long id=db.insert("insert into reviews(author_id,order_item_id,product_id,stars,content,status,created_at,updated_at,version) values(?,?,?,?,?,'VISIBLE',?,?,0)",p.userId(),f.orderItemId(),f.productId(),f.stars(),IdentityValidation.text(f.content(),0,2000),now,now);
        media.attach("review",id,files); return id;
    }
    @Transactional
    public long comment(UtetraPrincipal p,IdentityForms.Comment f,List<MultipartFile> files) {
        inputs.require(f);
        account.require(p,true); db.user(p.userId(),true); commerce.purchase(p.userId(),f.productId(),f.orderItemId());
        String content=IdentityValidation.text(f.content(),50,2000); var now=time(clock.instant());
        long id=db.insert("insert into product_comments(author_id,product_id,order_item_id,content,status,created_at,updated_at,version) values(?,?,?,?,'VISIBLE',?,?,0)",p.userId(),f.productId(),f.orderItemId(),content,now,now);
        media.attach("comment",id,files); return id;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> entries(UtetraPrincipal p,boolean review,int page,int size) {
        account.require(p,true); int offset=IdentityValidation.offset(page,size);
        String table=review?"reviews":"product_comments";
        var rows=db.list("select id,product_id,order_item_id,content,status,version,created_at"+(review?",stars":"")+" from "+table+" where author_id=? order by id desc limit ? offset ?",p.userId(),size,offset);
        for(var row:rows) row.put("media",media.list(review?"review":"comment",number(row,"id")));
        return rows;
    }
    @Transactional
    public void edit(UtetraPrincipal p,boolean review,long id,String text,int stars,long version) {
        IdentityValidation.id(id); IdentityValidation.version(version);
        account.require(p,true); String table=review?"reviews":"product_comments";
        var row=db.one("select * from "+table+" where id=? and author_id=? for update",id,p.userId());
        if(number(row,"version")!=version) throw IdentityException.conflict();
        if(!"VISIBLE".equals(string(row,"status"))) throw IdentityException.denied();
        String content=IdentityValidation.text(text,review?0:50,2000);
        if(review && (stars<1 || stars>5)) throw IdentityException.invalid("Số sao từ 1 đến 5.");
        if(review) db.update("update reviews set content=?,stars=?,updated_at=?,version=version+1 where id=?",content,stars,time(clock.instant()),id);
        else db.update("update product_comments set content=?,updated_at=?,version=version+1 where id=?",content,time(clock.instant()),id);
    }
    @Transactional
    public void delete(UtetraPrincipal p,boolean review,long id) {
        IdentityValidation.id(id);
        account.require(p,true); String table=review?"reviews":"product_comments";
        db.one("select id from "+table+" where id=? and author_id=? for update",id,p.userId());
        // Retain purchase/unique review evidence so deletion cannot bypass one-review-per-item.
        db.update("update "+table+" set status='HIDDEN',content='',updated_at=?,version=version+1 where id=?",time(clock.instant()),id);
        media.removeAll(review?"review":"comment",id);
    }
}
