package com.auraspark.note.ai.service;

import com.auraspark.note.ai.dto.ChatRequest;

import java.util.Map;

public interface ChatService {

    String chat(ChatRequest request, Long userId);

    Map<String, Object> editAndRegenerate(Long userId, Long conversationId, Long messageId,
            String newContent, String apiKey, String provider, String model, String baseUrl);
}
