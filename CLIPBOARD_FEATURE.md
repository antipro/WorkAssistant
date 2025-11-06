# Clipboard Feature Implementation

## Overview
This document describes the implementation of the clipboard feature for WorkAssistant, which allows users to paste text and images from their clipboard, have them processed with OCR and AI, and stored in Elasticsearch.

## Features Implemented

### 1. Fixed Keyboard Shortcut
- **Issue**: Original implementation used `Shift+Enter` to send messages
- **Solution**: Changed to `Ctrl+Enter` on Windows/Linux and `Cmd+Enter` on Mac
- **Location**: `src/main/resources/public/index.html` (lines 442-443)

### 2. Clipboard Button
- **Feature**: Added a clipboard button (📋) next to the send button
- **Functionality**: 
  - Reads clipboard content using Clipboard API
  - Supports both text and images
  - Converts images to base64 for transmission
- **Styling**: Green button with hover effects
- **Location**: `src/main/resources/public/index.html`

### 3. Image Storage
- **Directory**: `work/images/` (relative to application root)
- **Naming**: UUID-based filenames to avoid conflicts
- **Format Support**: PNG, JPEG, GIF, WebP
- **Security**: Directory is excluded from git via `.gitignore`
- **Serving**: Images served via GET `/api/chat/images/{imagePath}`

### 4. Structured Data Model

#### ClipboardData
```java
public class ClipboardData {
    private String text;
    private List<ClipboardImage> images;
    
    public static class ClipboardImage {
        private String path;
        private String type;
        private List<String> keywords; // OCR-extracted
    }
}
```

#### ClipboardContentDocument
```java
public class ClipboardContentDocument {
    private String id;
    private String title;  // AI-generated
    private String text;
    private List<ImageMetadata> images;
    private List<String> keywords;  // Combined from text and OCR
    private String channelId;
    private String userId;
    private LocalDateTime timestamp;
}
```

### 5. OCR Integration
- **Library**: Tesseract 4J (version 5.9.0)
- **Service**: `OCRService` singleton
- **Features**:
  - Text extraction from images
  - Keyword extraction (words > 3 chars)
  - Stop words filtering
  - Graceful degradation if Tesseract not installed
- **Configuration**: Auto-detects Tesseract data paths

### 6. AI Title Generation
- **Process**: 
  1. Builds prompt with text snippet, image count, and keywords
  2. Calls Ollama AI service
  3. Cleans response (removes quotes, prefixes, extra lines)
  4. Truncates to 100 characters if needed
- **Fallback**: "Clipboard Content" if AI generation fails
- **Method**: `generateClipboardTitle()` in ChatController

### 7. Elasticsearch Storage
- **Index**: `work_assistant_summaries_clipboard` (separate from regular summaries)
- **Analyzer**: IK analyzer for Chinese text support
- **Mapping**:
  ```json
  {
    "title": { "type": "text", "analyzer": "ik_max_word" },
    "text": { "type": "text", "analyzer": "ik_max_word" },
    "keywords": { "type": "text", "analyzer": "ik_max_word" },
    "images": { 
      "type": "nested",
      "properties": {
        "path": { "type": "keyword" },
        "keywords": { "type": "text", "analyzer": "ik_max_word" }
      }
    },
    "timestamp": { "type": "date" },
    "channelId": { "type": "keyword" },
    "userId": { "type": "keyword" }
  }
  ```

### 8. Asynchronous Processing
- **Thread Pool**: Uses existing `aiExecutor` (5 threads)
- **Job Type**: `CLIPBOARD_CONTENT` added to JobType enum
- **Process Flow**:
  1. User pastes content → Immediate message sent to chat
  2. Backend receives → Saves images to disk
  3. Async task started:
     - OCR processing on each image
     - Keyword extraction from text and images
     - AI title generation
     - Elasticsearch indexing
     - Confirmation message sent to chat

## API Endpoints

### POST /api/chat/clipboard
Submits clipboard content for processing.

**Request Body:**
```json
{
  "channelId": "channel-uuid",
  "userId": "user-uuid",
  "content": {
    "text": "Optional text content",
    "images": [
      {
        "data": "data:image/png;base64,...",
        "type": "image/png"
      }
    ]
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "message-uuid",
    "channelId": "channel-uuid",
    "userId": "user-uuid",
    "username": "John",
    "content": "Summary text...",
    "contentType": "CLIPBOARD",
    "clipboardData": { ... },
    "timestamp": "2025-11-06T01:30:00Z",
    "type": "USER"
  }
}
```

### GET /api/chat/images/{imagePath}
Serves uploaded images.

**Parameters:**
- `imagePath`: Filename (UUID + extension)

**Response:**
- Image binary data with appropriate Content-Type header

## Message Model Changes

