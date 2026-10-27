package com.utetra.repository.catalog;

import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.*;

/** Parameterized public search; filtering, stable ranking and pagination happen in the database. */
@Repository
public class CatalogQueryRepository {
    public record RankedProduct(Long id,long sold,BigDecimal rating,long favorites) {}
    public record Result(List<RankedProduct> items,long total) {}
    private final NamedParameterJdbcTemplate jdbc;
    private final CatalogStatisticsRepository statistics;
    public CatalogQueryRepository(NamedParameterJdbcTemplate jdbc,CatalogStatisticsRepository statistics) { this.jdbc=jdbc; this.statistics=statistics; }
    public Result search(String keyword,Long categoryId,Long storeId,BigDecimal min,BigDecimal max,String sort,int page,int size,boolean guest,boolean top20) {
        MapSqlParameterSource args=new MapSqlParameterSource();
        StringBuilder from=new StringBuilder(" from products p join categories c on c.id=p.category_id join stores s on s.id=p.store_id");
        boolean global=storeId==null;
        var existing=statistics.availableTables();
        boolean sold=existing.containsAll(List.of("orders","order_items")),rated=existing.contains("reviews"),liked=existing.contains("product_favorites");
        if(sold) from.append(global
            ? " left join (select sp.category_id,coalesce(sp.catalog_key,concat('product:',sp.id)) name_key,ss.owner_user_id,sum(i.quantity) metric from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id join products sp on sp.id=v.product_id join stores ss on ss.id=sp.store_id where o.status='DELIVERED' and o.payment_status='PAID' group by sp.category_id,coalesce(sp.catalog_key,concat('product:',sp.id)),ss.owner_user_id) sales on sales.category_id=p.category_id and sales.name_key=coalesce(p.catalog_key,concat('product:',p.id)) and sales.owner_user_id=s.owner_user_id"
            : " left join (select v.product_id,sum(i.quantity) metric from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id where o.status='DELIVERED' and o.payment_status='PAID' group by v.product_id) sales on sales.product_id=p.id");
        if(rated) from.append(global
            ? " left join (select rp.category_id,coalesce(rp.catalog_key,concat('product:',rp.id)) name_key,rs.owner_user_id,avg(r.stars) metric from reviews r join products rp on rp.id=r.product_id join stores rs on rs.id=rp.store_id where r.status='VISIBLE' group by rp.category_id,coalesce(rp.catalog_key,concat('product:',rp.id)),rs.owner_user_id) ratings on ratings.category_id=p.category_id and ratings.name_key=coalesce(p.catalog_key,concat('product:',p.id)) and ratings.owner_user_id=s.owner_user_id"
            : " left join (select product_id,avg(stars) metric from reviews where status='VISIBLE' group by product_id) ratings on ratings.product_id=p.id");
        if(liked) from.append(global
            ? " left join (select lp.category_id,coalesce(lp.catalog_key,concat('product:',lp.id)) name_key,ls.owner_user_id,count(distinct l.user_id) metric from product_favorites l join products lp on lp.id=l.product_id join stores ls on ls.id=lp.store_id group by lp.category_id,coalesce(lp.catalog_key,concat('product:',lp.id)),ls.owner_user_id) likes on likes.category_id=p.category_id and likes.name_key=coalesce(p.catalog_key,concat('product:',p.id)) and likes.owner_user_id=s.owner_user_id"
            : " left join (select product_id,count(*) metric from product_favorites group by product_id) likes on likes.product_id=p.id");
        String soldExpr=sold?"coalesce(sales.metric,0)":"0",ratingExpr=rated?"coalesce(ratings.metric,0)":"0",likeExpr=liked?"coalesce(likes.metric,0)":"0";
        from.append(" where p.is_active=true and c.is_active=true and s.status='ACTIVE'");
        if(global) from.append(" and not exists(select 1 from products p2 join stores s2 on s2.id=p2.store_id where s2.owner_user_id=s.owner_user_id and p2.category_id=p.category_id and p.catalog_key is not null and p2.catalog_key=p.catalog_key and p2.is_active=true and s2.status='ACTIVE' and ((p2.is_available=true and p.is_available=false) or (p2.is_available=p.is_available and (p2.base_price<p.base_price or (p2.base_price=p.base_price and p2.id<p.id)))))");
        if(keyword!=null && !keyword.isBlank()) {
            from.append(" and lower(p.name) like :keyword escape '!'");
            args.addValue("keyword","%"+keyword.trim().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
        if(categoryId!=null) { from.append(" and p.category_id=:category"); args.addValue("category",categoryId); }
        if(storeId!=null) { from.append(" and p.store_id=:store"); args.addValue("store",storeId); }
        if(min!=null) { from.append(" and p.base_price>=:min"); args.addValue("min",min); }
        if(max!=null) { from.append(" and p.base_price<=:max"); args.addValue("max",max); }
        if(guest) from.append(" and ").append(soldExpr).append(">10");
        Long count=jdbc.queryForObject("select count(*)"+from,args,Long.class);
        long total=top20?Math.min(20,count):count,offset=(long)page*size;
        if(offset>=total) return new Result(List.of(),total);
        String order=switch(sort) {
            case "price_asc" -> "p.base_price asc";
            case "price_desc" -> "p.base_price desc";
            case "sold" -> soldExpr+" desc";
            case "rating" -> ratingExpr+" desc";
            case "favorites" -> likeExpr+" desc";
            default -> "p.created_at desc";
        };
        // A literal zero in ORDER BY is interpreted as a column ordinal by SQL engines.
        if(order.equals("0 desc")) order="p.id desc";
        args.addValue("limit",Math.min(size,total-offset)); args.addValue("offset",offset);
        String sql="select p.id,"+soldExpr+" as sold,"+ratingExpr+" as rating,"+likeExpr+" as favorites"+from+" order by "+order+",p.id desc limit :limit offset :offset";
        return new Result(jdbc.query(sql,args,(rs,n) -> new RankedProduct(rs.getLong("id"),rs.getLong("sold"),rs.getBigDecimal("rating"),rs.getLong("favorites"))),total);
    }
}
