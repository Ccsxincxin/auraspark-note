package com.auraspark.note.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.password")
public class PasswordProperties {

    private int minLength = 8;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecial = false;

    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }
    public boolean isRequireUppercase() { return requireUppercase; }
    public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }
    public boolean isRequireLowercase() { return requireLowercase; }
    public void setRequireLowercase(boolean requireLowercase) { this.requireLowercase = requireLowercase; }
    public boolean isRequireDigit() { return requireDigit; }
    public void setRequireDigit(boolean requireDigit) { this.requireDigit = requireDigit; }
    public boolean isRequireSpecial() { return requireSpecial; }
    public void setRequireSpecial(boolean requireSpecial) { this.requireSpecial = requireSpecial; }

    public String buildPattern() {
        StringBuilder sb = new StringBuilder("^(?=");
        if (requireLowercase) sb.append("(?=.*[a-z])");
        if (requireUppercase) sb.append("(?=.*[A-Z])");
        if (requireDigit) sb.append("(?=.*\\d)");
        if (requireSpecial) sb.append("(?=.*[!@#$%^&*])");
        sb.append(").{").append(minLength).append(",}$");
        return sb.toString();
    }

    public String buildMessage() {
        StringBuilder sb = new StringBuilder("Password must be at least ").append(minLength).append(" characters");
        if (requireLowercase) sb.append(", include lowercase");
        if (requireUppercase) sb.append(", include uppercase");
        if (requireDigit) sb.append(", include digit");
        if (requireSpecial) sb.append(", include special character");
        return sb.toString();
    }
}
