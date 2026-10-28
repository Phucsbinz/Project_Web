package com.utetra.service;

import com.utetra.dto.CatalogRequests.PromotionInput;
import com.utetra.dto.CatalogViews.*;
import com.utetra.entity.catalog.*;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.*;
import java.time.Clock;
import java.util.*;
import static com.utetra.service.CatalogGuard.*;

@Service @Validated @Transactional(readOnly=true)
public class PromotionService {
    private final PromotionRepository promotions;
    private final PromotionProductRepository links;
    private final ProductRepository products;
    private final StoreRepository stores;
    private final CatalogGuard guard;
    private final Clock clock;
    private final com.utetra.identity.IdentityRepository walletDb;
    private final com.utetra.order.service.CheckoutAvailabilityEvents events;
    public PromotionService(PromotionRepository promotions,PromotionProductRepository links,ProductRepository products,
        StoreRepository stores,CatalogGuard guard,Clock clock,com.utetra.order.service.CheckoutAvailabilityEvents events,com.utetra.identity.IdentityRepository walletDb) {
        this.promotions=promotions; this.links=links; this.products=products; this.stores=stores; this.guard=guard; this.clock=clock;this.events=events;this.walletDb=walletDb;
    }
    public List<PromotionView> managed(UtetraPrincipal p,Long storeId) {
        access(p,storeId);
        return promotions.findAll().stream().filter(x -> Objects.equals(storeId,x.getStoreId())).map(this::view).toList();
    }
    public List<PromotionView> available(Long storeId) {
        require("ACTIVE".equals(found(stores.findById(storeId)).getStatus()),"Shop chưa hoạt động");
        Set<Long> personal=walletDb.list("select promotion_id from promotion_wallet where source not like 'SAVED:%'").stream().map(row->((Number)row.get("promotion_id")).longValue()).collect(java.util.stream.Collectors.toSet());
        return promotions.findAll().stream().filter(x -> x.getStoreId()==null || storeId.equals(x.getStoreId()))
            .filter(x->!personal.contains(x.getId())).filter(this::current).map(this::view).toList();
    }
    @Transactional public PromotionView save(UtetraPrincipal p,Long id,@Valid PromotionInput input) {
        access(p,input.storeId());
        if(input.storeId()!=null) found(stores.findById(input.storeId()));
        require(input.startsAt().isBefore(input.endsAt()),"Thời gian bắt đầu phải trước kết thúc");
        require(input.perUserLimit()<=input.usageLimit(),"Hạn mức mỗi người vượt tổng hạn mức");
        require(!"SHIPPING_PERCENT".equals(input.kind()) || input.productIds().isEmpty(),"Ưu đãi phí giao không gắn sản phẩm");
        for(Long productId:input.productIds()) {
            var product=found(products.findById(productId));
            require(input.storeId()==null || input.storeId().equals(product.getStoreId()),"Sản phẩm không thuộc phạm vi khuyến mãi");
        }
        var promotion=id==null?new Promotion():found(promotions.findById(id));
        if(id!=null) { access(p,promotion.getStoreId()); require(Objects.equals(promotion.getStoreId(),input.storeId()),"Không được chuyển phạm vi khuyến mãi"); version(promotion.getVersion(),input.version()); }
        else promotion.setCreatedBy(p.userId());
        promotion.setStoreId(input.storeId()); promotion.setScope(input.storeId()==null?"GLOBAL":"SHOP"); promotion.setFundedBy(input.storeId()==null?"APP":"SHOP");
        promotion.setName(input.name().trim()); promotion.setCode(input.code()==null?null:input.code().trim().toUpperCase(Locale.ROOT));
        promotion.setKind(input.kind()); promotion.setPercent(input.percent()); promotion.setMaxDiscount(input.maxDiscount()); promotion.setMinOrder(input.minOrder());
        promotion.setUsageLimit(input.usageLimit()); promotion.setPerUserLimit(input.perUserLimit()); promotion.setStartsAt(input.startsAt()); promotion.setEndsAt(input.endsAt()); promotion.setActive(input.active());
        promotion.touch(); promotions.saveAndFlush(promotion); links.deleteByPromotionId(promotion.getId()); links.flush();
        for(Long productId:input.productIds()) { var link=new PromotionProduct(); link.setPromotionId(promotion.getId()); link.setProductId(productId); links.save(link); }
        links.flush(); guard.audit(p,"SAVE","PROMOTION",promotion.getId()); events.changed(null,List.of(),List.of(promotion.getId())); return view(promotion);
    }
    /** Pure price calculation for checkout. Quota reservation belongs to Order's same transaction, never this preview. */
    public PromotionQuote quote(Long promotionId,Long storeId,Map<Long,BigDecimal> productAmounts,BigDecimal shipping) {
        require(walletDb.count("select count(*) from promotion_wallet where promotion_id=? and source not like 'SAVED:%'",promotionId)==0,"Quà cá nhân chỉ áp dụng tại thanh toán của tài khoản sở hữu");
        var promo=found(promotions.findById(promotionId));
        require(current(promo),"Khuyến mãi không còn hiệu lực");
        require(promo.getStoreId()==null || promo.getStoreId().equals(storeId),"Khuyến mãi khác shop");
        require("ACTIVE".equals(found(stores.findById(storeId)).getStatus()),"Shop không hoạt động");
        require(shipping!=null && shipping.signum()>=0,"Phí giao không hợp lệ");
        BigDecimal subtotal=BigDecimal.ZERO,eligible=BigDecimal.ZERO;
        Set<Long> allowed=links.findByPromotionId(promotionId).stream().map(PromotionProduct::getProductId).collect(java.util.stream.Collectors.toSet());
        for(var line:productAmounts.entrySet()) {
            var product=found(products.findById(line.getKey())); require(storeId.equals(product.getStoreId()),"Món khác shop");
            require(line.getValue()!=null && line.getValue().signum()>=0,"Giá trị dòng không hợp lệ");
            subtotal=subtotal.add(line.getValue());
            if(allowed.isEmpty() || allowed.contains(line.getKey())) eligible=eligible.add(line.getValue());
        }
        require(subtotal.compareTo(promo.getMinOrder())>=0,"Chưa đạt giá trị đơn tối thiểu");
        if("SHIPPING_PERCENT".equals(promo.getKind())) eligible=shipping;
        BigDecimal discount=eligible.multiply(promo.getPercent()).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN).min(promo.getMaxDiscount()).min(eligible);
        return new PromotionQuote(promotionId,promo.getKind(),eligible,discount,promo.getVersion());
    }
    private boolean current(Promotion p) { return p.getActive() && !clock.instant().isBefore(p.getStartsAt()) && clock.instant().isBefore(p.getEndsAt()); }
    private void access(UtetraPrincipal p,Long storeId) { if(storeId==null) guard.admin(p); else guard.store(p,storeId); }
    private PromotionView view(Promotion p) {
        return new PromotionView(p.getId(),p.getStoreId(),p.getScope(),p.getKind(),p.getCode(),p.getName(),p.getPercent(),p.getMaxDiscount(),p.getMinOrder(),p.getUsageLimit(),p.getPerUserLimit(),p.getStartsAt(),p.getEndsAt(),p.getActive(),p.getFundedBy(),p.getVersion(),
            links.findByPromotionId(p.getId()).stream().map(PromotionProduct::getProductId).collect(java.util.stream.Collectors.toSet()));
    }
}
