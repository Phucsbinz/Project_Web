package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.utetra.security.*;
import com.utetra.service.SessionIdentityService;
import com.utetra.service.MediaService;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")

class IdentityIntegrationTest {
    @Autowired IdentityRepository db;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private long row(String table,Map<String,Object> values) {return com.utetra.TestRows.insert(jdbc,table,values);}
    @Autowired AuthService auth;
    @Autowired OtpService otp;
    @Autowired AccountService account;
    @Autowired CommunityService community;
    @Autowired CommunityMediaService media;
    @Autowired LoyaltyService loyalty;
    @Autowired UserManagementService management;
    @Autowired JwtService jwt;
    @Autowired SecretTokens secretTokens;
    @Autowired SessionIdentityService identity;
    @Autowired TransactionTemplate tx;
    @Autowired IdentityStoreAccessPolicy stores;
    @Autowired MockMvc mvc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired jakarta.validation.Validator validator;
    @MockitoBean OtpDelivery delivery;
    @MockitoBean MediaService imageProvider;
    String email,username; AtomicReference<String> lastOtp;
    static final String PASSWORD="StrongPassword!42";
    @BeforeEach void prepare() {
        String suffix=UUID.randomUUID().toString().replace("-","").substring(0,12);
        username="user"+suffix; email=username+"@example.test"; lastOtp=new AtomicReference<>();
        doAnswer(call -> { lastOtp.set(call.getArgument(1)); return null; }).when(delivery).send(anyString(),anyString(),anyString());
        for(String role:List.of("USER","VENDOR","MANAGER","ADMIN","SHIPPER")) if(db.count("select count(*) from roles where name=?","ROLE_"+role)==0) db.update("insert into roles(name) values(?)","ROLE_"+role);
    }
    @AfterEach void disableTestAdmins() {
        db.update("update users set is_active=false where username=? and role_id=(select id from roles where name='ROLE_ADMIN')",username);
    }
    private long register() {
        auth.register(new IdentityForms.Register(username,email,PASSWORD,"Khách thử nghiệm"));
        return number(db.one("select id from users where email=?",email),"id");
    }
    private UtetraPrincipal buyer() {
        register(); otp.verify(email,"REGISTER",lastOtp.get()); return jwt.verify(auth.login(email,PASSWORD).access());
    }
    private UtetraPrincipal role(String role) {
        var p=buyer(); db.update("update users set role_id=(select id from roles where name=?) where id=?","ROLE_"+role,p.userId());
        return jwt.verify(auth.login(email,PASSWORD).access());
    }
    private long product(long owner) {
        long store=row("stores",Map.of("owner_user_id",owner,"name","Shop test","status","ACTIVE"));
        return row("products",Map.of("store_id",store,"name","Trà sữa","slug",UUID.randomUUID().toString(),"base_price",50000,"is_active",true));
    }
    private long order(long user,long product,String status,String payment,int points,int amount) {
        long variant=row("product_variants",Map.of("product_id",product));
        long store=number(db.one("select store_id from products where id=?",product),"store_id");
        long id=row("orders",Map.of("user_id",user,"store_id",store,"status",status,"payment_status",payment,"points_used",points,"points_earned",0,"subtotal",amount,"product_discount",0,"points_discount",points*1000));
        row("order_items",Map.of("order_id",id,"variant_id",variant,"product_name","Trà sữa","quantity",2)); return id;
    }
    @Test void registrationHashesPasswordAndRequiresActivation() {
        register(); var u=db.one("select * from users where email=?",email);
        assertThat(string(u,"password_hash")).startsWith("$2").doesNotContain(PASSWORD);
        assertThat(u.get("email_verified_at")).isNull();
        assertThatThrownBy(() -> auth.login(email,PASSWORD)).isInstanceOf(IdentityException.class);
        assertThat(string(db.one("select * from otp_challenges where email=?",email),"otp_hash")).hasSize(64).doesNotContain(lastOtp.get());
        otp.verify(email,"REGISTER",lastOtp.get()); assertThat(auth.login(email,PASSWORD).access()).isNotBlank();
        assertThatThrownBy(() -> otp.verify(email,"REGISTER",lastOtp.get())).isInstanceOf(OtpRejectedException.class);
    }
    @Test void wrongOtpAttemptsCommitAndBlockTheCorrectCode() {
        register(); String correct=lastOtp.get(); String wrong=correct.equals("000000")?"111111":"000000";
        for(int i=0;i<5;i++) assertThatThrownBy(() -> otp.verify(email,"REGISTER",wrong)).isInstanceOf(OtpRejectedException.class);
        assertThat(number(db.one("select attempts from otp_challenges where email=?",email),"attempts")).isEqualTo(5);
        assertThatThrownBy(() -> otp.verify(email,"REGISTER",correct)).isInstanceOf(OtpRejectedException.class);
    }
    @Test void otpStillWorksTenSecondsAfterIssueAndExpiryIsReportedOnlyForCorrectCode() {
        register(); String correct=lastOtp.get();
        db.update("update otp_challenges set created_at=?,expires_at=? where email=?",time(Instant.now().minusSeconds(10)),time(Instant.now().plusSeconds(290)),email);
        otp.verify(email,"REGISTER",correct);
        assertThatThrownBy(()->otp.verify(email,"REGISTER",correct)).isInstanceOf(OtpRejectedException.class).hasMessageContaining("đã được sử dụng");
        db.update("update otp_challenges set created_at=? where email=?",time(Instant.now().minusSeconds(61)),email);
        otp.request(email,"PASSWORD_RESET");final String expired=lastOtp.get();
        db.update("update otp_challenges set expires_at=? where email=?",time(Instant.now().minusSeconds(1)),email);
        String wrong=expired.equals("000000")?"111111":"000000";
        assertThatThrownBy(()->otp.verify(email,"PASSWORD_RESET",wrong)).isInstanceOf(OtpRejectedException.class).hasMessageNotContaining("đã hết hạn");
        assertThatThrownBy(()->otp.verify(email,"PASSWORD_RESET",expired)).isInstanceOf(OtpRejectedException.class).hasMessageContaining("đã hết hạn");
    }
    @Test void expiredWrongPurposeAndResendAreRejected() {
        register();
        assertThatThrownBy(() -> otp.verify(email,"PASSWORD_RESET",lastOtp.get())).isInstanceOf(OtpRejectedException.class);
        assertThatThrownBy(() -> otp.request(email,"REGISTER")).isInstanceOf(IdentityException.class);
        db.update("update otp_challenges set expires_at=? where email=?",time(Instant.now().minusSeconds(1)),email);
        assertThatThrownBy(() -> otp.verify(email,"REGISTER",lastOtp.get())).isInstanceOf(OtpRejectedException.class);
    }
    @Test void smtpFailureRollsBackRegistration() {
        doThrow(new IllegalStateException("SMTP unavailable")).when(delivery).send(anyString(),anyString(),anyString());
        assertThatThrownBy(this::register).isInstanceOf(IllegalStateException.class);
        assertThat(db.count("select count(*) from users where email=?",email)).isZero();
    }
    @Test void refreshRotatesAndKeepsAbsoluteExpiration() {
        var p=buyer(); var first=auth.login(email,PASSWORD); var token=jwt.verify(first.access());
        var before=db.one("select expires_at from auth_sessions where id=?",token.sessionId()).get("expires_at");
        var next=auth.refresh(first.refresh()); assertThat(next.refresh()).isNotEqualTo(first.refresh());
        assertThatThrownBy(() -> auth.refresh(first.refresh())).isInstanceOf(IdentityException.class);
        assertThat(db.one("select expires_at from auth_sessions where id=?",token.sessionId()).get("expires_at")).isEqualTo(before);
        auth.logout(token,next.refresh()); assertThat(identity.isCurrent(token)).isFalse();
        assertThatThrownBy(() -> auth.refresh(next.refresh())).isInstanceOf(IdentityException.class);
        assertThat(identity.isCurrent(p)).isTrue();
    }
    @Test void customerLoginRedirectsHomeWithoutChangingManagerDestination() throws Exception {
        buyer();
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password",PASSWORD))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.redirect").value("/"));
        db.update("update users set role_id=(select id from roles where name='ROLE_MANAGER') where email=?",email);
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password",PASSWORD))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.redirect").value("/account/profile"));
    }
    @Test void concurrentRefreshHasExactlyOneWinner() throws Exception {
        buyer(); var token=auth.login(email,PASSWORD).refresh();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var results=pool.invokeAll(List.of(() -> { try { auth.refresh(token); return true; } catch(IdentityException ex) { return false; } },() -> { try { auth.refresh(token); return true; } catch(IdentityException ex) { return false; } }));
            assertThat(results.stream().map(f -> { try { return f.get(); } catch(Exception ex) { throw new RuntimeException(ex); } }).filter(Boolean.TRUE::equals).count()).isEqualTo(1);
        }
    }
    @Test void resetGrantIsOneUseAndRevokesAllSessions() {
        var p=buyer(); db.update("update otp_challenges set created_at=? where email=?",time(Instant.now().minusSeconds(61)),email);
        otp.request(email,"PASSWORD_RESET"); String code=lastOtp.get(),grant=otp.verify(email,"PASSWORD_RESET",code);
        assertThatThrownBy(() -> otp.verify(email,"PASSWORD_RESET",code)).isInstanceOf(OtpRejectedException.class);
        var challenge=db.one("select * from otp_challenges where email=? and purpose='PASSWORD_RESET'",email);
        assertThat(challenge.get("consumed_at")).isNotNull();
        assertThat(challenge.get("revoked_at")).isNull();
        assertThat(instant(challenge,"expires_at")).isAfter(Instant.now());
        assertThat(secretTokens.matches(string(challenge,"reset_grant_hash"),secretTokens.hash(grant))).isTrue();
        db.update("update users set password_plaintext=? where id=?","123456",p.userId());
        auth.reset(grant,"ChangedPassword!42");
        assertThat(db.one("select password_plaintext from users where id=?",p.userId()).get("password_plaintext")).isNull();
        assertThat(identity.isCurrent(p)).isFalse(); assertThatThrownBy(() -> auth.reset(grant,"AnotherPassword42!")).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> auth.login(email,PASSWORD)).isInstanceOf(IdentityException.class);
        assertThat(auth.login(email,"ChangedPassword!42").access()).isNotBlank();
    }
    @Test void passwordByteLimitAndStaleProfileAreRejected() {
        assertThatThrownBy(() -> IdentityValidation.password("ậ".repeat(25))).isInstanceOf(IdentityException.class);
        var p=buyer(); long version=number(account.profile(p),"version");
        account.profile(p,new IdentityForms.Profile("Tên mới","0901234567",version));
        assertThatThrownBy(() -> account.profile(p,new IdentityForms.Profile("Ghi đè","",version))).isInstanceOf(IdentityException.class);
        assertThat(account.profile(p)).doesNotContainKeys("password_hash","password_plaintext","avatar_public_id");
        db.update("update users set password_plaintext=? where id=?","123456",p.userId());
        account.password(p,new IdentityForms.Password(PASSWORD,"ChangedPassword!42")); assertThat(identity.isCurrent(p)).isFalse();
        assertThat(db.one("select password_plaintext from users where id=?",p.userId()).get("password_plaintext")).isNull();
    }
    @Test void addressesAreOwnedAndHaveOneDefault() {
        var p=buyer(); var form=new IdentityForms.Address("Người nhận","0901234567","TP HCM",true);
        long first=account.saveAddress(p,null,form),second=account.saveAddress(p,null,form);
        assertThat(account.addresses(p).stream().filter(a -> flag(a,"is_default"))).hasSize(1);
        String oldEmail=email; email="other"+email; username="other"+username; var other=buyer();
        assertThatThrownBy(() -> account.deleteAddress(other,second)).isInstanceOf(IdentityException.class);
        account.deleteAddress(p,second); assertThat(flag(account.addresses(p).getFirst(),"is_default")).isTrue();
        assertThat(account.addressSnapshot(p,first).get("address_detail")).isEqualTo("TP HCM");
    }
    @Test void concurrentDefaultAddressWritesSerialize() throws Exception {
        var p=buyer(); var f=new IdentityForms.Address("Người nhận","0901234567","TP HCM",true);
        try(var pool=Executors.newFixedThreadPool(2)) {
            for(var task:pool.invokeAll(List.of(() -> account.saveAddress(p,null,f),() -> account.saveAddress(p,null,f)))) task.get();
        }
        assertThat(db.count("select count(*) from user_addresses where user_id=? and is_default=true",p.userId())).isEqualTo(1);
    }
    @Test void vendorCanLikeAndViewIdempotently() {
        var p=role("VENDOR"); long id=product(p.userId());
        assertThat(community.favorite(p,id,true)).isEqualTo(1); assertThat(community.favorite(p,id,true)).isEqualTo(1);
        community.viewed(p,id); community.viewed(p,id);
        assertThat(number(db.one("select view_count from product_views where user_id=?",p.userId()),"view_count")).isEqualTo(2);
        assertThat(community.products(p,false,0,20)).hasSize(1); assertThat(community.favorite(p,id,false)).isZero();
    }
    @Test void purchaseEvidenceAndCommentLengthAreEnforced() {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"NEW","UNPAID",0,50000);
        long item=number(db.one("select id from order_items where order_id=?",order),"id");
        var valid=new IdentityForms.Comment(product,item,"a".repeat(50));
        assertThatThrownBy(() -> community.comment(p,valid,List.of())).isInstanceOf(IdentityException.class);
        db.update("update orders set status='DELIVERED',payment_status='PAID' where id=?",order);
        assertThatThrownBy(() -> community.comment(p,new IdentityForms.Comment(product,item,"a".repeat(49)),List.of())).isInstanceOf(IdentityException.class);
        long comment=community.comment(p,valid,List.of());
        assertThat(comment).isPositive();
        long review=community.review(p,new IdentityForms.Review(product,item,5,"<script>alert(1)</script>"),List.of());
        assertThatThrownBy(() -> community.review(p,new IdentityForms.Review(product,item,4,""),List.of())).isInstanceOf(IdentityException.class);
        var publicData=community.publicContent(product,0,20);
        assertThat(((Number)publicData.get("soldUnits")).longValue()).isPositive();
        assertThat(((Number)publicData.get("commentCount")).longValue()).isEqualTo(1);
        community.delete(p,true,review);
        assertThat(((java.util.Map<?,?>)community.publicContent(product,0,20).get("rating")).get("review_count")).isEqualTo(0L);
        assertThatThrownBy(() -> community.review(p,new IdentityForms.Review(product,item,4,""),List.of())).isInstanceOf(IdentityException.class);
    }
    @Test void fakeVideoAndExcessMediaAreRejectedBeforeUpload() {
        var fake=new MockMultipartFile("files","fake.mp4","video/mp4","executable".getBytes());
        assertThatThrownBy(() -> media.validate(fake)).isInstanceOf(IdentityException.class);
        verifyNoInteractions(imageProvider);
    }
    @Test void loyaltyEarnRedeemReversalCreatesDebtAndRepaysIt() {
        var p=buyer(); long product=product(p.userId()),earned=order(p.userId(),product,"DELIVERED","PAID",0,1000000);
        tx.executeWithoutResult(s -> { loyalty.apply(earned,"EARN"); loyalty.apply(earned,"EARN"); });
        assertThat(loyalty.summary(p,0,20).get("balance")).isEqualTo(100L);
        long redeem=order(p.userId(),product,"NEW","UNPAID",80,500000);
        tx.executeWithoutResult(s -> loyalty.apply(redeem,"REDEEM"));
        db.update("update orders set payment_status='REFUNDED',status='REFUNDED' where id=?",earned);
        tx.executeWithoutResult(s -> { loyalty.apply(earned,"REVERSAL"); loyalty.apply(earned,"REVERSAL"); });
        assertThat(loyalty.summary(p,0,20)).containsEntry("balance",0L).containsEntry("debt",80L).containsEntry("tier","Đồng");
        long next=order(p.userId(),product,"DELIVERED","PAID",0,900000);
        tx.executeWithoutResult(s -> loyalty.apply(next,"EARN"));
        assertThat(loyalty.summary(p,0,20)).containsEntry("balance",10L).containsEntry("debt",0L);
        db.update("update orders set status='CANCELLED' where id=?",redeem);
        tx.executeWithoutResult(s -> { loyalty.apply(redeem,"REFUND"); loyalty.apply(redeem,"REFUND"); });
        assertThat(loyalty.summary(p,0,20).get("balance")).isEqualTo(90L);
        assertThat(db.count("select count(*) from loyalty_transactions where order_id=?",earned)).isEqualTo(2);
    }
    @Test void loyaltyRollsBackWithOrderTransactionAndRefusesUnpaidEarn() {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"DELIVERED","PAID",0,100000);
        tx.executeWithoutResult(s -> { loyalty.apply(order,"EARN"); s.setRollbackOnly(); });
        assertThat(loyalty.summary(p,0,20).get("balance")).isEqualTo(0L);
        db.update("update orders set payment_status='UNPAID' where id=?",order);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,"EARN"))).isInstanceOf(IdentityException.class);
    }
    @Test void managerCannotEscalateOrReadUnassignedCustomers() {
        var manager=role("MANAGER"); email="target"+email; username="target"+username; var target=buyer();
        long store=row("stores",Map.of("owner_user_id",target.userId(),"name","Assigned shop","status","ACTIVE"));
        db.update("insert into store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) values(?,?,'MANAGER',true,?,?,?)",manager.userId(),store,manager.userId(),time(Instant.now()),time(Instant.now()));
        long version=number(account.profile(target),"version");
        assertThatThrownBy(() -> management.manage(manager,target.userId(),new IdentityForms.Manage(true,"ROLE_ADMIN",version))).isInstanceOf(IdentityException.class);
        management.manage(manager,target.userId(),new IdentityForms.Manage(false,"ROLE_USER",version));
        assertThat(identity.isCurrent(target)).isFalse();
        assertThat(db.count("select count(*) from audit_logs where actor_id=?",manager.userId())).isEqualTo(1);
        assertThat(management.search(manager,"absent",0,20)).isEmpty();
    }
    @Test void lastAdminIsProtected() {
        var admin=role("ADMIN");
        long version=number(account.profile(admin),"version");
        assertThatThrownBy(() -> management.manage(admin,admin.userId(),new IdentityForms.Manage(false,"ROLE_USER",version))).isInstanceOf(IdentityException.class);
        // Do not leave an additional active Admin for other tests.
        db.update("update users set is_active=false where id=?",admin.userId());
    }
    @Test void csrfCookiesAndProfileRoleRulesWorkThroughHttp() throws Exception {
        var p=role("MANAGER"); String token=auth.login(email,PASSWORD).access();
        mvc.perform(post("/auth/login").contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk()).andExpect(header().stringValues("Set-Cookie",org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("HttpOnly"))));
        mvc.perform(get("/account/profile/data").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.password_hash").doesNotExist());
        mvc.perform(get("/account/addresses/data").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }
    @Test void otpResendRevokesOldChallengeAndUnknownEmailUsesSameDelivery() {
        register(); long old=number(db.one("select id from otp_challenges where email=?",email),"id");
        db.update("update otp_challenges set created_at=? where id=?",time(Instant.now().minusSeconds(61)),old);
        otp.request(email,"REGISTER");
        assertThat(db.one("select revoked_at from otp_challenges where id=?",old).get("revoked_at")).isNotNull();
        String unknown="unknown"+email; otp.request(unknown,"PASSWORD_RESET");
        verify(delivery).send(eq(unknown),anyString(),eq("PASSWORD_RESET"));
        assertThatThrownBy(() -> otp.verify(unknown,"PASSWORD_RESET",lastOtp.get())).isInstanceOf(OtpRejectedException.class);
    }
    @Test void combinedProfileSaveRollsBackDetailsWhenAvatarIsInvalid() {
        var p=buyer(); var before=account.profile(p);
        var invalid=new MockMultipartFile("file","bad.png","image/png",new byte[]{1,2,3});
        assertThatThrownBy(() -> account.profileWithAvatar(p,new IdentityForms.Profile("Không được lưu","0901234567",number(before,"version")),invalid)).isInstanceOf(IdentityException.class);
        assertThat(account.profile(p).get("full_name")).isEqualTo(before.get("full_name"));
        assertThat(account.profile(p).get("version")).isEqualTo(before.get("version"));
        account.profileWithAvatar(p,new IdentityForms.Profile("Đã lưu hồ sơ","0901234567",number(before,"version")),null);
        assertThat(account.profile(p).get("full_name")).isEqualTo("Đã lưu hồ sơ");
    }
    @Test void avatarUploadIsCompensatedOnRollbackAndOldAssetDeletedAfterCommit() {
        var p=buyer();
        var file=new MockMultipartFile("file","avatar.png","image/png",TestImages.png());
        when(imageProvider.uploadImage(file)).thenReturn(new com.utetra.dto.MediaUploadResult("https://example.test/avatar.png","utetra/new","image"));
        tx.executeWithoutResult(s -> { account.avatar(p,file); s.setRollbackOnly(); });
        verify(imageProvider).deleteOwnedAsset("utetra/new");
        assertThat(account.profile(p).get("avatar_url")).isNull();
        db.update("update users set avatar_public_id='utetra/old' where id=?",p.userId());
        account.avatar(p,file); verify(imageProvider).deleteOwnedAsset("utetra/old");
        assertThat(account.profile(p).get("avatar_url")).isEqualTo("https://example.test/avatar.png");
    }
    @Test void managerMembershipControlsStoreAccessAndAdminAssignmentRevokesSessions() {
        var manager=role("MANAGER"); long product=product(manager.userId());
        long store=number(db.one("select store_id from products where id=?",product),"store_id");
        assertThat(stores.canOperate(manager,store)).isFalse();
        db.update("insert into store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) values(?,?,'MANAGER',true,?,?,?)",manager.userId(),store,manager.userId(),time(Instant.now()),time(Instant.now()));
        assertThat(stores.canOperate(manager,store)).isTrue();
        assertThat(stores.canOperate(manager,store+100000)).isFalse();
        db.update("update store_memberships set is_active=false where user_id=?",manager.userId());
        assertThat(stores.canOperate(manager,store)).isFalse();
    }
    @Test void mediaRollbackCleansUploadedAssetAndPublicContentHidesModeratedRows() {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"DELIVERED","PAID",0,50000);
        long item=number(db.one("select id from order_items where order_id=?",order),"id");
        var file=new MockMultipartFile("files","image.png","image/png",TestImages.png());
        when(imageProvider.uploadImage(file)).thenReturn(new com.utetra.dto.MediaUploadResult("https://example.test/photo.png","utetra/comment","image"));
        tx.executeWithoutResult(s -> { community.comment(p,new IdentityForms.Comment(product,item,"a".repeat(50)),List.of(file)); s.setRollbackOnly(); });
        verify(imageProvider).deleteOwnedAsset("utetra/comment");
        long review=community.review(p,new IdentityForms.Review(product,item,4,"Nội dung"),List.of());
        assertThat((List<?>)community.publicContent(product,0,20).get("reviews")).hasSize(1);
        community.delete(p,true,review);
        assertThat((List<?>)community.publicContent(product,0,20).get("reviews")).isEmpty();
    }
    @Test void internalServiceCallsCannotBypassDtoAndPhoneValidation() {
        assertThatThrownBy(() -> auth.register(new IdentityForms.Register("x",email,PASSWORD,"Tên"))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> auth.register(new IdentityForms.Register(username,"bad email",PASSWORD,"Tên"))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> auth.register(new IdentityForms.Register(username,email," ".repeat(12),"Tên"))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> auth.register(new IdentityForms.Register(username,email,"Password\u0000Bad42","Tên"))).isInstanceOf(IdentityException.class);
        var p=buyer(); long version=number(account.profile(p),"version");
        for(String phone:List.of("--------","(       )","abc12345678","1234567","+1234567890123456")) {
            assertThatThrownBy(() -> account.profile(p,new IdentityForms.Profile("Tên",phone,version))).isInstanceOf(IdentityException.class);
            assertThatThrownBy(() -> account.saveAddress(p,null,new IdentityForms.Address("Tên",phone,"Địa chỉ",true))).isInstanceOf(IdentityException.class);
        }
        assertThatThrownBy(() -> account.profile(p,new IdentityForms.Profile("Tên","",null))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> account.profile(p,new IdentityForms.Profile("Tên","",-1L))).isInstanceOf(IdentityException.class);
        assertThat(account.addresses(p)).isEmpty();
    }
    @Test void emailNullDisplayNameAndPasswordByteOverflowFailSafely() {
        for(String bad:List.of("bad","bad\r\n@example.test","Name <real@example.test>","a".repeat(255)+"@example.test"))
            assertThatThrownBy(() -> otp.request(bad,"REGISTER")).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> otp.verify(null,"REGISTER","000000")).isInstanceOf(IdentityException.class);
        var p=buyer();
        assertThatThrownBy(() -> auth.login(email,"ậ".repeat(25))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> account.password(p,new IdentityForms.Password("ậ".repeat(25),PASSWORD))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> IdentityValidation.password("😀".repeat(7))).isInstanceOf(IdentityException.class);
        IdentityValidation.password("😀".repeat(8));
    }
    @Test void profileApiRejectsMissingNullNegativeAndCoercedVersionsWithoutWriting() throws Exception {
        var p=buyer(); String token=auth.login(email,PASSWORD).access(); long version=number(account.profile(p),"version");
        var bodies=List.of(
            "{\"fullName\":\"Tên\"}","{\"fullName\":\"Tên\",\"version\":null}",
            "{\"fullName\":\"Tên\",\"version\":-1}","{\"fullName\":\"Tên\",\"version\":0.9}",
            "{\"fullName\":\"Tên\",\"version\":\"0\"}","{\"fullName\":\"Tên\",\"version\":999999999999999999999}",
            "{\"fullName\":\" \",\"version\":"+version+"}",
            "{\"fullName\":\"Tên\",\"phone\":\"--------\",\"version\":"+version+"}");
        for(String body:bodies) mvc.perform(post("/account/profile").header("Authorization","Bearer "+token).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        assertThat(number(account.profile(p),"version")).isEqualTo(version);
    }
    @Test void malformedIdsPagingAndMissingMultipartPartsReturnJson400() throws Exception {
        var p=buyer(); String token=auth.login(email,PASSWORD).access();
        for(String path:List.of("/favorites/0","/favorites/-1","/favorites/abc","/favorites/999999999999999999999"))
            mvc.perform(post(path).header("Authorization","Bearer "+token).with(csrf())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        for(String query:List.of("page=-1","page=10001","page=abc","size=0","size=101"))
            mvc.perform(get("/favorites/data?"+query).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(multipart("/account/profile/avatar").header("Authorization","Bearer "+token).with(csrf())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
    }
    @Test void adminAssignmentRejectsMissingFlagsAndCoercedIds() throws Exception {
        role("ADMIN"); String token=auth.login(email,PASSWORD).access();
        for(String body:List.of("{}","{\"storeId\":1}","{\"storeId\":0,\"active\":true}","{\"storeId\":-1,\"active\":true}","{\"storeId\":1.8,\"active\":true}","{\"storeId\":\"1\",\"active\":true}","{\"storeId\":1,\"active\":\"false\"}"))
            mvc.perform(post("/admin/users/1/assignments").header("Authorization","Bearer "+token).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @Test void resetAnonymousChallengeAndResentGrantAreRejectedWithout500() {
        String unknown="unknown"+email; otp.request(unknown,"PASSWORD_RESET");
        long anonymous=number(db.one("select id from otp_challenges where email=?",unknown),"id");
        assertThatThrownBy(() -> auth.reset(anonymous+"."+secretTokens.random(),PASSWORD)).isInstanceOfSatisfying(IdentityException.class,e -> assertThat(e.status()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED));
        var p=buyer(); db.update("update otp_challenges set created_at=? where email=?",time(Instant.now().minusSeconds(61)),email);
        otp.request(email,"PASSWORD_RESET"); String grant=otp.verify(email,"PASSWORD_RESET",lastOtp.get());
        db.update("update otp_challenges set created_at=? where email=?",time(Instant.now().minusSeconds(61)),email);
        otp.request(email,"PASSWORD_RESET");
        assertThatThrownBy(() -> auth.reset(grant,"NewPassword42!")).isInstanceOf(IdentityException.class);
        assertThat(identity.isCurrent(p)).isTrue();
    }
    @Test void unicodeCommentBoundariesMatchDtoServiceAndOwnerRules() throws Exception {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"DELIVERED","PAID",0,50000);
        long item=number(db.one("select id from order_items where order_id=?",order),"id");
        assertThat(validator.validate(new IdentityForms.Comment(product,item,"😀".repeat(2000)))).isEmpty();
        assertThat(validator.validate(new IdentityForms.Comment(product,item,"😀".repeat(2001)))).isNotEmpty();
        assertThatThrownBy(() -> community.comment(p,new IdentityForms.Comment(product,item,"😀".repeat(49)),List.of())).isInstanceOf(IdentityException.class);
        long comment=community.comment(p,new IdentityForms.Comment(product,item,"  "+"😀".repeat(50)+"  "),List.of());
        assertThat(string(db.one("select content from product_comments where id=?",comment),"content")).isEqualTo("😀".repeat(50));
        String ownerEmail=email; email="other"+email; username="other"+username; var other=buyer();
        assertThatThrownBy(() -> community.edit(other,false,comment,"a".repeat(50),0,0)).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> community.delete(other,false,comment)).isInstanceOf(IdentityException.class);
        String token=auth.login(email,PASSWORD).access();
        mvc.perform(put("/comments/"+comment).header("Authorization","Bearer "+token).with(csrf()).contentType("application/json").content("{\"content\":\""+"a".repeat(50)+"\"}")).andExpect(status().isBadRequest());
    }
    @Test void invalidStarsPurchaseMismatchAndHiddenContentEditsAreRejected() {
        var p=buyer(); long product=product(p.userId()),otherProduct=product(p.userId()),order=order(p.userId(),product,"DELIVERED","PAID",0,50000);
        long item=number(db.one("select id from order_items where order_id=?",order),"id");
        for(int stars:new int[]{0,6,-1}) assertThatThrownBy(() -> community.review(p,new IdentityForms.Review(product,item,stars,""),List.of())).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> community.review(p,new IdentityForms.Review(otherProduct,item,5,""),List.of())).isInstanceOf(IdentityException.class);
        long review=community.review(p,new IdentityForms.Review(product,item,5,"Tốt"),List.of());
        assertThatThrownBy(() -> community.edit(p,true,review,"Tốt",0,0)).isInstanceOf(IdentityException.class);
        community.delete(p,true,review);
        assertThatThrownBy(() -> community.edit(p,true,review,"Tốt",5,1)).isInstanceOf(IdentityException.class);
    }
    @Test void fakeEmptyMismatchedAndOversizedImagesNeverReachTheProvider() {
        var p=buyer();
        for(var file:List.of(new MockMultipartFile("file","fake.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10}),
            new MockMultipartFile("file","empty.png","image/png",new byte[0]),
            new MockMultipartFile("file","mismatch.jpg","image/jpeg",TestImages.png()),
            new MockMultipartFile("file","large.png","image/png",new byte[5*1024*1024+1])))
            assertThatThrownBy(() -> account.avatar(p,file)).isInstanceOf(IdentityException.class);
        verifyNoInteractions(imageProvider);
    }
    @Test void moreThanFiveFilesAndNamedEmptyAttachmentRollBackContent() {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"DELIVERED","PAID",0,50000);
        long item=number(db.one("select id from order_items where order_id=?",order),"id");
        var form=new IdentityForms.Comment(product,item,"a".repeat(50));
        var file=new MockMultipartFile("files","image.png","image/png",TestImages.png());
        assertThatThrownBy(() -> community.comment(p,form,Collections.nCopies(6,file))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> community.comment(p,form,List.of(new MockMultipartFile("files","empty.png","image/png",new byte[0])))).isInstanceOf(IdentityException.class);
        assertThat(db.count("select count(*) from product_comments where author_id=?",p.userId())).isZero();
        verifyNoInteractions(imageProvider);
    }
    @Test void loyaltyRejectsInvalidSnapshotInsufficientBalanceAndPrematureReversal() {
        var p=buyer(); long product=product(p.userId()),order=order(p.userId(),product,"NEW","UNPAID",1,100000);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,"REDEEM"))).isInstanceOf(IdentityException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,null))).isInstanceOf(IdentityException.class);
        db.update("update orders set points_discount=0 where id=?",order);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,"REDEEM"))).isInstanceOf(IdentityException.class);
        db.update("update orders set points_used=0,subtotal=-1 where id=?",order);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,"EARN"))).isInstanceOf(IdentityException.class);
        db.update("update orders set subtotal=100000,status='CANCELLED',payment_status='UNPAID' where id=?",order);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> loyalty.apply(order,"REVERSAL"))).isInstanceOf(IdentityException.class);
        assertThat(db.count("select count(*) from loyalty_transactions where order_id=?",order)).isZero();
    }
}
