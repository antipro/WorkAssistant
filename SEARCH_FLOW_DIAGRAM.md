# SEARCH Action Flow Diagram

## Before This Implementation

```
User Query: "@eking search project architecture"
           ↓
    Extract Keywords
           ↓
    Search Elasticsearch
           ↓
    Format Results (Markdown)
           ↓
    Send to User
```

**Result:** Raw search results in markdown format

---

## After This Implementation

```
User Query: "@eking search project architecture"
           ↓
    Extract Keywords
           ↓
    Search Elasticsearch
           ↓
    ┌──────────────────────────────┐
    │  Format for AI Processing    │
    │  - Structured format         │
    │  - Content truncation (5KB)  │
    │  - Null safety               │
    └──────────────────────────────┘
           ↓
    ┌──────────────────────────────┐
    │  Sanitize Search Query       │
    │  - Remove newlines           │
    │  - Prevent prompt injection  │
    └──────────────────────────────┘
           ↓
    ┌──────────────────────────────┐
    │  Create AI Prompt            │
    │  - Include sanitized query   │
    │  - Include formatted results │
    │  - Add context instructions  │
    └──────────────────────────────┘
           ↓
    ┌──────────────────────────────┐
    │  Send to AI Model (Ollama)   │
    │  - ollamaService.generate()  │
    └──────────────────────────────┘
           ↓
    ┌──────────────────────────────┐
    │  Check AI Response           │
    │  - Null check                │
    │  - Empty check               │
    └──────────────────────────────┘
           ↓
    ┌─────────────┬────────────────┐
    │   Success   │     Failure    │
    └─────────────┴────────────────┘
           ↓              ↓
    AI Processed    Fallback to
     Response       Markdown Format
           ↓              ↓
           └──────┬───────┘
                  ↓
           Send to User
```

**Result:** Natural language response that:
- Summarizes the search results
- Answers the user's query contextually
- Provides relevant details in a conversational format

---

## Error Handling Flow

```
AI Processing Attempt
        ↓
  ┌─────┴─────┐
  │ Try Block │
  └─────┬─────┘
        ↓
  ┌──────────────────┐
  │ AI generates     │ ──── Success ────→ Send AI Response
  │ response         │
  └──────────────────┘
        ↓
   Exception?
        ↓
  ┌──────────────────┐
  │ Catch Block      │
  │ - Log error      │
  │ - Format results │
  │ - Send fallback  │
  └──────────────────┘
        ↓
   Send Markdown
    (Fallback)
```

---

## Security Layers

```
User Input: "@eking search <query>"
     ↓
┌────────────────────────┐
│ Layer 1: Input         │
│ - Extract keywords     │
│ - Basic validation     │
└────────────────────────┘
     ↓
┌────────────────────────┐
│ Layer 2: Query         │
│ Sanitization           │
│ - Remove newlines      │
│ - Prevent injection    │
└────────────────────────┘
     ↓
┌────────────────────────┐
│ Layer 3: Content       │
│ Limiting               │
│ - Max 5000 chars/doc   │
│ - Truncate indication  │
└────────────────────────┘
     ↓
┌────────────────────────┐
│ Layer 4: Null Safety   │
│ - Check all fields     │
│ - Provide defaults     │
└────────────────────────┘
     ↓
┌────────────────────────┐
│ Layer 5: Error         │
│ Handling               │
│ - Try-catch blocks     │
│ - Graceful fallback    │
└────────────────────────┘
     ↓
  Secure AI Response
```

---

## Example Transformation

### Input
```
User: @eking search project architecture
```

### Elasticsearch Results (Raw)
```json
[
  {
    "title": "System Architecture Discussion",
    "content": "We discussed the microservices architecture...",
    "keywords": ["architecture", "microservices", "project"],
    "timestamp": "2025-01-15T10:30:00"
  },
  {
    "title": "Database Architecture Notes",
    "content": "Notes on the database design patterns...",
    "keywords": ["database", "architecture", "design"],
    "timestamp": "2025-01-16T14:20:00"
  }
]
```

### AI Prompt (Generated)
```
Based on the user's search query: "project architecture"

Here are the search results from the knowledge base:

Found 2 matching documents:

Document 1:
Title: System Architecture Discussion
Content: We discussed the microservices architecture...
Keywords: architecture, microservices, project
Created: 2025-01-15T10:30:00

Document 2:
Title: Database Architecture Notes
Content: Notes on the database design patterns...
Keywords: database, architecture, design
Created: 2025-01-16T14:20:00

Please provide a helpful, natural language response that summarizes 
these results and answers the user's query. Include relevant details 
and format the response in a clear, readable way.
```

### AI Response (Output)
```
Based on your search for "project architecture", I found two relevant 
discussions:

1. **Microservices Architecture:** Your team discussed implementing a 
   microservices architecture for the new project on January 15th. The 
   conversation covered service boundaries, communication patterns, and 
   deployment strategies.

2. **Database Design:** There are also notes from January 16th about 
   database architecture, focusing on design patterns and best practices 
   for the database layer.

Both discussions are related to the overall system architecture. Would 
you like more details about either of these topics?
```

---

## Benefits Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Response Format** | Raw markdown with all results | Natural language summary |
| **Context Awareness** | None | Query-aware responses |
| **User Experience** | Technical, structured | Conversational, friendly |
| **Security** | Basic | Multi-layer protection |
| **Error Handling** | Simple | Comprehensive with fallback |
| **Content Safety** | No limits | 5KB per document limit |
| **Null Safety** | Partial | Complete |
