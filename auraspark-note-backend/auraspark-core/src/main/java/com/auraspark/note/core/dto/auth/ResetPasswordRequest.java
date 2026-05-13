package com.auraspark.note.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Pattern(regexp = "^(email|phone)$")
    private String target;

    @NotBlank
    private String value;

    @NotBlank
    private String code;

    @NotBlank
    private String newPassword;
}

