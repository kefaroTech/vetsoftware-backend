package com.vetsoftware.app.auth.infrastructure.security;

import com.vetsoftware.app.auth.application.port.out.TokenGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider implements TokenGenerator {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    public JwtProvider(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generate(Long id, String type, Long companyId, Long authVersion) {
        Instant now = Instant.now();
        var builder = Jwts.builder().subject(String.valueOf(id)).claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)));
        if (companyId != null)
            builder.claim("companyId", companyId);
        if (authVersion != null)
            builder.claim("authVersion", authVersion);
        return builder.signWith(secretKey).compact();
    }

    public Long extractId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String extractType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public Long extractAuthVersion(String token) {
        Object value = parseClaims(token).get("authVersion");
        if (value instanceof Number number)
            return number.longValue();
        if (value instanceof String text && !text.isBlank())
            return Long.parseLong(text);
        return null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
