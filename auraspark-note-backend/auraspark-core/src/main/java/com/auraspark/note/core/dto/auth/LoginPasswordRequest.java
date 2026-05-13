package com.auraspark.note.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginPasswordRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;

    private boolean rememberMe;
}

