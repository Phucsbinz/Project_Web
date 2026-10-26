package com.utetra.identity;

import static com.utetra.identity.IdentityRepository.*;
import com.utetra.security.UtetraPrincipal;
import com.utetra.service.SessionIdentityService;
import com.utetra.service.MediaService;
import java.time.Clock;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AccountService {
    private final IdentityRepository db;
    private final SessionIdentityService identity;
    private final PasswordEncoder encoder;
    private final AuthService auth;
    private final Clock clock;
    private final MediaService media;
    private final IdentityInputs inputs;
    private final MediaValidation mediaValidation;
    public AccountService(IdentityRepository db,SessionIdentityService identity,PasswordEncoder encoder,AuthService auth,Clock clock,MediaService media,IdentityInputs inputs,MediaValidation mediaValidation) {
        this.db=db; this.identity=identity; this.encoder=encoder; this.auth=auth; this.clock=clock; this.media=media;
        this.inputs=inputs; this.mediaValidation=mediaValidation;
    }
    public void require(UtetraPrincipal p,boolean buyer) {
        if(p==null || !identity.isCurrent(p)) throw IdentityException.denied();
        if(db.count("select count(*) from auth_sessions s join users u on u.id=s.user_id join roles r on r.id=u.role_id where s.id=? and u.id=? and r.name=? and u.is_active=true and u.email_verified_at is not null and s.revoked_at is null and s.expires_at>?",p.sessionId(),p.userId(),p.role(),time(clock.instant()))!=1) throw IdentityException.denied();
        if(buyer && !Set.of("ROLE_USER","ROLE_VENDOR").contains(p.role())) throw IdentityException.denied();
    }
    @Transactional(readOnly=true)
    public Map<String,Object> profile(UtetraPrincipal p) {
        require(p,false);
        return db.one("select id,username,email,full_name,phone,avatar_url,email_verified_at,created_at,version from users where id=?",p.userId());
    }
    @Transactional
    public void profile(UtetraPrincipal p,IdentityForms.Profile f) {
        inputs.require(f);
        require(p,false); db.user(p.userId(),true);
        if(db.update("update users set full_name=?,phone=?,updated_at=?,version=version+1 where id=? and version=?",
            IdentityValidation.text(f.fullName(),1,100),IdentityValidation.phone(f.phone(),false),time(clock.instant()),p.userId(),f.version())!=1) throw IdentityException.conflict();
    }
    @Transactional
    public void profileWithAvatar(UtetraPrincipal p,IdentityForms.Profile form,MultipartFile file) {
        profile(p,form);
        if(file!=null && !file.isEmpty()) avatar(p,file);
    }
    @Transactional
    public void password(UtetraPrincipal p,IdentityForms.Password f) {
        inputs.require(f); IdentityValidation.passwordInput(f.currentPassword());
        require(p,false); IdentityValidation.password(f.password()); var u=db.user(p.userId(),true);
        if(!encoder.matches(f.currentPassword(),string(u,"password_hash"))) throw IdentityException.invalid("Mật khẩu hiện tại không đúng.");
        db.update("update users set password_hash=?,password_plaintext=null,updated_at=?,version=version+1 where id=?",encoder.encode(f.password()),time(clock.instant()),p.userId());
        auth.revokeAll(p.userId());
    }
    @Transactional
    public void avatar(UtetraPrincipal p,MultipartFile file) {
        require(p,false); mediaValidation.image(file); var u=db.user(p.userId(),true); var result=media.uploadImage(file);
        // Register cleanup before persistence so every rollback path compensates the upload.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                String id=status==STATUS_COMMITTED?string(u,"avatar_public_id"):result.publicId();
                if(!id.isBlank()) cleanup(id);
            }
        });
        db.update("update users set avatar_url=?,avatar_public_id=?,updated_at=?,version=version+1 where id=?",result.secureUrl(),result.publicId(),time(clock.instant()),p.userId());
    }
    private void cleanup(String publicId) {
        try { media.deleteOwnedAsset(publicId); }
        catch(RuntimeException ex) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Avatar cleanup requires retry (provider unavailable)"); }
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> addresses(UtetraPrincipal p) {
        require(p,true); return db.list("select id,recipient_name,phone,address_detail,is_default from user_addresses where user_id=? order by is_default desc,id desc",p.userId());
    }
    /** Used by order owner to build an immutable order snapshot, never trusts userId from client. */
    @Transactional(readOnly=true)
    public Map<String,Object> addressSnapshot(UtetraPrincipal p,long id) {
        IdentityValidation.id(id);
        require(p,true); return db.one("select recipient_name,phone,address_detail from user_addresses where id=? and user_id=?",id,p.userId());
    }
    @Transactional
    public long saveAddress(UtetraPrincipal p,Long id,IdentityForms.Address f) {
        inputs.require(f); if(id!=null) IdentityValidation.id(id);
        String phone=IdentityValidation.phone(f.phone(),true);
        require(p,true); db.user(p.userId(),true);
        if(id!=null) db.one("select id from user_addresses where id=? and user_id=?",id,p.userId());
        boolean makeDefault=f.isDefault() || db.count("select count(*) from user_addresses where user_id=?",p.userId())==0;
        if(makeDefault) db.update("update user_addresses set is_default=false,updated_at=? where user_id=?",time(clock.instant()),p.userId());
        var now=time(clock.instant());
        if(id==null) return db.insert("insert into user_addresses(user_id,recipient_name,phone,address_detail,is_default,created_at,updated_at) values(?,?,?,?,?,?,?)",p.userId(),IdentityValidation.text(f.recipientName(),1,100),phone,IdentityValidation.text(f.addressDetail(),1,500),makeDefault,now,now);
        db.update("update user_addresses set recipient_name=?,phone=?,address_detail=?,is_default=?,updated_at=? where id=? and user_id=?",IdentityValidation.text(f.recipientName(),1,100),phone,IdentityValidation.text(f.addressDetail(),1,500),makeDefault,now,id,p.userId());
        return id;
    }
    @Transactional
    public void deleteAddress(UtetraPrincipal p,long id) {
        IdentityValidation.id(id);
        require(p,true); db.user(p.userId(),true);
        var address=db.one("select is_default from user_addresses where id=? and user_id=?",id,p.userId());
        db.update("delete from user_addresses where id=? and user_id=?",id,p.userId());
        if(flag(address,"is_default")) db.optional("select id from user_addresses where user_id=? order by id limit 1",p.userId()).ifPresent(a -> db.update("update user_addresses set is_default=true,updated_at=? where id=?",time(clock.instant()),number(a,"id")));
    }
}
