package com.seba.tasks.dto;

public record ErrorResponse(
        String errorCode,
        String message
) {
}
