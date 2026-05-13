package com.auraspark.note.ai.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatProviderTest {

    @Test
    void fromString_shouldReturnDeepSeek() {
        assertEquals(ChatProvider.DEEPSEEK, ChatProvider.fromString("deepseek"));
        assertEquals(ChatProvider.DEEPSEEK, ChatProvider.fromString("DEEPSEEK"));
    }

    @Test
    void fromString_shouldReturnOpenAi() {
        assertEquals(ChatProvider.OPENAI, ChatProvider.fromString("openai"));
        assertEquals(ChatProvider.OPENAI, ChatProvider.fromString("OPENAI"));
    }

    @Test
    void fromString_shouldReturnNullForUnknown() {
        assertNull(ChatProvider.fromString("claude"));
        assertNull(ChatProvider.fromString(""));
    }

    @Test
    void fromString_shouldReturnNullForNull() {
        assertNull(ChatProvider.fromString(null));
    }

    @Test
    void deepSeek_shouldHaveCorrectDefaults() {
        assertEquals("https://api.deepseek.com", ChatProvider.DEEPSEEK.getBaseUrl());
        assertEquals("deepseek-chat", ChatProvider.DEEPSEEK.getDefaultModel());
    }

    @Test
    void openAi_shouldHaveCorrectDefaults() {
        assertEquals("https://api.openai.com", ChatProvider.OPENAI.getBaseUrl());
        assertEquals("gpt-4o-mini", ChatProvider.OPENAI.getDefaultModel());
    }
}
