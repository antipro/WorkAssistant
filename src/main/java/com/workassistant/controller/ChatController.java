package com.workassistant.controller;

import com.workassistant.model.ApiResponse;
import com.workassistant.model.Channel;
import com.workassistant.model.Message;
import com.workassistant.model.User;
import com.workassistant.service.ChatService;
import com.workassistant.service.OllamaService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for chat operations
 */
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final OllamaService ollamaService;

    public ChatController(ChatService chatService, OllamaService ollamaService) {
        this.chatService = chatService;
        this.ollamaService = ollamaService;
    }

    public void login(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String nickname = body.get("nickname");
            
            if (nickname == null || nickname.trim().isEmpty()) {
                ctx.json(ApiResponse.error("Nickname is required"));
                return;
            }
            
            User user = chatService.createUser(nickname.trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("privateChannel", chatService.getPrivateChannelForUser(user.getId()));
            
            ctx.json(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Error during login", e);
            ctx.json(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    public void getUsers(Context ctx) {
        try {
            List<User> users = chatService.getOnlineUsers();
            ctx.json(ApiResponse.success(users));
        } catch (Exception e) {
            logger.error("Error getting users", e);
            ctx.json(ApiResponse.error("Failed to get users: " + e.getMessage()));
        }
    }

    public void getChannels(Context ctx) {
        try {
            String userId = ctx.queryParam("userId");
            List<Channel> channels;
            
            if (userId != null && !userId.isEmpty()) {
                channels = chatService.getUserChannels(userId);
            } else {
                channels = chatService.getAllChannels();
            }
            
            ctx.json(ApiResponse.success(channels));
        } catch (Exception e) {
            logger.error("Error getting channels", e);
            ctx.json(ApiResponse.error("Failed to get channels: " + e.getMessage()));
        }
    }

    public void createChannel(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String name = body.get("name");
            String userId = body.get("userId");
            
            if (name == null || name.trim().isEmpty()) {
                ctx.json(ApiResponse.error("Channel name is required"));
                return;
            }
            
            if (userId == null || userId.trim().isEmpty()) {
                ctx.json(ApiResponse.error("User ID is required"));
                return;
            }
            
            Channel channel = chatService.createChannel(name.trim(), userId);
            ctx.json(ApiResponse.success(channel));
        } catch (Exception e) {
            logger.error("Error creating channel", e);
            ctx.json(ApiResponse.error("Failed to create channel: " + e.getMessage()));
        }
    }

    public void getMessages(Context ctx) {
        try {
            String channelId = ctx.pathParam("channelId");
            String limitStr = ctx.queryParam("limit");
            
            List<Message> messages;
            if (limitStr != null) {
                int limit = Integer.parseInt(limitStr);
                messages = chatService.getChannelMessages(channelId, limit);
            } else {
                messages = chatService.getChannelMessages(channelId);
            }
            
            ctx.json(ApiResponse.success(messages));
        } catch (Exception e) {
            logger.error("Error getting messages", e);
            ctx.json(ApiResponse.error("Failed to get messages: " + e.getMessage()));
        }
    }

    public void sendMessage(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String channelId = body.get("channelId");
            String userId = body.get("userId");
            String content = body.get("content");
            
            if (channelId == null || userId == null || content == null || content.trim().isEmpty()) {
                ctx.json(ApiResponse.error("Channel ID, user ID, and content are required"));
                return;
            }
            
            // Check if message starts with # to create a new channel
            if (content.trim().startsWith("#")) {
                String channelName = content.trim().substring(1).trim();
                if (!channelName.isEmpty()) {
                    Channel newChannel = chatService.createChannel(channelName, userId);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "channel_created");
                    response.put("channel", newChannel);
                    
                    ctx.json(ApiResponse.success(response));
                    return;
                }
            }
            
            Message message = chatService.sendMessage(channelId, userId, content.trim());
            
            if (message == null) {
                ctx.json(ApiResponse.error("Failed to send message"));
                return;
            }
            
            // Check if message contains @eking to trigger AI response
            if (content.contains("@eking")) {
                handleAIRequest(channelId, content, message);
            }
            
            ctx.json(ApiResponse.success(message));
        } catch (Exception e) {
            logger.error("Error sending message", e);
            ctx.json(ApiResponse.error("Failed to send message: " + e.getMessage()));
        }
    }

    private void handleAIRequest(String channelId, String content, Message userMessage) {
        // Process AI request in a separate thread to not block the response
        new Thread(() -> {
            try {
                // Extract the prompt by removing @eking mention
                String prompt = content.replace("@eking", "").trim();
                
                if (prompt.isEmpty()) {
                    prompt = "Hello! How can I help you?";
                }
                
                // Call Ollama service
                String aiResponse = ollamaService.generateSimple(prompt);
                
                // Send AI response to the channel
                chatService.sendAIMessage(channelId, aiResponse);
                
                logger.info("AI response sent to channel: {}", channelId);
            } catch (Exception e) {
                logger.error("Error generating AI response", e);
                chatService.sendAIMessage(channelId, "Sorry, I encountered an error while processing your request.");
            }
        }).start();
    }
}
