package com.auraspark.note.core.controller.auth;

import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.core.dto.auth.*;
import com.auraspark.note.core.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "Register / Login / Password management / Token refresh")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Send verification code", description = "Supports email and phone, non-repeatable within 60 seconds")
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        authService.sendCode(request);
        return ApiResponse.success(null, "Verification code sent", "auth.code.sent");
    }

    @Operation(summary = "Register", description = "Verification code + password + nickname (required), creates user and token balance")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse resp = authService.register(request);
        return ApiResponse.success(resp);
    }

    @Operation(summary = "Login by password")
    @PostMapping("/login/password")
    public ApiResponse<AuthResponse> loginByPassword(@Valid @RequestBody LoginPasswordRequest request) {
        AuthResponse resp = authService.loginByPassword(request);
        return ApiResponse.success(resp);
    }

    @Operation(summary = "Login by verification code")
    @PostMapping("/login/code")
    public ApiResponse<AuthResponse> loginByCode(@Valid @RequestBody LoginCodeRequest request) {
        AuthResponse resp = authService.loginByCode(request);
        return ApiResponse.success(resp);
    }

    @Operation(summary = "Reset password", description = "Requires a reset password verification code first")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Password reset successfully", "auth.password.reset");
    }

    @Operation(summary = "Refresh token", description = "Exchange refresh_token for a new token pair")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse resp = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(resp);
    }

    @Operation(summary = "Logout", description = "Blacklists access_token and removes refresh_token from Redis")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = (request != null) ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.success(null, "Logged out successfully", "auth.logout");
    }

    @Operation(summary = "Password policy", description = "Frontend can validate password format dynamically")
    @GetMapping("/password-policy")
    public ApiResponse<PasswordPolicyResponse> getPasswordPolicy() {
        return ApiResponse.success(authService.getPasswordPolicy());
    }

    @Operation(summary = "Current user info", description = "Contains profile and token balance")
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getUserInfo(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "Update profile", description = "Update nickname, avatar, or bio")
    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestAttribute("userId") Long userId,
                                           @RequestBody Map<String, String> body) {
        authService.updateProfile(userId, body.get("nickname"), body.get("avatar"), body.get("bio"));
        return ApiResponse.success(null, "Profile updated", "auth.profile.updated");
    }

    @Operation(summary = "Delete account", description = "Requires current password, deletes all user data")
    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@RequestAttribute("userId") Long userId,
                                           @RequestBody DeleteAccountRequest request) {
        authService.deleteAccount(userId, request.getPassword());
        return ApiResponse.success(null, "Account deleted successfully", "auth.account.deleted");
    }
}
