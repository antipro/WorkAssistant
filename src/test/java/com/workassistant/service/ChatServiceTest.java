package com.workassistant.service;

import com.workassistant.model.Channel;
import com.workassistant.model.Message;
import com.workassistant.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatService
 */
class ChatServiceTest {
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // Create a new instance for each test
        chatService = ChatService.getInstance();
    }

    @Test
    void testCreateUser() {
        User user = chatService.createUser("TestUser");
        
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("TestUser", user.getNickname());
        assertTrue(user.isOnline());
    }

    @Test
    void testCreateUserCreatesPrivateChannel() {
        User user = chatService.createUser("Alice");
        
        Channel privateChannel = chatService.getPrivateChannelForUser(user.getId());
        assertNotNull(privateChannel);
        assertTrue(privateChannel.isPrivate());
        assertTrue(privateChannel.getName().contains("AI Assistant"));
        
        // Check welcome message exists
        List<Message> messages = chatService.getChannelMessages(privateChannel.getId());
        assertFalse(messages.isEmpty());
        assertEquals("eking", messages.get(0).getUsername());
    }

    @Test
    void testCreateChannel() {
        User user = chatService.createUser("Bob");
        Channel channel = chatService.createChannel("test-channel", user.getId());
        
        assertNotNull(channel);
        assertNotNull(channel.getId());
        assertEquals("test-channel", channel.getName());
        assertFalse(channel.isPrivate());
    }

    @Test
    void testSendMessage() {
        User user = chatService.createUser("Charlie");
        Channel channel = chatService.createChannel("test", user.getId());
        
        Message message = chatService.sendMessage(channel.getId(), user.getId(), "Hello World");
        
        assertNotNull(message);
        assertEquals("Hello World", message.getContent());
        assertEquals("Charlie", message.getUsername());
        assertEquals(Message.MessageType.USER, message.getType());
    }

    @Test
    void testSendAIMessage() {
        User user = chatService.createUser("Dave");
        Channel channel = chatService.createChannel("ai-test", user.getId());
        
        Message message = chatService.sendAIMessage(channel.getId(), "AI response here");
        
        assertNotNull(message);
        assertEquals("AI response here", message.getContent());
        assertEquals("eking", message.getUsername());
        assertEquals(Message.MessageType.AI, message.getType());
    }

    @Test
    void testGetUserChannels() {
        User user = chatService.createUser("Eve");
        chatService.createChannel("public-channel", user.getId());
        
        List<Channel> channels = chatService.getUserChannels(user.getId());
        
        // Should have: general, private channel, and the new public channel
        assertTrue(channels.size() >= 3);
        assertTrue(channels.stream().anyMatch(c -> c.getName().equals("general")));
        assertTrue(channels.stream().anyMatch(c -> c.isPrivate()));
        assertTrue(channels.stream().anyMatch(c -> c.getName().equals("public-channel")));
    }

    @Test
    void testGetOnlineUsers() {
        chatService.createUser("User1");
        chatService.createUser("User2");
        
        List<User> onlineUsers = chatService.getOnlineUsers();
        assertTrue(onlineUsers.size() >= 2);
    }

    @Test
    void testGetChannelMessages() {
        User user = chatService.createUser("Frank");
        Channel channel = chatService.createChannel("msg-test", user.getId());
        
        chatService.sendMessage(channel.getId(), user.getId(), "Message 1");
        chatService.sendMessage(channel.getId(), user.getId(), "Message 2");
        chatService.sendMessage(channel.getId(), user.getId(), "Message 3");
        
        List<Message> messages = chatService.getChannelMessages(channel.getId());
        assertEquals(3, messages.size());
    }

    @Test
    void testGetChannelMessagesWithLimit() {
        User user = chatService.createUser("Grace");
        Channel channel = chatService.createChannel("limit-test", user.getId());
        
        for (int i = 1; i <= 10; i++) {
            chatService.sendMessage(channel.getId(), user.getId(), "Message " + i);
        }
        
        List<Message> messages = chatService.getChannelMessages(channel.getId(), 5);
        assertEquals(5, messages.size());
        // Should get the last 5 messages
        assertEquals("Message 6", messages.get(0).getContent());
        assertEquals("Message 10", messages.get(4).getContent());
    }
    
    @Test
    void testIsNicknameExists() {
        chatService.createUser("UniqueAlice123");
        
        assertTrue(chatService.isNicknameExists("UniqueAlice123"));
        assertTrue(chatService.isNicknameExists("uniquealice123")); // Case insensitive
        assertFalse(chatService.isNicknameExists("NonExistentNickname999"));
    }
    
    @Test
    void testRemoveUser() {
        User user = chatService.createUser("TestRemove");
        String userId = user.getId();
        String privateChannelId = "private-" + userId;
        
        // Verify user and private channel exist
        assertNotNull(chatService.getUser(userId));
        assertNotNull(chatService.getChannel(privateChannelId));
        
        // Remove user
        chatService.removeUser(userId);
        
        // Verify user and private channel are removed
        assertNull(chatService.getUser(userId));
        assertNull(chatService.getChannel(privateChannelId));
    }
    
    @Test
    void testRemoveNonExistentUser() {
        // Should not throw exception
        chatService.removeUser("non-existent-id");
    }
}
