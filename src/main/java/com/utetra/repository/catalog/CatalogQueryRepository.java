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
        boolean sold=statistics.tables("orders","order_items"),rated=statistics.tables("reviews"),liked=statistics.tables("product_favorites");
        if(sold) from.append(" left join (select v.product_id,sum(i.quantity) metric from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id where o.status='DELIVERED' and o.payment_status='PAID' group by v.product_id) sales on sales.product_id=p.id");
        if(rated) from.append(" left join (select product_id,avg(stars) metric from reviews where status='VISIBLE' group by product_id) ratings on ratings.product_id=p.id");
        if(liked) from.append(" left join (select product_id,count(*) metric from product_favorites group by product_id) likes on likes.product_id=p.id");
        String soldExpr=sold?"coalesce(sales.metric,0)":"0",ratingExpr=rated?"coalesce(ratings.metric,0)":"0",likeExpr=liked?"coalesce(likes.metric,0)":"0";
        from.append(" where p.is_active=true and c.is_active=true and s.status='ACTIVE'");
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
