package com.utetra.service;

import com.utetra.security.UtetraPrincipal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.utetra.service.CatalogGuard.*;

/** Operational writes deliberately exclude price, promotions, ownership and settlement. */
@Service
@Transactional(readOnly=true)
public class ManagerOperationsService {
    private final JdbcTemplate db;
    private final CatalogGuard guard;
    private final StoreService stores;
    public ManagerOperationsService(JdbcTemplate db,CatalogGuard guard,StoreService stores) {this.db=db;this.guard=guard;this.stores=stores;}
    public Object stores(UtetraPrincipal p) { operatorRole(p);return stores.managedStores(p); }
    private void operatorRole(UtetraPrincipal p) { if(!Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(guard.role(p))) forbidden(); }
    private Map<String,Object> row(String sql,Object... args) {
        var rows=db.queryForList(sql,args);if(rows.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);return rows.getFirst();
    }
    @Transactional public Object accepting(UtetraPrincipal p,long store,Boolean accepting,Long version) {
        operatorRole(p);guard.mutation(p,store);require(accepting!=null,"Thiếu trạng thái nhận đơn");
        var s=row("select version,status from stores where id=? for update",store);expectedVersion(((Number)s.get("version")).longValue(),version);
        require("ACTIVE".equals(s.get("status")),"Chỉ thay khả năng nhận đơn của cửa hàng đang hoạt động");
        db.update("update stores set accepting_orders=?,version=version+1,updated_at=current_timestamp where id=?",accepting,store);guard.audit(p,"ACCEPTING_ORDERS","STORE",store);
        return Map.of("id",store,"acceptingOrders",accepting,"version",version+1);
    }
    @Transactional public Object availability(UtetraPrincipal p,long product,Boolean available,Long version) {
        operatorRole(p);var before=row("select store_id from products where id=?",product);long store=((Number)before.get("store_id")).longValue();guard.mutation(p,store);
        row("select id from stores where id=? for update",store);
        var current=row("select version,store_id from products where id=? for update",product);require(((Number)current.get("store_id")).longValue()==store,"Phạm vi món đã thay đổi");
        require(available!=null,"Thiếu trạng thái còn món");expectedVersion(((Number)current.get("version")).longValue(),version);
        db.update("update products set is_available=?,version=version+1,updated_at=current_timestamp where id=?",available,product);guard.audit(p,"AVAILABILITY","PRODUCT",product);
        return Map.of("id",product,"available",available,"version",version+1);
    }
    @Transactional public Object stock(UtetraPrincipal p,long variant,Integer quantity,Long version) {
        operatorRole(p);require(quantity!=null && quantity>=0 && quantity<=1000000,"Tồn kho phải từ 0 đến 1000000");
        var before=row("select p.store_id from products p join product_variants v on v.product_id=p.id where v.id=?",variant);long store=((Number)before.get("store_id")).longValue();guard.mutation(p,store);
        row("select id from stores where id=? for update",store);
        var v=row("select v.version,v.stock_quantity,p.store_id from product_variants v join products p on p.id=v.product_id where v.id=? for update",variant);
        require(((Number)v.get("store_id")).longValue()==store,"Phạm vi món đã thay đổi");expectedVersion(((Number)v.get("version")).longValue(),version);
        require(v.get("stock_quantity")!=null,"Chủ cửa hàng chưa bật theo dõi tồn kho cho size này");
        db.update("update product_variants set stock_quantity=?,version=version+1,updated_at=current_timestamp where id=?",quantity,variant);guard.audit(p,"STOCK","PRODUCT_VARIANT",variant);
        return Map.of("id",variant,"stockQuantity",quantity,"version",version+1);
    }
    public Object managers(UtetraPrincipal p) {
        guard.admin(p);
        var accounts=db.queryForList("select u.id,u.full_name as name,u.is_active as active from users u join roles r on r.id=u.role_id where r.name='ROLE_MANAGER' order by u.id").stream().map(m->Map.of("id",m.get("id"),"name",Objects.toString(m.get("name"),""),"active",flag(m.get("active")))).toList();
        var shopList=db.queryForList("select id,name,status from stores order by id").stream().map(m->Map.of("id",m.get("id"),"name",Objects.toString(m.get("name"),""),"status",m.get("status"))).toList();
        var assignments=db.queryForList("select user_id,store_id,is_active,member_role,assigned_by from store_memberships order by store_id,user_id").stream().map(m->Map.of("userId",m.get("user_id"),"storeId",m.get("store_id"),"active",m.get("is_active"),"catalogDelegation","CATALOG_ADMIN".equals(m.get("member_role")),"assignedBy",m.get("assigned_by"))).toList();
        return Map.of("managers",accounts,"stores",shopList,"assignments",assignments);
    }
    public Object assignedManagers(UtetraPrincipal p,long store) {
        if(!Set.of("ROLE_ADMIN","ROLE_VENDOR").contains(guard.role(p))) forbidden();guard.store(p,store);
        return db.queryForList("select u.id,u.full_name as name,m.is_active as active,m.member_role from store_memberships m join users u on u.id=m.user_id join roles r on r.id=u.role_id where m.store_id=? and r.name='ROLE_MANAGER' order by u.id",store).stream().map(m->Map.of("id",m.get("id"),"name",Objects.toString(m.get("name"),""),"active",flag(m.get("active")),"memberRole",m.get("member_role"))).toList();
    }
    private boolean flag(Object value) {return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue()!=0;}
}
