package com.seba.tasks.error;

import com.seba.tasks.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getErrorCode().name(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(errorResponse);
    }
}