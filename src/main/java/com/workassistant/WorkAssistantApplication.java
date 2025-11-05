package com.workassistant;

import com.workassistant.config.AppConfig;
import com.workassistant.controller.ChatController;
import com.workassistant.controller.OllamaController;
import com.workassistant.controller.ZentaoController;
import com.workassistant.service.ChatService;
import com.workassistant.service.OllamaService;
import com.workassistant.service.ZentaoService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for WorkAssistant
 */
public class WorkAssistantApplication {
    private static final Logger logger = LoggerFactory.getLogger(WorkAssistantApplication.class);

    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        
        // Initialize services
        OllamaService ollamaService = new OllamaService();
        ZentaoService zentaoService = new ZentaoService();
        ChatService chatService = ChatService.getInstance();
        
    // Initialize controllers
    OllamaController ollamaController = new OllamaController(ollamaService, zentaoService);
    ZentaoController zentaoController = new ZentaoController(zentaoService);
    ChatController chatController = new ChatController(chatService, ollamaService);

        // Create and configure Javalin app
        Javalin app = Javalin.create(javalinConfig -> {
            // Enable CORS
            if (config.isCorsEnabled()) {
                javalinConfig.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        String origins = config.getCorsOrigins();
                        if ("*".equals(origins)) {
                            it.anyHost();
                        } else {
                            it.allowHost(origins);
                        }
                    });
                });
            }
            
            // Configure static files for Vue.js frontend
            javalinConfig.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(config.getServerPort());

    // Configure routes
    com.workassistant.controller.AIController aiController = new com.workassistant.controller.AIController(zentaoService);
    configureRoutes(app, ollamaController, zentaoController, chatController, aiController);

        logger.info("WorkAssistant started on port {}", config.getServerPort());
        logger.info("Ollama URL: {}", config.getOllamaUrl());
        logger.info("Zentao URL: {}", config.getZentaoUrl());
    }

    private static void configureRoutes(Javalin app, OllamaController ollamaController, ZentaoController zentaoController, ChatController chatController, com.workassistant.controller.AIController aiController) {
        // Health check endpoint
        app.get("/api/health", ctx -> ctx.json(new HealthResponse("OK", "WorkAssistant is running")));

        // Ollama API routes
        app.post("/api/ollama/generate", ollamaController::generate);
    app.post("/api/ollama/assistant", ollamaController::assistantWithFunctions);
        app.get("/api/ollama/models", ollamaController::listModels);
        app.get("/api/ollama/status", ollamaController::status);

        // Zentao API routes
        app.get("/api/zentao/projects", zentaoController::getProjects);
        app.get("/api/zentao/tasks", zentaoController::getTasks);
        app.get("/api/zentao/bugs", zentaoController::getBugs);
        app.get("/api/zentao/status", zentaoController::status);
        // AI function-call endpoint for models/tooling
    app.post("/api/ai/function-call", aiController::functionCall);
        
        // Chat API routes
        app.post("/api/chat/login", chatController::login);
        app.get("/api/chat/users", chatController::getUsers);
        app.get("/api/chat/channels", chatController::getChannels);
        app.post("/api/chat/channels", chatController::createChannel);
        app.get("/api/chat/channels/{channelId}/messages", chatController::getMessages);
        app.post("/api/chat/messages", chatController::sendMessage);
        
        // WebSocket route
        app.ws("/ws/chat", ws -> {
            ws.onConnect(chatController::handleWebSocketConnect);
            ws.onMessage(chatController::handleWebSocketMessage);
            ws.onClose(chatController::handleWebSocketClose);
        });
    }

    // Simple health check response class
    private static class HealthResponse {
        private final String status;
        private final String message;

        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
