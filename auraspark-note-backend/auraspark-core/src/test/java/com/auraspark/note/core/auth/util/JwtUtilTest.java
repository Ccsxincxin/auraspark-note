package com.auraspark.note.core.test;

import com.auraspark.note.core.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private JwtUtil jwtUtil;
    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hs384";

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        jwtUtil = new JwtUtil(SECRET, 3600000L, 86400000L, 86400000L, 43200000L, redis);
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = jwtUtil.generateAccessToken(42L);
        Long userId = jwtUtil.validateAccessToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void validateAccessToken_shouldReturnNullForInvalidToken() {
        assertNull(jwtUtil.validateAccessToken("invalid-token"));
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(42L, false);
        when(valueOps.get(anyString())).thenReturn("42");
        Long userId = jwtUtil.validateRefreshToken(token);
        assertEquals(42L, userId);
    }

    @Test
    void blacklistAccessToken_shouldMakeTokenInvalid() {
        String token = jwtUtil.generateAccessToken(42L);
        var claims = jwtUtil.parseToken(token);
        assertNotNull(claims);

        when(redis.hasKey("bl:" + claims.getId())).thenReturn(true);
        assertNull(jwtUtil.validateAccessToken(token));
    }

    @Test
    void parseToken_shouldReturnClaims() {
        String token = jwtUtil.generateAccessToken(42L);
        var claims = jwtUtil.parseToken(token);
        assertNotNull(claims);
        assertEquals("42", claims.getSubject());
    }
}
