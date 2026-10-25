package com.utetra.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final byte[] key;
    private final String issuer;
    private final String audience;
    private final Duration accessDuration;
    private final Clock clock;
    private final com.utetra.repository.AccessTokenRegistry registry;
    public JwtService(String encodedKey,String issuer,String audience,Duration duration,Clock clock) {
        this(encodedKey,issuer,audience,duration,clock,null);
    }
    @org.springframework.beans.factory.annotation.Autowired
    public JwtService(@Value("${security.jwt.secret-key}") String encodedKey,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.access-duration}") Duration duration, Clock clock,
            com.utetra.repository.AccessTokenRegistry registry) {
        this.key=Base64.getDecoder().decode(encodedKey);
        if (key.length<32) throw new IllegalArgumentException("JWT key must contain at least 32 random bytes");
        this.issuer=issuer; this.audience=audience; this.accessDuration=duration; this.clock=clock;
        this.registry=registry;
    }
    /** Called only by the authentication service after validating the user/session. */
    public String issue(Long userId, Long sessionId, String role) {
        if (userId==null || userId<=0 || sessionId==null || sessionId<=0 || !validRole(role))
            throw new IllegalArgumentException("Invalid token identity");
        try {
            Instant now=clock.instant();
            String tokenId=UUID.randomUUID().toString();
            var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject(userId.toString()).claim("sid",sessionId.toString()).claim("role",role)
                .issueTime(Date.from(now)).notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessDuration))).jwtID(tokenId).build();
            var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),claims);
            token.sign(new MACSigner(key));
            String serialized=token.serialize();
            if(registry!=null) registry.register(userId,sessionId,tokenId,serialized,now.plus(accessDuration),now);
            return serialized;
        } catch (JOSEException exception) { throw new IllegalStateException("JWT signing failed",exception); }
    }
    public UtetraPrincipal verify(String value) {
        try {
            var token=SignedJWT.parse(value);
            if (!JWSAlgorithm.HS256.equals(token.getHeader().getAlgorithm()) || !token.verify(new MACVerifier(key)))
                throw new IllegalArgumentException("Invalid JWT signature");
            var claims=token.getJWTClaimsSet();
            Instant now=clock.instant();
            if (!issuer.equals(claims.getIssuer()) || !claims.getAudience().contains(audience)
                || claims.getExpirationTime()==null || !claims.getExpirationTime().toInstant().isAfter(now)
                || claims.getIssueTime()==null || claims.getIssueTime().toInstant().isAfter(now.plusSeconds(30))
                || (claims.getNotBeforeTime()!=null && claims.getNotBeforeTime().toInstant().isAfter(now.plusSeconds(30))))
                throw new IllegalArgumentException("Invalid JWT claims");
            Long userId=Long.valueOf(claims.getSubject());
            Long sessionId=Long.valueOf(claims.getStringClaim("sid"));
            String role=claims.getStringClaim("role");
            if (userId<=0 || sessionId<=0 || !validRole(role)) throw new IllegalArgumentException("Invalid JWT identity");
            String tokenId=claims.getJWTID();
            UUID.fromString(tokenId);
            return new UtetraPrincipal(userId,sessionId,role,claims.getExpirationTime().toInstant(),tokenId);
        } catch (Exception exception) { throw new IllegalArgumentException("Invalid or expired JWT",exception); }
    }
    private static boolean validRole(String role) {
        return Set.of("ROLE_USER","ROLE_VENDOR","ROLE_MANAGER","ROLE_ADMIN","ROLE_SHIPPER").contains(role==null?"":role);
    }
}
