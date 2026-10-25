package com.utetra.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Store the JWT fingerprint, never a reusable bearer token, in the login session. */
@Repository
public class AccessTokenRegistry {
    private final JdbcTemplate jdbc;
    public AccessTokenRegistry(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public static String fingerprint(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Transactional
    public void register(long user,long session,String tokenId,String token,Instant expires,Instant now) {
        int changed=jdbc.update("update auth_sessions set access_token_id=?,access_token_hash=?,access_expires_at=?,version=version+1 where id=? and user_id=? and revoked_at is null and expires_at>?",
            tokenId,fingerprint(token),Timestamp.from(expires),session,user,Timestamp.from(now));
        if(changed!=1) throw new IllegalArgumentException("Session unavailable for JWT issuance");
    }
}
