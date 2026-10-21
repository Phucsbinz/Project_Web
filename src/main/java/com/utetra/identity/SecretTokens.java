package com.utetra.identity;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SecretTokens {
    private final SecureRandom random=new SecureRandom();
    private final byte[] key;
    public SecretTokens(@Value("${app.otp.secret-key}") String key) {
        this.key=Base64.getDecoder().decode(key);
        if(this.key.length<32) throw new IllegalArgumentException("OTP key must contain at least 32 bytes");
    }
    public String random() { byte[] bytes=new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public String otp() { return String.format(Locale.ROOT,"%06d",random.nextInt(1000000)); }
    public String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
    public String otpHash(long id,String purpose,String email,String otp) {
        try {
            var mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((id+":"+purpose+":"+email+":"+otp).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch(GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
    public boolean matches(String left,String right) {
        return MessageDigest.isEqual(left.getBytes(java.nio.charset.StandardCharsets.UTF_8),right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
