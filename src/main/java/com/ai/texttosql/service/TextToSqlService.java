package com.ai.texttosql.service;

import com.ai.texttosql.config.MetricsConfig;
import com.ai.texttosql.config.OllamaClient;
import com.ai.texttosql.exception.QueryGenerationException;
import com.ai.texttosql.model.QueryRequest;
import com.ai.texttosql.model.QueryResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextToSqlService {

    private final SchemaAnalysisService schemaAnalysisService;
    private final OllamaClient ollamaClient;
    private final MeterRegistry meterRegistry;
    private final Timer queryConversionTimer;

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)^(SELECT|INSERT|UPDATE|DELETE|WITH|CREATE|DROP|ALTER)\\b.*"
    );

    private static final Pattern SQL_CONTINUATION = Pattern.compile(
            "(?i)^(FROM|WHERE|JOIN|LEFT|RIGHT|INNER|OUTER|GROUP|ORDER|HAVING|UNION|LIMIT|OFFSET)\\b.*"
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "'(\\d{4}-\\d{2}-\\d{2})'(?!\\s+\\d{2}:\\d{2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    public QueryResponse generateSqlQuery(QueryRequest request) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            validateRequest(request);
            String schemaContext = request.isIncludeSchemaContext()
                    ? schemaAnalysisService.generateSchemaContext()
                    : "No schema context provided.";

            String prompt = buildPrompt(schemaContext, request.getNaturalLanguageQuery());
            log.debug("Sending prompt to Ollama: {}", prompt);
            
            String rawResponse = ollamaClient.ask(prompt);
            log.debug("Ollama raw response: {}", rawResponse);

            String sqlQuery = extractSqlFromResponse(rawResponse);
            if (sqlQuery.trim().isEmpty()) {
                throw new QueryGenerationException("Failed to extract valid SQL from response");
            }

            sqlQuery = postProcessSql(sqlQuery);
            log.info("Generated SQL: {}", sqlQuery);

            QueryResponse response = new QueryResponse();
            response.setNaturalLanguageQuery(request.getNaturalLanguageQuery());
            response.setGeneratedSql(sqlQuery);
            response.setTimestamp(Instant.now());

            if (request.isExplainQuery()) {
                response.setExplanation(explainQuery(sqlQuery, request.getNaturalLanguageQuery()));
            }

            // Record successful query generation
            meterRegistry.counter("query.generation", "status", "success").increment();
            return response;

        } catch (Exception e) {
            log.error("Error generating SQL query for request: {}", request.getNaturalLanguageQuery(), e);
            meterRegistry.counter("query.generation", "status", "failed", "error", e.getClass().getSimpleName()).increment();
            throw new QueryGenerationException("Failed to generate SQL query: " + e.getMessage(), e);
        } finally {
            stopWatch.stop();
            queryConversionTimer.record(stopWatch.getTotalTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            log.debug("Query generation completed in {} ms", stopWatch.getTotalTimeMillis());
        }
    }

    private void validateRequest(QueryRequest request) {
        if (request == null || request.getNaturalLanguageQuery() == null || request.getNaturalLanguageQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Natural language query cannot be empty");
        }
    }

    private String buildPrompt(String schemaContext, String naturalLanguageQuery) {
        return String.format("""
                ### Database Schema:
                %s
                
                ### Task:
                Convert the following natural language query to SQL:
                "%s"
                
                ### Instructions:
                1. Generate only the SQL query without any explanations or markdown formatting
                2. Ensure the query is syntactically correct
                3. Use proper table aliases for better readability
                4. Include all necessary JOIN conditions
                5. Add appropriate WHERE clauses based on the query
                6. Include ORDER BY if sorting is implied
                
                ### SQL Query:
                """, schemaContext, naturalLanguageQuery);
    }

    private String extractSqlFromResponse(String response) {
        // Remove markdown code blocks if present
        String sql = response.replaceAll("```(?:sql)?\n?|```", "").trim();
        
        // Find the first SQL statement
        Matcher matcher = SQL_KEYWORDS.matcher(sql);
        if (matcher.find()) {
            return sql.substring(matcher.start());
        }
        return "";
    }

    private String postProcessSql(String sql) {
        // Add any necessary post-processing here
        return sql.trim()
                .replaceAll(";\s*$", "")  // Remove trailing semicolon
                .replaceAll("\\s+", " ")  // Normalize whitespace
                .trim();
    }

    private String explainQuery(String sql, String naturalLanguage) {
        return String.format("""
                The generated SQL query for "%s" is:
                
                ```sql
                %s
                ```
                
                This query retrieves data based on the natural language input by translating it into a structured SQL format.
                """, naturalLanguage, sql);
    }
}