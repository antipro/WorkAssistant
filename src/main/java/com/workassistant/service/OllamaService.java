package com.workassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workassistant.config.AppConfig;
import com.workassistant.config.SystemMessageConfig;
import com.workassistant.model.OllamaRequest;
import com.workassistant.model.OllamaResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service to interact with Ollama REST API
 */
public class OllamaService {
    private static final Logger logger = LoggerFactory.getLogger(OllamaService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final AppConfig config;
    private final SystemMessageConfig systemMessageConfig;
    private final String baseUrl;

    public OllamaService() {
        this.config = AppConfig.getInstance();
        this.systemMessageConfig = SystemMessageConfig.getInstance();
        this.baseUrl = config.getOllamaUrl();
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(config.getOllamaTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Generate completion from Ollama
     */
    public OllamaResponse generate(String prompt) throws IOException {
        return generate(prompt, config.getOllamaModel());
    }

    /**
     * Generate completion from Ollama with specific model
     */
    public OllamaResponse generate(String prompt, String model) throws IOException {
        logger.info("Generating completion with model: {}", model);

        OllamaRequest request = new OllamaRequest(model, prompt, false, false);
        String jsonRequest = objectMapper.writeValueAsString(request);
        // Print outgoing request to stdout so it is captured in app.out.log
        System.out.println("OLLAMA REQUEST: " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama API request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            // Print incoming response to stdout so it is captured in app.out.log
            System.out.println("OLLAMA RESPONSE: " + responseBody);
            OllamaResponse ollamaResponse = objectMapper.readValue(responseBody, OllamaResponse.class);
            logger.info("Ollama response received successfully");
            return ollamaResponse;
        } catch (IOException e) {
            logger.error("Error calling Ollama API", e);
            throw e;
        }
    }

    /**
     * List available models from Ollama
     */
    public String listModels() throws IOException {
        logger.info("Fetching available models from Ollama");

        Request request = new Request.Builder()
                .url(baseUrl + "/api/tags")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to fetch models: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            logger.info("Models fetched successfully");
            return responseBody;
        } catch (IOException e) {
            logger.error("Error fetching models from Ollama", e);
            throw e;
        }
    }

    /**
     * Generate completion with function definitions included.
     * functionsJson should be a JSON array or object as a String describing available functions.
     */
    public OllamaResponse generateWithFunctions(String prompt, String model, String functionsJson) throws IOException {
        logger.info("Generating completion with model (functions): {}", model);

        // Build a dynamic request object to include functions
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("prompt", prompt);
        root.put("stream", false);

        if (functionsJson != null && !functionsJson.isEmpty()) {
            try {
                JsonNode functionsNode = objectMapper.readTree(functionsJson);
                root.set("functions", functionsNode);
            } catch (Exception e) {
                logger.warn("Invalid functionsJson provided, ignoring functions field: {}", e.getMessage());
            }
        }

    String jsonRequest = objectMapper.writeValueAsString(root);
    // Print outgoing functions request to stdout so it is captured in app.out.log
    System.out.println("OLLAMA REQUEST (functions): " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama API request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            // Print incoming response to stdout so it is captured in app.out.log
            System.out.println("OLLAMA RESPONSE (functions): " + responseBody);
            OllamaResponse ollamaResponse = objectMapper.readValue(responseBody, OllamaResponse.class);
            logger.info("Ollama response received successfully (functions)");
            return ollamaResponse;
        } catch (IOException e) {
            logger.error("Error calling Ollama API", e);
            throw e;
        }
    }

    /**
     * Convenience overload that uses default model from configuration
     */
    public OllamaResponse generateWithFunctions(String prompt, String functionsJson) throws IOException {
        return generateWithFunctions(prompt, config.getOllamaModel(), functionsJson);
    }

    /**
     * Check if Ollama service is available
     */
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/tags")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            logger.warn("Ollama service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a simple text response (convenience method for chat)
     */
    public String generateSimple(String prompt) {
        try {
            OllamaResponse response = generate(prompt);
            return response.getResponse();
        } catch (IOException e) {
            logger.error("Error generating simple response", e);
            return "Sorry, I'm having trouble connecting to the AI service right now.";
        }
    }

    /**
     * Send a raw chat-style JSON payload to Ollama /api/chat and return raw response wrapped in OllamaResponse
     */
    public OllamaResponse generateChat(String jsonPayload) throws IOException {
        logger.info("Sending chat request to Ollama /api/chat");
        System.out.println("OLLAMA CHAT REQUEST: " + jsonPayload);

        RequestBody body = RequestBody.create(jsonPayload, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama chat request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("OLLAMA CHAT RESPONSE: " + responseBody);
            // Wrap raw response body into OllamaResponse.response for compatibility
            OllamaResponse ollamaResponse = new OllamaResponse();
            ollamaResponse.setResponse(responseBody);
            logger.info("Ollama chat response received successfully");
            return ollamaResponse;
        } catch (IOException e) {
            logger.error("Error calling Ollama chat API", e);
            throw e;
        }
    }

    /**
     * Generate chat completion with function tools using Ollama chat API
     * @param prompt User prompt/question
     * @param toolsJson JSON array string containing tool definitions
     * @return OllamaResponse containing the chat response
     * @throws IOException if the request fails
     */
    public OllamaResponse generateChatWithTools(String prompt, String toolsJson) throws IOException {
        return generateChatWithTools(prompt, config.getOllamaModel(), toolsJson);
    }

    /**
     * Continue chat conversation with function result.
     * This method is used after the model has called a tool/function.
     * It sends the function result back to Ollama to get a natural language response.
     * 
     * Note: Currently processes only the first tool call if multiple are present.
     * 
     * @param originalPrompt The original user prompt
     * @param toolCalls The tool calls made by the model (as JsonNode)
     * @param functionResult The result from executing the function
     * @param toolsJson JSON array string containing tool definitions
     * @return OllamaResponse containing the final natural language response
     * @throws IOException if the request fails
     */
    public OllamaResponse continueConversationWithFunctionResult(
            String originalPrompt, 
            JsonNode toolCalls, 
            String functionResult, 
            String toolsJson) throws IOException {
        return continueConversationWithFunctionResult(originalPrompt, config.getOllamaModel(), toolCalls, functionResult, toolsJson);
    }

    /**
     * Continue chat conversation with function result using specific model.
     * 
     * Note: Currently processes only the first tool call if multiple are present.
     * 
     * @param originalPrompt The original user prompt
     * @param model Model name to use
     * @param toolCalls The tool calls made by the model (as JsonNode)
     * @param functionResult The result from executing the function
     * @param toolsJson JSON array string containing tool definitions
     * @return OllamaResponse containing the final natural language response
     * @throws IOException if the request fails
     */
    public OllamaResponse continueConversationWithFunctionResult(
            String originalPrompt,
            String model,
            JsonNode toolCalls,
            String functionResult,
            String toolsJson) throws IOException {
        logger.info("Continuing conversation with function result using model: {}", model);

        // Build chat request with full conversation history:
        // 1. (Optional) Default system message if configured
        // 2. User's original message
        // 3. Assistant's tool call response
        // 4. Tool result message
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", false);

        ArrayNode messages = objectMapper.createArrayNode();
        
        // Prepend default system message if configured and not already present
        prependDefaultSystemMessage(messages);
        
        // Message 1: User's original prompt
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", "根据最初的用户问题，结合工具调用的结果，给出最终的回答：\n" + originalPrompt);
        messages.add(userMessage);

        // Message 2: Assistant's response with tool_calls
        ObjectNode assistantMessage = objectMapper.createObjectNode();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", ""); // Usually empty when calling tools
        assistantMessage.set("tool_calls", toolCalls);
        messages.add(assistantMessage);

        // Message 3: Tool result message
        // We need to send a message with role "tool" containing the function result
        // Note: Currently only processing the first tool call for simplicity
        if (toolCalls.isArray() && toolCalls.size() > 0) {
            JsonNode firstToolCall = toolCalls.get(0);
            JsonNode functionNode = firstToolCall.get("function");
            if (functionNode != null) {
                ObjectNode toolMessage = objectMapper.createObjectNode();
                toolMessage.put("role", "tool");
                toolMessage.put("content", functionResult);
                // Include tool_call_id if available (optional field, depends on Ollama model)
                if (firstToolCall.has("id")) {
                    toolMessage.put("tool_call_id", firstToolCall.get("id").asText());
                }
                messages.add(toolMessage);
            }
        }

        root.set("messages", messages);

        // Add tools again for potential subsequent calls
        if (toolsJson != null && !toolsJson.isEmpty()) {
            try {
                JsonNode toolsNode = objectMapper.readTree(toolsJson);
                root.set("tools", toolsNode);
            } catch (Exception e) {
                logger.warn("Invalid toolsJson provided, ignoring tools field: {}", e.getMessage());
            }
        }

        String jsonRequest = objectMapper.writeValueAsString(root);
        System.out.println("OLLAMA CHAT REQUEST (with function result): " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama chat API request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("OLLAMA CHAT RESPONSE (with function result): " + responseBody);

            // Parse the response to extract the final message content
            try {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode messageNode = responseJson.get("message");

                OllamaResponse ollamaResponse = new OllamaResponse();

                if (messageNode != null) {
                    JsonNode contentNode = messageNode.get("content");
                    if (contentNode != null) {
                        ollamaResponse.setResponse(contentNode.asText());
                    } else {
                        ollamaResponse.setResponse(responseBody);
                    }
                } else {
                    // Fallback to raw response
                    ollamaResponse.setResponse(responseBody);
                }

                logger.info("Ollama chat response with function result received successfully");
                return ollamaResponse;
            } catch (Exception e) {
                logger.warn("Failed to parse chat response, returning raw response", e);
                OllamaResponse ollamaResponse = new OllamaResponse();
                ollamaResponse.setResponse(responseBody);
                return ollamaResponse;
            }
        } catch (IOException e) {
            logger.error("Error calling Ollama chat API with function result", e);
            throw e;
        }
    }

    /**
     * Generate chat completion with function tools using Ollama chat API
     * @param prompt User prompt/question
     * @param model Model name to use
     * @param toolsJson JSON array string containing tool definitions
     * @return OllamaResponse containing the chat response
     * @throws IOException if the request fails
     */
    public OllamaResponse generateChatWithTools(String prompt, String model, String toolsJson) throws IOException {
        logger.info("Generating chat with tools using model: {}", model);

        // Build chat request with messages and tools
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", false);

        // Create messages array with user message
        ArrayNode messages = objectMapper.createArrayNode();
        
        // Prepend default system message if configured and not already present
        prependDefaultSystemMessage(messages);
        
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        root.set("messages", messages);

        // Add tools if provided
        if (toolsJson != null && !toolsJson.isEmpty()) {
            try {
                JsonNode toolsNode = objectMapper.readTree(toolsJson);
                root.set("tools", toolsNode);
            } catch (Exception e) {
                logger.warn("Invalid toolsJson provided, ignoring tools field: {}", e.getMessage());
            }
        }

        String jsonRequest = objectMapper.writeValueAsString(root);
        System.out.println("OLLAMA CHAT REQUEST (with tools): " + jsonRequest);

        RequestBody body = RequestBody.create(jsonRequest, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Ollama chat API request failed: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = response.body().string();
            System.out.println("OLLAMA CHAT RESPONSE (with tools): " + responseBody);
            
            // Parse the response to extract the message content
            try {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode messageNode = responseJson.get("message");
                
                OllamaResponse ollamaResponse = new OllamaResponse();
                
                if (messageNode != null) {
                    // Check if there's a tool_calls field (function call from model)
                    JsonNode toolCalls = messageNode.get("tool_calls");
                    if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                        // Model wants to call a function - return tool call info
                        ollamaResponse.setResponse("FUNCTION_CALL: " + toolCalls.toString());
                    } else {
                        // Regular response
                        JsonNode contentNode = messageNode.get("content");
                        if (contentNode != null) {
                            ollamaResponse.setResponse(contentNode.asText());
                        } else {
                            ollamaResponse.setResponse(responseBody);
                        }
                    }
                } else {
                    // Fallback to raw response
                    ollamaResponse.setResponse(responseBody);
                }
                
                logger.info("Ollama chat response with tools received successfully");
                return ollamaResponse;
            } catch (Exception e) {
                logger.warn("Failed to parse chat response, returning raw response", e);
                OllamaResponse ollamaResponse = new OllamaResponse();
                ollamaResponse.setResponse(responseBody);
                return ollamaResponse;
            }
        } catch (IOException e) {
            logger.error("Error calling Ollama chat API with tools", e);
            throw e;
        }
    }
    
    /**
     * Prepend default system message to messages array if configured and not already present.
     * This method checks if the messages array already contains a system message as the first entry.
     * If not, and if a default system message is configured, it prepends the default message.
     * 
     * @param messages The messages array to potentially prepend to
     */
    private void prependDefaultSystemMessage(ArrayNode messages) {
        // Check if system message is enabled
        if (!systemMessageConfig.isEnabled()) {
            return;
        }
        
        // Check if messages array already has a system message as the first entry
        if (messages.size() > 0) {
            JsonNode firstMessage = messages.get(0);
            if (firstMessage.has("role") && "system".equals(firstMessage.get("role").asText())) {
                logger.debug("System message already present, skipping default system message");
                return;
            }
        }
        
        // Prepend the default system message
        String defaultMessage = systemMessageConfig.getDefaultSystemMessage();
        if (defaultMessage != null) {
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", defaultMessage);
            messages.insert(0, systemMessage);
            logger.debug("Prepended default system message to chat request");
        }
    }
}
