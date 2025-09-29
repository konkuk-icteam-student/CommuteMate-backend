package com.better.CommuteMate.global.security.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    @Value("${jwt.algorithm:HmacSHA256}")
    private String hmacAlgorithm;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessValidityInMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshValidityInMs;

   private byte[] secretBytes;

    /** Precomputes the byte array for the secret key after dependency injection is complete. */
    @PostConstruct
    public void init() {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessValidityInMs);
    }

    public String createRefreshToken(String email) {
        return createToken(email, "REFRESH", refreshValidityInMs);
    }

    private String createToken(String email, String role, long validityInMs) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + validityInMs;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String headerEncoded = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", email);
        payload.put("role", role);
        payload.put("iat", nowMillis / 1000);
        payload.put("exp", expMillis / 1000);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to create JWT payload", e);
        }
        String payloadEncoded = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String headerAndPayload = headerEncoded + "." + payloadEncoded;
        String signature = sign(headerAndPayload);
        return headerAndPayload + "." + signature;
    }

    public void validateToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        String headerAndPayload = parts[0] + "." + parts[1];
        String expectedSignature = sign(headerAndPayload);
        if (!expectedSignature.equals(parts[2])) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }
        byte[] payloadBytes = base64UrlDecode(parts[1]);
        Map<?, ?> payloadMap;
        try {
            payloadMap = objectMapper.readValue(payloadBytes, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT payload", e);
        }
        Object expClaim = payloadMap.get("exp");
        if (expClaim == null) {
            throw new IllegalArgumentException("Missing exp claim");
        }
        long exp = Long.parseLong(expClaim.toString());
        long nowSeconds = Instant.now().getEpochSecond();
        if (exp < nowSeconds) {
            throw new IllegalArgumentException("JWT expired");
        }
    }

    public String getEmail(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        Map<?, ?> payloadMap;
        try {
            payloadMap = objectMapper.readValue(base64UrlDecode(parts[1]), Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT payload", e);
        }
        Object sub = payloadMap.get("sub");
        return sub != null ? sub.toString() : null;
    }

    public Long getExpiration(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        Map<?, ?> payloadMap;
        try {
            payloadMap = objectMapper.readValue(base64UrlDecode(parts[1]), Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT payload", e);
        }
        Object exp = payloadMap.get("exp");
        if (exp == null) {
            return null;
        }
        long expSeconds = Long.parseLong(exp.toString());
        return expSeconds * 1000;
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, hmacAlgorithm);
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign JWT", e);
        }
    }
}