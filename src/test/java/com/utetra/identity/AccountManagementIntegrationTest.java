package com.utetra.identity;

import static org.assertj.core.api.Assertions.*;
import static com.utetra.identity.IdentityRepository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import com.utetra.security.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class AccountManagementIntegrationTest {
    @Autowired UserManagementService management;
    @Autowired AuthService auth;
    @Autowired IdentityRepository db;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired JwtService jwt;
    @MockitoBean OtpDelivery delivery;
    private static final String PASSWORD="StaffPassword!42";
    private UtetraPrincipal actor(String role) {
        for(String value:List.of("USER","VENDOR","MANAGER","ADMIN","SHIPPER"))
            if(db.count("select count(*) from roles where name=?","ROLE_"+value)==0) db.update("insert into roles(name) values(?)","ROLE_"+value);
        String username="a"+UUID.randomUUID().toString().replace("-","");
        auth.register(new IdentityForms.Register(username,username+"@example.test",PASSWORD,"Người quản lý"));
        db.update("update users set role_id=(select id from roles where name=?),email_verified_at=current_timestamp where username=?",role,username);
        return jwt.verify(auth.login(username+"@example.test",PASSWORD).access());
    }
    @Test void adminCreatesStaffButEmailVerificationStillRequiredAndSearchFiltersRole() {
        var admin=actor("ROLE_ADMIN");
        String username="s"+UUID.randomUUID().toString().replace("-","");
        long id=management.create(admin,new IdentityForms.CreateAccount(username,username+"@example.test",PASSWORD,"Nhân viên giao hàng","ROLE_SHIPPER"));
        var row=db.user(id,false);
        assertThat(string(row,"role_name")).isEqualTo("ROLE_SHIPPER");
        assertThat(row.get("email_verified_at")).isNull();
        assertThat(string(row,"password_hash")).doesNotContain(PASSWORD);
        assertThatThrownBy(()->auth.login(username+"@example.test",PASSWORD)).isInstanceOf(IdentityException.class);
        assertThat(management.search(admin,username,"ROLE_SHIPPER",0,20)).hasSize(1);
        assertThat(management.search(admin,username,"ROLE_MANAGER",0,20)).isEmpty();
    }
    @Test void disablingAndReactivatingPreservesAccountAndRevokesOldSession() {
        var admin=actor("ROLE_ADMIN"); var shipper=actor("ROLE_SHIPPER");
        var original=db.user(shipper.userId(),false);
        management.manage(admin,shipper.userId(),new IdentityForms.Manage(false,"ROLE_SHIPPER",number(original,"version")));
        var disabled=db.user(shipper.userId(),false);
        assertThat(flag(disabled,"is_active")).isFalse();
        assertThat(db.one("select revoked_at from auth_sessions where id=?",shipper.sessionId()).get("revoked_at")).isNotNull();
        assertThatThrownBy(()->auth.login(string(original,"email"),PASSWORD)).isInstanceOf(IdentityException.class);
        management.manage(admin,shipper.userId(),new IdentityForms.Manage(true,"ROLE_SHIPPER",number(disabled,"version")));
        assertThat(auth.login(string(original,"email"),PASSWORD).role()).isEqualTo("ROLE_SHIPPER");
        assertThat(string(db.user(shipper.userId(),false),"username")).isEqualTo(string(original,"username"));
    }
    @Test void activePickupPreventsDisablingOrChangingShipperRoleButConfirmedCanBeReassigned() {
        var admin=actor("ROLE_ADMIN"); var shipper=actor("ROLE_SHIPPER");
        long store=com.utetra.TestRows.insert(jdbc,"stores",Map.of("owner_user_id",admin.userId(),"name","Account safety shop","status","ACTIVE"));
        long order=com.utetra.TestRows.insert(jdbc,"orders",Map.of("user_id",admin.userId(),"store_id",store,"status","PICKED_UP","payment_status","PAID"));
        com.utetra.TestRows.insert(jdbc,"delivery_assignments",Map.of("order_id",order,"shipper_id",shipper.userId(),"assigned_by",admin.userId(),"status","ACTIVE"));
        long version=number(db.user(shipper.userId(),false),"version");
        assertThatThrownBy(()->management.manage(admin,shipper.userId(),new IdentityForms.Manage(false,"ROLE_SHIPPER",version))).isInstanceOf(IdentityException.class).hasMessageContaining("đang giao");
        db.update("update orders set status='DELIVERING' where id=?",order);
        assertThatThrownBy(()->management.manage(admin,shipper.userId(),new IdentityForms.Manage(true,"ROLE_USER",version))).isInstanceOf(IdentityException.class).hasMessageContaining("đang giao");
        assertThat(flag(db.user(shipper.userId(),false),"is_active")).isTrue();
        db.update("update orders set status='CONFIRMED' where id=?",order);
        management.manage(admin,shipper.userId(),new IdentityForms.Manage(false,"ROLE_SHIPPER",version));
        assertThat(flag(db.user(shipper.userId(),false),"is_active")).isFalse();
    }
    @Test void moderationIsAdminOnlyAndHideRestoreRetainsContentAndRejectsDeletedContent() {
        var admin=actor("ROLE_ADMIN"); var manager=actor("ROLE_MANAGER");
        long store=com.utetra.TestRows.insert(jdbc,"stores",Map.of("owner_user_id",admin.userId(),"name","Moderation shop","status","ACTIVE"));
        long product=com.utetra.TestRows.insert(jdbc,"products",Map.of("store_id",store,"name","Moderation tea","slug",UUID.randomUUID().toString(),"base_price",10000,"is_active",true));
        long variant=com.utetra.TestRows.insert(jdbc,"product_variants",Map.of("product_id",product));
        long order=com.utetra.TestRows.insert(jdbc,"orders",Map.of("user_id",admin.userId(),"store_id",store,"status","DELIVERED","payment_status","PAID"));
        long item=com.utetra.TestRows.insert(jdbc,"order_items",Map.of("order_id",order,"variant_id",variant,"product_name","Moderation tea","quantity",1));
        long review=com.utetra.TestRows.insert(jdbc,"reviews",Map.of("author_id",admin.userId(),"product_id",product,"order_item_id",item,"stars",5,"content","Đánh giá giữ nguyên sau kiểm duyệt","status","VISIBLE"));
        long reviewVersion=number(db.one("select version from reviews where id=?",review),"version");
        management.visibility(admin,true,review,false,reviewVersion);
        assertThat(management.moderation(admin,true,"Moderation tea","HIDDEN",0,20)).anyMatch(row->number(row,"id")==review && number(row,"stars")==5);
        management.visibility(admin,true,review,true,reviewVersion+1);
        assertThat(string(db.one("select status from reviews where id=?",review),"status")).isEqualTo("VISIBLE");
        String content="Nội dung bình luận hợp lệ của khách hàng sau khi nhận đơn hàng thử nghiệm.";
        long id=com.utetra.TestRows.insert(jdbc,"product_comments",Map.of("author_id",admin.userId(),"product_id",product,"order_item_id",item,"content",content,"status","VISIBLE"));
        assertThatThrownBy(()->management.moderation(manager,false,"","",0,20)).isInstanceOf(IdentityException.class);
        assertThatThrownBy(()->management.visibility(manager,false,id,false,0)).isInstanceOf(IdentityException.class);
        assertThat(management.moderation(admin,false,"Moderation tea","VISIBLE",0,20)).anyMatch(row->number(row,"id")==id);
        long version=number(db.one("select version from product_comments where id=?",id),"version");
        management.visibility(admin,false,id,false,version);
        var hidden=db.one("select * from product_comments where id=?",id);
        assertThat(string(hidden,"content")).isEqualTo(content);
        assertThat(management.moderation(admin,false,"","HIDDEN",0,100)).anyMatch(row->number(row,"id")==id);
        management.visibility(admin,false,id,true,number(hidden,"version"));
        assertThat(string(db.one("select status from product_comments where id=?",id),"status")).isEqualTo("VISIBLE");
        db.update("update product_comments set status='HIDDEN',content='' where id=?",id);
        long deletedVersion=number(db.one("select version from product_comments where id=?",id),"version");
        assertThatThrownBy(()->management.visibility(admin,false,id,true,deletedVersion)).isInstanceOf(IdentityException.class);
    }
    @Test void managerCannotCreateOrFilterIntoStaffOutsideTheirScope() {
        var manager=actor("ROLE_MANAGER");
        assertThatThrownBy(()->management.create(manager,new IdentityForms.CreateAccount("newstaff","newstaff@example.test",PASSWORD,"Nhân viên","ROLE_SHIPPER"))).isInstanceOf(IdentityException.class);
        assertThat(management.search(manager,"","ROLE_ADMIN",0,20)).isEmpty();
    }
    @Test void adminCannotCreateAdminOrAcceptAnUnknownRole() {
        var admin=actor("ROLE_ADMIN");
        assertThatThrownBy(()->management.create(admin,new IdentityForms.CreateAccount("newadmin","newadmin@example.test",PASSWORD,"Admin","ROLE_ADMIN"))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(()->management.search(admin,"","UNKNOWN",0,20)).isInstanceOf(IdentityException.class);
    }
}
