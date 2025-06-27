package com.ai.texttosql.controller;

import com.ai.texttosql.model.QueryHistory;
import com.ai.texttosql.model.QueryRequest;
import com.ai.texttosql.model.QueryResponse;
import com.ai.texttosql.service.QueryExecutionService;
import com.ai.texttosql.service.QueryHistoryService;
import com.ai.texttosql.service.TextToSqlService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final TextToSqlService textToSqlService;
    private final QueryExecutionService queryExecutionService;
    private final QueryHistoryService queryHistoryService;

    @PostMapping("/text-to-sql")
    @Operation(summary = "Convert natural language to SQL")
    public ResponseEntity<QueryResponse> textToSql(@RequestBody QueryRequest request) {
        QueryResponse response = textToSqlService.generateSqlQuery(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute a SQL query")
    public ResponseEntity<QueryResponse> executeQuery(@RequestBody QueryRequest request) {
        // First convert to SQL
        QueryResponse sqlResponse = textToSqlService.generateSqlQuery(request);

        // Then execute
        QueryResponse executionResponse = queryExecutionService.executeQuery(sqlResponse.getGeneratedSql());

        // Merge responses
        executionResponse.setNaturalLanguageQuery(request.getNaturalLanguageQuery());
        executionResponse.setExplanation(sqlResponse.getExplanation());

        // Save to history
        queryHistoryService.saveQuery(executionResponse,"system");

        return ResponseEntity.ok(executionResponse);
    }

    @GetMapping("/history")
    @Operation(summary = "Get query history")
    public ResponseEntity<List<QueryHistory>> getQueryHistory() {
        List<QueryHistory> history = queryHistoryService.getQueryHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/explain/{queryId}")
    @Operation(summary = "Get explanation for a specific query")
    public ResponseEntity<Optional<QueryHistory>> explainQuery(@PathVariable UUID queryId) {
        Optional<QueryHistory> history = queryHistoryService.getQueryById(queryId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent queries")
    public ResponseEntity<List<QueryHistory>> getRecentQueries() {
        List<QueryHistory> recentQueries = queryHistoryService.getRecentQueries();
        return ResponseEntity.ok(recentQueries);
    }

    @GetMapping("/explain/{userId}")
    @Operation(summary = "Get explanation for a specific query By UserID")
    public ResponseEntity<Optional<QueryHistory>> getQueryHistoryByUserID(@PathVariable String userId) {
        Optional<QueryHistory> history = queryHistoryService.getQueryHistoryByUserID(userId);
        return ResponseEntity.ok(history);
    }
}