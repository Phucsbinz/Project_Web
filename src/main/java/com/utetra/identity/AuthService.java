package com.utetra.identity;
import static com.utetra.identity.IdentityRepository.*;
import com.utetra.security.JwtService;
import com.utetra.security.UtetraPrincipal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public record Tokens(String access,String refresh) {}
    private final IdentityRepository db; private final PasswordEncoder encoder; private final SecretTokens secrets;
    private final JwtService jwt; private final OtpService otp; private final Clock clock; private final Duration refreshDuration;
    private final String dummy;
    private final org.springframework.security.authentication.AuthenticationManager authentication;
    private final IdentityInputs inputs;
    public AuthService(IdentityRepository db,PasswordEncoder encoder,SecretTokens secrets,JwtService jwt,OtpService otp,Clock clock,
        @Value("${security.jwt.refresh-duration}") Duration refreshDuration,org.springframework.security.authentication.AuthenticationManager authentication,IdentityInputs inputs) {
        this.db=db; this.encoder=encoder; this.secrets=secrets; this.jwt=jwt; this.otp=otp; this.clock=clock; this.refreshDuration=refreshDuration;
        this.dummy=encoder.encode(secrets.random());
        this.authentication=authentication;
        this.inputs=inputs;
    }
    @Transactional
    public void register(IdentityForms.Register f) {
        inputs.require(f);
        IdentityValidation.password(f.password()); String email=IdentityValidation.email(f.email()),username=f.username().strip().toLowerCase(Locale.ROOT);
        if(db.count("select count(*) from users where email=? or username=?",email,username)>0) throw IdentityException.invalid("Email hoặc tên đăng nhập đã được sử dụng.");
        long role=number(db.one("select id from roles where name='ROLE_USER'"),"id"); Instant now=clock.instant();
        long id=db.insert("insert into users(role_id,username,email,password_hash,full_name,is_active,created_at,updated_at,version) values(?,?,?,?,?,true,?,?,0)",role,username,email,encoder.encode(f.password()),IdentityValidation.text(f.fullName(),1,100),time(now),time(now));
        otp.issueLocked(db.user(id,true),"REGISTER");
    }
    @Transactional
    public Tokens login(String email,String password) {
        try { IdentityValidation.passwordInput(password); } catch(IdentityException ex) { throw credentials(); }
        var optional=db.optional("select id from users where email=?",IdentityValidation.email(email));
        if(optional.isEmpty()) { encoder.matches(password,dummy); throw credentials(); }
        var u=db.user(number(optional.get(),"id"),true);
        try { authentication.authenticate(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(IdentityValidation.email(email),password)); }
        catch(org.springframework.security.core.AuthenticationException ex) { throw credentials(); }
        if(!flag(u,"is_active") || u.get("email_verified_at")==null) throw credentials();
        Instant now=clock.instant(); String raw=secrets.random();
        long id=db.insert("insert into auth_sessions(user_id,refresh_token_hash,created_at,expires_at,version) values(?,?,?,?,0)",number(u,"id"),secrets.hash(raw),time(now),time(now.plus(refreshDuration)));
        return new Tokens(jwt.issue(number(u,"id"),id,string(u,"role_name")),id+"."+raw);
    }
    @Transactional
    public Tokens refresh(String value) {
        long id=tokenId(value); var hint=db.optional("select user_id from auth_sessions where id=?",id).orElseThrow(this::credentials);
        var u=db.user(number(hint,"user_id"),true);
        var s=db.one("select * from auth_sessions where id=? for update",id); Instant now=clock.instant();
        String raw=value.substring(value.indexOf('.')+1);
        if(s.get("revoked_at")!=null || !instant(s,"expires_at").isAfter(now) || !flag(u,"is_active") || u.get("email_verified_at")==null || !secrets.matches(string(s,"refresh_token_hash"),secrets.hash(raw))) throw credentials();
        String next=secrets.random();
        db.update("update auth_sessions set refresh_token_hash=?,last_refreshed_at=?,version=version+1 where id=?",secrets.hash(next),time(now),id);
        return new Tokens(jwt.issue(number(u,"id"),id,string(u,"role_name")),id+"."+next);
    }
    @Transactional
    public void logout(UtetraPrincipal principal,String refresh) {
        if(principal!=null) db.update("update auth_sessions set revoked_at=?,version=version+1 where id=? and user_id=? and revoked_at is null",time(clock.instant()),principal.sessionId(),principal.userId());
        if(refresh!=null) {
            try { long id=tokenId(refresh); db.update("update auth_sessions set revoked_at=?,version=version+1 where id=? and refresh_token_hash=? and revoked_at is null",time(clock.instant()),id,secrets.hash(refresh.substring(refresh.indexOf('.')+1))); }
            catch(IdentityException ignored) { /* malformed cookies are still cleared by controller */ }
        }
    }
    @Transactional
    public void reset(String grant,String password) {
        IdentityValidation.password(password); long id=tokenId(grant);
        var hint=db.optional("select user_id from otp_challenges where id=? and user_id is not null and purpose='PASSWORD_RESET'",id).orElseThrow(this::credentials);
        var u=db.user(number(hint,"user_id"),true);
        var c=db.one("select * from otp_challenges where id=? for update",id);
        if(!flag(u,"is_active") || u.get("email_verified_at")==null || !"PASSWORD_RESET".equals(string(c,"purpose")) || c.get("consumed_at")==null || c.get("revoked_at")!=null
            || !instant(c,"expires_at").isAfter(clock.instant()) || !secrets.matches(string(c,"reset_grant_hash"),secrets.hash(grant))) throw credentials();
        db.update("update users set password_hash=?,password_plaintext=null,updated_at=?,version=version+1 where id=?",encoder.encode(password),time(clock.instant()),number(u,"id"));
        db.update("update otp_challenges set reset_grant_hash=null,revoked_at=?,version=version+1 where user_id=? and revoked_at is null",time(clock.instant()),number(u,"id"));
        revokeAll(number(u,"id"));
    }
    public void revokeAll(long userId) { db.update("update auth_sessions set revoked_at=?,version=version+1 where user_id=? and revoked_at is null",time(clock.instant()),userId); }
    private long tokenId(String value) {
        try { if(value==null || value.length()>200 || !value.matches("[1-9][0-9]*\\.[A-Za-z0-9_-]{43}")) throw credentials(); return Long.parseLong(value.substring(0,value.indexOf('.'))); }
        catch(NumberFormatException ex) { throw credentials(); }
    }
    private IdentityException credentials() { return new IdentityException(HttpStatus.UNAUTHORIZED,"Thông tin xác thực không hợp lệ."); }
}
