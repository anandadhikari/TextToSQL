
package com.ai.texttosql.service;

import com.ai.texttosql.model.QueryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueryExecutionService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public QueryResponse executeQuery(String sqlQuery) {
        QueryResponse response = new QueryResponse();
        response.setGeneratedSql(sqlQuery);
        response.setTimestamp(Instant.now());

        long startTime = System.currentTimeMillis();

        try {
            Query query = entityManager.createNativeQuery(sqlQuery);
            List<Object[]> results = query.getResultList();

            long executionTime = System.currentTimeMillis() - startTime;

            response.setResults(convertResultsToMap(results));
            response.setExecutionMetrics(new QueryResponse.ExecutionMetrics(
                    executionTime,
                    results.size(),
                    "SUCCESS"
            ));
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            response.setExecutionMetrics(new QueryResponse.ExecutionMetrics(
                    executionTime,
                    0,
                    "FAILED"
            ));
            response.setError(e.getMessage());
        }

        return response;
    }

    private List<Map<String, Object>> convertResultsToMap(List<Object[]> results) {
        return results.stream()
                .map(row -> {
                    Object[] columns = row;
                    Map<String, Object> rowMap = new java.util.HashMap<>();
                    for (int i = 0; i < columns.length; i++) {
                        rowMap.put("column_" + (i + 1), columns[i]);
                    }
                    return rowMap;
                })
                .collect(Collectors.toList());
    }
}