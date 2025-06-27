package com.ai.texttosql.service;

import com.ai.texttosql.config.OllamaClient;
import com.ai.texttosql.model.QueryRequest;
import com.ai.texttosql.model.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class TextToSqlService {

    private final SchemaAnalysisService schemaAnalysisService;
    private final OllamaClient ollamaClient;

    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "(?i)^(SELECT|INSERT|UPDATE|DELETE|WITH|CREATE|DROP|ALTER)\\b.*"
    );

    private static final Pattern SQL_CONTINUATION = Pattern.compile(
            "(?i)^(FROM|WHERE|JOIN|LEFT|RIGHT|INNER|OUTER|GROUP|ORDER|HAVING|UNION|LIMIT|OFFSET)\\b.*"
    );

    // Pattern to detect date literals in SQL
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "'(\\d{4}-\\d{2}-\\d{2})'(?!\\s+\\d{2}:\\d{2}:\\d{2})",
            Pattern.CASE_INSENSITIVE
    );

    public QueryResponse generateSqlQuery(QueryRequest request) {
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
                throw new IllegalStateException("Failed to extract valid SQL from response");
            }

            // Post-process the SQL to fix common issues
            sqlQuery = postProcessSql(sqlQuery);

            QueryResponse response = new QueryResponse();
            response.setNaturalLanguageQuery(request.getNaturalLanguageQuery());
            response.setGeneratedSql(sqlQuery);

            if (request.isExplainQuery()) {
                response.setExplanation(explainQuery(sqlQuery, request.getNaturalLanguageQuery()));
            }

            return response;

        } catch (Exception e) {
            log.error("Error generating SQL query for request: {}", request.getNaturalLanguageQuery(), e);
            return createErrorResponse(request, e.getMessage());
        }
    }

    /**
     * Post-process SQL to fix common issues like date formatting
     */
    private String postProcessSql(String sqlQuery) {
        try {
            // Fix date literals that might cause TIMESTAMP errors
            sqlQuery = fixDateLiterals(sqlQuery);

            // Add DISTINCT if there are JOINs that might cause duplicates
            sqlQuery = addDistinctIfNeeded(sqlQuery);

            // Format for better readability
            sqlQuery = formatSqlQuery(sqlQuery);

            return sqlQuery;
        } catch (Exception e) {
            log.warn("Error in post-processing SQL, returning original: {}", e.getMessage());
            return sqlQuery;
        }
    }

    /**
     * Fix date literals to proper TIMESTAMP format
     */
    private String fixDateLiterals(String sqlQuery) {
        Matcher matcher = DATE_PATTERN.matcher(sqlQuery);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String dateValue = matcher.group(1);
            try {
                // Validate the date format
                LocalDate.parse(dateValue, DateTimeFormatter.ISO_LOCAL_DATE);

                // Replace with proper TIMESTAMP format for MySQL
                String replacement = "TIMESTAMP('" + dateValue + "')";
                matcher.appendReplacement(result, replacement);

                log.debug("Fixed date literal: '{}' -> {}", dateValue, replacement);
            } catch (DateTimeParseException e) {
                // Keep original if date is invalid
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Add DISTINCT if query has JOINs and SELECT * to prevent duplicates
     */
    private String addDistinctIfNeeded(String sqlQuery) {
        String upperQuery = sqlQuery.trim().toUpperCase();

        // Check if query has JOINs and SELECT * but no DISTINCT
        if (upperQuery.contains("JOIN") &&
                upperQuery.contains("SELECT") &&
                !upperQuery.contains("DISTINCT") &&
                (upperQuery.contains("SELECT *") || upperQuery.matches(".*SELECT\\s+\\w+\\.\\*.*"))) {

            // Add DISTINCT after SELECT
            sqlQuery = sqlQuery.replaceFirst("(?i)SELECT\\s+", "SELECT DISTINCT ");
            log.debug("Added DISTINCT to prevent duplicate rows from JOIN");
        }

        return sqlQuery;
    }

    /**
     * Format SQL query for better readability
     */
    private String formatSqlQuery(String sqlQuery) {
        return sqlQuery.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*;\\s*$", ";"); // Ensure single semicolon at end
    }

    private void validateRequest(QueryRequest request) {
        if (request == null || request.getNaturalLanguageQuery() == null ||
                request.getNaturalLanguageQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Natural language query cannot be null or empty");
        }

        if (request.getNaturalLanguageQuery().length() > 1000) {
            throw new IllegalArgumentException("Query too long. Maximum 1000 characters allowed");
        }
    }

    private String buildPrompt(String schemaContext, String naturalLanguageQuery) {
        return """
                You are a SQL expert. Convert the following natural language query into a valid SQL query for the given MySQL database schema.

                Database Schema:
                %s

                Natural Language Query: %s

                Requirements:
                - Output ONLY the SQL query
                - No explanation, no extra text, no markdown formatting
                - Use proper JOINs, WHERE clauses, and optimizations
                - For date comparisons, use TIMESTAMP() function or full datetime format (YYYY-MM-DD HH:MM:SS)
                - Use DISTINCT when JOINs might cause duplicate rows
                - Ensure the query is syntactically correct for MySQL
                - Use appropriate table aliases for readability
                - For date/time columns, always use proper MySQL date functions

                Examples of proper date handling:
                - WHERE date_column > TIMESTAMP('2022-01-01')
                - WHERE DATE(datetime_column) = '2022-01-01'
                - WHERE datetime_column >= '2022-01-01 00:00:00'

                SQL Query:""".formatted(schemaContext, naturalLanguageQuery);
    }

    public String extractSqlFromResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "";
        }

        // Remove code block markers if present
        String cleanResponse = rawResponse.replaceAll("```sql|```", "").trim();

        // Split by lines and extract SQL
        String[] lines = cleanResponse.split("\n");
        StringBuilder sqlBuilder = new StringBuilder();
        boolean foundSqlStart = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                if (foundSqlStart) {
                    sqlBuilder.append(" ");
                }
                continue;
            }

            // Check if line starts with SQL keywords
            if (SQL_KEYWORDS.matcher(trimmedLine).matches()) {
                foundSqlStart = true;
                sqlBuilder.append(trimmedLine).append(" ");
            } else if (foundSqlStart) {
                // Continue building SQL if it looks like part of the query
                if (SQL_CONTINUATION.matcher(trimmedLine).matches() ||
                        trimmedLine.matches(".*[,;()\\s]$") ||
                        trimmedLine.matches("^\\s*[A-Za-z0-9_]+\\s*[=<>!]+.*") ||
                        trimmedLine.matches("^\\s*AND\\b|OR\\b|ON\\b.*")) {
                    sqlBuilder.append(trimmedLine).append(" ");
                } else {
                    // Stop if we hit non-SQL content
                    break;
                }
            }
        }

        String extractedSql = sqlBuilder.toString().trim();

        // Fallback to original logic if extraction fails
        if (extractedSql.isEmpty()) {
            extractedSql = Arrays.stream(cleanResponse.split("\n"))
                    .filter(line -> SQL_KEYWORDS.matcher(line.trim()).matches())
                    .findFirst()
                    .orElse(cleanResponse);
        }

        return extractedSql;
    }

    private String explainQuery(String sqlQuery, String naturalLanguageQuery) {
        try {
            String explanationPrompt = """
                    Explain the following SQL query in simple terms.

                    Original Question: %s
                    SQL Query: %s

                    Provide a clear, concise explanation in 2-3 sentences focusing on:
                    - What data is being retrieved
                    - Which tables are involved
                    - Key conditions or filters applied

                    Explanation:""".formatted(naturalLanguageQuery, sqlQuery);

            String explanation = ollamaClient.ask(explanationPrompt);
            return explanation.replaceAll("(?i)^Explanation[:\\s]*", "").trim();

        } catch (Exception e) {
            log.error("Error generating explanation for SQL query", e);
            return "Unable to generate explanation for this query.";
        }
    }

    private QueryResponse createErrorResponse(QueryRequest request, String errorMessage) {
        QueryResponse response = new QueryResponse();
        response.setNaturalLanguageQuery(request != null ? request.getNaturalLanguageQuery() : "");
        response.setGeneratedSql("-- Error: " + errorMessage);
        response.setError(errorMessage);
        return response;
    }
}