package com.auraspark.note.core.filter;

import com.auraspark.note.core.util.JwtUtil;
import com.auraspark.note.common.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(3)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/send-code")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/login/")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/password-policy")
                || path.startsWith("/api/test/")
                || path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Not logged in, please login first");
            return;
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtil.validateAccessToken(token);
        if (userId == null) {
            writeUnauthorized(response, "Login expired, please login again");
            return;
        }

        request.setAttribute("userId", userId);
        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(401, message));
    }
}
