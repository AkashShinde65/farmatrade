package com.farmatrade.auth.security;

import com.farmatrade.auth.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final String issuer;
    private final String audience;
    private final String kid;
    private final Duration expiry;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final Clock clock;

    public JwtUtil(
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.audience}") String audience,
            @Value("${auth.jwt.kid}") String kid,
            @Value("${auth.jwt.access-token-minutes}") long accessTokenMinutes,
            KeyPair keyPair,
            Clock clock
    ) {
        this.issuer = issuer;
        this.audience = audience;
        this.kid = kid;
        this.expiry = Duration.ofMinutes(accessTokenMinutes);
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        this.clock = clock;
    }

    public String generateToken(User user) {
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(expiry)))
                .jwtID(UUID.randomUUID().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(JOSEObjectType.JWT).build(),
                claims
        );
        try {
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public ValidatedJwt validate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm()) || !kid.equals(jwt.getHeader().getKeyID())) {
                throw new IllegalArgumentException("Invalid JWT header");
            }
            if (!jwt.verify(new RSASSAVerifier(publicKey))) {
                throw new IllegalArgumentException("Invalid JWT signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!issuer.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("Invalid JWT issuer");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
                throw new IllegalArgumentException("Invalid JWT audience");
            }
            if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(clock.instant())) {
                throw new IllegalArgumentException("JWT expired");
            }
            String role = claims.getStringClaim("role");
            if (claims.getSubject() == null || role == null || claims.getJWTID() == null || claims.getIssueTime() == null) {
                throw new IllegalArgumentException("JWT missing required claims");
            }
            return new ValidatedJwt(Long.parseLong(claims.getSubject()), role, claims.getStringClaim("email"));
        } catch (ParseException | JOSEException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid JWT", e);
        }
    }

    public Map<String, Object> jwks() {
        RSAKey key = new RSAKey.Builder(publicKey)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();
        return new JWKSet(List.of(key)).toJSONObject();
    }

    public long expiresInSeconds() {
        return expiry.toSeconds();
    }

    public record ValidatedJwt(Long userId, String role, String email) {
    }
}
