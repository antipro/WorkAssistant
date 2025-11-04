package com.workassistant.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.workassistant.config.AppConfig;
import com.workassistant.model.SummaryDocument;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Elasticsearch operations
 * Manages index template creation and document storage
 */
public class ElasticsearchService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);
    private static ElasticsearchService instance;
    
    private ElasticsearchClient client;
    private final String indexName;
    private final String host;
    private final int port;

    private ElasticsearchService() {
        AppConfig config = AppConfig.getInstance();
        this.host = config.getProperty("elasticsearch.host", "localhost");
        this.port = config.getIntProperty("elasticsearch.port", 9200);
        this.indexName = config.getProperty("elasticsearch.index", "work_assistant_summaries");
        
        try {
            initializeClient();
            createIndexWithTemplate();
        } catch (Exception e) {
            logger.error("Failed to initialize Elasticsearch client", e);
        }
    }

    public static synchronized ElasticsearchService getInstance() {
        if (instance == null) {
            instance = new ElasticsearchService();
        }
        return instance;
    }

    private void initializeClient() {
        RestClient restClient = RestClient.builder(
            new HttpHost(host, port, "http")
        ).build();
        
        RestClientTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper()
        );
        
        this.client = new ElasticsearchClient(transport);
        logger.info("Elasticsearch client initialized: {}:{}", host, port);
    }

    /**
     * Create index with IK analyzer template if it doesn't exist
     */
    private void createIndexWithTemplate() throws IOException {
        try {
            // Check if index exists
            boolean exists = client.indices().exists(
                ExistsRequest.of(e -> e.index(indexName))
            ).value();
            
            if (!exists) {
                // Create index with mappings
                Map<String, Property> properties = new HashMap<>();
                
                // title field with IK analyzer
                properties.put("title", Property.of(p -> p
                    .text(TextProperty.of(t -> t
                        .analyzer("ik_max_word")
                        .searchAnalyzer("ik_smart")
                    ))
                ));
                
                // content field with IK analyzer (markdown format)
                properties.put("content", Property.of(p -> p
                    .text(TextProperty.of(t -> t
                        .analyzer("ik_max_word")
                        .searchAnalyzer("ik_smart")
                    ))
                ));
                
                // keywords field with IK analyzer
                properties.put("keywords", Property.of(p -> p
                    .text(TextProperty.of(t -> t
                        .analyzer("ik_max_word")
                        .searchAnalyzer("ik_smart")
                    ))
                ));
                
                // timestamp field
                properties.put("timestamp", Property.of(p -> p
                    .date(DateProperty.of(d -> d
                        .format("strict_date_optional_time||epoch_millis")
                    ))
                ));
                
                // channelId field (keyword - not analyzed)
                properties.put("channelId", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))
                ));
                
                // userId field (keyword - not analyzed)
                properties.put("userId", Property.of(p -> p
                    .keyword(KeywordProperty.of(k -> k))
                ));
                
                // Create index with mappings
                client.indices().create(CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(TypeMapping.of(m -> m
                        .properties(properties)
                    ))
                ));
                
                logger.info("Created Elasticsearch index: {} with IK analyzer mappings", indexName);
            } else {
                logger.info("Elasticsearch index already exists: {}", indexName);
            }
        } catch (Exception e) {
            logger.warn("Could not create Elasticsearch index (IK analyzer might not be available): {}", e.getMessage());
            // Try creating with standard analyzer as fallback
            createIndexWithStandardAnalyzer();
        }
    }

    /**
     * Fallback: Create index with standard analyzer if IK is not available
     */
    private void createIndexWithStandardAnalyzer() throws IOException {
        try {
            boolean exists = client.indices().exists(
                ExistsRequest.of(e -> e.index(indexName))
            ).value();
            
            if (!exists) {
                Map<String, Property> properties = new HashMap<>();
                
                properties.put("title", Property.of(p -> p.text(TextProperty.of(t -> t))));
                properties.put("content", Property.of(p -> p.text(TextProperty.of(t -> t))));
                properties.put("keywords", Property.of(p -> p.text(TextProperty.of(t -> t))));
                properties.put("timestamp", Property.of(p -> p
                    .date(DateProperty.of(d -> d.format("strict_date_optional_time||epoch_millis")))
                ));
                properties.put("channelId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
                properties.put("userId", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));
                
                client.indices().create(CreateIndexRequest.of(c -> c
                    .index(indexName)
                    .mappings(TypeMapping.of(m -> m.properties(properties)))
                ));
                
                logger.info("Created Elasticsearch index: {} with standard analyzer", indexName);
            }
        } catch (Exception e) {
            logger.error("Failed to create Elasticsearch index with standard analyzer", e);
        }
    }

    /**
     * Index a summary document to Elasticsearch
     */
    public String indexSummary(SummaryDocument document) throws IOException {
        IndexResponse response = client.index(IndexRequest.of(i -> i
            .index(indexName)
            .id(document.getId())
            .document(document)
        ));
        
        logger.info("Indexed document: {} with result: {}", document.getId(), response.result());
        return response.id();
    }

    /**
     * Check if Elasticsearch is available
     */
    public boolean isAvailable() {
        try {
            return client.ping().value();
        } catch (Exception e) {
            logger.warn("Elasticsearch not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Search for summaries by keywords
     * Searches across title, content, and keywords fields
     * 
     * @param queryText The search query text
     * @param maxResults Maximum number of results to return (default: 10)
     * @return List of matching SummaryDocument objects
     */
    public List<SummaryDocument> searchSummaries(String queryText, int maxResults) throws IOException {
        List<SummaryDocument> results = new ArrayList<>();
        
        if (queryText == null || queryText.trim().isEmpty()) {
            return results;
        }
        
        // Create a multi-match query that searches across title, content, and keywords
        Query multiMatchQuery = Query.of(q -> q
            .multiMatch(m -> m
                .query(queryText)
                .fields("title^3", "content", "keywords^2")  // Boost title and keywords
            )
        );
        
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(indexName)
            .query(multiMatchQuery)
            .size(maxResults)
        );
        
        SearchResponse<SummaryDocument> response = client.search(searchRequest, SummaryDocument.class);
        
        for (Hit<SummaryDocument> hit : response.hits().hits()) {
            SummaryDocument doc = hit.source();
            if (doc != null) {
                results.add(doc);
            }
        }
        
        logger.info("Found {} summaries for query: {}", results.size(), queryText);
        return results;
    }
    
    /**
     * Search for summaries by keywords with default max results (10)
     */
    public List<SummaryDocument> searchSummaries(String queryText) throws IOException {
        return searchSummaries(queryText, 10);
    }
    
    /**
     * Get summaries from a specific channel
     * 
     * @param channelId The channel ID to filter by
     * @param maxResults Maximum number of results to return
     * @return List of summaries from the specified channel
     */
    public List<SummaryDocument> getSummariesByChannel(String channelId, int maxResults) throws IOException {
        List<SummaryDocument> results = new ArrayList<>();
        
        if (channelId == null || channelId.trim().isEmpty()) {
            return results;
        }
        
        Query termQuery = Query.of(q -> q
            .term(t -> t
                .field("channelId")
                .value(channelId)
            )
        );
        
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(indexName)
            .query(termQuery)
            .size(maxResults)
            .sort(so -> so
                .field(f -> f
                    .field("timestamp")
                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                )
            )
        );
        
        SearchResponse<SummaryDocument> response = client.search(searchRequest, SummaryDocument.class);
        
        for (Hit<SummaryDocument> hit : response.hits().hits()) {
            SummaryDocument doc = hit.source();
            if (doc != null) {
                results.add(doc);
            }
        }
        
        logger.info("Found {} summaries for channel: {}", results.size(), channelId);
        return results;
    }
}
