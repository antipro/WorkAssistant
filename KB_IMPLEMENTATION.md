# KB Index Implementation

## Overview

This document describes the implementation of the unified Knowledge Base (KB) index and the KB query function calling feature for the WorkAssistant application.

## Changes Made

### 1. Unified KB Index

**Previous Setup:**
- Two separate Elasticsearch indices:
  - `work_assistant_summaries` - for AI-generated summaries
  - `work_assistant_summaries_clipboard` - for clipboard content

**New Setup:**
- Single unified index: `kb` (Knowledge Base)
- Stores both summaries and clipboard content in the same index
- Unified schema supporting all document types

**Benefits:**
- Simplified index management
- Unified search across all knowledge types
- Better resource utilization
- Consistent naming convention

### 2. Index Schema

The KB index uses a unified schema that supports both summary documents and clipboard content:

```json
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "text": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "keywords": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "timestamp": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis"
      },
      "channelId": {
        "type": "keyword"
      },
      "userId": {
        "type": "keyword"
      },
      "images": {
        "type": "nested",
        "properties": {
          "path": {
            "type": "keyword"
          },
          "keywords": {
            "type": "text",
            "analyzer": "ik_max_word",
            "search_analyzer": "ik_smart"
          }
        }
      }
    }
  }
}
```

**Field Descriptions:**
- `title` - Title of the document (summaries and clipboard content)
- `content` - Main content in markdown format (summaries)
- `text` - Plain text content (clipboard content)
- `keywords` - Keywords extracted from content
- `timestamp` - Creation timestamp
- `channelId` - Associated channel ID
- `userId` - User who created the document
- `images` - Nested array of images with paths and OCR keywords (clipboard content)

### 3. KB Query Function

Added a new AI function calling capability: `query_kb`

**Function Definition:**
```json
{
  "type": "function",
  "function": {
    "name": "query_kb",
    "description": "Search the knowledge base (kb) for relevant information. Use this to find summaries, clipboard content, notes, and other stored knowledge.",
    "parameters": {
      "type": "object",
      "required": ["query"],
      "properties": {
        "query": {
          "type": "string",
          "description": "Search query to find relevant knowledge. Can be keywords, phrases, or questions."
        },
        "maxResults": {
          "type": "integer",
          "description": "Maximum number of results to return (default: 5, max: 20)"
        }
      }
    }
  }
}
```

**How It Works:**

1. **User Query:** User asks a question that requires knowledge from the KB
   ```
   @eking what did we discuss about the new feature last week?
   ```

2. **AI Decision:** The AI model receives both KB and Zentao function tools and decides to call `query_kb`

3. **Function Execution:** The application searches the KB index using Elasticsearch

4. **Result Processing:** Search results are returned to the AI in JSON format

5. **AI Response:** The AI generates a natural language summary of the findings
   ```
   Based on the knowledge base, last week you discussed the new authentication feature...
   ```

### 4. Elasticsearch Status Page

Created a web-based status page at `/status.html` that displays:

- **Index Name:** Current KB index name
- **Total Documents:** Number of documents stored
- **Active Shards:** Number of active shards
- **Active Primary Shards:** Number of active primary shards
- **Health Status:** Index health (green/yellow/red)
- **Auto-refresh:** Updates every 30 seconds

**Access:** Navigate to `http://localhost:8080/status.html`

## API Changes

### New Endpoint

**GET /api/elasticsearch/status**

Returns Elasticsearch KB index status information.

**Response Example:**
```json
{
  "success": true,
  "data": {
    "exists": true,
    "indexName": "kb",
    "documentCount": 42,
    "status": "green",
    "activeShards": 1,
    "activePrimaryShards": 1,
    "available": true
  }
}
```

## Configuration Changes

**application.properties:**
```properties
# Old
elasticsearch.index=work_assistant_summaries

# New
elasticsearch.index=kb
```

## Usage Examples

### Example 1: Storing Knowledge

**User:** `@eking please summarize our discussion about microservices architecture`

**Result:** AI generates a summary and stores it in the KB index

### Example 2: Querying Knowledge

**User:** `@eking what did we decide about the database choice?`

**AI Action:** 
- Recognizes this is a knowledge query
- Calls `query_kb` function with query: "database choice"
- Searches KB index for relevant documents
- Returns natural language summary based on findings

**Response:** "Based on previous discussions, you decided to use PostgreSQL for..."

### Example 3: Combined Query

**User:** `@eking show me my tasks and any notes about testing`

**AI Action:**
- Recognizes two needs: tasks (Zentao) and notes (KB)
- May call both `get_tasks` and `query_kb` functions
- Combines results into a comprehensive response

## Code Structure

### New Files

1. **KBFunctionProvider.java**
   - Provides KB function definitions
   - Combines KB and Zentao tools
   - Similar pattern to ZentaoFunctionProvider

2. **ElasticsearchController.java**
   - Handles Elasticsearch status endpoint
   - Returns index statistics

3. **status.html**
   - Web-based status page
   - Real-time index monitoring

### Modified Files

1. **ElasticsearchService.java**
   - Unified index creation
   - Combined schema for all document types
   - Added `getIndexStatus()` method
   - Removed separate clipboard index logic

2. **ChatController.java**
   - Integrated KB query function
   - Uses combined function tools (KB + Zentao)
   - Added `executeKBQuery()` method

3. **WorkAssistantApplication.java**
   - Registered Elasticsearch status endpoint

4. **application.properties.example**
   - Updated default index name to "kb"

## Testing

### New Tests

**KBFunctionProviderTest.java** - 10 tests covering:
- Function tool structure validation
- Query KB function definition
- Parameter validation
- Combined tools (KB + Zentao)
- JSON serialization

**Test Results:**
- Total: 70 tests
- Passed: 70
- Failed: 0
- Coverage: Complete for new functionality

## Migration Notes

### From Old Indices to KB

If you have existing data in the old indices:

1. **Data Migration:**
   ```bash
   # Reindex from old summary index to kb
   POST /_reindex
   {
     "source": { "index": "work_assistant_summaries" },
     "dest": { "index": "kb" }
   }
   
   # Reindex from old clipboard index to kb
   POST /_reindex
   {
     "source": { "index": "work_assistant_summaries_clipboard" },
     "dest": { "index": "kb" }
   }
   ```

2. **Delete Old Indices (optional):**
   ```bash
   DELETE /work_assistant_summaries
   DELETE /work_assistant_summaries_clipboard
   ```

3. **Or Start Fresh:**
   - Simply delete the old indices
   - The kb index will be created automatically on first use

## Future Enhancements

Potential improvements for consideration:

1. **Advanced Search:**
   - Add filters for date ranges
   - Search by user or channel
   - Search by document type

2. **Bulk Operations:**
   - Batch import/export
   - Backup and restore

3. **Analytics:**
   - Most searched terms
   - Knowledge usage patterns
   - Popular documents

4. **Enhanced Status Page:**
   - Document type breakdown
   - Storage usage graphs
   - Recent additions

## Troubleshooting

### Issue: KB index not found

**Solution:** The index is created automatically on first document storage. Create a summary or store clipboard content to initialize the index.

### Issue: Function calling not working

**Solution:** Ensure you're using an Ollama model that supports function calling (e.g., qwen2.5, llama3.1, mistral-nemo).

### Issue: Status page shows "not available"

**Solution:** Check Elasticsearch connection settings in application.properties and ensure Elasticsearch is running.

## References

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Ollama Function Calling](https://ollama.com/blog/tool-support)
- [IK Analyzer Plugin](https://github.com/medcl/elasticsearch-analysis-ik)
