package com.auraspark.note.ai.domain;

import lombok.Getter;

@Getter
public enum ChatProvider {

    DEEPSEEK("https://api.deepseek.com", "deepseek-chat"),
    OPENAI("https://api.openai.com", "gpt-4o-mini");

    private final String baseUrl;
    private final String defaultModel;

    ChatProvider(String baseUrl, String defaultModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
    }

    public static ChatProvider fromString(String value) {
        if (value == null) return null;
        try {
            return ChatProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
