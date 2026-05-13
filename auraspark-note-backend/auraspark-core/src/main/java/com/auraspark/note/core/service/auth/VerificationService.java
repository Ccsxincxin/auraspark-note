package com.auraspark.note.core.service.auth;

import com.auraspark.note.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class VerificationService {

    private static final String VC_PREFIX = "vc:";
    private static final String LOCK_PREFIX = "lock:";
    private static final String RATE_VC_PREFIX = "rl:vc:";
    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";

    private final StringRedisTemplate redis;
    private final CodeSender codeSender;
    private final SecureRandom secureRandom;
    private final int loginAttemptLimit;
    private final int lockMinutes;

    public VerificationService(StringRedisTemplate redis, CodeSender codeSender,
                               @Value("${rate-limit.login-attempt}") int loginAttemptLimit,
                               @Value("${rate-limit.lock-minutes}") int lockMinutes) {
        this.redis = redis;
        this.codeSender = codeSender;
        this.secureRandom = new SecureRandom();
        this.loginAttemptLimit = loginAttemptLimit;
        this.lockMinutes = lockMinutes;
    }

    public void sendCode(String type, String target) {
        String cacheKey = VC_PREFIX + type + ":" + target;

        redis.delete(cacheKey);
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        redis.opsForValue().set(cacheKey, sha256(code) + "|attempts:0", 5, TimeUnit.MINUTES);
        sendCodeAsync(target, code, type);
    }

    @Async
    protected void sendCodeAsync(String target, String code, String type) {
        codeSender.send(target, code, type);
    }

    public void verifyCode(String cacheKeySuffix, String code) {
        if (code == null || !Pattern.matches("^\\d{6}$", code)) {
            throw new BusinessException(400, "验证码格式不正确", "auth.code.invalidFormat");
        }
        String cacheKey = VC_PREFIX + cacheKeySuffix;
        String stored = redis.opsForValue().get(cacheKey);
        if (stored == null) {
            throw new BusinessException(400, "Invalid or expired verification code", "auth.code.invalidOrExpired");
        }

        String[] parts = stored.split("\\|");
        String storedHash = parts[0];
        String attemptInfo = parts.length > 1 ? parts[1] : "attempts:0";
        int attempts = Integer.parseInt(attemptInfo.split(":")[1]);

        if (!storedHash.equals(sha256(code))) {
            attempts++;
            if (attempts >= 5) {
                redis.delete(cacheKey);
                redis.opsForValue().set(LOCK_PREFIX + cacheKeySuffix, "1",
                        lockMinutes, TimeUnit.MINUTES);
                throw new BusinessException(429, "Too many incorrect attempts, please try again later", "auth.code.tooManyAttempts");
            }
            redis.opsForValue().set(cacheKey, storedHash + "|attempts:" + attempts,
                    5, TimeUnit.MINUTES);
            throw new BusinessException(400, "Incorrect verification code", "auth.code.incorrect");
        }
        redis.delete(cacheKey);
    }

    public void incrementLoginAttempt(String account, String lockKey) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + account;
        Long attempts = redis.opsForValue().increment(attemptKey);
        if (attempts != null && attempts == 1) {
            redis.expire(attemptKey, 1, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts >= loginAttemptLimit) {
            redis.opsForValue().set(lockKey, "1", lockMinutes, TimeUnit.MINUTES);
            redis.delete(attemptKey);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
