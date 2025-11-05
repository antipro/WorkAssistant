# Zentao Function Calling with Ollama

## Overview

This feature enables the AI assistant (eking) to call Zentao project management functions directly when users ask questions about projects, tasks, or bugs. The implementation uses Ollama's function calling feature via the `/api/chat` endpoint with tool definitions.

## How It Works

When a user mentions `@eking` in a chat message, the system:

1. **Sends the request to Ollama with Zentao function definitions**: The chat request includes a `tools` parameter with all available Zentao functions (get_projects, get_tasks, get_bugs).

2. **Ollama decides whether to call a function**: Based on the user's prompt, the AI model can choose to:
   - Call one of the Zentao functions if the user is asking about projects, tasks, or bugs
   - Respond with regular text if no function call is needed

3. **Executes the function and returns results**: If the model decides to call a function, the system executes the corresponding Zentao API call and returns the results to the user.

## Architecture

### Components

#### 1. ZentaoFunctionProvider
`src/main/java/com/workassistant/service/ZentaoFunctionProvider.java`

A utility class that provides Zentao function definitions in Ollama-compatible format:

```java
ArrayNode tools = ZentaoFunctionProvider.getZentaoFunctionTools();
String toolsJson = ZentaoFunctionProvider.getZentaoFunctionToolsJson();
```

Functions provided:
- **get_projects**: Get all projects from Zentao
- **get_tasks**: Get tasks (with optional filters: assignedTo, project, status)
- **get_bugs**: Get bugs (with optional filters: assignedTo, project, status)

#### 2. OllamaService Enhancement
`src/main/java/com/workassistant/service/OllamaService.java`

Enhanced with two key methods:

**`generateChatWithTools()`**:
- Uses Ollama's `/api/chat` endpoint (instead of `/api/generate`)
- Accepts a `tools` parameter containing function definitions
- Formats the request according to Ollama's chat API format
- Parses function call responses from the model

```java
OllamaResponse response = ollamaService.generateChatWithTools(prompt, toolsJson);
```

**`continueConversationWithFunctionResult()`** (NEW):
- Sends function execution results back to Ollama
- Includes full conversation history (user message, assistant tool call, tool result)
- Receives a natural language response from Ollama
- Enables regularized, human-readable answers

```java
OllamaResponse finalResponse = ollamaService.continueConversationWithFunctionResult(
    originalPrompt, toolCalls, functionResult, toolsJson);
```

#### 3. ChatController Updates
`src/main/java/com/workassistant/controller/ChatController.java`

Enhanced AI request handling:
- Includes Zentao function tools in every AI request
- Detects when the model wants to call a function
- Executes the appropriate Zentao function
- **Sends function result back to Ollama for regularization**
- Returns natural language response to the user

## Request Format

The system sends requests to Ollama in the following format:

```json
{
  "model": "qwen3:8b",
  "stream": false,
  "messages": [
    {
      "role": "user",
      "content": "What projects do we have?"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_projects",
        "description": "Get all projects from Zentao project management system",
        "parameters": {
          "type": "object",
          "required": [],
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "get_tasks",
        "description": "Get tasks from Zentao project management system. Can filter by assignedTo, project, or status",
        "parameters": {
          "type": "object",
          "required": [],
          "properties": {
            "assignedTo": {
              "type": "string",
              "description": "Filter tasks assigned to a specific user"
            },
            "project": {
              "type": "string",
              "description": "Filter tasks by project ID"
            },
            "status": {
              "type": "string",
              "description": "Filter tasks by status (e.g., 'doing', 'done', 'wait')"
            }
          }
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "get_bugs",
        "description": "Get bugs from Zentao project management system. Can filter by assignedTo, project, or status",
        "parameters": {
          "type": "object",
          "required": [],
          "properties": {
            "assignedTo": {
              "type": "string",
              "description": "Filter bugs assigned to a specific user"
            },
            "project": {
              "type": "string",
              "description": "Filter bugs by project ID"
            },
            "status": {
              "type": "string",
              "description": "Filter bugs by status (e.g., 'active', 'resolved', 'closed')"
            }
          }
        }
      }
    }
  ]
}
```

## Response Handling

### Regular Text Response

If Ollama responds with regular text (no function call):

```json
{
  "model": "qwen3:8b",
  "message": {
    "role": "assistant",
    "content": "I can help you with that. Let me check the projects."
  }
}
```

The system extracts the content and sends it to the user.

### Function Call Response

If Ollama decides to call a function:

```json
{
  "model": "qwen3:8b",
  "message": {
    "role": "assistant",
    "content": "",
    "tool_calls": [
      {
        "function": {
          "name": "get_projects",
          "arguments": {}
        }
      }
    ]
  }
}
```

The system:
1. Detects the function call
2. Executes `zentaoService.getProjects()`
3. Sends the function result back to Ollama along with the conversation history
4. Ollama generates a natural language response based on the function result
5. Returns the regularized answer to the user

