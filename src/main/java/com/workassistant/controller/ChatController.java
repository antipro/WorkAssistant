package com.workassistant.controller;

import com.workassistant.model.ApiResponse;
import com.workassistant.model.Channel;
import com.workassistant.model.Message;
import com.workassistant.model.User;
import com.workassistant.model.JobType;
import com.workassistant.model.SummaryDocument;
import com.workassistant.service.ChatService;
import com.workassistant.service.OllamaService;
import com.workassistant.service.ElasticsearchService;
import io.javalin.http.Context;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for chat operations
 */
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final int MIN_KEYWORD_LENGTH = 3;
    
    private final ChatService chatService;
    private final OllamaService ollamaService;
    private final ElasticsearchService elasticsearchService;
    private final ExecutorService aiExecutor;
    private final Map<String, WsConnectContext> userSessions;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, OllamaService ollamaService) {
        this.chatService = chatService;
        this.ollamaService = ollamaService;
        this.elasticsearchService = ElasticsearchService.getInstance();
        this.aiExecutor = Executors.newFixedThreadPool(5);
        this.userSessions = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
    }

    public void login(Context ctx) {
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String nickname = body.get("nickname");
            
            if (nickname == null || nickname.trim().isEmpty()) {
                ctx.json(ApiResponse.error("Nickname is required"));
                return;
            }
            
            // Check if nickname already exists
            if (chatService.isNicknameExists(nickname.trim())) {
                ctx.json(ApiResponse.error("Nickname already exists. Please choose a different one."));
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
            
            // Broadcast message to all connected clients via WebSocket
            broadcastMessage(message);
            
            // Trigger AI if message contains @eking or the channel is a private AI assistant channel
            Channel channel = chatService.getChannel(channelId);
            boolean triggerAI = content.contains("@eking") || (channel != null && channel.isPrivate());
            if (triggerAI) {
                handleAIRequest(channelId, content, message);
            }
            
            ctx.json(ApiResponse.success(message));
        } catch (Exception e) {
            logger.error("Error sending message", e);
            ctx.json(ApiResponse.error("Failed to send message: " + e.getMessage()));
        }
    }

    private void handleAIRequest(String channelId, String content, Message userMessage) {
        // Process AI request using thread pool
        aiExecutor.submit(() -> {
            try {
                // Extract the prompt by removing @eking mention
                String prompt = content.replace("@eking", "").trim();
                
                if (prompt.isEmpty()) {
                    prompt = "Hello! How can I help you?";
                }
                
                // Check if user wants to create a summary (contains "summary", "summarize", or "summarise")
                boolean isSummaryRequest = prompt.toLowerCase().contains("summary") 
                    || prompt.toLowerCase().contains("summarize") 
                    || prompt.toLowerCase().contains("summarise");
                
                // Check if user wants to search (contains "search", "find", or "look for")
                boolean isSearchRequest = prompt.toLowerCase().contains("search") 
                    || prompt.toLowerCase().contains("find") 
                    || prompt.toLowerCase().contains("look for")
                    || prompt.toLowerCase().contains("查找")
                    || prompt.toLowerCase().contains("搜索");
                
                if (isSummaryRequest && !isSearchRequest) {
                    handleSummaryRequest(channelId, prompt, userMessage);
                } else if (isSearchRequest) {
                    handleSearchRequest(channelId, prompt, userMessage);
                } else {
                    // Regular chat response
                    String aiResponse = ollamaService.generateSimple(prompt);
                    Message aiMessage = chatService.sendAIMessage(channelId, aiResponse);
                    // Broadcast AI message to all connected clients
                    if (aiMessage != null) {
                        broadcastMessage(aiMessage);
                    }
                    logger.info("AI response sent to channel: {}", channelId);
                }
            } catch (Exception e) {
                logger.error("Error generating AI response", e);
                Message errorMessage = chatService.sendAIMessage(channelId, "Sorry, I encountered an error while processing your request.");
                if (errorMessage != null) {
                    broadcastMessage(errorMessage);
                }
            }
        });
    }
    
    private void handleSummaryRequest(String channelId, String prompt, Message userMessage) {
        try {
            // Create a summary prompt
            String summaryPrompt = "Please create a structured summary in markdown format with the following sections:\n" +
                "1. Title (one line)\n" +
                "2. Summary (markdown formatted)\n" +
                "3. Keywords (comma-separated list)\n\n" +
                "For this request: " + prompt;
            
            String aiResponse = ollamaService.generateSimple(summaryPrompt);
            
            // Parse the AI response to extract title, content, and keywords
            SummaryDocument summaryDoc = parseSummaryResponse(aiResponse, channelId, userMessage.getUserId());
            
            // Store in Elasticsearch if available
            if (elasticsearchService.isAvailable()) {
                try {
                    elasticsearchService.indexSummary(summaryDoc);
                    String successMsg = "✅ Summary created and stored in Elasticsearch!\n\n" +
                        "**Title:** " + summaryDoc.getTitle() + "\n\n" +
                        "**Content:**\n" + summaryDoc.getContent() + "\n\n" +
                        "**Keywords:** " + String.join(", ", summaryDoc.getKeywords());
                    Message aiMessage = chatService.sendAIMessage(channelId, successMsg);
                    if (aiMessage != null) {
                        broadcastMessage(aiMessage);
                    }
                    logger.info("Summary indexed in Elasticsearch: {}", summaryDoc.getId());
                } catch (Exception e) {
                    logger.error("Failed to index summary in Elasticsearch", e);
                    Message aiMessage = chatService.sendAIMessage(channelId, "Summary created but failed to store in Elasticsearch:\n\n" + aiResponse);
                    if (aiMessage != null) {
                        broadcastMessage(aiMessage);
                    }
                }
            } else {
                logger.warn("Elasticsearch not available, sending summary without indexing");
                Message aiMessage = chatService.sendAIMessage(channelId, "⚠️ Elasticsearch is not available. Summary:\n\n" + aiResponse);
                if (aiMessage != null) {
                    broadcastMessage(aiMessage);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling summary request", e);
            Message aiMessage = chatService.sendAIMessage(channelId, "Sorry, I encountered an error while creating the summary.");
            if (aiMessage != null) {
                broadcastMessage(aiMessage);
            }
        }
    }
    
    private SummaryDocument parseSummaryResponse(String aiResponse, String channelId, String userId) {
        // Simple parsing - extract title, content, and keywords
        String title = "AI Summary";
        String content = aiResponse;
        List<String> keywords = new ArrayList<>();
        
        // Try to extract title from first line or header
        String[] lines = aiResponse.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            // Remove markdown heading markers if present
            if (firstLine.startsWith("#")) {
                title = firstLine.replaceAll("^#+\\s*", "").trim();
                // Content is everything after the first line
                content = aiResponse.substring(aiResponse.indexOf("\n") + 1).trim();
            } else {
                title = firstLine;
            }
        }
        
        // Try to extract keywords from the content (look for "Keywords:" or similar)
        Pattern keywordPattern = Pattern.compile("(?i)keywords?:\\s*(.+?)(?:\n|$)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = keywordPattern.matcher(aiResponse);
        if (matcher.find()) {
            String keywordsStr = matcher.group(1).trim();
            String[] keywordArray = keywordsStr.split(",");
            for (String keyword : keywordArray) {
                keywords.add(keyword.trim());
            }
        }
        
        // If no keywords found, extract some basic keywords from the title and content
        if (keywords.isEmpty()) {
            // Add some basic keywords from title
            String[] titleWords = title.split("\\s+");
            for (String word : titleWords) {
                if (word.length() > MIN_KEYWORD_LENGTH) {
                    keywords.add(word.toLowerCase().replaceAll("[^a-z0-9]", ""));
                }
            }
        }
        
        String docId = UUID.randomUUID().toString();
        return new SummaryDocument(docId, title, content, keywords, channelId, userId);
    }
    
    private void handleSearchRequest(String channelId, String prompt, Message userMessage) {
        try {
            // Check if Elasticsearch is available
            if (!elasticsearchService.isAvailable()) {
                Message aiMessage = chatService.sendAIMessage(channelId, 
                    "⚠️ Sorry, the search service is not available. Elasticsearch is not connected.");
                if (aiMessage != null) {
                    broadcastMessage(aiMessage);
                }
                logger.warn("Search request received but Elasticsearch is not available");
                return;
            }
            
            // Extract search keywords from the prompt
            String searchQuery = extractSearchKeywords(prompt);
            
            if (searchQuery.isEmpty()) {
                Message aiMessage = chatService.sendAIMessage(channelId, 
                    "Please provide keywords to search. For example: '@eking search project architecture'");
                if (aiMessage != null) {
                    broadcastMessage(aiMessage);
                }
                return;
            }
            
            // Search Elasticsearch for matching summaries
            List<SummaryDocument> results = elasticsearchService.searchSummaries(searchQuery, 5);
            
            if (results.isEmpty()) {
                // If no results from ES, try to get AI to help
                String aiResponse = ollamaService.generateSimple(
                    "User is asking about: " + searchQuery + ". No previous summaries found. Provide a helpful response.");
                Message aiMessage = chatService.sendAIMessage(channelId, 
                    "🔍 No previous summaries found for: **" + searchQuery + "**\n\n" + aiResponse);
                if (aiMessage != null) {
                    broadcastMessage(aiMessage);
                }
            } else {
                // Format results in markdown
                String formattedResults = formatSearchResults(searchQuery, results);
                Message aiMessage = chatService.sendAIMessage(channelId, formattedResults);
                if (aiMessage != null) {
                    broadcastMessage(aiMessage);
                }
                logger.info("Search results sent for query: {} - {} results", searchQuery, results.size());
            }
        } catch (Exception e) {
            logger.error("Error handling search request", e);
            Message aiMessage = chatService.sendAIMessage(channelId, 
                "Sorry, I encountered an error while searching. Please try again later.");
            if (aiMessage != null) {
                broadcastMessage(aiMessage);
            }
        }
    }
    
    /**
     * Extract search keywords from the prompt
     * Removes common search words like "search", "find", "look for"
     */
    private String extractSearchKeywords(String prompt) {
        String cleaned = prompt.toLowerCase()
            .replaceAll("\\b(search|find|look for|about|查找|搜索)\\b", "")
            .replaceAll("\\s+", " ")
            .trim();
        return cleaned;
    }
    
    /**
     * Format search results in markdown
     */
    private String formatSearchResults(String query, List<SummaryDocument> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 **Search Results for: ").append(query).append("**\n\n");
        sb.append("Found ").append(results.size()).append(" matching ");
        sb.append(results.size() == 1 ? "summary" : "summaries").append(":\n\n");
        sb.append("---\n\n");
        
        int count = 1;
        for (SummaryDocument doc : results) {
            sb.append("### ").append(count).append(". ").append(doc.getTitle()).append("\n\n");
            
            // Add a snippet of the content (first 300 characters)
            String content = doc.getContent();
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            sb.append(content).append("\n\n");
            
            // Add keywords if available
            if (doc.getKeywords() != null && !doc.getKeywords().isEmpty()) {
                sb.append("**Keywords:** ").append(String.join(", ", doc.getKeywords())).append("\n\n");
            }
            
            // Add metadata
            sb.append("*Created: ").append(doc.getTimestamp()).append("*\n\n");
            
            if (count < results.size()) {
                sb.append("---\n\n");
            }
            count++;
        }
        
        return sb.toString();
    }
    
    // WebSocket handlers
    public void handleWebSocketConnect(WsConnectContext ctx) {
        String userId = ctx.queryParam("userId");
        if (userId != null && !userId.isEmpty()) {
            userSessions.put(userId, ctx);
            chatService.setUserOnline(userId, true);
            logger.info("User connected via WebSocket: {}", userId);
            
            // Broadcast user list update to all connected clients
            broadcastUserListUpdate();
        }
    }
    
    public void handleWebSocketMessage(WsMessageContext ctx) {
        try {
            String messageJson = ctx.message();
            Map<String, String> messageData = objectMapper.readValue(messageJson, Map.class);
            
            String type = messageData.get("type");
            if ("ping".equals(type)) {
                // Respond to ping to keep connection alive
                ctx.send("{\"type\":\"pong\"}");
                return;
            }
            
            // Handle other message types if needed
            logger.debug("Received WebSocket message: {}", messageJson);
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
        }
    }
    
    public void handleWebSocketClose(WsCloseContext ctx) {
        String userId = ctx.queryParam("userId");
        if (userId != null && !userId.isEmpty()) {
            // Remove session and user atomically
            removeUserSession(userId);
            logger.info("User disconnected via WebSocket and removed from memory: {}", userId);
            
            // Broadcast user list update to all connected clients
            broadcastUserListUpdate();
        }
    }
    
    private synchronized void removeUserSession(String userId) {
        userSessions.remove(userId);
        chatService.removeUser(userId);
    }
    
    private void broadcastUserListUpdate() {
        try {
            List<User> onlineUsers = chatService.getOnlineUsers();
            Map<String, Object> message = new HashMap<>();
            message.put("type", "users_update");
            message.put("users", onlineUsers);
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            
            // Send to all connected clients
            for (WsConnectContext session : userSessions.values()) {
                try {
                    session.send(jsonMessage);
                } catch (Exception e) {
                    logger.error("Error sending user list update to client", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting user list update", e);
        }
    }
    
    public void broadcastMessage(Message message) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "new_message");
            wsMessage.put("message", message);
            
            String jsonMessage = objectMapper.writeValueAsString(wsMessage);
            
            // Send to all connected clients
            for (WsConnectContext session : userSessions.values()) {
                try {
                    session.send(jsonMessage);
                } catch (Exception e) {
                    logger.error("Error broadcasting message to client", e);
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting message", e);
        }
    }
}
