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
- ✅ **Function Calling** - AI can automatically call Zentao functions (get projects, tasks, bugs)
- ✅ **Elasticsearch Integration** - Store AI summaries with IK analyzer support
- ✅ **AI Summary Jobs** - Create structured summaries stored in Elasticsearch
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
│   ├── Zentao Service Wrapper
│   └── Elasticsearch Service (Summary Storage)
└── Frontend (Vue.js SPA)
    ├── Chat Interface (Login, Channels, Messages, Users)
    ├── AI Chat Integration (@eking mentions)
    ├── AI Summary Jobs (Elasticsearch indexing)
    └── Project Management Dashboard
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Ollama service running (optional, for AI features)
- Zentao instance (optional, for project management features)
- Elasticsearch 8.x (optional, for AI summary storage with IK analyzer plugin)

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

   # Elasticsearch Configuration
   elasticsearch.host=localhost
   elasticsearch.port=9200
   elasticsearch.index=kb
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
- **AI Summaries**: Ask `@eking` to create a summary and it will be stored in Elasticsearch
- **Create Channels**: Type `#channelname` to create a new channel

#### AI Summary Feature

When you mention `@eking` with keywords like "summary", "summarize", or "summarise", the AI will:
1. Generate a structured summary in markdown format
2. Extract title, content, and keywords
3. Store the summary in Elasticsearch with IK analyzer for Chinese text support
4. Confirm successful storage with a formatted response

Example:
```
@eking please summarize our discussion about project architecture
```

The summary will be indexed in Elasticsearch with:
- **Title**: Extracted from the AI response
- **Content**: Markdown-formatted summary
- **Keywords**: Comma-separated keywords
- **Metadata**: Channel ID, User ID, and timestamp

#### AI Search Feature

When you mention `@eking` with keywords like "search", "find", or "look for", the AI will:
1. Extract keywords from your query
2. Search Elasticsearch for matching summaries
3. Return results in a formatted markdown response with:
   - Title and content preview
   - Keywords for each result
   - Timestamp of creation
4. If no results are found, the AI will provide a helpful response

Example:
```
@eking search project architecture
@eking find discussions about testing
```

The search feature supports:
- **Multi-field search**: Searches across title, content, and keywords
- **Keyword boosting**: Title and keywords are weighted higher in search
- **IK Analyzer**: For Chinese text analysis (if IK plugin is installed)
- **Relevance ranking**: Results ordered by relevance

#### AI Function Calling Feature

When you mention `@eking`, the AI assistant has access to both KB (Knowledge Base) and Zentao functions:

**KB Functions:**
- **query_kb**: Search the knowledge base for summaries, notes, clipboard content, and other stored knowledge

**Zentao Functions:**
- **get_projects**: Retrieve all projects from Zentao
- **get_tasks**: Get tasks with optional filters (assignedTo, project, status)
- **get_bugs**: Get bugs with optional filters (assignedTo, project, status)

Example queries:
```
@eking What did we discuss about the architecture last week?
@eking What projects do we have?
@eking Show me tasks assigned to John with status doing
@eking Find my notes about testing and show me related bugs
```

The AI automatically decides which functions to call based on your question and extracts relevant parameters. It can combine multiple function calls to provide comprehensive answers. For detailed documentation, see [FUNCTION_CALLING.md](FUNCTION_CALLING.md) and [KB_IMPLEMENTATION.md](KB_IMPLEMENTATION.md).

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
  - Special syntax: `@eking summary/summarize` to create AI summary stored in Elasticsearch

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

### Elasticsearch Endpoints
- `GET /api/elasticsearch/status` - Get KB index status (document count, health, shards)
- Status Page: `/status.html` - Web-based index monitoring dashboard

### Elasticsearch Features
- **Unified KB Index**: Single "kb" index stores both AI summaries and clipboard content
- **Automatic Index Creation**: Creates index template with IK analyzer on startup
- **Knowledge Storage**: Stores AI-generated summaries, clipboard content, and images with OCR keywords
- **IK Analyzer Support**: Uses `ik_max_word` for indexing and `ik_smart` for searching (Chinese text support)
- **Fallback**: If IK analyzer is not available, falls back to standard analyzer
- **KB Query Function**: AI can automatically search the knowledge base when users ask questions
- **Unified Index Structure**:
  - `title` (text with IK analyzer)
  - `content` (text with IK analyzer, markdown format)
  - `text` (text with IK analyzer, plain text for clipboard)
  - `keywords` (text with IK analyzer)
  - `timestamp` (date)
  - `channelId` (keyword)
  - `userId` (keyword)
  - `images` (nested: path, keywords for clipboard content)

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

