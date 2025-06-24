
package com.ai.texttosql.exception;

import com.ai.texttosql.model.QueryResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<QueryResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        QueryResponse response = new QueryResponse();
        response.setError(ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<QueryResponse> handleConstraintViolation(ConstraintViolationException ex) {
        QueryResponse response = new QueryResponse();
        response.setError(ex.getConstraintViolations().iterator().next().getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<QueryResponse> handleDataAccessException(DataAccessException ex) {
        QueryResponse response = new QueryResponse();
        response.setError("Database error: " + ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<QueryResponse> handleEntityNotFound(EntityNotFoundException ex) {
        QueryResponse response = new QueryResponse();
        response.setError(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<QueryResponse> handleAccessDenied(AccessDeniedException ex) {
        QueryResponse response = new QueryResponse();
        response.setError("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<QueryResponse> handleGeneralException(Exception ex) {
        QueryResponse response = new QueryResponse();
        response.setError("An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}