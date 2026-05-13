package com.auraspark.note.core.service.auth.impl;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.core.service.auth.CodeSender;
import com.auraspark.note.core.service.auth.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private CodeSender codeSender;
    @Mock private ValueOperations<String, String> valueOps;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        verificationService = new VerificationService(redis, codeSender, 5, 15);
    }

    @Test
    void sendCode_shouldCreateCodeInRedis() {
        when(redis.opsForValue().get(anyString())).thenReturn(null);

        verificationService.sendCode("REGISTER", "test@auraspark.com");

        verify(valueOps, times(2)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(codeSender).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendCode_shouldThrowWhenRateLimited() {
        when(redis.opsForValue().get(anyString())).thenReturn("1");

        assertThrows(BusinessException.class,
                () -> verificationService.sendCode("REGISTER", "test@auraspark.com"));
    }

    @Test
    void verifyCode_shouldPassWithCorrectCode() {
        String code = "123456";
        String hash = sha256(code);
        when(valueOps.get(anyString())).thenReturn(hash + "|attempts:0");

        assertDoesNotThrow(() -> verificationService.verifyCode("REGISTER:test@auraspark.com", code));
    }

    @Test
    void verifyCode_shouldThrowWithWrongCode() {
        when(valueOps.get(anyString())).thenReturn(sha256("654321") + "|attempts:0");

        assertThrows(BusinessException.class,
                () -> verificationService.verifyCode("REGISTER:test@auraspark.com", "123456"));
    }

    @Test
    void verifyCode_shouldThrowWhenExpired() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> verificationService.verifyCode("REGISTER:test@auraspark.com", "123456"));
    }

    private String sha256(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

