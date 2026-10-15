package com.utetra.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/** Reads owner modules' committed data. Missing optional modules produce zero metrics, never invented sales. */
@Service
public class CatalogStatistics {
    private final JdbcTemplate jdbc;
    public CatalogStatistics(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public boolean tables(String... names) {
        return Boolean.TRUE.equals(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c -> {
            Set<String> existing=new HashSet<>();
            try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"%",new String[]{"TABLE","BASE TABLE"})) {
                while(rs.next()) existing.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
            return existing.containsAll(List.of(names));
        }));
    }
    public Map<Long,Long> sold() {
        if(!tables("orders","order_items")) return Map.of();
        return longs("select v.product_id as product_id, sum(i.quantity) as metric from order_items i join orders o on o.id=i.order_id join product_variants v on v.id=i.variant_id where o.status='DELIVERED' and o.payment_status='PAID' group by v.product_id");
    }
    public Map<Long,Long> favorites() {
        if(!tables("product_favorites")) return Map.of();
        return longs("select product_id,count(*) as metric from product_favorites group by product_id");
    }
    public Map<Long,BigDecimal> ratings() {
        if(!tables("reviews")) return Map.of();
        Map<Long,BigDecimal> result=new HashMap<>();
        jdbc.query("select product_id,avg(stars) as metric from reviews where status='VISIBLE' group by product_id",(org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getLong("product_id"),rs.getBigDecimal("metric")));
        return result;
    }
    private Map<Long,Long> longs(String sql) {
        Map<Long,Long> result=new HashMap<>();
        jdbc.query(sql,(org.springframework.jdbc.core.RowCallbackHandler) rs -> result.put(rs.getLong("product_id"),rs.getLong("metric"))); return result;
    }
}
