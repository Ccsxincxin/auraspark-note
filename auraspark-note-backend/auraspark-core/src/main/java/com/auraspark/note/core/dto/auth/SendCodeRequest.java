package com.auraspark.note.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {

    @NotBlank
    @Pattern(regexp = "^(email|phone)$", message = "target must be email or phone")
    private String target;

    @NotBlank
    private String value;

    @NotBlank
    @Pattern(regexp = "^(REGISTER|LOGIN|RESET_PASSWORD)$")
    private String type;
}

