package com.utetra.service;

import com.utetra.dto.CatalogRequests.*;
import com.utetra.dto.CatalogViews.*;
import com.utetra.entity.catalog.*;
import com.utetra.repository.catalog.*;
import com.utetra.security.UtetraPrincipal;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.Clock;
import java.util.*;
import static com.utetra.service.CatalogGuard.*;

@Service @Validated @Transactional(readOnly=true)
public class StoreService {
    private final StoreRepository stores;
    private final StoreRegistrationRequestRepository requests;
    private final StoreDeliveryAreaRepository areas;
    private final StoreMembershipRepository memberships;
    private final CatalogGuard guard;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final CatalogStatistics schema;
    public StoreService(StoreRepository stores,StoreRegistrationRequestRepository requests,StoreDeliveryAreaRepository areas,
        StoreMembershipRepository memberships,CatalogGuard guard,JdbcTemplate jdbc,Clock clock,CatalogStatistics schema) {
        this.stores=stores; this.requests=requests; this.areas=areas; this.memberships=memberships;
        this.guard=guard; this.jdbc=jdbc; this.clock=clock; this.schema=schema;
    }
    public List<DeliveryRateView> deliveryRates(Long storeId) {
        publicStore(storeId);
        if(!schema.tables("carriers","carrier_area_rates")) return List.of();
        return jdbc.query("select a.id,a.name,c.id carrier_id,c.name carrier_name,c.base_fee+r.additional_fee fee from store_delivery_areas a join carrier_area_rates r on r.delivery_area_id=a.id join carriers c on c.id=r.carrier_id where a.store_id=? and a.is_active=true and r.is_active=true and c.is_active=true order by a.id,c.id",(rs,n) -> new DeliveryRateView(rs.getLong("id"),rs.getString("name"),rs.getLong("carrier_id"),rs.getString("carrier_name"),rs.getBigDecimal("fee")),storeId);
    }
    public List<StoreView> publicStores() { return stores.findAll().stream().filter(s -> "ACTIVE".equals(s.getStatus())).map(this::view).toList(); }
    public StoreView publicStore(Long id) {
        var s=found(stores.findById(id)); require("ACTIVE".equals(s.getStatus()),"Shop chưa hoạt động"); return view(s);
    }
    public StoreView managedStore(UtetraPrincipal p,Long id) { guard.store(p,id); return view(found(stores.findById(id))); }
    public List<StoreView> managedStores(UtetraPrincipal p) {
        guard.role(p);
        return stores.findAll().stream().filter(s -> guard.operates(p,s.getId())).map(this::view).toList();
    }
    @Transactional public StoreView register(UtetraPrincipal p,@Valid StoreInput input) {
        guard.buyer(p); validate(input);
        var store=new Store(); store.setOwnerUserId(p.userId()); apply(store,input);
        store.setStatus("PENDING_APPROVAL"); store.setAcceptingOrders(false);
        stores.saveAndFlush(store);
        var request=new StoreRegistrationRequest(); request.setStoreId(store.getId()); request.setApplicantId(p.userId()); request.setStatus("PENDING");
        requests.save(request); guard.audit(p,"REGISTER","STORE",store.getId()); return view(store);
    }
    @Transactional public StoreView update(UtetraPrincipal p,Long id,@Valid StoreInput input) {
        guard.store(p,id); validate(input); var s=found(stores.findById(id)); version(s.getVersion(),input.version());
        require(!input.acceptingOrders() || "ACTIVE".equals(s.getStatus()),"Shop chưa được duyệt hoặc đang bị khóa");
        apply(s,input); stores.flush(); guard.audit(p,"UPDATE","STORE",id); return view(s);
    }
    @Transactional public StoreView review(UtetraPrincipal p,Long id,@Valid Decision input) {
        guard.reviewer(p,id); var s=found(stores.findById(id)); version(s.getVersion(),input.version());
        require(!s.getOwnerUserId().equals(p.userId()),"Không được tự duyệt shop");
        require("PENDING_APPROVAL".equals(s.getStatus()),"Hồ sơ đã được xử lý");
        if("APPROVED".equals(input.decision())) {
            require(input.commissionPolicyId()!=null,"Cần chọn chính sách hoa hồng đã được quản trị phê duyệt");
            Integer policies=jdbc.queryForObject("select count(*) from commission_policies where id=? and is_active=true",Integer.class,input.commissionPolicyId());
            require(policies!=null && policies==1,"Chính sách hoa hồng không hợp lệ");
        }
        var request=found(requests.findFirstByStoreIdOrderByIdDesc(id));
        request.setStatus(input.decision()); request.setNote(input.note().trim()); request.setReviewedBy(p.userId()); request.setReviewedAt(clock.instant());
        s.setStatus("APPROVED".equals(input.decision())?"ACTIVE":"REJECTED"); s.setAcceptingOrders(false);
        if("APPROVED".equals(input.decision())) {
            s.setCommissionPolicyId(input.commissionPolicyId());
            // Update only an active, verified buyer; role promotion and session revocation share this transaction.
            int changed=jdbc.update("update users set role_id=(select id from roles where name='ROLE_VENDOR'), version=version+1, updated_at=? where id=? and is_active=true and email_verified_at is not null and role_id in (select id from roles where name in ('ROLE_USER','ROLE_VENDOR'))", java.sql.Timestamp.from(clock.instant()),s.getOwnerUserId());
            require(changed==1,"Chủ shop không còn là tài khoản mua hàng hợp lệ");
            jdbc.update("update auth_sessions set revoked_at=?, version=version+1 where user_id=? and revoked_at is null",java.sql.Timestamp.from(clock.instant()),s.getOwnerUserId());
        }
        stores.flush(); guard.audit(p,input.decision(),"STORE",id); return view(s);
    }
    @Transactional public StoreView state(UtetraPrincipal p,Long id,@Valid StoreState input) {
        guard.reviewer(p,id); var s=found(stores.findById(id)); version(s.getVersion(),input.version());
        require(Set.of("ACTIVE","SUSPENDED").contains(s.getStatus()),"Cần duyệt hồ sơ trước");
        s.setStatus(input.status()); if(!"ACTIVE".equals(s.getStatus())) s.setAcceptingOrders(false);
        stores.flush(); guard.audit(p,"STATUS_"+input.status(),"STORE",id); return view(s);
    }
    @Transactional public AreaView area(UtetraPrincipal p,Long storeId,Long areaId,@Valid AreaInput input) {
        guard.store(p,storeId); var area=areaId==null?new StoreDeliveryArea():found(areas.findById(areaId));
        require(areaId==null || storeId.equals(area.getStoreId()),"Khu vực không thuộc shop");
        area.setStoreId(storeId); area.setAreaCode(input.areaCode().trim()); area.setName(input.name().trim()); area.setActive(input.active());
        areas.saveAndFlush(area); guard.audit(p,"SAVE","DELIVERY_AREA",area.getId()); return areaView(area);
    }
    @Transactional public void assign(UtetraPrincipal p,Long storeId,@Valid Membership input,boolean catalogDelegation) {
        guard.admin(p); found(stores.findById(storeId));
        Integer count=jdbc.queryForObject("select count(*) from users u join roles r on r.id=u.role_id where u.id=? and r.name='ROLE_MANAGER' and u.is_active=true and u.email_verified_at is not null",Integer.class,input.userId());
        require(count!=null && count==1,"Chỉ phân công Manager đang hoạt động");
        var m=memberships.findAll().stream().filter(x -> x.getUserId().equals(input.userId()) && x.getStoreId().equals(storeId)).findFirst().orElseGet(StoreMembership::new);
        m.setStoreId(storeId); m.setUserId(input.userId()); m.setAssignedBy(p.userId()); m.setActive(input.active()); m.setMemberRole(catalogDelegation?"CATALOG_ADMIN":"MANAGER");
        memberships.saveAndFlush(m); guard.audit(p,"ASSIGN","STORE",storeId);
    }
    private void validate(StoreInput i) { require(i.openTime().isBefore(i.closeTime()),"Giờ mở phải trước giờ đóng, không qua đêm"); }
    private void apply(Store s,StoreInput i) {
        s.setName(i.name().trim()); s.setSlug(i.slug()); s.setAddress(i.address().trim()); s.setPhone(i.phone().trim());
        s.setDescription(i.description()); s.setOpenTime(i.openTime()); s.setCloseTime(i.closeTime()); s.setAcceptingOrders(i.acceptingOrders());
    }
    public StoreView view(Store s) {
        return new StoreView(s.getId(),s.getOwnerUserId(),s.getName(),s.getSlug(),s.getAddress(),s.getPhone(),s.getDescription(),
            s.getImageUrl(),s.getBannerUrl(),s.getOpenTime(),s.getCloseTime(),s.getStatus(),s.getAcceptingOrders(),s.getVersion(),
            areas.findByStoreId(s.getId()).stream().map(this::areaView).toList());
    }
    private AreaView areaView(StoreDeliveryArea a) { return new AreaView(a.getId(),a.getAreaCode(),a.getName(),a.getActive()); }
}
