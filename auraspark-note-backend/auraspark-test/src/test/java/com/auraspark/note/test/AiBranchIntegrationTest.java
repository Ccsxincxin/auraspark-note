package com.auraspark.note.test;

import com.auraspark.note.ai.entity.Conversation;
import com.auraspark.note.ai.entity.Message;
import com.auraspark.note.ai.mapper.ConversationMapper;
import com.auraspark.note.ai.mapper.MessageMapper;
import com.auraspark.note.ai.service.ConversationService;
import com.auraspark.note.common.exception.BusinessException;
import com.auraspark.note.launcher.AurasparkLauncherApplication;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AurasparkLauncherApplication.class)
@Transactional
class AiBranchIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Long USER_ID = 999L;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("INSERT INTO users (id, email, password, status) VALUES (?, ?, ?, 'ACTIVE') ON CONFLICT (id) DO NOTHING",
                USER_ID, "branch-test-" + USER_ID + "@auraspark.com", "test-pass");
    }

    @Test
    void newChat_shouldCreateConversationAndMessages() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        assertNotNull(c.getId());
        assertEquals("New Chat", c.getTitle());

        conversationService.addMessage(c.getId(), "user", "Hello", 10);
        conversationService.addMessage(c.getId(), "assistant", "Hi there", 10);
        conversationService.addMessage(c.getId(), "user", "What's the weather?", 10);
        conversationService.addMessage(c.getId(), "assistant", "It's sunny", 10);

        List<Message> allMessages = conversationService.getMessages(c.getId());
        assertEquals(4, allMessages.size());
        assertTrue(allMessages.stream().allMatch(m -> m.getBranch() == 0));
    }

    @Test
    void editMessage_shouldCreateNewBranchWithHistory() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "Hello", 10);
        Message m2 = conversationService.addMessage(convId, "assistant", "Hi there", 10);
        Message m3 = conversationService.addMessage(convId, "user", "What's the weather?", 10);
        Message m4 = conversationService.addMessage(convId, "assistant", "It's sunny", 10);

        Message edited = conversationService.editMessage(USER_ID, convId, m3.getId(), "How's the weather?");

        assertEquals(1, edited.getBranch());

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);

        List<Message> branch0 = branches.get(0);
        assertEquals(4, branch0.size());
        assertEquals("What's the weather?", branch0.get(2).getContent());

        List<Message> branch1 = branches.get(1);
        assertEquals(3, branch1.size());
        assertEquals("Hello", branch1.get(0).getContent());
        assertEquals("Hi there", branch1.get(1).getContent());
        assertEquals("How's the weather?", branch1.get(2).getContent());
    }

    @Test
    void editMessage_shouldSupportMultipleEditsFromSameMessage() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "Hello", 10);
        conversationService.addMessage(convId, "assistant", "Hi", 10);

        Message edit1 = conversationService.editMessage(USER_ID, convId, m1.getId(), "Hey");
        assertEquals(1, edit1.getBranch());

        Message edit2 = conversationService.editMessage(USER_ID, convId, m1.getId(), "Yo");
        assertEquals(2, edit2.getBranch());

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(3, branches.size());
        assertEquals("Hey", branches.get(1).get(0).getContent());
        assertEquals("Yo", branches.get(2).get(0).getContent());
    }

    @Test
    void editMessage_shouldPreserveOldBranch() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        conversationService.addMessage(convId, "user", "A", 10);
        conversationService.addMessage(convId, "assistant", "B", 10);
        Message target = conversationService.addMessage(convId, "user", "C", 10);
        conversationService.addMessage(convId, "assistant", "D", 10);

        conversationService.editMessage(USER_ID, convId, target.getId(), "C'");

        List<Message> branch0 = conversationService.getBranchMessages(convId).get(0);
        assertEquals(4, branch0.size());
        assertEquals("C", branch0.get(2).getContent());
        assertEquals("D", branch0.get(3).getContent());
    }

    @Test
    void editLastUserMessage_shouldCreateBranchWithNoExtraReply() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message first = conversationService.addMessage(convId, "user", "Hello", 10);
        conversationService.addMessage(convId, "assistant", "Hi", 10);
        Message lastUser = conversationService.addMessage(convId, "user", "How are you?", 10);
        conversationService.addMessage(convId, "assistant", "I'm fine", 10);

        Message edited = conversationService.editMessage(USER_ID, convId, lastUser.getId(), "What's up?");

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        List<Message> branch1 = branches.get(1);
        assertEquals(3, branch1.size());
        assertEquals("Hello", branch1.get(0).getContent());
        assertEquals("Hi", branch1.get(1).getContent());
        assertEquals("What's up?", branch1.get(2).getContent());
    }

    @Test
    void editFirstMessage_shouldCreateBranchWithOnlyEditedMessage() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message first = conversationService.addMessage(convId, "user", "Hello", 10);
        conversationService.addMessage(convId, "assistant", "Hi", 10);

        Message edited = conversationService.editMessage(USER_ID, convId, first.getId(), "Hey");

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        List<Message> branch1 = branches.get(1);
        assertEquals(1, branch1.size());
        assertEquals("Hey", branch1.get(0).getContent());
    }

    @Test
    void editMessage_shouldThrowWhenWrongUser() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();
        Message m = conversationService.addMessage(convId, "user", "Hello", 10);

        assertThrows(Exception.class, () ->
                conversationService.editMessage(9999L, convId, m.getId(), "Hi"));
    }

    @Test
    void continueChat_shouldAddToActiveBranch() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "Hello", 10);
        conversationService.addMessage(convId, "assistant", "Hi", 10);

        Message edited = conversationService.editMessage(USER_ID, convId, m1.getId(), "Hey");

        assertEquals(1, edited.getBranch());
        assertEquals(1, conversationService.getActiveBranch(convId));
    }

    @Test
    void editOnExistingBranch_shouldCreateDeeperBranch() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "A", 10);
        conversationService.addMessage(convId, "assistant", "B", 10);
        Message m3 = conversationService.addMessage(convId, "user", "C", 10);

        Message edit1 = conversationService.editMessage(USER_ID, convId, m1.getId(), "A'");
        assertEquals(1, edit1.getBranch());

        Message edit2 = conversationService.editMessage(USER_ID, convId, edit1.getId(), "A''");
        assertEquals(2, edit2.getBranch());

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(3, branches.size());
        assertEquals("A''", branches.get(2).get(0).getContent());
    }

    @Test
    void treeBranchStructure_multipleEditPoints() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "A", 10);
        Message m2 = conversationService.addMessage(convId, "assistant", "B", 10);
        Message m3 = conversationService.addMessage(convId, "user", "C", 10);

        Message e1 = conversationService.editMessage(USER_ID, convId, m1.getId(), "A'");
        Message e2 = conversationService.editMessage(USER_ID, convId, m3.getId(), "C'");

        assertEquals(1, e1.getBranch());
        assertEquals(2, e2.getBranch());

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(3, branches.size());

        List<Message> branch1 = branches.get(1);
        assertEquals(1, branch1.size());
        assertEquals("A'", branch1.get(0).getContent());

        List<Message> branch2 = branches.get(2);
        assertEquals(3, branch2.size());
        assertEquals("A", branch2.get(0).getContent());
        assertEquals("B", branch2.get(1).getContent());
        assertEquals("C'", branch2.get(2).getContent());
    }

    @Test
    void listConversations_shouldReturnAllConversations() {
        Conversation c1 = conversationService.createConversation(USER_ID, "Chat One");
        conversationService.createConversation(USER_ID, "Chat Two");

        List<Conversation> list = conversationService.listConversations(USER_ID);
        assertTrue(list.size() >= 2);
        assertTrue(list.stream().anyMatch(conv -> "Chat One".equals(conv.getTitle())));
        assertTrue(list.stream().anyMatch(conv -> "Chat Two".equals(conv.getTitle())));
    }

    @Test
    void fullFlow_createChatEditAndContinue() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "Hello", 5);
        conversationService.addMessage(convId, "assistant", "Hi!", 5);

        assertEquals(0, conversationService.getActiveBranch(convId));

        Message edited = conversationService.editMessage(USER_ID, convId, m1.getId(), "Hey");
        assertEquals(1, edited.getBranch());

        conversationService.addMessage(convId, "user", "What's up?", 5, 1);
        conversationService.addMessage(convId, "assistant", "All good!", 5, 1);

        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(2, branches.size());

        List<Message> branch1 = branches.get(1);
        assertEquals(3, branch1.size());
        assertEquals("Hey", branch1.get(0).getContent());
        assertEquals("What's up?", branch1.get(1).getContent());
        assertEquals("All good!", branch1.get(2).getContent());

        assertEquals(2, branches.get(0).size());
    }

    @Test
    void editAssistantMessage_shouldThrow() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        conversationService.addMessage(convId, "user", "Hello", 5);
        Message reply = conversationService.addMessage(convId, "assistant", "Hi there", 5);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.editMessage(USER_ID, convId, reply.getId(), "edited"));
        assertTrue(ex.getMessage().contains("Only user messages"));
    }

    @Test
    void deleteConversation_shouldRemoveAllMessages() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        conversationService.addMessage(convId, "user", "Hello", 5);
        conversationService.addMessage(convId, "assistant", "Hi", 5);
        conversationService.editMessage(USER_ID, convId,
                conversationService.getMessages(convId).get(0).getId(), "Hey");

        conversationService.deleteConversation(USER_ID, convId);

        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getConversationId, convId));
        assertTrue(messages.isEmpty());

        assertNull(conversationMapper.selectById(convId));
    }

    @Test
    void deleteMessageChain_shouldDeleteSubsequentMessages() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        conversationService.addMessage(convId, "user", "A", 5);
        conversationService.addMessage(convId, "assistant", "B", 5);
        Message target = conversationService.addMessage(convId, "user", "C", 5);
        conversationService.addMessage(convId, "assistant", "D", 5);

        conversationService.deleteMessageChain(USER_ID, convId, target.getId());

        List<Message> remaining = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, convId)
                        .eq(Message::getDeleted, false));
        assertEquals(2, remaining.size());
        assertEquals("A", remaining.get(0).getContent());
        assertEquals("B", remaining.get(1).getContent());

        List<Message> all = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, convId));
        assertEquals(4, all.size());
        assertTrue(all.get(2).getDeleted());
        assertTrue(all.get(3).getDeleted());
    }

    @Test
    void updateTitle_shouldChangeTitle() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        assertEquals("New Chat", c.getTitle());

        conversationService.updateTitle(USER_ID, c.getId(), "My AI Chat");

        Conversation updated = conversationMapper.selectById(c.getId());
        assertEquals("My AI Chat", updated.getTitle());
    }

    @Test
    void getBranchMessages_shouldOnlyReturnNonDeleted() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        Message m1 = conversationService.addMessage(convId, "user", "Hello", 5);
        conversationService.addMessage(convId, "assistant", "Hi", 5);

        Map<Integer, List<Message>> before = conversationService.getBranchMessages(convId);
        assertEquals(1, before.size());
        assertEquals(2, before.get(0).size());

        conversationService.deleteMessageChain(USER_ID, convId, m1.getId());

        Map<Integer, List<Message>> after = conversationService.getBranchMessages(convId);
        assertEquals(0, after.size());
    }

    @Test
    void editAssistantMessage_shouldRollbackTransaction() {
        Conversation c = conversationService.createConversation(USER_ID, null);
        Long convId = c.getId();

        conversationService.addMessage(convId, "user", "Hello", 5);
        conversationService.addMessage(convId, "assistant", "Hi", 5);
        conversationService.addMessage(convId, "user", "How are you?", 5);
        conversationService.addMessage(convId, "assistant", "I'm good", 5);

        // Find the first assistant message and try to edit it
        List<Message> allMessages = conversationService.getMessages(convId);
        Message assistantMsg = allMessages.get(1);
        assertEquals("assistant", assistantMsg.getRole());

        assertThrows(BusinessException.class, () ->
                conversationService.editMessage(USER_ID, convId, assistantMsg.getId(), "edited"));

        // Verify no partial branch data was left behind
        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(1, branches.size());
        assertEquals(4, branches.get(0).size());
    }

    @Test
    void fullCrudLifecycle_withRollbackCheck() {
        // ---- CREATE ----
        Conversation c = conversationService.createConversation(USER_ID, "CRUD Test");
        Long convId = c.getId();
        assertNotNull(convId);

        conversationService.addMessage(convId, "user", "Q1", 5);
        conversationService.addMessage(convId, "assistant", "A1", 5);
        conversationService.addMessage(convId, "user", "Q2", 5);
        conversationService.addMessage(convId, "assistant", "A2", 5);

        // ---- READ ----
        List<Conversation> convs = conversationService.listConversations(USER_ID);
        assertTrue(convs.stream().anyMatch(x -> x.getId().equals(convId)));
        assertEquals(1, conversationService.getBranchMessages(convId).size());

        // ---- UPDATE (edit) ----
        List<Message> allMsgs = conversationService.getMessages(convId);
        Message q2 = allMsgs.get(2);
        assertEquals("Q2", q2.getContent());

        Message edited = conversationService.editMessage(USER_ID, convId, q2.getId(), "Q2 edited");
        assertEquals(1, edited.getBranch());

        // ---- READ after edit ----
        Map<Integer, List<Message>> branches = conversationService.getBranchMessages(convId);
        assertEquals(3, branches.get(1).size());
        assertEquals("Q2 edited", branches.get(1).get(2).getContent());

        // ---- UPDATE (title) ----
        conversationService.updateTitle(USER_ID, convId, "Updated Title");
        Conversation updatedConv = conversationMapper.selectById(convId);
        assertEquals("Updated Title", updatedConv.getTitle());

        // ---- DELETE (message chain) ----
        conversationService.deleteMessageChain(USER_ID, convId, edited.getId());
        Map<Integer, List<Message>> branchesAfterDelete = conversationService.getBranchMessages(convId);
        List<Message> branch1after = branchesAfterDelete.get(1);
        assertNotNull(branch1after);
        assertEquals(2, branch1after.size());
        assertTrue(branch1after.stream().noneMatch(m -> m.getContent().equals("Q2 edited")));

        // ---- DELETE (conversation) ----
        conversationService.deleteConversation(USER_ID, convId);
        assertNull(conversationMapper.selectById(convId));
        assertTrue(messageMapper.selectList(
                new LambdaQueryWrapper<Message>().eq(Message::getConversationId, convId)).isEmpty());
    }
}
