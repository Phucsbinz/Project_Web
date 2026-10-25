package com.utetra.service;

import com.utetra.dto.CatalogRequests.*;
import com.utetra.dto.CatalogViews.*;
import com.utetra.entity.catalog.*;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.util.*;
import static com.utetra.service.CatalogGuard.*;

@Service @Validated @Transactional(readOnly=true)
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ToppingRepository toppings;
    private final ProductVariantRepository variants;
    private final ProductToppingRepository links;
    private final StoreToppingRepository stock;
    private final StoreRepository stores;
    private final CatalogGuard guard;
    private final CatalogStatisticsRepository statistics;
    private final CatalogQueryRepository queries;
    private final java.time.Clock clock;
    private final com.utetra.order.service.CheckoutAvailabilityEvents availabilityEvents;
    public CatalogService(ProductRepository products,CategoryRepository categories,ToppingRepository toppings,
        ProductVariantRepository variants,ProductToppingRepository links,StoreToppingRepository stock,StoreRepository stores,
        CatalogGuard guard,CatalogStatisticsRepository statistics,CatalogQueryRepository queries,java.time.Clock clock,com.utetra.order.service.CheckoutAvailabilityEvents availabilityEvents) {
        this.products=products; this.categories=categories; this.toppings=toppings; this.variants=variants;
        this.links=links; this.stock=stock; this.stores=stores; this.guard=guard; this.statistics=statistics; this.queries=queries; this.clock=clock; this.availabilityEvents=availabilityEvents;
    }
    public List<CategoryView> categories(boolean management,UtetraPrincipal p) {
        if(management) guard.catalog(p);
        return categories.findAll().stream().filter(c -> management || c.getActive())
            .sorted(Comparator.comparingInt(Category::getDisplayOrder).thenComparing(Category::getId)).map(this::categoryView).toList();
    }
    public List<ToppingView> toppings(UtetraPrincipal p) { guard.role(p); return toppings.findAll().stream().map(this::toppingView).toList(); }
    @Transactional public CategoryView saveCategory(UtetraPrincipal p,Long id,@Valid CategoryInput input) {
        guard.catalog(p); var c=id==null?new Category():found(categories.findById(id));
        c.setName(input.name().trim()); c.setSlug(input.slug()); c.setDescription(input.description()); c.setDisplayOrder(input.displayOrder()); c.setActive(input.active());
        categories.saveAndFlush(c); guard.audit(p,"SAVE","CATEGORY",c.getId()); return categoryView(c);
    }
    @Transactional public ToppingView saveTopping(UtetraPrincipal p,Long id,@Valid ToppingInput input) {
        guard.catalog(p); var t=id==null?new Topping():found(toppings.findById(id));
        t.setName(input.name().trim()); t.setPrice(input.price()); t.setActive(input.active());
        toppings.saveAndFlush(t); guard.audit(p,"SAVE","TOPPING",t.getId()); return toppingView(t);
    }
    @Transactional public void availability(UtetraPrincipal p,Long storeId,Long toppingId,boolean available) {
        guard.store(p,storeId); found(toppings.findById(toppingId));
        var s=stock.findByStoreIdAndToppingId(storeId,toppingId).orElseGet(StoreTopping::new);
        s.setStoreId(storeId); s.setToppingId(toppingId); s.setAvailable(available); stock.saveAndFlush(s); guard.audit(p,"AVAILABILITY","TOPPING",toppingId);
    }
    public PageView<ProductView> search(String keyword,Long categoryId,Long storeId,BigDecimal min,BigDecimal max,
        String sort,int page,int size,boolean guestTop,boolean top20) {
        require(page>=0 && size>=1 && size<=50,"Phân trang không hợp lệ");
        require(keyword==null || keyword.length()<=150,"Từ khóa tối đa 150 ký tự");
        require((min==null || min.signum()>=0) && (max==null || max.signum()>=0) && (min==null || max==null || min.compareTo(max)<=0),"Khoảng giá không hợp lệ");
        require(Set.of("newest","price_asc","price_desc","sold","rating","favorites").contains(sort),"Kiểu sắp xếp không hợp lệ");
        var result=queries.search(keyword,categoryId,storeId,min,max,sort,page,size,guestTop,top20);
        var items=result.items().stream().map(x -> view(found(products.findById(x.id())),x.sold(),x.rating(),x.favorites())).toList();
        return new PageView<>(items,page,size,result.total(),(result.total()+size-1)/size);
    }
    public ProductView detail(Long id) {
        var p=found(products.findById(id)); require(visible(p),"Sản phẩm không được bán");
        return view(p,statistics.sold().getOrDefault(id,0L),statistics.ratings().getOrDefault(id,BigDecimal.ZERO),statistics.favorites().getOrDefault(id,0L));
    }
    public List<ProductView> managed(UtetraPrincipal p,Long storeId) {
        guard.store(p,storeId); return products.findAll().stream().filter(x -> x.getStoreId().equals(storeId)).map(x -> view(x,0,BigDecimal.ZERO,0)).toList();
    }
    @Transactional public ProductView saveProduct(UtetraPrincipal p,Long id,@Valid ProductInput input) {
        guard.store(p,input.storeId()); var product=id==null?new Product():found(products.findById(id));
        if(id!=null) { guard.store(p,product.getStoreId()); require(product.getStoreId().equals(input.storeId()),"Không được chuyển món sang shop khác"); version(product.getVersion(),input.version()); }
        require(found(categories.findById(input.categoryId())).getActive(),"Danh mục đã ngừng hoạt động");
        require(input.variants().stream().map(VariantInput::sizeName).distinct().count()==input.variants().size(),"Size bị trùng");
        require(input.variants().stream().anyMatch(VariantInput::active),"Cần ít nhất một size hoạt động");
        for(Long toppingId:input.toppingIds()) require(found(toppings.findById(toppingId)).getActive(),"Topping đã ngừng hoạt động");
        product.setStoreId(input.storeId()); product.setCategoryId(input.categoryId()); product.setName(input.name().trim()); product.setSlug(input.slug());
        product.setDescription(input.description()); product.setBasePrice(input.basePrice()); product.setActive(input.active()); product.setAvailable(input.available()); product.setFeatured(input.featured());
        product.touch(); products.saveAndFlush(product);
        var old=variants.findByProductId(product.getId()); old.forEach(v -> v.setActive(false));
        for(var i:input.variants()) {
            var v=old.stream().filter(x -> x.getSizeName().equals(i.sizeName())).findFirst().orElseGet(ProductVariant::new);
            if(i.stockQuantity()!=null && v.getId()!=null) require(i.stockVersion()!=null && i.stockVersion()==v.getVersion(),"Tồn kho đã thay đổi; tải lại sản phẩm trước khi chỉnh số lượng");
            v.setProductId(product.getId()); v.setSizeName(i.sizeName()); v.setExtraPrice(i.extraPrice()); v.setActive(i.active()); if(i.stockQuantity()!=null) v.setStockQuantity(i.stockQuantity()); variants.save(v);
        }
        links.deleteByProductId(product.getId()); links.flush();
        for(Long toppingId:input.toppingIds()) { var link=new ProductTopping(); link.setProductId(product.getId()); link.setToppingId(toppingId); links.save(link); }
        variants.flush(); products.flush(); availabilityEvents.changed(product.getStoreId(),variants.findByProductId(product.getId()).stream().map(ProductVariant::getId).toList(),List.of()); guard.audit(p,"SAVE","PRODUCT",product.getId()); return view(product,0,BigDecimal.ZERO,0);
    }
    @Transactional public void disableProduct(UtetraPrincipal p,Long id,long suppliedVersion) {
        var product=found(products.findById(id)); guard.store(p,product.getStoreId()); version(product.getVersion(),suppliedVersion);
        product.setActive(false); product.setAvailable(false); products.flush(); guard.audit(p,"DISABLE","PRODUCT",id);
    }
    public DrinkQuote quote(Long id,@Valid DrinkInput input) {
        var product=found(products.findById(id)); require(visible(product) && product.getAvailable(),"Món đang ngừng bán");
        var store=found(stores.findById(product.getStoreId())); require(store.getAcceptingOrders(),"Shop đang tạm ngừng nhận đơn");
        var time=java.time.LocalTime.now(clock.withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        require(!time.isBefore(store.getOpenTime()) && time.isBefore(store.getCloseTime()),"Shop đang ngoài giờ phục vụ");
        require(Set.of(0,30,50,70,100).contains(input.sugarLevel()) && Set.of(0,30,50,70,100).contains(input.iceLevel()),"Mức đường/đá không hợp lệ");
        var variant=found(variants.findById(input.variantId())); require(id.equals(variant.getProductId()) && variant.getActive(),"Size không hợp lệ");
        require(variant.getStockQuantity()==null || variant.getStockQuantity()>=input.quantity(),"Size không còn đủ số lượng");
        var allowed=links.findByProductId(id).stream().map(ProductTopping::getToppingId).collect(java.util.stream.Collectors.toSet());
        BigDecimal extra=BigDecimal.ZERO;
        for(Long toppingId:input.toppingIds()) {
            require(allowed.contains(toppingId),"Topping không thuộc món"); var t=found(toppings.findById(toppingId));
            require(t.getActive() && stock.findByStoreIdAndToppingId(store.getId(),toppingId).map(StoreTopping::getAvailable).orElse(false),"Topping hết hàng tại shop"); extra=extra.add(t.getPrice());
        }
        BigDecimal unit=product.getBasePrice().add(variant.getExtraPrice());
        return new DrinkQuote(id,store.getId(),variant.getId(),product.getVersion(),store.getVersion(),unit,extra,input.quantity(),unit.add(extra).multiply(BigDecimal.valueOf(input.quantity())));
    }
    private boolean visible(Product p) {
        return p.getActive() && categories.findById(p.getCategoryId()).map(Category::getActive).orElse(false)
            && stores.findById(p.getStoreId()).map(s -> "ACTIVE".equals(s.getStatus())).orElse(false);
    }
    private ProductView view(Product p,long sold,BigDecimal rating,long favorites) {
        Set<Long> available=stock.findByStoreId(p.getStoreId()).stream().filter(StoreTopping::getAvailable).map(StoreTopping::getToppingId).collect(java.util.stream.Collectors.toSet());
        List<ToppingView> options=links.findByProductId(p.getId()).stream().map(x -> found(toppings.findById(x.getToppingId())))
            .filter(t -> t.getActive() && available.contains(t.getId())).map(this::toppingView).toList();
        return new ProductView(p.getId(),p.getStoreId(),p.getCategoryId(),p.getName(),p.getSlug(),p.getDescription(),p.getBasePrice(),p.getImageUrl(),
            p.getActive(),p.getAvailable(),p.getFeatured(),p.getVersion(),p.getCreatedAt(),variants.findByProductId(p.getId()).stream()
            .map(v -> new VariantView(v.getId(),v.getSizeName(),v.getExtraPrice(),v.getActive(),v.getStockQuantity(),v.getVersion())).toList(),options,
            links.findByProductId(p.getId()).stream().map(ProductTopping::getToppingId).collect(java.util.stream.Collectors.toSet()),sold,rating,favorites);
    }
    private CategoryView categoryView(Category c) { return new CategoryView(c.getId(),c.getName(),c.getSlug(),c.getDescription(),c.getDisplayOrder(),c.getActive()); }
    private ToppingView toppingView(Topping t) { return new ToppingView(t.getId(),t.getName(),t.getPrice(),t.getActive()); }
}
