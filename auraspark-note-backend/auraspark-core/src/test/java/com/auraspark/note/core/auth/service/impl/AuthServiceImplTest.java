package com.auraspark.note.core.service.auth.impl;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.core.dto.auth.LoginPasswordRequest;
import com.auraspark.note.core.config.PasswordProperties;
import com.auraspark.note.core.service.auth.SecurityAuditService;
import com.auraspark.note.core.service.auth.VerificationService;
import com.auraspark.note.core.util.JwtUtil;
import com.auraspark.note.core.entity.User;
import com.auraspark.note.core.entity.UserProfile;
import com.auraspark.note.core.mapper.UserMapper;
import com.auraspark.note.core.mapper.UserProfileMapper;
import com.auraspark.note.core.mapper.UserTokenBalanceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private StringRedisTemplate redis;
    @Mock private UserMapper userMapper;
    @Mock private UserProfileMapper profileMapper;
    @Mock private UserTokenBalanceMapper tokenBalanceMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private VerificationService verificationService;
    @Mock private SecurityAuditService auditService;
    @Mock private PasswordProperties passwordProperties;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private ListOperations<String, String> listOps;

    private AuthServiceImpl authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(redis.opsForList()).thenReturn(listOps);
        lenient().when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        lenient().when(listOps.size(anyString())).thenReturn(1L);

        authService = new AuthServiceImpl(redis, userMapper, profileMapper,
                tokenBalanceMapper, jwtUtil, verificationService, auditService, passwordProperties);
    }

    @Test
    void loginByPassword_shouldSucceedWithValidCredentials() {
        LoginPasswordRequest request = new LoginPasswordRequest();
        request.setAccount("test@auraspark.com");
        request.setPassword("TestPass123");
        request.setRememberMe(false);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@auraspark.com");
        user.setPassword(encoder.encode("TestPass123"));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        UserProfile profile = new UserProfile();
        profile.setNickname("鐢ㄦ埛");

        when(redis.hasKey(anyString())).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(profileMapper.selectOne(any())).thenReturn(profile);
        when(jwtUtil.generateAccessToken(1L)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L, false)).thenReturn("refresh-token");
        when(jwtUtil.parseToken("access-token")).thenReturn(
                io.jsonwebtoken.Jwts.claims().id("jti-1").add("sub", "1").build());

        var resp = authService.loginByPassword(request);

        assertEquals("access-token", resp.getAccessToken());
        assertEquals("refresh-token", resp.getRefreshToken());
        assertEquals("鐢ㄦ埛", resp.getNickname());
    }

    @Test
    void loginByPassword_shouldThrowWhenAccountNotFound() {
        LoginPasswordRequest request = new LoginPasswordRequest();
        request.setAccount("wrong@auraspark.com");
        request.setPassword("TestPass123");

        when(redis.hasKey(anyString())).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.loginByPassword(request));
    }

    @Test
    void loginByPassword_shouldThrowWhenPasswordWrong() {
        LoginPasswordRequest request = new LoginPasswordRequest();
        request.setAccount("test@auraspark.com");
        request.setPassword("WrongPass1");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@auraspark.com");
        user.setPassword(encoder.encode("TestPass123"));

        when(redis.hasKey(anyString())).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThrows(BusinessException.class, () -> authService.loginByPassword(request));
        verify(verificationService).incrementLoginAttempt(anyString(), anyString());
    }
}

