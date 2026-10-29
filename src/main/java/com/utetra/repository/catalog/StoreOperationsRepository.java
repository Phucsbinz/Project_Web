package com.utetra.repository.catalog;

import com.utetra.dto.CatalogViews.*;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Cross-module persistence for shop approval; transaction belongs to StoreService. */
@Repository
public class StoreOperationsRepository {
    private final JdbcTemplate jdbc;
    public StoreOperationsRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    public List<DeliveryRateView> deliveryRates(long storeId) {
        return jdbc.query("select a.id,a.name,c.id carrier_id,c.name carrier_name,c.base_fee+r.additional_fee fee from store_delivery_areas a join carrier_area_rates r on r.delivery_area_id=a.id join carriers c on c.id=r.carrier_id where a.store_id=? and a.is_active=true and r.is_active=true and c.is_active=true and (c.managed_store_id is null or c.managed_store_id=a.store_id) order by a.id,c.id",
            (rs,n) -> new DeliveryRateView(rs.getLong("id"),rs.getString("name"),rs.getLong("carrier_id"),rs.getString("carrier_name"),rs.getBigDecimal("fee")),storeId);
    }
    public List<CommissionPolicyView> commissionPolicies() {
        return jdbc.query("select id,name,rate_percent from commission_policies where is_active=true order by name,id",
            (rs,n) -> new CommissionPolicyView(rs.getLong("id"),rs.getString("name"),rs.getBigDecimal("rate_percent")));
    }
    public boolean activePolicy(long id) {
        return Integer.valueOf(1).equals(jdbc.queryForObject("select count(*) from commission_policies where id=? and is_active=true",Integer.class,id));
    }
    public int promoteVendor(long userId, Instant now) {
        return jdbc.update("update users set role_id=(select id from roles where name='ROLE_VENDOR'), version=version+1, updated_at=? where id=? and is_active=true and email_verified_at is not null and role_id in (select id from roles where name in ('ROLE_USER','ROLE_VENDOR'))",Timestamp.from(now),userId);
    }
    public void revokeSessions(long userId, Instant now) {
        jdbc.update("update auth_sessions set revoked_at=?, version=version+1 where user_id=? and revoked_at is null",Timestamp.from(now),userId);
    }
    public List<java.util.Map<String,Object>> vendors() {
        return jdbc.query("select u.id,u.full_name as name from users u join roles r on r.id=u.role_id where r.name='ROLE_VENDOR' and u.is_active=true and u.email_verified_at is not null order by u.full_name,u.id",
            (rs,n)->java.util.Map.<String,Object>of("id",rs.getLong("id"),"name",rs.getString("name")));
    }
    public boolean lockActiveVendor(long userId) {
        var rows=jdbc.queryForList("select role_id,is_active,email_verified_at from users where id=? for update",userId);
        if(rows.isEmpty()) return false;
        var row=rows.getFirst();Object active=row.get("is_active");
        return (Boolean.TRUE.equals(active) || active instanceof Number n && n.intValue()!=0) && row.get("email_verified_at")!=null
            && "ROLE_VENDOR".equals(jdbc.queryForObject("select name from roles where id=?",String.class,row.get("role_id")));
    }
    public boolean activeManager(long userId) {
        return Integer.valueOf(1).equals(jdbc.queryForObject("select count(*) from users u join roles r on r.id=u.role_id where u.id=? and r.name='ROLE_MANAGER' and u.is_active=true and u.email_verified_at is not null",Integer.class,userId));
    }
}
