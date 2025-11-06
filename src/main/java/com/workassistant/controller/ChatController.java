package com.workassistant.controller;

import com.workassistant.model.ApiResponse;
import com.workassistant.model.Channel;
import com.workassistant.model.Message;
import com.workassistant.model.User;
import com.workassistant.model.JobType;
import com.workassistant.model.SummaryDocument;
import com.workassistant.model.OllamaResponse;
import com.workassistant.model.ClipboardData;
import com.workassistant.model.ClipboardContentDocument;
import com.workassistant.service.ChatService;
import com.workassistant.service.OllamaService;
import com.workassistant.service.ElasticsearchService;
import com.workassistant.service.ZentaoFunctionProvider;
import com.workassistant.service.ZentaoService;
import com.workassistant.service.OCRService;
import io.javalin.http.Context;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsMessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for chat operations
 */
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final int MIN_KEYWORD_LENGTH = 3;
    private static final String WORK_IMAGES_DIR = "work/images";
    
    private final ChatService chatService;
    private final OllamaService ollamaService;
    private final ZentaoService zentaoService;
    private final ElasticsearchService elasticsearchService;
    private final OCRService ocrService;
    private final ExecutorService aiExecutor;
    private final ScheduledExecutorService sessionCleanup;
    private final Map<String, java.util.concurrent.atomic.AtomicInteger> sessionCounts;
    // Support multiple websocket sessions per user (e.g., multiple tabs)
    private final Map<String, java.util.Set<io.javalin.websocket.WsConnectContext>> userSessions;
    private final ObjectMapper objectMapper;

    public ChatController(ChatService chatService, OllamaService ollamaService) {
        this.chatService = chatService;
        this.ollamaService = ollamaService;
        this.zentaoService = new ZentaoService();
        this.elasticsearchService = ElasticsearchService.getInstance();
        this.ocrService = OCRService.getInstance();
        this.aiExecutor = Executors.newFixedThreadPool(5);
    this.sessionCleanup = Executors.newSingleThreadScheduledExecutor();
    this.sessionCounts = new ConcurrentHashMap<>();
    this.userSessions = new ConcurrentHashMap<>();
    this.objectMapper = new ObjectMapper();
    // Register JavaTimeModule to support Java 8 date/time types (e.g., LocalDateTime)
    this.objectMapper.registerModule(new JavaTimeModule());
    // Write dates as ISO-8601 strings instead of timestamps
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Create work images directory
        createWorkImagesDirectory();
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
            // Mark the user as online immediately upon successful login
            chatService.setUserOnline(user.getId(), true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("privateChannel", chatService.getPrivateChannelForUser(user.getId()));
            
            ctx.json(ApiResponse.success(response));
            // Broadcast user list update so existing connected clients see the new user
            broadcastUserListUpdate();
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
            
            // Return a consistent payload with formatted timestamp for HTTP clients
            ctx.json(ApiResponse.success(convertMessageToMap(message)));
        } catch (Exception e) {
            logger.error("Error sending message", e);
            ctx.json(ApiResponse.error("Failed to send message: " + e.getMessage()));
        }
    }

    private Map<String, Object> convertMessageToMap(Message m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("channelId", m.getChannelId());
        map.put("userId", m.getUserId());
        map.put("username", m.getUsername());
        map.put("content", m.getContent());
        // Format timestamp as ISO_OFFSET_DATE_TIME
        String ts = m.getTimestamp()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        map.put("timestamp", ts);
        map.put("type", m.getType().name());
        return map;
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
                    // Regular chat response with Zentao function calling support
                    String zentaoTools = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
                    OllamaResponse response = ollamaService.generateChatWithTools(prompt, zentaoTools);
                    String aiResponse = response.getResponse();
                    
                    // Check if the response is a function call
                    if (aiResponse != null && aiResponse.startsWith("FUNCTION_CALL:")) {
                        // Model wants to call a Zentao function
                        handleFunctionCall(channelId, aiResponse, prompt);
                    } else {
                        // Regular text response
                        Message aiMessage = chatService.sendAIMessage(channelId, aiResponse);
                        // Broadcast AI message to all connected clients
                        if (aiMessage != null) {
                            broadcastMessage(aiMessage);
                        }
                        logger.info("AI response sent to channel: {}", channelId);
                    }
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
            // Get original user message content
            String originalUserMessage = userMessage.getContent();
            
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
            // Get original user message content
            String originalUserMessage = userMessage.getContent();
            
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
    
    /**
     * Handle function calls from the AI model
     */
    private void handleFunctionCall(String channelId, String functionCallResponse, String originalPrompt) {
        try {
            logger.info("Handling function call for channel: {}", channelId);
            
            // Parse the function call JSON (remove "FUNCTION_CALL:" prefix)
            String functionCallJson = functionCallResponse.substring("FUNCTION_CALL:".length()).trim();
            JsonNode toolCalls = objectMapper.readTree(functionCallJson);
            
            if (!toolCalls.isArray() || toolCalls.size() == 0) {
                Message errorMsg = chatService.sendAIMessage(channelId, 
                    "⚠️ I wanted to call a function but the format was incorrect.");
                if (errorMsg != null) {
                    broadcastMessage(errorMsg);
                }
                return;
            }
            
            // Process the first tool call (for simplicity)
            JsonNode toolCall = toolCalls.get(0);
            JsonNode functionNode = toolCall.get("function");
            
            if (functionNode == null) {
                Message errorMsg = chatService.sendAIMessage(channelId, 
                    "⚠️ Function call format error: no function node found.");
                if (errorMsg != null) {
                    broadcastMessage(errorMsg);
                }
                return;
            }
            
            String functionName = functionNode.get("name").asText();
            JsonNode argumentsNode = functionNode.get("arguments");
            
            logger.info("AI wants to call function: {} with args: {}", functionName, argumentsNode);
            
            // Execute the appropriate Zentao function
            String functionResult = executeFunctionCall(functionName, argumentsNode);
            
            // Send the function result back to Ollama to get a regularized natural language response
            String zentaoTools = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
            OllamaResponse finalResponse = ollamaService.continueConversationWithFunctionResult(
                originalPrompt, toolCalls, functionResult, zentaoTools);
            
            // Send the regularized AI response to the user
            String regularizedAnswer = finalResponse.getResponse();
            Message aiMessage = chatService.sendAIMessage(channelId, regularizedAnswer);
            if (aiMessage != null) {
                broadcastMessage(aiMessage);
            }
            logger.info("Function result sent to Ollama and regularized response delivered to channel: {}", channelId);
            
        } catch (Exception e) {
            logger.error("Error handling function call", e);
            Message errorMsg = chatService.sendAIMessage(channelId, 
                "⚠️ Error executing function call: " + e.getMessage());
            if (errorMsg != null) {
                broadcastMessage(errorMsg);
            }
        }
    }
    
    /**
     * Execute the Zentao function based on the function name
     */
    private String executeFunctionCall(String functionName, JsonNode arguments) {
        try {
            switch (functionName) {
                case "get_projects":
                    return zentaoService.getProjects();
                    
                case "get_tasks":
                    Map<String, String> taskParams = parseArgumentsToMap(arguments);
                    return zentaoService.getTasks(taskParams);
                    
                case "get_bugs":
                    Map<String, String> bugParams = parseArgumentsToMap(arguments);
                    return zentaoService.getBugs(bugParams);
                    
                default:
                    return "⚠️ Unknown function: " + functionName;
            }
        } catch (Exception e) {
            logger.error("Error executing function: {}", functionName, e);
            return "⚠️ Error executing " + functionName + ": " + e.getMessage();
        }
    }
    
    /**
     * Parse function arguments JSON to a Map
     */
    private Map<String, String> parseArgumentsToMap(JsonNode arguments) {
        Map<String, String> params = new HashMap<>();
        if (arguments != null && arguments.isObject()) {
            arguments.fields().forEachRemaining(entry -> {
                params.put(entry.getKey(), entry.getValue().asText());
            });
        }
        return params;
    }
    
    // WebSocket handlers
    public void handleWebSocketConnect(WsConnectContext ctx) {
        String userId = ctx.queryParam("userId");
        if (userId != null && !userId.isEmpty()) {

            // Add this websocket session to the user's session set
            java.util.Set<WsConnectContext> sessions = userSessions.computeIfAbsent(userId, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
            sessions.add(ctx);

            // Increment session count for this user
            sessionCounts.computeIfAbsent(userId, k -> new java.util.concurrent.atomic.AtomicInteger(0)).incrementAndGet();

            chatService.setUserOnline(userId, true);
            logger.info("User connected via WebSocket: {} (sessions={})", userId, sessionCounts.getOrDefault(userId, new java.util.concurrent.atomic.AtomicInteger(0)).get());

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
            // Decrement session count
            java.util.concurrent.atomic.AtomicInteger cnt = sessionCounts.get(userId);
            if (cnt != null) cnt.decrementAndGet();

            // Schedule a delayed check before marking user offline to avoid transient disconnects
            sessionCleanup.schedule(() -> {
                java.util.concurrent.atomic.AtomicInteger current = sessionCounts.get(userId);
                int remaining = (current == null) ? 0 : current.get();
                if (remaining <= 0) {
                    // No remaining sessions; remove mappings and mark offline
                    userSessions.remove(userId);
                    sessionCounts.remove(userId);
                    chatService.setUserOnline(userId, false);
                    logger.info("User disconnected via WebSocket and marked offline after delay: {}", userId);
                    broadcastUserListUpdate();
                } else {
                    logger.info("User {} still has {} sessions; not marking offline", userId, remaining);
                }
            }, 3, TimeUnit.SECONDS);
        }
    }
    
    private synchronized void removeUserSession(String userId) {
    // Only remove the session mapping. Do not remove the user record to avoid
    // users disappearing from the UI on transient disconnects.
    userSessions.remove(userId);
    // Mark user offline
    chatService.setUserOnline(userId, false);
    }
    
    private void broadcastUserListUpdate() {
        try {
            List<User> onlineUsers = chatService.getOnlineUsers();
            // For each connected user, send a personalized users_update where that user
            // appears first and each user entry includes an isCurrent flag.
            for (Map.Entry<String, java.util.Set<WsConnectContext>> entry : userSessions.entrySet()) {
                String recipientUserId = entry.getKey();
                java.util.Set<WsConnectContext> sessions = entry.getValue();

                // Build personalized list: put recipient first and add isCurrent flag
                List<Map<String, Object>> personalized = buildPersonalizedUserList(onlineUsers, recipientUserId);

                Map<String, Object> message = new HashMap<>();
                message.put("type", "users_update");
                message.put("users", personalized);

                String jsonMessage = objectMapper.writeValueAsString(message);

                for (WsConnectContext session : sessions) {
                    try {
                        session.send(jsonMessage);
                    } catch (Exception e) {
                        logger.error("Error sending user list update to client, removing session", e);
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting user list update", e);
        }
    }

    private List<Map<String, Object>> buildPersonalizedUserList(List<User> users, String currentUserId) {
        List<Map<String, Object>> list = new ArrayList<>();

        // First, find the current user (if present) and add it first
        users.stream()
            .filter(u -> u.getId().equals(currentUserId))
            .findFirst()
            .ifPresent(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("nickname", u.getNickname());
                // Format joinedAt as ISO_OFFSET_DATE_TIME (includes offset) for reliable JS parsing
                String joinedAtStr = u.getJoinedAt()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                m.put("joinedAt", joinedAtStr);
                m.put("online", u.isOnline());
                m.put("isCurrent", true);
                list.add(m);
            });

        // Add the rest (excluding current user)
        users.stream()
            .filter(u -> !u.getId().equals(currentUserId))
            .forEach(u -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("nickname", u.getNickname());
                String joinedAtStr = u.getJoinedAt()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                m.put("joinedAt", joinedAtStr);
                m.put("online", u.isOnline());
                m.put("isCurrent", false);
                list.add(m);
            });

        return list;
    }
    
    public void broadcastMessage(Message message) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "new_message");
            wsMessage.put("message", message);
            
            String jsonMessage = objectMapper.writeValueAsString(wsMessage);
            
            // Send to all connected clients (all sessions for each user)
            for (java.util.Set<WsConnectContext> sessions : userSessions.values()) {
                for (WsConnectContext session : sessions) {
                    try {
                        session.send(jsonMessage);
                    } catch (Exception e) {
                        logger.error("Error broadcasting message to client, removing session", e);
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting message", e);
        }
    }

    /**
     * Broadcast a message to all connected clients, optionally excluding sessions belonging
     * to a specific user (useful to avoid echoing a user's own message back to them).
     */
    public void broadcastMessage(Message message, String excludeUserId) {
        try {
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("type", "new_message");
            wsMessage.put("message", message);

            String jsonMessage = objectMapper.writeValueAsString(wsMessage);

            // Send to all connected clients (all sessions for each user)
            for (Map.Entry<String, java.util.Set<WsConnectContext>> entry : userSessions.entrySet()) {
                String userId = entry.getKey();
                if (excludeUserId != null && excludeUserId.equals(userId)) {
                    // Skip all sessions for the excluded user
                    continue;
                }
                java.util.Set<WsConnectContext> sessions = entry.getValue();
                for (WsConnectContext session : sessions) {
                    try {
                        session.send(jsonMessage);
                    } catch (Exception e) {
                        logger.error("Error broadcasting message to client, removing session", e);
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error broadcasting message", e);
        }
    }
    
    /**
     * Create work images directory if it doesn't exist
     */
    private void createWorkImagesDirectory() {
        try {
            java.io.File dir = new java.io.File(WORK_IMAGES_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
                logger.info("Created work images directory: {}", WORK_IMAGES_DIR);
            }
        } catch (Exception e) {
            logger.error("Failed to create work images directory", e);
        }
    }
    
    /**
     * Handle clipboard content submission
     */
    public void sendClipboardContent(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String channelId = (String) body.get("channelId");
            String userId = (String) body.get("userId");
            Map<String, Object> contentMap = (Map<String, Object>) body.get("content");
            
            if (channelId == null || userId == null || contentMap == null) {
                ctx.json(ApiResponse.error("Channel ID, user ID, and content are required"));
                return;
            }
            
            // Parse clipboard content
            ClipboardData clipboardData = parseClipboardContent(contentMap);
            
            // Send initial message
            Message message = chatService.sendClipboardMessage(channelId, userId, clipboardData);
            if (message == null) {
                ctx.json(ApiResponse.error("Failed to send clipboard content"));
                return;
            }
            
            // Broadcast message to all connected clients
            broadcastMessage(message);
            
            // Process clipboard content asynchronously
            processClipboardContent(channelId, userId, message.getId(), clipboardData);
            
            ctx.json(ApiResponse.success(convertMessageToMap(message)));
        } catch (Exception e) {
            logger.error("Error sending clipboard content", e);
            ctx.json(ApiResponse.error("Failed to send clipboard content: " + e.getMessage()));
        }
    }
    
    /**
     * Parse clipboard content from request
     */
    private ClipboardData parseClipboardContent(Map<String, Object> contentMap) throws Exception {
        ClipboardData data = new ClipboardData();
        
        // Parse text
        if (contentMap.containsKey("text")) {
            data.setText((String) contentMap.get("text"));
        }
        
        // Parse images
        if (contentMap.containsKey("images")) {
            List<Map<String, String>> imagesList = (List<Map<String, String>>) contentMap.get("images");
            for (Map<String, String> imageMap : imagesList) {
                String base64Data = imageMap.get("data");
                String type = imageMap.get("type");
                
                // Save image and get path
                String imagePath = saveBase64Image(base64Data, type);
                
                ClipboardData.ClipboardImage image = new ClipboardData.ClipboardImage();
                image.setPath(imagePath);
                image.setType(type);
                data.addImage(image);
            }
        }
        
        return data;
    }
    
    /**
     * Save base64 encoded image to disk
     */
    private String saveBase64Image(String base64Data, String type) throws Exception {
        // Remove data URI prefix if present
        String base64Image = base64Data;
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        
        // Decode base64
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
        
        // Generate unique filename
        String extension = type.split("/")[1];
        String filename = UUID.randomUUID().toString() + "." + extension;
        String relativePath = filename;
        String fullPath = WORK_IMAGES_DIR + "/" + filename;
        
        // Save to disk
        java.io.File outputFile = new java.io.File(fullPath);
        java.nio.file.Files.write(outputFile.toPath(), imageBytes);
        
        logger.info("Saved image: {}", fullPath);
        return relativePath;
    }
    
    /**
     * Serve image files
     */
    public void serveImage(Context ctx) {
        try {
            String imagePath = ctx.pathParam("imagePath");
            String fullPath = WORK_IMAGES_DIR + "/" + imagePath;
            
            java.io.File imageFile = new java.io.File(fullPath);
            if (!imageFile.exists()) {
                ctx.status(404).result("Image not found");
                return;
            }
            
            // Determine content type from file extension
            String contentType = "image/jpeg";
            if (imagePath.endsWith(".png")) {
                contentType = "image/png";
            } else if (imagePath.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (imagePath.endsWith(".webp")) {
                contentType = "image/webp";
            }
            
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());
            ctx.contentType(contentType).result(imageBytes);
        } catch (Exception e) {
            logger.error("Error serving image", e);
            ctx.status(500).result("Failed to serve image");
        }
    }
    
    /**
     * Process clipboard content asynchronously
     * - Perform OCR on images to extract keywords
     * - Generate AI title
     * - Store in Elasticsearch
     */
    private void processClipboardContent(String channelId, String userId, String messageId, ClipboardData clipboardData) {
        aiExecutor.submit(() -> {
            try {
                logger.info("Processing clipboard content for message: {}", messageId);
                
                // Extract OCR keywords from images
                List<ClipboardContentDocument.ImageMetadata> imageMetadataList = new ArrayList<>();
                List<String> allKeywords = new ArrayList<>();
                
                if (clipboardData.getImages() != null) {
                    for (ClipboardData.ClipboardImage img : clipboardData.getImages()) {
                        if (ocrService.isAvailable()) {
                            String imagePath = WORK_IMAGES_DIR + "/" + img.getPath();
                            java.io.File imageFile = new java.io.File(imagePath);
                            
                            if (imageFile.exists()) {
                                List<String> keywords = ocrService.extractKeywords(imageFile);
                                img.setKeywords(keywords);
                                allKeywords.addAll(keywords);
                                
                                ClipboardContentDocument.ImageMetadata metadata = 
                                    new ClipboardContentDocument.ImageMetadata(img.getPath(), keywords);
                                imageMetadataList.add(metadata);
                                
                                logger.info("Extracted {} keywords from image: {}", keywords.size(), img.getPath());
                            }
                        }
                    }
                }
                
                // Extract keywords from text if available
                if (clipboardData.getText() != null && !clipboardData.getText().isEmpty()) {
                    String[] words = clipboardData.getText().toLowerCase()
                        .replaceAll("[^a-zA-Z0-9\\s]", " ")
                        .split("\\s+");
                    
                    for (String word : words) {
                        if (word.length() > MIN_KEYWORD_LENGTH) {
                            allKeywords.add(word);
                        }
                    }
                }
                
                // Generate AI title
                String title = generateClipboardTitle(clipboardData, allKeywords);
                
                // Create document for Elasticsearch
                ClipboardContentDocument document = new ClipboardContentDocument(
                    UUID.randomUUID().toString(),
                    title,
                    clipboardData.getText(),
                    imageMetadataList,
                    allKeywords,
                    channelId,
                    userId
                );
                
                // Store in Elasticsearch if available
                if (elasticsearchService.isAvailable()) {
                    try {
                        elasticsearchService.indexClipboardContent(document);
                        
                        // Send confirmation message
                        String confirmMsg = "✅ Clipboard content processed and stored!\n\n" +
                            "**Title:** " + title + "\n" +
                            "**Text:** " + (clipboardData.getText() != null && !clipboardData.getText().isEmpty() ? "Yes" : "No") + "\n" +
                            "**Images:** " + (clipboardData.getImages() != null ? clipboardData.getImages().size() : 0) + "\n" +
                            "**Keywords:** " + allKeywords.size();
                        
                        Message aiMessage = chatService.sendAIMessage(channelId, confirmMsg);
                        if (aiMessage != null) {
                            broadcastMessage(aiMessage);
                        }
                        
                        logger.info("Clipboard content indexed: {}", document.getId());
                    } catch (Exception e) {
                        logger.error("Failed to index clipboard content", e);
                        Message errorMsg = chatService.sendAIMessage(channelId, 
                            "⚠️ Clipboard content processed but failed to store in Elasticsearch.");
                        if (errorMsg != null) {
                            broadcastMessage(errorMsg);
                        }
                    }
                } else {
                    logger.warn("Elasticsearch not available for clipboard content");
                    Message warningMsg = chatService.sendAIMessage(channelId, 
                        "⚠️ Clipboard content processed but Elasticsearch is not available for storage.");
                    if (warningMsg != null) {
                        broadcastMessage(warningMsg);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing clipboard content", e);
                Message errorMsg = chatService.sendAIMessage(channelId, 
                    "❌ Error processing clipboard content: " + e.getMessage());
                if (errorMsg != null) {
                    broadcastMessage(errorMsg);
                }
            }
        });
    }
    
    /**
     * Generate AI title for clipboard content
     */
    private String generateClipboardTitle(ClipboardData clipboardData, List<String> keywords) {
        try {
            StringBuilder prompt = new StringBuilder("Generate a short, descriptive title (max 10 words) for the following content:\n\n");
            
            if (clipboardData.getText() != null && !clipboardData.getText().isEmpty()) {
                String text = clipboardData.getText();
                if (text.length() > 500) {
                    text = text.substring(0, 500) + "...";
                }
                prompt.append("Text: ").append(text).append("\n\n");
            }
            
            if (clipboardData.getImages() != null && !clipboardData.getImages().isEmpty()) {
                prompt.append("Images: ").append(clipboardData.getImages().size()).append("\n\n");
            }
            
            if (!keywords.isEmpty()) {
                prompt.append("Keywords: ").append(String.join(", ", keywords.subList(0, Math.min(10, keywords.size())))).append("\n\n");
            }
            
            prompt.append("Reply with ONLY the title, nothing else.");
            
            String aiResponse = ollamaService.generateSimple(prompt.toString());
            
            // Clean up the response - remove quotes, extra whitespace, etc.
            String title = aiResponse.trim()
                .replaceAll("^[\"']|[\"']$", "")  // Remove leading/trailing quotes
                .replaceAll("Title:\\s*", "")      // Remove "Title:" prefix
                .replaceAll("\\n.*", "");          // Remove everything after first newline
            
            // Truncate if too long
            if (title.length() > 100) {
                title = title.substring(0, 97) + "...";
            }
            
            return title.isEmpty() ? "Clipboard Content" : title;
        } catch (Exception e) {
            logger.error("Failed to generate AI title", e);
            return "Clipboard Content";
        }
    }
}
