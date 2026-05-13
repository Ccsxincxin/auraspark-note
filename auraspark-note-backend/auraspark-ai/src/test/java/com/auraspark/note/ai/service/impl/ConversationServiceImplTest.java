package com.auraspark.note.ai.service.impl;

import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.mapper.ConversationMapper;
import com.auraspark.note.ai.mapper.MessageMapper;
import com.auraspark.note.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private Conversation conversation;
    private List<Message> branch0Messages;

    @BeforeEach
    void setUp() {
        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setTitle("Test Chat");
        conversation.setStatus("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        branch0Messages = new ArrayList<>();

        Message m1 = new Message();
        m1.setId(10L);
        m1.setConversationId(1L);
        m1.setRole("user");
        m1.setContent("Hello");
        m1.setBranch(0);
        m1.setCreatedAt(now);
        m1.setDeleted(false);
        branch0Messages.add(m1);

        Message m2 = new Message();
        m2.setId(11L);
        m2.setConversationId(1L);
        m2.setRole("assistant");
        m2.setContent("Hi there");
        m2.setBranch(0);
        m2.setCreatedAt(now.plusSeconds(1));
        m2.setDeleted(false);
        branch0Messages.add(m2);

        Message m3 = new Message();
        m3.setId(12L);
        m3.setConversationId(1L);
        m3.setRole("user");
        m3.setContent("What's the weather?");
        m3.setBranch(0);
        m3.setCreatedAt(now.plusSeconds(2));
        m3.setDeleted(false);
        branch0Messages.add(m3);

        Message m4 = new Message();
        m4.setId(13L);
        m4.setConversationId(1L);
        m4.setRole("assistant");
        m4.setContent("It's sunny");
        m4.setBranch(0);
        m4.setCreatedAt(now.plusSeconds(3));
        m4.setDeleted(false);
        branch0Messages.add(m4);
    }

    @Test
    void editMessage_shouldCreateNewBranchWithHistory() {
        Message msg = branch0Messages.get(2);
        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        when(messageMapper.selectById(12L)).thenReturn(msg);
        when(messageMapper.selectList(any())).thenReturn(branch0Messages, branch0Messages);
        when(messageMapper.insert(any(Message.class))).thenReturn(1);

        Message edited = conversationService.editMessage(1L, 1L, 12L, "How's the weather?");

        assertEquals("How's the weather?", edited.getContent());
        assertEquals(12L, edited.getVersionOf());
        assertEquals(1, edited.getBranch());

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, times(3)).insert(captor.capture());
        List<Message> inserted = captor.getAllValues();

        Message copy1 = inserted.get(0);
        assertEquals(10L, copy1.getVersionOf());
        assertEquals("Hello", copy1.getContent());
        assertEquals(1, copy1.getBranch());

        Message copy2 = inserted.get(1);
        assertEquals(11L, copy2.getVersionOf());
        assertEquals("Hi there", copy2.getContent());
        assertEquals(1, copy2.getBranch());

        Message editedInsert = inserted.get(2);
        assertEquals(12L, editedInsert.getVersionOf());
        assertEquals("How's the weather?", editedInsert.getContent());
        assertEquals(1, editedInsert.getBranch());

        verify(conversationMapper, times(2)).selectById(1L);
        verify(conversationMapper, times(1)).updateById(any(Conversation.class));
    }

    @Test
    void editMessage_shouldSupportMultipleEditsFromSameMessage() {
        Message msg = branch0Messages.get(2);
        List<Message> allWithBranch1 = new ArrayList<>(branch0Messages);
        Message existingBranch1 = new Message();
        existingBranch1.setId(20L);
        existingBranch1.setConversationId(1L);
        existingBranch1.setRole("user");
        existingBranch1.setContent("edited v1");
        existingBranch1.setBranch(1);
        existingBranch1.setCreatedAt(LocalDateTime.now().plusSeconds(10));
        existingBranch1.setDeleted(false);
        allWithBranch1.add(existingBranch1);

        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        when(messageMapper.selectById(12L)).thenReturn(msg);
        when(messageMapper.selectList(any()))
                .thenReturn(branch0Messages)
                .thenReturn(allWithBranch1);
        when(messageMapper.insert(any(Message.class))).thenReturn(1);

        Message edited = conversationService.editMessage(1L, 1L, 12L, "edited v2");

        assertEquals("edited v2", edited.getContent());
        assertEquals(2, edited.getBranch());
        assertEquals(12L, edited.getVersionOf());
    }

    @Test
    void editMessage_shouldThrowWhenConversationNotFound() {
        when(conversationMapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                conversationService.editMessage(1L, 1L, 12L, "test"));
    }

    @Test
    void editMessage_shouldThrowWhenMessageNotFound() {
        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        when(messageMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                conversationService.editMessage(1L, 1L, 99L, "test"));
    }

    @Test
    void editMessage_shouldThrowWhenMessageNotInConversation() {
        Message orphan = new Message();
        orphan.setId(99L);
        orphan.setConversationId(999L);

        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        when(messageMapper.selectById(99L)).thenReturn(orphan);

        assertThrows(BusinessException.class, () ->
                conversationService.editMessage(1L, 1L, 99L, "test"));
    }

    @Test
    void editMessage_shouldThrowWhenWrongUser() {
        Conversation otherUserConv = new Conversation();
        otherUserConv.setId(1L);
        otherUserConv.setUserId(2L);

        when(conversationMapper.selectById(1L)).thenReturn(otherUserConv);

        assertThrows(BusinessException.class, () ->
                conversationService.editMessage(1L, 1L, 12L, "test"));
    }

    @Test
    void editMessage_shouldThrowWhenEditingAssistantMessage() {
        Message assistant = new Message();
        assistant.setId(50L);
        assistant.setConversationId(1L);
        assistant.setRole("assistant");
        assistant.setContent("I am a reply");
        assistant.setBranch(0);

        when(conversationMapper.selectById(1L)).thenReturn(conversation);
        when(messageMapper.selectById(50L)).thenReturn(assistant);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.editMessage(1L, 1L, 50L, "edited reply"));
        assertEquals(400, ex.getCode());
        assertEquals("Only user messages can be edited", ex.getMessage());
    }
}
