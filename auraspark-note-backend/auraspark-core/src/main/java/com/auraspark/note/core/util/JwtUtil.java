package com.auraspark.note.core.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    private static final String BLACKLIST_PREFIX = "bl:";
    private static final String REFRESH_PREFIX = "rt:";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final long refreshExpirationRememberMe;
    private final long refreshExpirationDefault;
    private final StringRedisTemplate redis;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshExpiration,
            @Value("${jwt.refresh-token-expiration-remember-me}") long refreshExpirationRememberMe,
            @Value("${jwt.refresh-token-expiration-default}") long refreshExpirationDefault,
            StringRedisTemplate redis) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.refreshExpirationRememberMe = refreshExpirationRememberMe;
        this.refreshExpirationDefault = refreshExpirationDefault;
        this.redis = redis;
    }

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, boolean rememberMe) {
        long ttl = rememberMe ? refreshExpirationRememberMe : refreshExpirationDefault;
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(secretKey)
                .compact();
        redis.opsForValue().set(REFRESH_PREFIX + jti, String.valueOf(userId), ttl, TimeUnit.MILLISECONDS);
        return token;
    }

    public Long validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
            if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + claims.getId()))) {
                return null;
            }
            return Long.parseLong(claims.getSubject());
        } catch (io.jsonwebtoken.JwtException e) {
            return null;
        }
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public void blacklistJti(String jti) {
        redis.opsForValue().set(BLACKLIST_PREFIX + jti, "1", accessExpiration, TimeUnit.MILLISECONDS);
    }

    public void blacklistAccessToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                redis.opsForValue().set(BLACKLIST_PREFIX + claims.getId(), "1",
                        ttl, TimeUnit.MILLISECONDS);
            }
        }
    }

    public Long validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
            String stored = redis.opsForValue().get(REFRESH_PREFIX + claims.getId());
            if (stored == null) return null;
            return Long.parseLong(stored);
        } catch (Exception e) {
            return null;
        }
    }

    public void removeRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            redis.delete(REFRESH_PREFIX + claims.getId());
        }
    }
}
