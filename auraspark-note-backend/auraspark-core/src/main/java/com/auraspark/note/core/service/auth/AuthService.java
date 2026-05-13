package com.auraspark.note.core.service.auth;

import com.auraspark.note.core.dto.auth.*;

public interface AuthService {
    void sendCode(SendCodeRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse loginByPassword(LoginPasswordRequest request);
    AuthResponse loginByCode(LoginCodeRequest request);
    void resetPassword(ResetPasswordRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String accessToken, String refreshToken);
    PasswordPolicyResponse getPasswordPolicy();
    UserInfoResponse getUserInfo(Long userId);
    void updateProfile(Long userId, String nickname, String avatar, String bio);
    void deleteAccount(Long userId, String password);
}


