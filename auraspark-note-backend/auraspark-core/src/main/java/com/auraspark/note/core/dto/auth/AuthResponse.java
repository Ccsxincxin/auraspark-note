package com.auraspark.note.core.dto.auth;

import com.auraspark.note.core.config.HashIdConfig;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @HashIdConfig.EncodedId
    private Long userId;
    private String nickname;
    private String avatar;
    private long expiresIn;
}

