package com.auraspark.note.common.model;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Unified API response wrapper.
 *
 * All backend responses follow this structure. The frontend should use
 * {@code messageCode} (not {@code code} or {@code message}) for i18n display.
 *
 * Response examples:
 *
 * 1. Success with data:
 *    { "code": 200, "message": "success", "messageCode": null, "data": {...} }
 *
 * 2. Success with message:
 *    { "code": 200, "message": "Conversation deleted", "messageCode": "conversation.deleted", "data": null }
 *
 * 3. Business error:
 *    { "code": 404, "message": "Conversation not found", "messageCode": "conversation.not.found", "data": null }
 *
 * 4. Validation error (from @Valid):
 *    { "code": 400, "message": "apiKey: API key is required", "messageCode": null, "data": null }
 *
 *
 * Frontend i18n mapping example:
 * <pre>
 * const messages = {
 *   'zh-CN': {
 *     'conversation.not.found': '对话不存在',
 *     'conversation.deleted': '对话已删除',
 *     'message.not.found': '消息不存在',
 *     'note.not.found': '笔记不存在',
 *     'auth.login.invalid': '账号或密码错误',
 *     'auth.login.locked': '账户已被锁定，请稍后重试',
 *     'auth.register.duplicate': '该账户已注册',
 *     'file.upload.failed': '文件上传失败',
 *     'validation.title.empty': '标题不能为空',
 *     'validation.apiKey.required': 'API密钥不能为空',
 *     'ai.call.failed': 'AI服务调用失败',
 *   },
 *   en: {
 *     'conversation.not.found': 'Conversation not found',
 *     'conversation.deleted': 'Conversation deleted',
 *     // ...
 *   }
 * }
 * </pre>
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private String messageCode;
    private T data;
    private LocalDateTime timestamp;

    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> success(T data, String message, String messageCode) {
        ApiResponse<T> r = success(data);
        r.message = message;
        r.messageCode = messageCode;
        return r;
    }

    public static <T> ApiResponse<T> error(int code, String message, String messageCode) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        r.messageCode = messageCode;
        r.data = null;
        return r;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return error(code, message, null);
    }
}
