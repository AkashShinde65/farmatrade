package com.farmatrade.auth;

import com.farmatrade.auth.entity.User;
import com.farmatrade.auth.security.JwtUtil;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.lang.reflect.Field;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

final class TestJwtFactory {
    private TestJwtFactory() {
    }

    static String createTokenLike(JwtUtil jwtUtil, User user, String issuer, String audience, long expiredMinutes) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(user.getId()))
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(now.minusSeconds(120)))
                    .expirationTime(Date.from(now.minusSeconds(expiredMinutes * 60)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("role", user.getRole().name())
                    .claim("email", user.getEmail())
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID((String) field(jwtUtil, "kid"))
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claims
            );
            jwt.sign(new RSASSASigner((RSAPrivateKey) field(jwtUtil, "privateKey")));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
