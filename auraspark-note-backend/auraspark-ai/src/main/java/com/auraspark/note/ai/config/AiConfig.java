package com.auraspark.note.ai.config;

import com.auraspark.note.ai.domain.ChatProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

@Configuration
@ConditionalOnClass(OpenAiChatModel.class)
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @PostConstruct
    public void verifyAiDependencies() {
        log.info("Spring AI (OpenAI-compatible) dependencies loaded successfully");
        log.info("Available AI providers: {}", Arrays.toString(ChatProvider.values()));
    }
}
