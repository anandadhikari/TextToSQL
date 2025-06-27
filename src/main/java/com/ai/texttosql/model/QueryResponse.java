package com.ai.texttosql.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    private String naturalLanguageQuery;
    private String generatedSql;
    private List<Map<String, Object>> results;
    private String explanation;
    private String error;
    private Instant timestamp;
    private PageInfo page;
    private ExecutionMetrics executionMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;

        public PageInfo(int pageNumber, int pageSize, long totalElements) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMetrics {
        private long executionTimeMs;
        private int resultCount;
        private String status;
        private String databaseType;
        private String queryType;

        public ExecutionMetrics(long executionTimeMs, int resultCount, String status) {
            this.executionTimeMs = executionTimeMs;
            this.resultCount = resultCount;
            this.status = status;
            this.databaseType = "MySQL";
            this.queryType = "SELECT"; // Default, can be overridden
        }
    }
}