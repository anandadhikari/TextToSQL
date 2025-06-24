package com.ai.texttosql.service;

import com.ai.texttosql.model.QueryHistory;
import com.ai.texttosql.model.QueryResponse;
import com.ai.texttosql.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QueryHistoryService {

    private final QueryHistoryRepository queryHistoryRepository;

    public QueryHistory saveQuery(QueryResponse queryResponse) {
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setNaturalLanguageQuery(queryResponse.getNaturalLanguageQuery());
        queryHistory.setGeneratedSql(queryResponse.getGeneratedSql());
        queryHistory.setExplanation(queryResponse.getExplanation());
        queryHistory.setTimestamp(queryResponse.getTimestamp() != null ? queryResponse.getTimestamp() : Instant.now());
        queryHistory.setId(UUID.randomUUID());

        // Extract execution metrics from the nested object
        if (queryResponse.getExecutionMetrics() != null) {
            QueryResponse.ExecutionMetrics metrics = queryResponse.getExecutionMetrics();
            queryHistory.setExecutionTimeMillis(metrics.getExecutionTimeMillis());
            queryHistory.setResultCount(metrics.getResultCount());
            queryHistory.setStatus(metrics.getStatus());

            Map<String, Object> map = new HashMap<>();
            map.put("executionTimeMillis", metrics.getExecutionTimeMillis());
            map.put("resultCount", metrics.getResultCount());
            map.put("status", metrics.getStatus());
            queryHistory.setExecutionMetrics(map); // Also store the full metrics object if needed
        }

        // Set a default userId or leave it null/empty
        queryHistory.setUserId("system"); // or just don't set it if you want null

        return queryHistoryRepository.save(queryHistory);
    }

    public List<QueryHistory> getQueryHistory() {
        return queryHistoryRepository.findAllByOrderByTimestampDesc();
    }

    public Optional<QueryHistory> getQueryById(UUID queryId) {
        return queryHistoryRepository.findById(queryId);
    }

    public List<QueryHistory> getRecentQueries() {
        return queryHistoryRepository.findTop50ByOrderByTimestampDesc();
    }
}