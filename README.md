# WorkAssistant

A work assistant integrated with AI model and middleware - integrating Ollama AI and Zentao project management.

## Overview

WorkAssistant is a Java 17 Maven-based web application that provides a unified interface to interact with:
- **Ollama**: An AI model service for natural language processing
- **Zentao**: A project management and collaboration tool

The application uses Javalin as a lightweight web server and features a single-page Vue.js frontend.

## Features

- ✅ **Java 17 Maven Project** - Modern Java development with Maven build system
- ✅ **Javalin Web Server** - Fast and lightweight REST API server
- ✅ **Vue.js Single Page Application** - Modern, responsive UI
- ✅ **Chat Application** - Real-time chat with channels and AI integration
- ✅ **Ollama Integration** - Wrap Ollama REST API with convenient endpoints
- ✅ **Zentao Integration** - Wrap Zentao REST API for project management
- ✅ **Configurable Properties** - Easy configuration via properties file
- ✅ **CORS Support** - Cross-origin resource sharing enabled
- ✅ **Unit Tests** - Comprehensive test coverage

## Architecture

```
WorkAssistant
├── Backend (Javalin + Java 17)
│   ├── REST API Endpoints
│   ├── Chat Service (Users, Channels, Messages)
│   ├── Ollama Service Wrapper
│   └── Zentao Service Wrapper
└── Frontend (Vue.js SPA)
    ├── Chat Interface (Login, Channels, Messages, Users)
    ├── AI Chat Integration (@eking mentions)
    └── Project Management Dashboard
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Ollama service running (optional, for AI features)
- Zentao instance (optional, for project management features)

## Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/antipro/WorkAssistant.git
   cd WorkAssistant
   ```

2. **Configure the application**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

3. **Edit `application.properties` with your settings**
   ```properties
   # Server Configuration
   server.port=8080

   # Zentao Configuration
   zentao.url=http://your-zentao-server/zentao
   zentao.account=your-username
   zentao.password=your-password

   # Ollama Configuration
   ollama.url=http://localhost:11434
   ollama.model=llama2
   ollama.timeout=120000

   # CORS Configuration
   cors.enabled=true
   cors.origins=*
   ```

4. **Build the project**
   ```bash
   mvn clean package
   ```

## Running the Application

### Option 1: Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.workassistant.WorkAssistantApplication"
```

### Option 2: Using the JAR file
```bash
java -jar target/work-assistant-1.0.0-SNAPSHOT.jar
```

The application will start on port 8080 (or the port specified in your configuration).

Access the web interface at: `http://localhost:8080`

### Chat Application Features

The default interface is now a chat application with:
- **Simple Login**: Enter nickname (no password required)
- **Channels**: Left sidebar shows available channels
- **Chat Area**: Center panel for messages
- **Users**: Right sidebar shows online users
- **AI Integration**: Private AI channel for each user, or mention `@eking` in any channel
- **Create Channels**: Type `#channelname` to create a new channel

For detailed chat documentation, see [CHAT.md](CHAT.md).

## API Endpoints

### Health Check
- `GET /api/health` - Check application status

### Chat Endpoints
- `POST /api/chat/login` - Login with nickname (no password required)
- `GET /api/chat/users` - Get online users
- `GET /api/chat/channels` - Get channels for a user
- `POST /api/chat/channels` - Create a new channel
- `GET /api/chat/channels/{id}/messages` - Get messages from a channel
- `POST /api/chat/messages` - Send a message
  - Special syntax: `#channelname` to create a channel
  - Special syntax: `@eking` to call AI assistant

### Ollama Endpoints
- `POST /api/ollama/generate` - Generate AI completion
  ```json
  {
    "model": "llama2",
    "prompt": "Your question here",
    "stream": false
  }
  ```
- `GET /api/ollama/models` - List available models
- `GET /api/ollama/status` - Check Ollama service status

### Zentao Endpoints
- `GET /api/zentao/projects` - Get all projects
- `GET /api/zentao/tasks` - Get all tasks
- `GET /api/zentao/bugs` - Get all bugs
- `GET /api/zentao/status` - Check Zentao service status

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/workassistant/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # API controllers
│   │   ├── model/           # Data models
│   │   ├── service/         # Business logic services
│   │   └── WorkAssistantApplication.java  # Main application
│   └── resources/
│       ├── public/          # Vue.js frontend
│       └── application.properties
└── test/
    └── java/                # Unit tests
```

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean compile    # Compile only
mvn clean test       # Run tests
mvn clean package    # Build JAR
```

## Configuration Details

### Server Configuration
- `server.port` - The port on which the application runs (default: 8080)

### Zentao Configuration
- `zentao.url` - Base URL of your Zentao instance
- `zentao.account` - Zentao username for authentication
- `zentao.password` - Zentao password for authentication

### Ollama Configuration
- `ollama.url` - Base URL of Ollama service (default: http://localhost:11434)
- `ollama.model` - Default model to use (default: llama2)
- `ollama.timeout` - Request timeout in milliseconds (default: 120000)

### CORS Configuration
- `cors.enabled` - Enable/disable CORS (default: true)
- `cors.origins` - Allowed origins (default: *)

## Technologies Used

- **Java 17** - Programming language
- **Maven** - Build and dependency management
- **Javalin 6.1.3** - Web framework
- **Vue.js 3** - Frontend framework
- **OkHttp 4.12.0** - HTTP client for API calls
- **Jackson 2.16.1** - JSON processing
- **SLF4J 2.0.9** - Logging framework
- **JUnit 5** - Testing framework

## License

This project is available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please use the GitHub issue tracker.
