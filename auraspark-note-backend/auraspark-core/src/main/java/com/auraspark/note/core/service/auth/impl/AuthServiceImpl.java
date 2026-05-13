package com.auraspark.note.core.service.auth.impl;

import com.auraspark.note.core.dto.auth.*;
import com.auraspark.note.core.service.auth.AuthService;
import com.auraspark.note.core.service.auth.SecurityAuditService;
import com.auraspark.note.core.service.auth.VerificationService;
import com.auraspark.note.core.util.JwtUtil;
import com.auraspark.note.core.config.PasswordProperties;
import com.auraspark.note.core.entity.User;
import com.auraspark.note.core.entity.UserProfile;
import com.auraspark.note.core.entity.UserTokenBalance;
import com.auraspark.note.core.mapper.UserMapper;
import com.auraspark.note.core.mapper.UserProfileMapper;
import com.auraspark.note.core.mapper.UserTokenBalanceMapper;
import com.auraspark.note.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String SESSION_PREFIX = "sess:";
    private static final String LOCK_PREFIX = "lock:";
    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final int MAX_SESSIONS = 3;

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserTokenBalanceMapper tokenBalanceMapper;
    private final JwtUtil jwtUtil;
    private final VerificationService verificationService;
    private final SecurityAuditService auditService;
    private final PasswordProperties passwordProperties;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(StringRedisTemplate redis, UserMapper userMapper,
                           UserProfileMapper profileMapper,
                           UserTokenBalanceMapper tokenBalanceMapper,
                           JwtUtil jwtUtil,
                           VerificationService verificationService,
                           SecurityAuditService auditService,
                           PasswordProperties passwordProperties) {
        this.redis = redis;
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.tokenBalanceMapper = tokenBalanceMapper;
        this.jwtUtil = jwtUtil;
        this.verificationService = verificationService;
        this.auditService = auditService;
        this.passwordProperties = passwordProperties;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        validateTarget(request.getTarget(), request.getValue());
        verificationService.sendCode(request.getType(), request.getValue());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateTarget(request.getTarget(), request.getValue());
        verificationService.verifyCode("REGISTER:" + request.getValue(), request.getCode());

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if ("email".equals(request.getTarget())) {
            query.eq(User::getEmail, request.getValue());
        } else {
            query.eq(User::getPhone, request.getValue());
        }
        if (userMapper.selectCount(query) > 0) {
            throw new BusinessException(409, "Account already registered", "auth.register.duplicate");
        }

        User user = new User();
        if ("email".equals(request.getTarget())) {
            user.setEmail(request.getValue());
        } else {
            user.setPhone(request.getValue());
        }
        validatePassword(request.getPassword());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(request.getNickname() != null && !request.getNickname().isBlank() ? request.getNickname() : "User");
        profile.setAvatar(request.getAvatar());
        profileMapper.insert(profile);

        UserTokenBalance balance = new UserTokenBalance();
        balance.setUserId(user.getId());
        balance.setBalance(100);
        balance.setTotalGranted(100L);
        balance.setTotalUsed(0L);
        tokenBalanceMapper.insert(balance);

        auditService.logRegister(user.getId(), getClientAccount(user), getClientIp());
        return buildAuthResponse(user, profile, false);
    }

    @Override
    public AuthResponse loginByPassword(LoginPasswordRequest request) {
        String lockKey = LOCK_PREFIX + "PASSWORD:" + request.getAccount();
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new BusinessException(429, "Account locked, please try again later", "auth.login.locked");
        }

        User user = findUserByAccount(request.getAccount());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            verificationService.incrementLoginAttempt(request.getAccount(), lockKey);
            throw new BusinessException(401, "Invalid account or password", "auth.login.invalid");
        }

        redis.delete(LOGIN_ATTEMPT_PREFIX + request.getAccount());
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
        auditService.logLogin(user.getId(), getClientAccount(user), getClientIp(), true);
        return buildAuthResponse(user, profile, request.isRememberMe());
    }

    @Override
    public AuthResponse loginByCode(LoginCodeRequest request) {
        validateTarget(request.getTarget(), request.getValue());
        verificationService.verifyCode("LOGIN:" + request.getValue(), request.getCode());

        User user = findUserByAccount(request.getValue());
        if (user == null) {
            throw new BusinessException(404, "Account not found", "auth.account.notFound");
        }

        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, user.getId()));
        auditService.logLogin(user.getId(), getClientAccount(user), getClientIp(), true);
        return buildAuthResponse(user, profile, request.isRememberMe());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateTarget(request.getTarget(), request.getValue());
        verificationService.verifyCode("RESET_PASSWORD:" + request.getValue(), request.getCode());

        User user = findUserByAccount(request.getValue());
        if (user == null) {
            throw new BusinessException(404, "Account not found", "auth.account.notFound");
        }

        validatePassword(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        auditService.logPasswordReset(getClientAccount(user), getClientIp());
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        Long userId = jwtUtil.parseToken(refreshToken) != null
                ? Long.parseLong(jwtUtil.parseToken(refreshToken).getSubject()) : null;
        String rateKey = "rl:refresh:" + (userId != null ? userId : "anon");
        if (Boolean.TRUE.equals(redis.hasKey(rateKey))) {
            throw new BusinessException(429, "Too many requests, please try again later", "auth.tooManyRequests");
        }
        redis.opsForValue().set(rateKey, "1", 1, TimeUnit.SECONDS);

        userId = jwtUtil.validateRefreshToken(refreshToken);
        if (userId == null) {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(refreshToken);
            if (claims != null && redis.opsForValue().get("rt:" + claims.getId()) == null) {
                redis.opsForValue().set("rt:replay:" + claims.getId(), "1", 30, TimeUnit.MINUTES);
                redis.opsForList().rightPush(
                        SESSION_PREFIX + Long.parseLong(claims.getSubject()), "FORCE_LOGOUT");
            }
            throw new BusinessException(401, "Login expired, please login again", "auth.login.expired");
        }
        jwtUtil.removeRefreshToken(refreshToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "User not found", "auth.user.notFound");
        }
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        return buildAuthResponse(user, profile, false);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            Claims claims = jwtUtil.parseToken(accessToken);
            if (claims != null) {
                removeSession(claims.getId(), Long.parseLong(claims.getSubject()));
            }
            jwtUtil.blacklistAccessToken(accessToken);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwtUtil.removeRefreshToken(refreshToken);
        }
    }

    @Override
    public PasswordPolicyResponse getPasswordPolicy() {
        PasswordPolicyResponse policy = new PasswordPolicyResponse();
        policy.setMinLength(passwordProperties.getMinLength());
        policy.setRequireUppercase(passwordProperties.isRequireUppercase());
        policy.setRequireLowercase(passwordProperties.isRequireLowercase());
        policy.setRequireDigit(passwordProperties.isRequireDigit());
        policy.setRequireSpecial(passwordProperties.isRequireSpecial());
        policy.setPattern(passwordProperties.buildPattern());
        policy.setMessage(passwordProperties.buildMessage());
        return policy;
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found", "auth.user.notFound");
        }
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        UserTokenBalance balance = tokenBalanceMapper.selectOne(
                new LambdaQueryWrapper<UserTokenBalance>().eq(UserTokenBalance::getUserId, userId));

        UserInfoResponse resp = new UserInfoResponse();
        resp.setUserId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setNickname(profile != null ? profile.getNickname() : null);
        resp.setAvatar(profile != null ? profile.getAvatar() : null);
        resp.setBio(profile != null ? profile.getBio() : null);
        resp.setTokenBalance(balance != null ? balance.getBalance() : 0);
        resp.setTotalUsed(balance != null ? balance.getTotalUsed() : 0);
        resp.setTotalGranted(balance != null ? balance.getTotalGranted() : 0);
        return resp;
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatar, String bio) {
        if (nickname != null && (nickname.isBlank() || nickname.length() > 16)) {
            throw new BusinessException(400, "Nickname must be 1-16 characters", "validation.nickname.length");
        }
        UserProfile profile = profileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setNickname(nickname);
            profile.setAvatar(avatar);
            profile.setBio(bio);
            profileMapper.insert(profile);
        } else {
            if (nickname != null) profile.setNickname(nickname);
            if (avatar != null) profile.setAvatar(avatar);
            if (bio != null) profile.setBio(bio);
            profileMapper.updateById(profile);
        }
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found", "auth.user.notFound");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(401, "Incorrect password", "auth.password.incorrect");
        }

        String sessionKey = SESSION_PREFIX + userId;
        List<String> sessions = redis.opsForList().range(sessionKey, 0, -1);
        if (sessions != null) {
            sessions.forEach(jti -> {
                if (!"FORCE_LOGOUT".equals(jti)) {
                    jwtUtil.blacklistJti(jti);
                }
            });
        }
        redis.delete(sessionKey);

        tokenBalanceMapper.delete(
                new LambdaQueryWrapper<UserTokenBalance>().eq(UserTokenBalance::getUserId, userId));
        profileMapper.delete(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        userMapper.deleteById(userId);
        auditService.logPasswordReset(getClientAccount(user), getClientIp());
    }

    private void validatePassword(String password) {
        String pattern = passwordProperties.buildPattern();
        if (!java.util.regex.Pattern.matches(pattern, password)) {
            throw new BusinessException(400, passwordProperties.buildMessage(), "auth.password.policy");
        }
    }

    private AuthResponse buildAuthResponse(User user, UserProfile profile, boolean rememberMe) {
        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), rememberMe);

        Claims claims = jwtUtil.parseToken(accessToken);
        if (claims != null) {
            String sessionKey = SESSION_PREFIX + user.getId();
            redis.opsForList().rightPush(sessionKey, claims.getId());
            redis.expire(sessionKey, 30, TimeUnit.DAYS);
            Long size = redis.opsForList().size(sessionKey);
            if (size != null && size > MAX_SESSIONS) {
                long removeCount = size - MAX_SESSIONS;
                List<String> removed = redis.opsForList().range(sessionKey, 0, removeCount - 1);
                redis.opsForList().trim(sessionKey, removeCount, -1);
                if (removed != null) {
                    removed.forEach(jti -> jwtUtil.blacklistJti(jti));
                }
            }
        }

        AuthResponse resp = new AuthResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(user.getId());
        resp.setNickname(profile != null ? profile.getNickname() : null);
        resp.setAvatar(profile != null ? profile.getAvatar() : null);
        resp.setExpiresIn(7200000);
        return resp;
    }

    private void removeSession(String jti, Long userId) {
        redis.opsForList().remove(SESSION_PREFIX + userId, 1, jti);
    }

    private User findUserByAccount(String account) {
        User byEmail = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, account));
        if (byEmail != null) return byEmail;
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, account));
    }

    private String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getClientAccount(User user) {
        return user.getEmail() != null ? user.getEmail() : user.getPhone();
    }

    private void validateTarget(String target, String value) {
        if ("email".equals(target)) {
            if (!Pattern.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", value)) {
                throw new BusinessException(400, "Invalid email format", "validation.email.invalid");
            }
        } else if ("phone".equals(target)) {
            if (!Pattern.matches("^1[3-9]\\d{9}$", value)) {
                throw new BusinessException(400, "Invalid phone number format", "validation.phone.invalid");
            }
        }
    }
}
