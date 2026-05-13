package com.auraspark.note.common.exception;

import lombok.Getter;

/**
 * Business logic exception.
 *
 * The {@code messageCode} field is a unique string key for i18n.
 * See {@code ApiResponse.messageCode} for the complete list of codes.
 *
 * Usage:
 *   throw new BusinessException(404, "Conversation not found", "conversation.not.found");
 *
 * The frontend should map messageCode to localized strings:
 *   zh-CN: "conversation.not.found" → "对话不存在"
 *   en:    "conversation.not.found" → "Conversation not found"
 */
@Getter
public class BusinessException extends RuntimeException {

    /** HTTP status-like code: 400, 401, 404, 409, 429, 500 */
    private final int code;

    /** i18n key for frontend localization. null means use message directly. */
    private final String messageCode;

    public BusinessException(int code, String message, String messageCode) {
        super(message);
        this.code = code;
        this.messageCode = messageCode;
    }

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(String message) {
        this(400, message, null);
    }
}
