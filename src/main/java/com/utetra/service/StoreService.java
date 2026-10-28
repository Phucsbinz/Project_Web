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
    private final StoreOperationsRepository operations;
    private final Clock clock;
    private final CatalogStatisticsRepository schema;
    public StoreService(StoreRepository stores,StoreRegistrationRequestRepository requests,StoreDeliveryAreaRepository areas,
        StoreMembershipRepository memberships,CatalogGuard guard,StoreOperationsRepository operations,Clock clock,CatalogStatisticsRepository schema) {
        this.stores=stores; this.requests=requests; this.areas=areas; this.memberships=memberships;
        this.guard=guard; this.operations=operations; this.clock=clock; this.schema=schema;
    }
    public List<DeliveryRateView> deliveryRates(Long storeId) {
        publicStore(storeId);
        if(!schema.tables("carriers","carrier_area_rates")) return List.of();
        return operations.deliveryRates(storeId);
    }
    public List<StoreView> publicStores() { return stores.findAll().stream().filter(s -> "ACTIVE".equals(s.getStatus())).map(this::view).toList(); }
    public StoreView publicStore(Long id) {
        var s=found(stores.findById(id)); require("ACTIVE".equals(s.getStatus()),"Shop chưa hoạt động"); return view(s);
    }
    public StoreView managedStore(UtetraPrincipal p,Long id) { guard.store(p,id); return view(found(stores.findById(id))); }
    public List<RegistrationView> registrations(UtetraPrincipal p) {
        guard.buyer(p);
        return requests.findByApplicantIdOrderByIdDesc(p.userId()).stream()
            .map(r -> new RegistrationView(view(found(stores.findById(r.getStoreId()))),r.getStatus(),r.getNote(),r.getCreatedAt(),r.getReviewedAt())).toList();
    }
    /** Read existing policies for shop approval; creating policies belongs to Order. */
    public List<CommissionPolicyView> commissionPolicies(UtetraPrincipal p,Long storeId) {
        guard.reviewer(p,storeId);
        return operations.commissionPolicies();
    }
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
            boolean policyActive=operations.activePolicy(input.commissionPolicyId());
            require(policyActive,"Chính sách hoa hồng không hợp lệ");
        }
        var request=found(requests.findFirstByStoreIdOrderByIdDesc(id));
        request.setStatus(input.decision()); request.setNote(input.note().trim()); request.setReviewedBy(p.userId()); request.setReviewedAt(clock.instant());
        s.setStatus("APPROVED".equals(input.decision())?"ACTIVE":"REJECTED"); s.setAcceptingOrders(false);
        if("APPROVED".equals(input.decision())) {
            s.setCommissionPolicyId(input.commissionPolicyId());
            // Update only an active, verified buyer; role promotion and session revocation share this transaction.
            int changed=operations.promoteVendor(s.getOwnerUserId(),clock.instant());
            require(changed==1,"Chủ shop không còn là tài khoản mua hàng hợp lệ");
            operations.revokeSessions(s.getOwnerUserId(),clock.instant());
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
        boolean managerActive=operations.activeManager(input.userId());
        require(managerActive,"Chỉ phân công Manager đang hoạt động");
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
