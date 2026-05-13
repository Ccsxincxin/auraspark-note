package com.auraspark.note.core.dto.auth;

import lombok.Data;

@Data
public class PasswordPolicyResponse {
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;
    private String pattern;
    private String message;
}

