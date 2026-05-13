package com.auraspark.note.ai.service.impl;

import com.auraspark.note.ai.domain.ChatProvider;
import com.auraspark.note.ai.dto.ChatRequest;
import com.auraspark.note.ai.service.ChatService;
import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.service.ConversationService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTEXT_TOKENS = 4000;
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ConversationService conversationService;

    public ChatServiceImpl(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public String chat(ChatRequest request, Long userId) {
        ChatProvider provider = ChatProvider.fromString(request.getProvider());

        String baseUrl = request.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = provider != null ? provider.getBaseUrl() : "https://api.openai.com";
        }

        String model = request.getModel();
        if (model == null || model.isBlank()) {
            model = provider != null ? provider.getDefaultModel() : "gpt-4o-mini";
        }

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(request.getApiKey())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        Long conversationId = request.getConversationId();
        Conversation conversation = null;

        if (conversationId != null) {
            conversation = conversationService.getConversation(userId, conversationId);
        }

        int branch = request.getBranch() != null ? request.getBranch()
                : (conversation != null ? conversationService.getActiveBranch(conversation.getId()) : 0);

        String context = conversation != null ? buildContext(conversation.getId(), branch, request.getApiKey(), baseUrl, model) : "";

        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                    .system("You are an intelligent note assistant, helping users organize and generate notes. " +
                            "Answer in a friendly tone and keep it concise." +
                            (context.isEmpty() ? "" : "\n\nBelow is the conversation history context:\n" + context))
                    .user(request.getMessage())
                    .call()
                    .content();
        } catch (Exception e) {
            throw new BusinessException(502, "AI service call failed, " + (conversation == null ? "conversation was not created" : "please try again"), "ai.call.failed");
        }

        if (conversation == null) {
            conversation = conversationService.createConversation(userId, null);
        }

        int userTokens = estimateTokens(request.getMessage());
        conversationService.addMessage(conversation.getId(), "user", request.getMessage(), userTokens, branch);

        int assistantTokens = estimateTokens(aiResponse);
        conversationService.addMessage(conversation.getId(), "assistant", aiResponse, assistantTokens, branch);

        if ("New Chat".equals(conversation.getTitle())) {
            String aiTitle = generateTitle(request.getMessage(), api, model);
            conversationService.updateTitle(userId, conversation.getId(), aiTitle);
        }

        if (totalTokens(conversation.getId()) > MAX_CONTEXT_TOKENS) {
            compressConversation(conversation.getId(), model, api);
        }

        return aiResponse;
    }

    @Override
    public Map<String, Object> editAndRegenerate(Long userId, Long conversationId, Long messageId,
            String newContent, String apiKey, String provider, String model, String baseUrl) {
        ChatProvider chatProvider = ChatProvider.fromString(provider);

        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = chatProvider != null ? chatProvider.getBaseUrl() : "https://api.openai.com";
        }
        if (model == null || model.isBlank()) {
            model = chatProvider != null ? chatProvider.getDefaultModel() : "gpt-4o-mini";
        }

        Message edited = conversationService.editMessage(userId, conversationId, messageId, newContent);
        int activeBranch = conversationService.getActiveBranch(conversationId);

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String context = buildContext(conversationId, activeBranch, apiKey, baseUrl, model);

        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                    .system("You are an intelligent note assistant, helping users organize and generate notes. " +
                            "Answer in a friendly tone and keep it concise." +
                            (context.isEmpty() ? "" : "\n\nBelow is the conversation history context:\n" + context))
                    .user(newContent)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new BusinessException(502, "AI service call failed, please try again", "ai.call.failed");
        }

        int assistantTokens = estimateTokens(aiResponse);
        Message response = conversationService.addMessage(conversationId, "assistant", aiResponse, assistantTokens, activeBranch);

        if (totalTokens(conversationId) > MAX_CONTEXT_TOKENS) {
            compressConversation(conversationId, model, api);
        }

        return Map.of("edited", edited, "response", response);
    }

    private String buildContext(Long conversationId, int branch, String apiKey, String baseUrl, String model) {
        List<Message> messages = conversationService.getBranchMessages(conversationId)
                .getOrDefault(branch, List.of());

        int totalTokens = 0;
        StringBuilder context = new StringBuilder();
        int count = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if ("system".equals(m.getRole())) continue;
            if (Boolean.TRUE.equals(m.getCompressed())) continue;
            if (count >= MAX_HISTORY_MESSAGES) break;

            String prefix = "user".equals(m.getRole()) ? "User: " : "Assistant: ";
            String entry = prefix + m.getContent() + "\n";
            int entryTokens = estimateTokens(entry);

            if (totalTokens + entryTokens > MAX_CONTEXT_TOKENS / 2) break;

            context.insert(0, entry);
            totalTokens += entryTokens;
            count++;
        }

        return context.toString();
    }

    private int totalTokens(Long conversationId) {
        List<Message> messages = conversationService.getMessages(conversationId);
        return messages.stream().filter(m -> !Boolean.TRUE.equals(m.getCompressed()))
                .mapToInt(m -> m.getTokens() != null ? m.getTokens() : 0)
                .sum();
    }

    private void compressConversation(Long conversationId, String model, OpenAiApi api) {
        List<Message> messages = conversationService.getMessages(conversationId);
        List<Message> toCompress = messages.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getCompressed()) && !"system".equals(m.getRole()))
                .limit(messages.size() / 2)
                .collect(Collectors.toList());

        if (toCompress.isEmpty()) return;

        StringBuilder history = new StringBuilder();
        for (Message m : toCompress) {
            String prefix = "user".equals(m.getRole()) ? "User: " : "Assistant: ";
            history.append(prefix).append(m.getContent()).append("\n");
        }

        try {
            OpenAiChatModel summaryModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                    .build();
            ChatClient client = ChatClient.builder(summaryModel).build();

            String summary = client.prompt()
                    .system("Summarize the above conversation concisely, keeping key information. The result will be used as context for subsequent conversations.")
                .user(history.toString())
                    .call()
                    .content();

            Message summaryMsg = conversationService.addMessage(
                    conversationId, "system", "Conversation summary: " + summary, estimateTokens(summary));

            for (Message m : toCompress) {
                m.setCompressed(true);
            }
        } catch (Exception e) {
            int keep = Math.min(messages.size(), 10);
            List<Message> recent = messages.subList(Math.max(0, messages.size() - keep), messages.size());
            for (int i = 0; i < messages.size() - keep; i++) {
                if (!Boolean.TRUE.equals(messages.get(i).getCompressed())) {
                }
            }
        }
    }

    private String generateTitle(String userMessage, OpenAiApi api, String model) {
        try {
            OpenAiChatModel titleModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                    .build();
            ChatClient client = ChatClient.builder(titleModel).build();
            return client.prompt()
                    .system("你是一个标题生成器。根据用户的第一条消息，生成一个简洁的对话标题（最多一句话），语言与用户消息保持一致。只返回标题文本，不要引号或额外字符。")
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            return guessTitle(userMessage);
        }
    }

    private String guessTitle(String message) {
        if (message == null || message.isBlank()) return "New Chat";
        String cleaned = message.replaceAll("[\\n\\r]+", " ").trim();
        return cleaned.length() > 30 ? cleaned.substring(0, 30) + "..." : cleaned;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int charCount = text.length();
        int chineseChars = 0;
        for (char c : text.toCharArray()) {
            if (c > 0x4E00 && c < 0x9FFF) chineseChars++;
        }
        return (int) Math.ceil((charCount - chineseChars) / 4.0 + chineseChars / 2.0) + 10;
    }
}
