
package com.ai.texttosql.controller;

import com.ai.texttosql.model.SchemaInfo;
import com.ai.texttosql.service.SchemaAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaAnalysisService schemaAnalysisService;

    @GetMapping("/tables")
    @Operation(summary = "Get all table names in the database")
    public ResponseEntity<List<String>> getAllTableNames() {
        return ResponseEntity.ok(schemaAnalysisService.getAllTableNames());
    }

    @GetMapping("/table/{tableName}")
    @Operation(summary = "Get schema information for a specific table")
    public ResponseEntity<SchemaInfo> getTableSchema(@PathVariable String tableName) {
        return ResponseEntity.ok(schemaAnalysisService.getTableSchema(tableName));
    }
}