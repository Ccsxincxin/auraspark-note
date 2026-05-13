package com.auraspark.note.ai.service;

import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;

import java.util.List;
import java.util.Map;

public interface ConversationService {
    Conversation createConversation(Long userId, String title);
    List<Conversation> listConversations(Long userId);
    void deleteConversation(Long userId, Long conversationId);
    Conversation getConversation(Long userId, Long conversationId);

    List<Message> getMessages(Long conversationId);
    Map<Integer, List<Message>> getBranchMessages(Long conversationId);
    int getActiveBranch(Long conversationId);

    Message addMessage(Long conversationId, String role, String content, int tokens);
    Message addMessage(Long conversationId, String role, String content, int tokens, int branch);
    Message addMessage(Long conversationId, String role, String content, int tokens, int branch, Long versionOf);

    Message editMessage(Long userId, Long conversationId, Long messageId, String newContent);
    void deleteMessageChain(Long userId, Long conversationId, Long messageId);

    void updateTitle(Long userId, Long conversationId, String newTitle);
}

