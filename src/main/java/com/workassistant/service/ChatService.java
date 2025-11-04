package com.workassistant.service;

import com.workassistant.model.Channel;
import com.workassistant.model.Message;
import com.workassistant.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Chat service to manage users, channels, and messages
 */
public class ChatService {
    private static ChatService instance;
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();
    private final Map<String, List<Message>> channelMessages = new ConcurrentHashMap<>();
    
    private ChatService() {
        // Initialize general channel
        Channel generalChannel = new Channel("general", "general", "system", false);
        channels.put("general", generalChannel);
        channelMessages.put("general", new ArrayList<>());
    }
    
    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }
    
    // User management
    public User createUser(String nickname) {
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, nickname);
        users.put(userId, user);
        
        // Create private AI channel for this user
        String privateChannelId = "private-" + userId;
        Channel privateChannel = new Channel(privateChannelId, "AI Assistant (Private)", userId, true);
        privateChannel.addMember(userId);
        channels.put(privateChannelId, privateChannel);
        channelMessages.put(privateChannelId, new ArrayList<>());
        
        // Add welcome message to private channel
        Message welcomeMsg = new Message(
            UUID.randomUUID().toString(),
            privateChannelId,
            "ai-eking",
            "eking",
            "Hello " + nickname + "! I'm eking, your AI assistant. You can ask me anything here or mention me with @eking in any channel.",
            Message.MessageType.AI
        );
        channelMessages.get(privateChannelId).add(welcomeMsg);
        
        return user;
    }
    
    public User getUser(String userId) {
        return users.get(userId);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public List<User> getOnlineUsers() {
        return users.values().stream()
                .filter(User::isOnline)
                .collect(Collectors.toList());
    }
    
    public void setUserOnline(String userId, boolean online) {
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(online);
        }
    }
    
    // Channel management
    public Channel createChannel(String name, String createdBy) {
        String channelId = UUID.randomUUID().toString();
        Channel channel = new Channel(channelId, name, createdBy, false);
        channels.put(channelId, channel);
        channelMessages.put(channelId, new ArrayList<>());
        return channel;
    }
    
    public Channel getChannel(String channelId) {
        return channels.get(channelId);
    }
    
    public List<Channel> getAllChannels() {
        return new ArrayList<>(channels.values());
    }
    
    public List<Channel> getUserChannels(String userId) {
        return channels.values().stream()
                .filter(channel -> !channel.isPrivate() || channel.getMembers().contains(userId) || channel.getId().equals("private-" + userId))
                .collect(Collectors.toList());
    }
    
    public Channel getPrivateChannelForUser(String userId) {
        return channels.get("private-" + userId);
    }
    
    // Message management
    public Message sendMessage(String channelId, String userId, String content) {
        User user = users.get(userId);
        if (user == null) {
            return null;
        }
        
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(messageId, channelId, userId, user.getNickname(), content, Message.MessageType.USER);
        
        List<Message> messages = channelMessages.get(channelId);
        if (messages != null) {
            messages.add(message);
        }
        
        return message;
    }
    
    public Message sendAIMessage(String channelId, String content) {
        String messageId = UUID.randomUUID().toString();
        Message message = new Message(messageId, channelId, "ai-eking", "eking", content, Message.MessageType.AI);
        
        List<Message> messages = channelMessages.get(channelId);
        if (messages != null) {
            messages.add(message);
        }
        
        return message;
    }
    
    public List<Message> getChannelMessages(String channelId) {
        return channelMessages.getOrDefault(channelId, new ArrayList<>());
    }
    
    public List<Message> getChannelMessages(String channelId, int limit) {
        List<Message> messages = channelMessages.getOrDefault(channelId, new ArrayList<>());
        int size = messages.size();
        if (size <= limit) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(size - limit, size));
    }
}
