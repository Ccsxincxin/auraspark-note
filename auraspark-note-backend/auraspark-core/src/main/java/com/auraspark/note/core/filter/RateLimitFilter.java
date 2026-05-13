package com.auraspark.note.core.filter;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.service.auth.SecurityAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_PREFIX = "rl:ip:";
    private static final String BAN_PREFIX = "ban:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SecurityAuditService auditService;
    private final int defaultLimit;
    private final int authLimit;
    private final int lockMinutes;
    private final List<String> whitelist;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper,
                           SecurityAuditService auditService,
                            @Value("${rate-limit.default}") int defaultLimit,
                            @Value("${rate-limit.auth}") int authLimit,
                            @Value("${rate-limit.lock-minutes}") int lockMinutes,
                            @Value("${rate-limit.whitelist}") String whitelistStr) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.defaultLimit = defaultLimit;
        this.authLimit = authLimit;
        this.lockMinutes = lockMinutes;
        this.whitelist = whitelistStr == null || whitelistStr.isBlank()
                ? List.of() : List.of(whitelistStr.split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = getClientIp(request);

        if (isWhitelisted(ip)) {
            chain.doFilter(request, response);
            return;
        }

        if (Boolean.TRUE.equals(redis.hasKey(BAN_PREFIX + ip))) {
            writeTooMany(response);
            return;
        }

        String path = request.getRequestURI();
        int limit = (path.startsWith("/api/auth/")) ? authLimit : defaultLimit;

        String key = RATE_PREFIX + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, 1, TimeUnit.SECONDS);
        }

        if (count != null && count > limit) {
            redis.opsForValue().set(BAN_PREFIX + ip, "1", lockMinutes, TimeUnit.MINUTES);
            redis.delete(key);
            auditService.logIpBan(ip, "Rate limit exceeded");
            writeTooMany(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeTooMany(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(429, "Too many requests, please try again later", "auth.tooManyRequests"));
    }

    private boolean isWhitelisted(String ip) {
        return whitelist.contains(ip);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
