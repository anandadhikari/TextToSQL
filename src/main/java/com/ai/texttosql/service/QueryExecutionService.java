package com.ai.texttosql.service;

import com.ai.texttosql.config.MetricsConfig;
import com.ai.texttosql.exception.QueryExecutionException;
import com.ai.texttosql.model.QueryResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutionService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;
    private final Timer queryExecutionTimer;

    @Transactional(readOnly = true)
    public QueryResponse executeQuery(String sqlQuery) {
        return executeQuery(sqlQuery, PageRequest.of(0, 100));
    }

    @Transactional(readOnly = true)
    public QueryResponse executeQuery(String sqlQuery, Pageable pageable) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        QueryResponse response = new QueryResponse();
        response.setGeneratedSql(sqlQuery);
        response.setTimestamp(Instant.now());

        try {
            validateSqlQuery(sqlQuery);
            log.info("Executing SQL query: {}", sqlQuery);

            // Create count query for pagination
            String countSql = "SELECT COUNT(*) FROM (" + sqlQuery + ") as count_query";
            Query countQuery = entityManager.createNativeQuery(countSql);
            long total = ((Number) countQuery.getSingleResult()).longValue();

            // Create the main query with pagination
            Query query = entityManager.createNativeQuery(sqlQuery);
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            
            // Execute query and process results
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> resultMaps = convertResultsToMap(results);

            // Build response
            response.setResults(resultMaps);
            response.setPage(createPageInfo(pageable, total));
            response.setExecutionMetrics(createExecutionMetrics(stopWatch, results.size(), "SUCCESS"));

            // Record successful execution
            meterRegistry.counter("query.execution", "status", "success").increment();
            
            return response;

        } catch (Exception e) {
            log.error("Error executing SQL query: {}", sqlQuery, e);
            response.setExecutionMetrics(createExecutionMetrics(stopWatch, 0, "FAILED"));
            response.setError("Error executing query: " + e.getMessage());
            
            // Record failed execution
            meterRegistry.counter("query.execution", "status", "failed", "error", e.getClass().getSimpleName())
                    .increment();
                    
            throw new QueryExecutionException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            stopWatch.stop();
            queryExecutionTimer.record(stopWatch.getTotalTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            log.debug("Query execution completed in {} ms", stopWatch.getTotalTimeMillis());
        }
    }

    private QueryResponse.PageInfo createPageInfo(Pageable pageable, long total) {
        return new QueryResponse.PageInfo(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                total
        );
    }

    private QueryResponse.ExecutionMetrics createExecutionMetrics(StopWatch stopWatch, int resultCount, String status) {
        return new QueryResponse.ExecutionMetrics(
                stopWatch.getTotalTimeMillis(),
                resultCount,
                status
        );
    }

    private void validateSqlQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query cannot be empty");
        }
        
        // Basic SQL injection prevention - this is a simple check and should be enhanced
        String upperQuery = sqlQuery.toUpperCase();
        if (upperQuery.matches("(?i).*\\b(DROP|DELETE|TRUNCATE|ALTER|CREATE|RENAME|GRANT|REVOKE)\\b.*")) {
            throw new SecurityException("Modification queries are not allowed");
        }
    }

    private List<Map<String, Object>> convertResultsToMap(List<Object[]> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        return results.stream()
                .map(this::convertRowToMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> convertRowToMap(Object[] row) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < row.length; i++) {
            rowMap.put("column_" + (i + 1), convertValue(row[i]));
        }
        return rowMap;
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        // Convert specific types if needed
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValue();
        }
        return value;
    }
}