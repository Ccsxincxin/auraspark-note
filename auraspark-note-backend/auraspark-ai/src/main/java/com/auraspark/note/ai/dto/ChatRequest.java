package com.auraspark.note.ai.dto;

import com.auraspark.note.core.config.HashIdConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    private String provider;

    @NotBlank(message = "API key is required")
    private String apiKey;

    private String model;

    @NotBlank(message = "Message is required")
    private String message;

    private String baseUrl;

    @HashIdConfig.EncodedId
    private Long conversationId;

    private Integer branch;
}
