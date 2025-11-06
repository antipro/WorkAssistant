package com.workassistant.controller;

import com.workassistant.model.Message;
import com.workassistant.model.User;
import com.workassistant.service.ChatService;
import com.workassistant.service.OllamaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatController - focusing on username inclusion in AI requests
 */
class ChatControllerTest {
    private ChatService chatService;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        chatService = ChatService.getInstance();
    }

    @Test
    void testMessageContainsUsername() {
        // Create a test user
        User user = chatService.createUser("TestNickname123");
        String channelId = "test-channel";
        
        // Send a message
        Message message = chatService.sendMessage(channelId, user.getId(), "Hello AI");
        
        // Verify message has username
        assertNotNull(message);
        assertEquals("TestNickname123", message.getUsername());
        assertEquals(user.getId(), message.getUserId());
    }

    @Test
    void testUsernameIsSetInMessage() {
        // Create users with different nicknames
        User user1 = chatService.createUser("Alice456");
        User user2 = chatService.createUser("Bob789");
        
        String channelId = chatService.createChannel("test", user1.getId()).getId();
        
        // Send messages from different users
        Message msg1 = chatService.sendMessage(channelId, user1.getId(), "Message from Alice");
        Message msg2 = chatService.sendMessage(channelId, user2.getId(), "Message from Bob");
        
        // Verify usernames are correctly set
        assertEquals("Alice456", msg1.getUsername());
        assertEquals("Bob789", msg2.getUsername());
    }
}
