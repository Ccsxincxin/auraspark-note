package com.auraspark.note.ai.controller;

import com.auraspark.note.ai.domain.ChatProvider;
import com.auraspark.note.ai.dto.ChatRequest;
import com.auraspark.note.ai.service.ChatService;
import com.auraspark.note.common.model.ApiResponse;
import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.service.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "AI Chat", description = "Multi-model AI chat supporting DeepSeek, OpenAI, etc. with context memory and branching")
@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    @Operation(summary = "AI Chat", description = "Pass conversationId to continue context, or omit to auto-create a new conversation")
    @PostMapping
    public ApiResponse<Map<String, Object>> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestAttribute("userId") Long userId) {
        String response = chatService.chat(request, userId);
        return ApiResponse.success(Map.of("response", response));
    }

    @Operation(summary = "Conversation List")
    @GetMapping("/conversations")
    public ApiResponse<List<Conversation>> listConversations(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(conversationService.listConversations(userId));
    }

    @Operation(summary = "Delete Conversation")
    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> deleteConversation(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        conversationService.deleteConversation(userId, id);
        return ApiResponse.success(null, "Conversation deleted", "conversation.deleted");
    }

    @Operation(summary = "Update Conversation Title")
    @PutMapping("/conversations/{id}/title")
    public ApiResponse<Void> updateTitle(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ApiResponse.error(400, "Title cannot be empty", "validation.title.empty");
        }
        conversationService.updateTitle(userId, id, title);
        return ApiResponse.success(null, "Title updated", "conversation.title.updated");
    }

    @Operation(summary = "Get Messages (Grouped by Branch)", description = "Returns messages of all branches with active branch indicator")
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<Map<String, Object>> getMessages(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id) {
        conversationService.getConversation(userId, id);
        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(id);
        int active = conversationService.getActiveBranch(id);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, List<Message>> entry : branches.entrySet()) {
            Map<String, Object> branchInfo = new LinkedHashMap<>();
            branchInfo.put("branch", entry.getKey());
            branchInfo.put("active", entry.getKey() == active);
            branchInfo.put("messages", entry.getValue());
            result.add(branchInfo);
        }
        return ApiResponse.success(Map.of("branches", result));
    }

    @Operation(summary = "Edit Message & Regenerate Reply", description = "Edits the message on a new branch, then calls AI to generate a new reply. Old branch preserved.")
    @PutMapping("/conversations/{cid}/messages/{mid}")
    public ApiResponse<Map<String, Object>> editMessage(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long cid,
            @PathVariable Long mid,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "Content cannot be empty", "validation.content.empty");
        }
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            return ApiResponse.error(400, "apiKey is required", "validation.apiKey.required");
        }
        Map<String, Object> result = chatService.editAndRegenerate(
                userId, cid, mid, content,
                apiKey, body.get("provider"), body.get("model"), body.get("baseUrl"));
        return ApiResponse.success(result);
    }

    @Operation(summary = "Delete Message", description = "Deletes the message and all subsequent messages in the same branch")
    @DeleteMapping("/conversations/{cid}/messages/{mid}")
    public ApiResponse<Void> deleteMessage(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long cid,
            @PathVariable Long mid) {
        conversationService.deleteMessageChain(userId, cid, mid);
        return ApiResponse.success(null, "Message deleted", "message.deleted");
    }

    @Operation(summary = "Available Providers")
    @GetMapping("/providers")
    public ApiResponse<ChatProvider[]> getProviders() {
        return ApiResponse.success(ChatProvider.values());
    }
}
