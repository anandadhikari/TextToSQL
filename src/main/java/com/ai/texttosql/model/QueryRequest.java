
package com.ai.texttosql.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QueryRequest {
    @NotBlank(message = "Natural language query cannot be blank")
    private String naturalLanguageQuery;

    private boolean explainQuery = true;
    private boolean validateQuery = true;
    private boolean includeSchemaContext = true;
}