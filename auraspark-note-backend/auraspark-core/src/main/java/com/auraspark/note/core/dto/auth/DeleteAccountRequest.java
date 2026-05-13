package com.auraspark.note.core.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {
    @NotBlank
    private String password;
}

