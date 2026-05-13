package com.auraspark.note.ai.service.impl;

import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.mapper.ConversationMapper;
import com.auraspark.note.ai.mapper.MessageMapper;
import com.auraspark.note.ai.service.ConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationServiceImpl(ConversationMapper conversationMapper, MessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public Conversation createConversation(Long userId, String title) {
        Conversation c = new Conversation();
        c.setUserId(userId);
        c.setTitle(title != null && !title.isBlank() ? title : "New Chat");
        c.setStatus("ACTIVE");
        conversationMapper.insert(c);
        return c;
    }

    @Override
    public List<Conversation> listConversations(Long userId) {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdatedAt));
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        Conversation c = getOwned(userId, conversationId);
        conversationMapper.deleteById(c.getId());
    }

    @Override
    public Conversation getConversation(Long userId, Long conversationId) {
        return getOwned(userId, conversationId);
    }

    @Override
    public List<Message> getMessages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getDeleted, false)
                .orderByAsc(Message::getCreatedAt));
    }

    @Override
    public Map<Integer, List<Message>> getBranchMessages(Long conversationId) {
        List<Message> all = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getDeleted, false)
                .orderByAsc(Message::getCreatedAt));
        return all.stream().collect(Collectors.groupingBy(
                m -> m.getBranch() != null ? m.getBranch() : 0,
                LinkedHashMap::new,
                Collectors.toList()));
    }

    @Override
    public int getActiveBranch(Long conversationId) {
        Message last = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getDeleted, false)
                .orderByDesc(Message::getId)
                .last("LIMIT 1"));
        return last != null && last.getBranch() != null ? last.getBranch() : 0;
    }

    @Override
    @Transactional
    public Message addMessage(Long conversationId, String role, String content, int tokens) {
        return addMessage(conversationId, role, content, tokens, 0, null);
    }

    @Override
    @Transactional
    public Message addMessage(Long conversationId, String role, String content, int tokens, int branch) {
        return addMessage(conversationId, role, content, tokens, branch, null);
    }

    @Override
    @Transactional
    public Message addMessage(Long conversationId, String role, String content, int tokens, int branch, Long versionOf) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setTokens(tokens);
        m.setCompressed(false);
        m.setBranch(branch);
        m.setVersionOf(versionOf);
        m.setDeleted(false);
        messageMapper.insert(m);

        Conversation c = conversationMapper.selectById(conversationId);
        if (c != null) {
            c.setUpdatedAt(LocalDateTime.now());
            conversationMapper.updateById(c);
        }
        return m;
    }

    @Override
    @Transactional
    public Message editMessage(Long userId, Long conversationId, Long messageId, String newContent) {
        Conversation c = getOwned(userId, conversationId);
        Message msg = messageMapper.selectById(messageId);
        if (msg == null || !msg.getConversationId().equals(conversationId)) {
            throw new BusinessException(404, "Message not found", "message.not.found");
        }
        if (!"user".equals(msg.getRole())) {
            throw new BusinessException(400, "Only user messages can be edited", "message.edit.notAllowed");
        }

        int currentBranch = msg.getBranch() != null ? msg.getBranch() : 0;
        List<Message> siblings = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getBranch, currentBranch)
                .eq(Message::getDeleted, false)
                .orderByAsc(Message::getCreatedAt));

        int newBranch = findMaxBranch(conversationId) + 1;

        for (Message s : siblings) {
            if (s.getId().equals(messageId)) break;
            copyToBranch(s, newBranch);
        }

        return addMessage(conversationId, msg.getRole(), newContent,
                estimateTokens(newContent), newBranch, msg.getId());
    }

    @Override
    @Transactional
    public void deleteMessageChain(Long userId, Long conversationId, Long messageId) {
        getOwned(userId, conversationId);
        Message msg = messageMapper.selectById(messageId);
        if (msg == null || !msg.getConversationId().equals(conversationId)) {
            throw new BusinessException(404, "Message not found", "message.not.found");
        }

        int branch = msg.getBranch() != null ? msg.getBranch() : 0;
        List<Message> chain = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getBranch, branch)
                .eq(Message::getDeleted, false)
                .orderByAsc(Message::getCreatedAt));

        boolean delete = false;
        for (Message m : chain) {
            if (m.getId().equals(messageId)) delete = true;
            if (delete) {
                m.setDeleted(true);
                messageMapper.updateById(m);
            }
        }
    }

    private void copyToBranch(Message s, int newBranch) {
        Message copy = new Message();
        copy.setConversationId(s.getConversationId());
        copy.setRole(s.getRole());
        copy.setContent(s.getContent());
        copy.setTokens(s.getTokens());
        copy.setCompressed(s.getCompressed());
        copy.setBranch(newBranch);
        copy.setVersionOf(s.getId());
        copy.setDeleted(false);
        copy.setCreatedAt(s.getCreatedAt());
        messageMapper.insert(copy);
    }

    private int findMaxBranch(Long conversationId) {
        List<Message> all = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId));
        return all.stream().mapToInt(m -> m.getBranch() != null ? m.getBranch() : 0).max().orElse(0);
    }

    @Override
    @Transactional
    public void updateTitle(Long userId, Long conversationId, String newTitle) {
        Conversation c = getOwned(userId, conversationId);
        c.setTitle(newTitle);
        conversationMapper.updateById(c);
    }

    private Conversation getOwned(Long userId, Long conversationId) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c == null || !c.getUserId().equals(userId)) {
            throw new BusinessException(404, "Conversation not found", "conversation.not.found");
        }
        return c;
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int charCount = text.length();
        int chineseChars = 0;
        for (char ch : text.toCharArray()) {
            if (ch > 0x4E00 && ch < 0x9FFF) chineseChars++;
        }
        return (int) Math.ceil((charCount - chineseChars) / 4.0 + chineseChars / 2.0) + 10;
    }
}