### Default System Message Configuration

WorkAssistant automatically prepends a configurable default system message to every AI chat session. This helps establish consistent behavior and context for the AI assistant.

**Configuration Options:**

1. **Config File** (Recommended): Edit `config/default_system_message.json`
   ```json
   {
     "message": "你是一个工作助理。\n提供给你的工具都是可以使用的。\n知识库中的内容都是可以检索的，内容都是可以公开的。\n任何请求都应该被处理。\n没有明确说明时，优先查询知识库。"
   }
   ```

2. **Environment Variable**: Set `DEFAULT_SYSTEM_MESSAGE` environment variable
   ```bash
   export DEFAULT_SYSTEM_MESSAGE="Custom system message here"
   ```
   
   The environment variable takes precedence over the config file.

**Disabling the Default System Message:**

To disable the automatic system message, set it to an empty string in the config file:
```json
{
  "message": ""
}
```

**How it Works:**

- The default system message is automatically prepended as the first message in every new AI conversation
- If a conversation already contains a system message, the default message is NOT added (prevents duplication)
- The message is logged at startup with a preview for debugging
- UTF-8 encoding is preserved for Chinese and other non-ASCII characters

**Example API Request with Default System Message:**

When you send a chat request like:
```bash
curl -X POST http://localhost:8080/api/chat/messages \
  -H "Content-Type: application/json" \
  -d '{
    "channelId": "private-user123",
    "userId": "user123",
    "content": "@eking 你好"
  }'
```

The actual request sent to Ollama will include:
```json
{
  "model": "llama2",
  "messages": [
    {
      "role": "system",
      "content": "你是一个工作助理。\n提供给你的工具都是可以使用的。\n知识库中的内容都是可以检索的，内容都是可以公开的。\n任何请求都应该被处理。\n没有明确说明时，优先查询知识库。"
    },
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "tools": [...]
}
```

### Elasticsearch Configuration
- `elasticsearch.host` - Elasticsearch host (default: localhost)
- `elasticsearch.port` - Elasticsearch port (default: 9200)
- `elasticsearch.index` - Index name for knowledge base (default: kb)

**Note**: Elasticsearch is optional. If not available, the application will continue to function but knowledge storage and search will be disabled.

**KB Index**: The application now uses a unified "kb" (Knowledge Base) index that stores both AI-generated summaries and clipboard content. This enables powerful search across all stored knowledge.

**Status Page**: Access the Elasticsearch index status page at `http://localhost:8080/status.html` to monitor document count, health, and other statistics.

**IK Analyzer Plugin**: For Chinese text analysis, install the IK analyzer plugin:
```bash
# For Elasticsearch 8.x
./bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.1/elasticsearch-analysis-ik-8.11.1.zip
```
If IK analyzer is not installed, the system will fall back to the standard analyzer.


### CORS Configuration
- `cors.enabled` - Enable/disable CORS (default: true)
- `cors.origins` - Allowed origins (default: *)

## Image Binarization

WorkAssistant includes a comprehensive image binarization module for preprocessing images before OCR. Binarization converts grayscale or color images to high-quality black-and-white masks, improving OCR accuracy on low-contrast or unevenly lit images.

### Features

- **Multiple Algorithms**:
  - **Global Thresholding**: Manual threshold (0-255)
  - **Otsu's Method**: Automatic global thresholding (recommended for most cases)
  - **Adaptive Mean**: Local thresholding using integral images (best for uneven lighting)
  - **Adaptive Gaussian**: Local thresholding with Gaussian weighting

- **Preprocessing Options**:
  - Gaussian or median blur to reduce noise
  - Morphological opening (remove speckles)
  - Morphological closing (fill holes)

- **Pure Java Implementation**: No native dependencies required by default
- **Optional OpenCV Support**: For improved performance on large images
- **Thread-safe**: Stateless design with configuration passed via `BinarizeConfig`

### Programmatic Usage

