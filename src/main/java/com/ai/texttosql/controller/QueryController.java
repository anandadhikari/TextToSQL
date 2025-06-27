package com.ai.texttosql.controller;

import com.ai.texttosql.model.QueryHistory;
import com.ai.texttosql.model.QueryRequest;
import com.ai.texttosql.model.QueryResponse;
import com.ai.texttosql.service.QueryExecutionService;
import com.ai.texttosql.service.QueryHistoryService;
import com.ai.texttosql.service.TextToSqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Query API", description = "APIs for text-to-SQL conversion and query execution")
@Validated
@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TextToSqlService textToSqlService;
    private final QueryExecutionService queryExecutionService;
    private final QueryHistoryService queryHistoryService;

    @Operation(summary = "Convert natural language to SQL")
    @PostMapping("/text-to-sql")
    public ResponseEntity<QueryResponse> textToSql(@Validated @RequestBody QueryRequest request) {
        QueryResponse response = textToSqlService.generateSqlQuery(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Execute a SQL query")
    @PostMapping("/execute")
    public ResponseEntity<QueryResponse> executeQuery(
            @Validated @RequestBody QueryRequest request,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
            
        // Validate pagination parameters
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        
        // First convert to SQL
        QueryResponse sqlResponse = textToSqlService.generateSqlQuery(request);

        // Then execute with pagination
        QueryResponse executionResponse = queryExecutionService.executeQuery(
            sqlResponse.getGeneratedSql(), 
            PageRequest.of(page, size)
        );

        // Merge responses
        executionResponse.setNaturalLanguageQuery(request.getNaturalLanguageQuery());
        executionResponse.setExplanation(sqlResponse.getExplanation());

        // Save to history
        queryHistoryService.saveQuery(executionResponse, "system");

        return ResponseEntity.ok(executionResponse);
    }

    @Operation(summary = "Get query history with pagination")
    @GetMapping("/history")
    public ResponseEntity<Page<QueryHistory>> getQueryHistory(
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
                
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        
        return ResponseEntity.ok(queryHistoryService.getQueryHistory(PageRequest.of(page, size)));
    }

    @Operation(summary = "Get explanation for a specific query by ID")
    @GetMapping("/explain/{queryId}")
    public ResponseEntity<QueryHistory> explainQuery(
            @Parameter(description = "ID of the query to explain") 
            @PathVariable UUID queryId) {
                
        return queryHistoryService.getQueryById(queryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get recent queries with pagination")
    @GetMapping("/recent")
    public ResponseEntity<Page<QueryHistory>> getRecentQueries(
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
                
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        
        return ResponseEntity.ok(queryHistoryService.getRecentQueries(PageRequest.of(page, size)));
    }

    @Operation(summary = "Get query history by user ID with pagination")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<QueryHistory>> getQueryHistoryByUserID(
            @Parameter(description = "User ID to filter by") 
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") 
            @RequestParam(defaultValue = "20") int size) {
                
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        
        return ResponseEntity.ok(queryHistoryService.getQueryHistoryByUserID(userId, PageRequest.of(page, size)));
    }
}