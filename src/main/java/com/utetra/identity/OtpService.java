package com.utetra.identity;
import static com.utetra.identity.IdentityRepository.*;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {
    private final IdentityRepository db; private final SecretTokens tokens; private final OtpDelivery delivery; private final Clock clock;
    private final Duration ttl,cooldown; private final int attempts;
    public OtpService(IdentityRepository db,SecretTokens tokens,OtpDelivery delivery,Clock clock,
        @Value("${app.otp.ttl}") Duration ttl,@Value("${app.otp.resend-cooldown}") Duration cooldown,@Value("${app.otp.max-attempts}") int attempts) {
        this.db=db; this.tokens=tokens; this.delivery=delivery; this.clock=clock; this.ttl=ttl; this.cooldown=cooldown; this.attempts=attempts;
    }
    @Transactional
    public void request(String address,String purpose) {
        String email=IdentityValidation.email(address); checkPurpose(purpose);
        // Serialize issuance for unknown addresses too; response/provider/limits must not reveal account existence.
        db.one("select id from roles where name='ROLE_USER' for update");
        var possible=db.optional("select id from users where email=?",email);
        if(possible.isPresent()) issueLocked(db.user(number(possible.get(),"id"),true),purpose);
        else {
            var anonymous=new java.util.HashMap<String,Object>(); anonymous.put("email",email);
            issueLocked(anonymous,purpose);
        }
    }
    void issueLocked(Map<String,Object> user,String purpose) {
        String email=string(user,"email"); Instant now=clock.instant();
        var previous=db.optional("select created_at from otp_challenges where email=? order by id desc limit 1",email);
        if(previous.isPresent() && instant(previous.get(),"created_at").plus(cooldown).isAfter(now))
            throw new IdentityException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ trước khi gửi lại OTP.");
        if(db.count("select count(*) from otp_challenges where email=? and created_at>?",email,time(now.minusSeconds(3600)))>=10)
            throw new IdentityException(HttpStatus.TOO_MANY_REQUESTS,"Đã đạt giới hạn gửi OTP.");
        db.update("update otp_challenges set revoked_at=?,reset_grant_hash=null,version=version+1 where email=? and purpose=? and revoked_at is null",time(now),email,purpose);
        Long userId=user.get("id")==null?null:number(user,"id");
        long id=db.insert("insert into otp_challenges(user_id,email,purpose,otp_hash,attempts,created_at,expires_at,version) values(?,?,?,'pending',0,?,?,0)",userId,email,purpose,time(now),time(now.plus(ttl)));
        String otp=tokens.otp();
        db.update("update otp_challenges set otp_hash=? where id=?",tokens.otpHash(id,purpose,email,otp),id);
        delivery.send(email,otp,purpose);
    }
    @Transactional(noRollbackFor=OtpRejectedException.class)
    public String verify(String address,String purpose,String otp) {
        checkPurpose(purpose); String email=IdentityValidation.email(address); Instant now=clock.instant();
        var found=db.optional("select id from users where email=?",email);
        if(found.isEmpty()) throw new OtpRejectedException();
        var user=db.user(number(found.get(),"id"),true);
        var foundChallenge=db.optional("select * from otp_challenges where email=? and purpose=? order by id desc limit 1 for update",email,purpose);
        if(foundChallenge.isEmpty()) throw new OtpRejectedException();
        var c=foundChallenge.get(); long id=number(c,"id");
        if(!flag(user,"is_active") || purpose.equals("PASSWORD_RESET") && user.get("email_verified_at")==null || c.get("user_id")==null || number(c,"user_id")!=number(user,"id") || c.get("revoked_at")!=null || number(c,"attempts")>=attempts)
            throw new OtpRejectedException();
        if(otp==null || !otp.matches("[0-9]{6}") || !tokens.matches(string(c,"otp_hash"),tokens.otpHash(id,purpose,email,otp))) {
            if(c.get("consumed_at")==null && instant(c,"expires_at").isAfter(now))
                db.update("update otp_challenges set attempts=attempts+1,version=version+1 where id=?",id);
            throw new OtpRejectedException();
        }
        // Explain expiry/reuse only after the caller proves knowledge of this code.
        if(c.get("consumed_at")!=null) throw new OtpRejectedException("Mã OTP này đã được sử dụng. Vui lòng đăng nhập hoặc yêu cầu mã mới.");
        if(!instant(c,"expires_at").isAfter(now)) throw new OtpRejectedException("Mã OTP đã hết hạn. Vui lòng chọn Gửi lại OTP để nhận mã mới.");
        if(purpose.equals("REGISTER")) {
            db.update("update users set email_verified_at=?,updated_at=?,version=version+1 where id=? and email_verified_at is null",time(now),time(now),number(user,"id"));
            db.update("update otp_challenges set consumed_at=?,version=version+1 where id=?",time(now),id); return "";
        }
        String grant=id+"."+tokens.random();
        // consumed_at prevents OTP replay; the hashed grant may be consumed once until expires_at.
        db.update("update otp_challenges set consumed_at=?,reset_grant_hash=?,expires_at=?,version=version+1 where id=?",time(now),tokens.hash(grant),time(now.plusSeconds(300)),id);
        return grant;
    }
    private void checkPurpose(String purpose) { if(!"REGISTER".equals(purpose) && !"PASSWORD_RESET".equals(purpose)) throw IdentityException.invalid("Mục đích OTP không hợp lệ."); }
}
