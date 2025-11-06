# Default System Message Feature Implementation

## Overview

This implementation adds a configurable default system message that is automatically prepended to every AI chat session in the WorkAssistant application.

## Implementation Details

### Files Created

1. **config/default_system_message.json**
   - Contains the default system message in Chinese
   - Can be edited to customize the default message
   - Uses UTF-8 encoding to preserve Chinese characters

2. **src/main/java/com/workassistant/config/SystemMessageConfig.java**
   - Singleton configuration loader
   - Reads from config file or environment variable
   - Provides methods: `getDefaultSystemMessage()` and `isEnabled()`
   - Logs the active configuration at startup

3. **src/test/java/com/workassistant/config/SystemMessageConfigTest.java**
   - 5 unit tests for configuration loading
   - Tests UTF-8 preservation and Chinese characters
   - Tests configuration file existence and validity

4. **src/test/java/com/workassistant/service/OllamaServiceSystemMessageTest.java**
   - 5 integration tests for the system message feature
   - Tests configuration loading and JSON validation
   - Tests message formatting and structure

### Files Modified

1. **src/main/java/com/workassistant/service/OllamaService.java**
   - Added `SystemMessageConfig` dependency
   - Created `prependDefaultSystemMessage()` helper method
   - Modified `generateChatWithTools()` to prepend system message
   - Modified `continueConversationWithFunctionResult()` to prepend system message
   - System message is only added if not already present

2. **README.md**
   - Added "Default System Message Configuration" section
   - Documented configuration options (file and environment variable)
   - Provided examples for overriding and disabling
   - Showed API request examples with system messages

## Configuration

### Priority Order

1. **Environment Variable** (highest priority): `DEFAULT_SYSTEM_MESSAGE`
2. **Config File**: `config/default_system_message.json`
3. **Hardcoded Default**: Built into `SystemMessageConfig` class

### Configuration Options

#### Using Config File (Recommended)

Edit `config/default_system_message.json`:
```json
{
  "message": "你是一个工作助理。\n提供给你的工具都是可以使用的。\n知识库中的内容都是可以检索的，内容都是可以公开的。\n任何请求都应该被处理。"
}
```

#### Using Environment Variable

```bash
export DEFAULT_SYSTEM_MESSAGE="Custom system message"
```

#### Disabling the Feature

Set empty message in config file:
```json
{
  "message": ""
}
```

## How It Works

1. **Startup**: `SystemMessageConfig` loads the configuration and logs it:
   ```
   [main] INFO SystemMessageConfig - Loaded default system message from config file: 
   config/default_system_message.json (length: 63 chars, preview: 你是一个工作助理。 提供给你的工具都是可以使用的。 知识库中...)
   ```

2. **Chat Request**: When `OllamaService.generateChatWithTools()` is called:
   - Creates an empty messages array
   - Calls `prependDefaultSystemMessage()` which:
     - Checks if system message is enabled
     - Checks if messages array already has a system message as first entry
     - If no system message exists, prepends the default message with role="system"
   - Adds the user message
   - Sends the complete messages array to Ollama

3. **Example**: User message `@eking 你好` becomes:
   ```json
   {
     "model": "llama2",
     "messages": [
       {
         "role": "system",
         "content": "你是一个工作助理。\n提供给你的工具都是可以使用的。\n知识库中的内容都是可以检索的，内容都是可以公开的。\n任何请求都应该被处理。"
       },
       {
         "role": "user",
         "content": "你好"
       }
     ],
     "tools": [...]
   }
   ```

## Testing

All tests pass successfully:

```bash
mvn test -Dtest=SystemMessageConfigTest,OllamaServiceSystemMessageTest
```

**Results:**
- Tests run: 10
- Failures: 0
- Errors: 0
- Skipped: 0

### Test Coverage

**SystemMessageConfigTest:**
- testLoadFromConfigFile
- testMessageIsNotEmpty
- testMessagePreservesUtf8
- testConfigFileExists
- testIsEnabled

**OllamaServiceSystemMessageTest:**
- testSystemMessageConfigIsLoaded
- testSystemMessagePreservesUtf8
- testMessageFormatting
- testConfigFileExists
- testConfigFileIsValidJson

## Build and Deployment

```bash
# Build
mvn clean compile

# Run tests
mvn test

# Package
mvn clean package

# Run application
java -jar target/work-assistant-1.0.0-SNAPSHOT.jar

# Or with custom environment variable
DEFAULT_SYSTEM_MESSAGE="Custom message" java -jar target/work-assistant-1.0.0-SNAPSHOT.jar
```

## Benefits

1. **Consistent AI Behavior**: Every chat session starts with the same context
2. **Configurable**: Easy to customize without code changes
3. **No Duplication**: Intelligent detection prevents duplicate system messages
4. **UTF-8 Support**: Proper handling of Chinese and other non-ASCII characters
5. **Debuggable**: Startup logging helps verify active configuration
6. **Flexible**: Can be overridden via environment variable or disabled entirely

## Migration Guide

No migration needed. This feature is automatically enabled when the application starts. The default message is provided in the config file.

To customize:
1. Edit `config/default_system_message.json`
2. Restart the application
3. The new message will be logged at startup

## Future Enhancements

Possible future improvements:
1. Per-user or per-channel custom system messages
2. Dynamic system message updates without restart
3. System message templates with variable substitution
4. Multi-language support with locale detection
