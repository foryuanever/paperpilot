package com.paperpilot.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** Issues short, signed sessions. User ids are never accepted as credentials. */
@Service
public class SessionTokenService {
    private static final long TTL_SECONDS = 60L * 60L * 24L * 30L;
    private final byte[] secret;

    public SessionTokenService(@Value("${PAPERPILOT_SESSION_SECRET:}") String configuredSecret) {
        if (configuredSecret == null || configuredSecret.trim().length() < 32) {
            throw new IllegalStateException("PAPERPILOT_SESSION_SECRET must be at least 32 characters in production");
        }
        this.secret = configuredSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long userId) {
        return issue(userId, false);
    }

    public String issue(Long userId, boolean impersonated) {
        long expiresAt = Instant.now().getEpochSecond() + TTL_SECONDS;
        String payload = userId + "." + expiresAt + "." + UUID.randomUUID();
        if (impersonated) {
            payload += ".impersonated";
        }
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encoded + "." + sign(encoded);
    }

    public boolean isImpersonated(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) return false;
        try {
            String[] payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split("\\.");
            return payload.length == 4 && "impersonated".equals(payload[3]) && Long.parseLong(payload[1]) >= Instant.now().getEpochSecond();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public Optional<Long> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) return Optional.empty();
        try {
            String[] payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8).split("\\.");
            if ((payload.length != 3 && payload.length != 4) || Long.parseLong(payload[1]) < Instant.now().getEpochSecond()) return Optional.empty();
            return Optional.of(Long.parseLong(payload[0]));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign session", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
