package com.ai.texttosql.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when there is an error generating an SQL query from natural language.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class QueryGenerationException extends RuntimeException {

    public QueryGenerationException(String message) {
        super(message);
    }

    public QueryGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public QueryGenerationException(Throwable cause) {
        super(cause);
    }
}