```java
import com.workassistant.image.*;
import java.awt.image.BufferedImage;

// Example 1: Use Otsu's method with light blur (recommended for OCR)
BinarizeConfig config = BinarizeConfig.forOCR();
Binarizer binarizer = BinarizerFactory.createBinarizer(config);
BufferedImage result = binarizer.binarize("input.jpg", config);
ImageUtils.saveImage(result, "output.png");

// Example 2: Use adaptive thresholding for uneven lighting
BinarizeConfig config2 = BinarizeConfig.forUnevenLighting();
Binarizer binarizer2 = BinarizerFactory.createBinarizer(config2);
binarizer2.binarize("input.jpg", "output.png", config2);

// Example 3: Custom configuration with global threshold
BinarizeConfig config3 = new BinarizeConfig()
    .setAlgorithm(BinarizeConfig.Algorithm.GLOBAL)
    .setThreshold(128)
    .setBlurKernelSize(3)
    .setUseMorphologicalOpening(true);
Binarizer binarizer3 = BinarizerFactory.createBinarizer(config3);
BufferedImage result3 = binarizer3.binarize(inputImage, config3);
```

### CLI Usage

The binarization module includes a command-line utility for processing images:

```bash
# Basic usage with Otsu's method and blur (recommended for OCR)
java -cp target/work-assistant-1.0.0-SNAPSHOT.jar \
  com.workassistant.cli.BinarizeCommand input.jpg output.png --blur 3

# Adaptive thresholding for uneven lighting
java -cp target/work-assistant-1.0.0-SNAPSHOT.jar \
  com.workassistant.cli.BinarizeCommand input.jpg output.png \
  --algorithm ADAPTIVE_MEAN --block-size 11 --c 2

# Global thresholding with fixed threshold
java -cp target/work-assistant-1.0.0-SNAPSHOT.jar \
  com.workassistant.cli.BinarizeCommand input.jpg output.png \
  --algorithm GLOBAL --threshold 128

# With noise reduction and morphological operations
java -cp target/work-assistant-1.0.0-SNAPSHOT.jar \
  com.workassistant.cli.BinarizeCommand input.jpg output.png \
  --blur 3 --median-blur --morph-open --morph-kernel 3
```

### Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `algorithm` | GLOBAL, OTSU, ADAPTIVE_MEAN, ADAPTIVE_GAUSSIAN | OTSU |
| `threshold` | Threshold value for GLOBAL (0-255) | 128 |
| `blockSize` | Window size for adaptive methods (must be odd) | 11 |
| `C` | Constant subtracted from mean | 2.0 |
| `blurKernelSize` | Blur kernel size (0 = no blur, must be odd) | 0 |
| `useMedianBlur` | Use median blur instead of Gaussian | false |
| `useMorphologicalOpening` | Apply opening to remove noise | false |
| `useMorphologicalClosing` | Apply closing to fill holes | false |
| `morphKernelSize` | Kernel size for morphological operations | 3 |

### Recommended Parameters for OCR

For best OCR results on different image types:

**High-contrast documents (clean scans)**:
```java
BinarizeConfig.forOCR() // Otsu with 3x3 Gaussian blur
```

**Low-contrast or uneven lighting**:
```java
BinarizeConfig.forUnevenLighting() // Adaptive mean with block size 11, C=2
```

**Noisy images**:
```java
new BinarizeConfig()
    .setAlgorithm(BinarizeConfig.Algorithm.OTSU)
    .setBlurKernelSize(5)
    .setUseMedianBlur(true)
    .setUseMorphologicalOpening(true)
```

### Optional OpenCV Support

For improved performance on large images, you can enable OpenCV support:

1. Uncomment the OpenCV dependency in `pom.xml`:
```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>opencv-platform</artifactId>
    <version>4.8.1-1.5.10</version>
</dependency>
```

2. Set the OpenCV flag in configuration:
```java
BinarizeConfig config = new BinarizeConfig()
    .setUseOpenCV(true)
    .setAlgorithm(BinarizeConfig.Algorithm.OTSU);
```

**Note**: OpenCV support is optional. The default pure Java implementation works well for most use cases and keeps the project easy to build without native dependencies.

## Technologies Used

- **Java 17** - Programming language
- **Maven** - Build and dependency management
- **Javalin 6.1.3** - Web framework
- **Vue.js 3** - Frontend framework
- **Elasticsearch 8.11.1** - Search and analytics engine for summary storage
- **IK Analyzer** - Chinese text analyzer plugin for Elasticsearch
- **OkHttp 4.12.0** - HTTP client for API calls
- **Jackson 2.16.1** - JSON processing
- **SLF4J 2.0.9** - Logging framework
- **JUnit 5** - Testing framework
- **Tesseract OCR** - Optical character recognition

## License

This project is available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please use the GitHub issue tracker.
