
package com.ai.texttosql.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {
    private String naturalLanguageQuery;
    private String generatedSql;
    private String explanation;
    private List<Map<String, Object>> results;
    private ExecutionMetrics executionMetrics;
    private Instant timestamp;
    private String queryId;
    private String error;

    @Data
    @AllArgsConstructor
    public static class ExecutionMetrics {
        private long executionTimeMillis;
        private int resultCount;
        private String status;
    }
}