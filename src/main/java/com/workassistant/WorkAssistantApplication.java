package com.workassistant;

import com.workassistant.config.AppConfig;
import com.workassistant.controller.OllamaController;
import com.workassistant.controller.ZentaoController;
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
        
        // Initialize controllers
        OllamaController ollamaController = new OllamaController(ollamaService);
        ZentaoController zentaoController = new ZentaoController(zentaoService);

        // Create and configure Javalin app
        Javalin app = Javalin.create(javalinConfig -> {
            // Enable CORS
            if (config.isCorsEnabled()) {
                javalinConfig.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.allowHost(config.getCorsOrigins());
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
        configureRoutes(app, ollamaController, zentaoController);

        logger.info("WorkAssistant started on port {}", config.getServerPort());
        logger.info("Ollama URL: {}", config.getOllamaUrl());
        logger.info("Zentao URL: {}", config.getZentaoUrl());
    }

    private static void configureRoutes(Javalin app, OllamaController ollamaController, ZentaoController zentaoController) {
        // Health check endpoint
        app.get("/api/health", ctx -> ctx.json(new HealthResponse("OK", "WorkAssistant is running")));

        // Ollama API routes
        app.post("/api/ollama/generate", ollamaController::generate);
        app.get("/api/ollama/models", ollamaController::listModels);
        app.get("/api/ollama/status", ollamaController::status);

        // Zentao API routes
        app.get("/api/zentao/projects", zentaoController::getProjects);
        app.get("/api/zentao/tasks", zentaoController::getTasks);
        app.get("/api/zentao/bugs", zentaoController::getBugs);
        app.get("/api/zentao/status", zentaoController::status);
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