### Function Result Processing (NEW)

After executing a function, the system sends a follow-up request to Ollama with the complete conversation:

**Request to Ollama after function execution:**
```json
{
  "model": "qwen3:8b",
  "stream": false,
  "messages": [
    {
      "role": "user",
      "content": "What projects do we have?"
    },
    {
      "role": "assistant",
      "content": "",
      "tool_calls": [
        {
          "function": {
            "name": "get_projects",
            "arguments": {}
          }
        }
      ]
    },
    {
      "role": "tool",
      "content": "{\"projects\":[{\"id\":1,\"name\":\"Project Alpha\"},{\"id\":2,\"name\":\"Project Beta\"}]}"
    }
  ],
  "tools": [...]
}
```

**Ollama's Final Response:**
```json
{
  "model": "qwen3:8b",
  "message": {
    "role": "assistant",
    "content": "Based on the data, we currently have 2 active projects: Project Alpha and Project Beta."
  }
}
```

This regularization process ensures that:
- Raw JSON data is converted to natural language
- Responses are contextual and conversational
- Users get easy-to-understand answers
- The AI can interpret and summarize the data

## Usage Examples
1. Detects the function call
2. Executes `zentaoService.getProjects()`
3. Sends the function result back to Ollama along with the conversation history
4. Ollama generates a natural language response based on the function result
5. Returns the regularized answer to the user

## Usage Examples

### Example 1: Getting Projects

**User**: `@eking What projects do we have?`

**System sends to Ollama**: Request with user prompt + Zentao function tools

**Ollama response**: Function call to `get_projects`

**System executes**: `zentaoService.getProjects()`

**System sends function result to Ollama**: Full conversation history including function result

**Ollama generates**: Natural language response summarizing the projects

**User receives**: Formatted, readable response like "We have 3 projects: Alpha, Beta, and Gamma..."

### Example 2: Getting Filtered Tasks

**User**: `@eking Show me tasks assigned to John with status doing`

**System sends to Ollama**: Request with user prompt + Zentao function tools

**Ollama response**: Function call to `get_tasks` with parameters:
```json
{
  "assignedTo": "John",
  "status": "doing"
}
```

**System executes**: `zentaoService.getTasks({"assignedTo": "John", "status": "doing"})`

**System sends function result to Ollama**: Full conversation history including task data

**Ollama generates**: Natural language response interpreting the task data

**User receives**: Human-readable response like "John is currently working on 2 tasks: Task #42 'Implement feature X' and Task #43 'Fix bug Y'..."

### Example 3: Regular Chat

**User**: `@eking Hello, how are you?`

**System sends to Ollama**: Request with user prompt + Zentao function tools

**Ollama response**: Regular text response (no function call)

**User receives**: AI's text response directly (no function execution needed)

## Testing

### Unit Tests

**ZentaoFunctionProviderTest** (7 tests):
- Verifies function tool structure
- Tests JSON serialization
- Validates function definitions match Ollama format
- Ensures all required fields are present

### Integration Tests

**ZentaoFunctionCallingIntegrationTest** (6 tests):
- Verifies complete request format matches Ollama specification
- Tests all Zentao functions are present
- Validates parameter structures
- Demonstrates sample requests

Run tests:
```bash
mvn test -Dtest=ZentaoFunctionProviderTest
mvn test -Dtest=ZentaoFunctionCallingIntegrationTest
mvn test  # Run all 51 tests
```

## Configuration

No additional configuration is required. The feature automatically:
- Uses the existing Ollama configuration from `application.properties`
- Uses the existing Zentao configuration from `application.properties`
- Includes function tools in every AI request

## Benefits

1. **Automatic Function Discovery**: The AI model knows about Zentao functions without explicit commands
2. **Natural Language**: Users can ask questions naturally without learning specific syntax
3. **Smart Function Selection**: The AI decides when to call functions vs. respond with text
4. **Parameter Extraction**: The AI extracts relevant parameters from the user's question
5. **Seamless Integration**: Works within the existing chat interface

## Limitations

1. **Model Support**: Requires an Ollama model that supports function calling (e.g., qwen2.5, llama3.1, mistral-nemo)
2. **Zentao Connection**: Requires a working Zentao instance and valid credentials
3. **Function Execution**: Currently executes functions synchronously; complex queries may have longer response times

## Future Enhancements

Potential improvements:
1. Add more Zentao functions (create task, update bug, etc.)
2. Support for multi-step function calls
3. Caching of frequently requested data
4. Better error handling and user feedback
5. Function call logging and analytics

## References

- [Ollama API Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Ollama Function Calling](https://ollama.com/blog/tool-support)
- [Zentao API Documentation](https://www.zentao.pm/book/zentaopmshelp/90.html)
