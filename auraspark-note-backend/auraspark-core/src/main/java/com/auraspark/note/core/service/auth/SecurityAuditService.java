package com.auraspark.note.core.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public void logLogin(Long userId, String account, String ip, boolean success) {
        auditLog.info("LOGIN userId={} account={} ip={} success={}", userId, account, ip, success);
    }

    public void logRegister(Long userId, String account, String ip) {
        auditLog.info("REGISTER userId={} account={} ip={}", userId, account, ip);
    }

    public void logIpBan(String ip, String reason) {
        auditLog.warn("IP_BANNED ip={} reason={}", ip, reason);
    }

    public void logPasswordReset(String account, String ip) {
        auditLog.info("PASSWORD_RESET account={} ip={}", account, ip);
    }

    public void logDeviceKick(Long userId, String kickedJti, String ip) {
        auditLog.warn("DEVICE_KICKED userId={} jti={} ip={}", userId, kickedJti, ip);
    }
}

