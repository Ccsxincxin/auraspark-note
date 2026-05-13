package com.auraspark.note.core.dto.auth;

import com.auraspark.note.core.config.HashIdConfig;
import lombok.Data;

@Data
public class UserInfoResponse {
    @HashIdConfig.EncodedId
    private Long userId;
    private String email;
    private String phone;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer tokenBalance;
    private Long totalUsed;
    private Long totalGranted;
}

