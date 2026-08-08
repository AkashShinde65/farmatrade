package com.farmatrade.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RsaKeyConfig {
    @Bean
    public KeyPair jwtKeyPair(
            @Value("${auth.jwt.private-key-pem:}") String privateKeyPem,
            @Value("${auth.jwt.private-key-file:}") String privateKeyFile,
            @Value("${auth.jwt.public-key-pem:}") String publicKeyPem,
            @Value("${auth.jwt.public-key-file:}") String publicKeyFile,
            @Value("${auth.jwt.allow-ephemeral-key:false}") boolean allowEphemeralKey
    ) throws GeneralSecurityException, IOException {
        String privatePem = resolvePem(privateKeyPem, privateKeyFile);
        if (StringUtils.hasText(privatePem)) {
            RSAPrivateKey privateKey = parsePrivateKey(privatePem);
            String publicPem = resolvePem(publicKeyPem, publicKeyFile);
            RSAPublicKey publicKey = StringUtils.hasText(publicPem) ? parsePublicKey(publicPem) : derivePublicKey(privateKey);
            return new KeyPair(publicKey, privateKey);
        }
        if (allowEphemeralKey) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
        throw new IllegalStateException("Configure AUTH_RSA_PRIVATE_KEY_PEM or AUTH_RSA_PRIVATE_KEY_FILE for RS256 signing");
    }

    private String resolvePem(String privateKeyPem, String privateKeyFile) throws IOException {
        if (StringUtils.hasText(privateKeyPem)) {
            return privateKeyPem;
        }
        if (StringUtils.hasText(privateKeyFile)) {
            return Files.readString(Path.of(privateKeyFile), StandardCharsets.UTF_8);
        }
        return "";
    }

    private RSAPrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private RSAPublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws GeneralSecurityException {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalArgumentException("RSA private key must include public exponent data");
        }
        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
    }
}
