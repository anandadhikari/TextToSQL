
package com.ai.texttosql.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "natural_language_query", nullable = false)
    private String naturalLanguageQuery;

    @Column(name = "generated_sql", nullable = false, columnDefinition = "TEXT")
    private String generatedSql;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(name = "timestamp")
    private Instant timestamp;

    @Column(name = "execution_time_millis")
    private Long executionTimeMillis;

    @Column(name = "result_count")
    private Integer resultCount;

    @Column(name = "status")
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_metrics", columnDefinition = "json")
    private Map<String, Object> executionMetrics;
}
