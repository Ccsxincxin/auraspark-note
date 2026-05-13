package com.auraspark.note.ai.controller;

import com.auraspark.note.ai.dto.ChatRequest;
import com.auraspark.note.ai.service.ChatService;
import com.auraspark.note.common.handler.GlobalExceptionHandler;
import com.auraspark.note.core.config.HashIdConfig;
import com.auraspark.note.core.config.SpringContextHolder;
import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.service.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import({GlobalExceptionHandler.class, HashIdConfig.class, SpringContextHolder.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private ConversationService conversationService;

    private RequestPostProcessor withUserId = r -> {
        r.setAttribute("userId", 1L);
        return r;
    };

    @Test
    void chat_shouldReturnResponse() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setProvider("deepseek");
        request.setApiKey("sk-test-key");
        request.setModel("deepseek-chat");
        request.setMessage("Hello");

        when(chatService.chat(any(), anyLong())).thenReturn("Test AI response");

        mockMvc.perform(post("/api/ai/chat")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.response").value("Test AI response"));
    }

    @Test
    void chat_shouldReturnBadRequestWhenApiKeyMissing() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setProvider("deepseek");
        request.setMessage("Hello");

        mockMvc.perform(post("/api/ai/chat")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("apiKey: API key is required"));
    }

    @Test
    void chat_shouldReturnBadRequestWhenMessageMissing() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setProvider("deepseek");
        request.setApiKey("sk-test-key");

        mockMvc.perform(post("/api/ai/chat")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("message: Message is required"));
    }

    @Test
    void providers_shouldReturnList() throws Exception {
        mockMvc.perform(get("/api/ai/chat/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void editMessage_shouldReturnEditedMessageAndNewReply() throws Exception {
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("edited", Map.of("id", "200", "content", "edited content", "branch", 1));
        mockResult.put("response", Map.of("id", "201", "content", "new AI reply", "branch", 1));

        when(chatService.editAndRegenerate(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResult);

        mockMvc.perform(put("/api/ai/chat/conversations/1/messages/100")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"edited content\",\"apiKey\":\"sk-test\",\"provider\":\"openai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.edited.content").value("edited content"))
                .andExpect(jsonPath("$.data.edited.branch").value(1))
                .andExpect(jsonPath("$.data.response.content").value("new AI reply"))
                .andExpect(jsonPath("$.data.response.branch").value(1));

        verify(chatService).editAndRegenerate(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void editMessage_shouldReturnErrorWhenContentEmpty() throws Exception {
        mockMvc.perform(put("/api/ai/chat/conversations/1/messages/100")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Content cannot be empty"));
    }

    @Test
    void editMessage_shouldReturnErrorWhenApiKeyMissing() throws Exception {
        mockMvc.perform(put("/api/ai/chat/conversations/1/messages/100")
                        .with(withUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"edited\"}"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("apiKey is required"));
    }

    @Test
    void deleteMessage_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/ai/chat/conversations/1/messages/100")
                        .with(withUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Message deleted"));

        verify(conversationService).deleteMessageChain(1L, 1L, 100L);
    }

    @Test
    void getMessages_shouldReturnBranchGroups() throws Exception {
        Conversation c = new Conversation();
        c.setId(1L);
        c.setUserId(1L);
        when(conversationService.getConversation(anyLong(), anyLong())).thenReturn(c);

        List<Message> branch0 = new ArrayList<>();
        Message m1 = new Message();
        m1.setId(10L);
        m1.setConversationId(1L);
        m1.setRole("user");
        m1.setContent("hello");
        m1.setBranch(0);
        branch0.add(m1);

        List<Message> branch1 = new ArrayList<>();
        Message m2 = new Message();
        m2.setId(20L);
        m2.setConversationId(1L);
        m2.setRole("user");
        m2.setContent("edited hello");
        m2.setBranch(1);
        branch1.add(m2);

        Map<Integer, List<Message>> branches = new LinkedHashMap<>();
        branches.put(0, branch0);
        branches.put(1, branch1);

        when(conversationService.getBranchMessages(anyLong())).thenReturn(branches);
        when(conversationService.getActiveBranch(anyLong())).thenReturn(1);

        mockMvc.perform(get("/api/ai/chat/conversations/1/messages")
                        .with(withUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.branches.length()").value(2))
                .andExpect(jsonPath("$.data.branches[0].branch").value(0))
                .andExpect(jsonPath("$.data.branches[0].active").value(false))
                .andExpect(jsonPath("$.data.branches[0].messages.length()").value(1))
                .andExpect(jsonPath("$.data.branches[0].messages[0].content").value("hello"))
                .andExpect(jsonPath("$.data.branches[1].branch").value(1))
                .andExpect(jsonPath("$.data.branches[1].active").value(true))
                .andExpect(jsonPath("$.data.branches[1].messages[0].content").value("edited hello"));
    }
}
