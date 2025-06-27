package com.ai.texttosql.service;

import com.ai.texttosql.model.QueryHistory;
import com.ai.texttosql.model.QueryResponse;
import com.ai.texttosql.repository.QueryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryHistoryService {

    private static final int MAX_RECENT_QUERIES = 50;
    
    private final QueryHistoryRepository queryHistoryRepository;

    @Transactional
    public QueryHistory saveQuery(QueryResponse queryResponse, String userId) {
        try {
            QueryHistory queryHistory = new QueryHistory();
            queryHistory.setNaturalLanguageQuery(queryResponse.getNaturalLanguageQuery());
            queryHistory.setGeneratedSql(queryResponse.getGeneratedSql());
            queryHistory.setExplanation(queryResponse.getExplanation());
            queryHistory.setTimestamp(queryResponse.getTimestamp() != null ? queryResponse.getTimestamp() : Instant.now());
            queryHistory.setId(UUID.randomUUID());

            // Extract execution metrics from the nested object
            if (queryResponse.getExecutionMetrics() != null) {
                QueryResponse.ExecutionMetrics metrics = queryResponse.getExecutionMetrics();
                queryHistory.setExecutionTimeMillis(metrics.getExecutionTimeMs());
                queryHistory.setResultCount(metrics.getResultCount());
                queryHistory.setStatus(metrics.getStatus());

                Map<String, Object> map = new HashMap<>();
                map.put("executionTimeMillis", metrics.getExecutionTimeMs());
                map.put("resultCount", metrics.getResultCount());
                map.put("status", metrics.getStatus());
                queryHistory.setExecutionMetrics(map);
            }

            queryHistory.setUserId(userId);
            return queryHistoryRepository.save(queryHistory);
        } catch (Exception e) {
            log.error("Failed to save query history: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save query history", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getQueryHistory(Pageable pageable) {
        return queryHistoryRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<QueryHistory> getQueryById(UUID queryId) {
        return queryHistoryRepository.findById(queryId);
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getQueryHistoryByUserID(String userId, Pageable pageable) {
        return queryHistoryRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<QueryHistory> getRecentQueries(Pageable pageable) {
        return queryHistoryRepository.findAllByOrderByTimestampDesc(
            PageRequest.of(0, Math.min(pageable.getPageSize(), MAX_RECENT_QUERIES))
        );
    }
}