### ContentType Enum
```java
public enum ContentType {
    TEXT,       // Regular text message
    CLIPBOARD   // Clipboard content with text and/or images
}
```

### Message Fields
- `contentType`: Type of message content
- `clipboardData`: ClipboardData object (for CLIPBOARD type)

## Frontend Changes

### UI Components
1. **Clipboard Button**: Green button with emoji (📋)
2. **Image Display**: Responsive grid layout for images in messages
3. **Hover Effects**: Images scale up slightly on hover
4. **Error Handling**: User-friendly error messages for clipboard access failures

### Keyboard Shortcuts
- `Ctrl+Enter` / `Cmd+Enter`: Send message
- Previously was `Shift+Enter`

### Clipboard API Usage
```javascript
async pasteFromClipboard() {
    const clipboardItems = await navigator.clipboard.read();
    // Process text and images
    // Send to server
}
```

## Testing

### Test Coverage
- All 60 existing tests pass
- Updated `JobTypeTest` to include `CLIPBOARD_CONTENT`
- No security vulnerabilities detected by CodeQL

### Test Areas
1. JobType enum values and serialization
2. Model class instantiation
3. Service integration tests
4. Elasticsearch connectivity

## Dependencies Added

### Maven Dependency
```xml
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.9.0</version>
</dependency>
```

### System Dependencies
- **Tesseract OCR** (optional): Required for OCR functionality
  - If not installed, OCR gracefully degrades
  - Installation: `apt-get install tesseract-ocr` (Linux)
- **Tesseract Language Data**: English data files
  - Path: `/usr/share/tesseract-ocr/*/tessdata`

## Configuration

### No New Configuration Required
All existing configuration in `application.properties` is sufficient:
- Elasticsearch settings used for indexing
- Ollama settings used for AI title generation
- No new properties needed

## Error Handling

### Graceful Degradation
1. **OCR Service Not Available**: 
   - Images stored without OCR keywords
   - Feature continues to work

2. **Elasticsearch Not Available**:
   - Content processed and displayed in chat
   - Not indexed for future search
   - Warning message sent to user

3. **AI Service Failure**:
   - Default title "Clipboard Content" used
   - Processing continues

4. **Image Save Failure**:
   - Error logged
   - User notified via chat message

### Frontend Error Handling
1. **Clipboard API Not Supported**: Alert message to user
2. **No Clipboard Permission**: Prompts user to grant permission
3. **FileReader Error**: Caught and reported to user
4. **Network Error**: Retry logic in axios

## Performance Considerations

### Optimization Strategies
1. **Async Processing**: Heavy operations don't block user interface
2. **Thread Pool**: Limited to 5 concurrent operations
3. **Image Size**: No server-side resizing (client sends as-is)
4. **OCR Keyword Limit**: Max 20 keywords per image
5. **Title Length**: Truncated at 100 characters

### Resource Usage
- **Memory**: Images converted to base64 in memory
- **Disk**: Images stored permanently in work/images/
- **CPU**: OCR and AI processing in background threads
- **Network**: Images uploaded as base64 (larger than binary)

## Future Enhancements

### Potential Improvements
1. **Image Compression**: Resize large images before upload
2. **Multiple Languages**: OCR support for languages beyond English
3. **Image Preview**: Thumbnail generation for large images
4. **Clipboard History**: Store recent clipboard items
5. **Drag & Drop**: Support file drag and drop
6. **Progress Indicator**: Show OCR/AI processing progress
7. **Binary Upload**: Use FormData for more efficient image upload
8. **Image Annotations**: Allow users to add captions to images
9. **PDF Support**: Extract text from PDF files
10. **Voice Notes**: Support audio from clipboard

## Security Considerations

### Security Measures
1. **Path Traversal Prevention**: UUID-based filenames prevent directory traversal
2. **Content Type Validation**: Only image types accepted
3. **File Size Limits**: No explicit limits (consider adding)
4. **Authentication**: Uses existing user authentication
5. **Authorization**: Images only accessible via authenticated endpoints
6. **XSS Prevention**: Images served with correct Content-Type headers
7. **CodeQL Analysis**: No vulnerabilities detected

### Security Recommendations
1. Add maximum file size limit (e.g., 10MB per image)
2. Implement rate limiting on clipboard endpoint
3. Add virus scanning for uploaded images
4. Implement image content validation
5. Add CSRF protection if not already present

## Maintenance

### Monitoring
1. Monitor work/images directory size
2. Track OCR processing time
3. Monitor Elasticsearch index size
4. Log failed clipboard operations

### Cleanup
- No automatic cleanup implemented
- Consider periodic cleanup of old images
- Elasticsearch retention policies apply

## Documentation Files
- Implementation plan: PR description
- Code comments: Inline in Java and JavaScript
- This file: CLIPBOARD_FEATURE.md

## Contact
For questions or issues related to this feature, please create an issue in the GitHub repository.
