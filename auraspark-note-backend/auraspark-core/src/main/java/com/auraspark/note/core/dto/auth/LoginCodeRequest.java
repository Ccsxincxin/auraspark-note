package com.auraspark.note.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginCodeRequest {

    @NotBlank
    private String target;

    @NotBlank
    private String value;

    @NotBlank
    private String code;

    private boolean rememberMe;
}

