package com.seba.tasks.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
public enum ErrorCode {
    TASK_NOT_FOUND(NOT_FOUND, "Task with id %s not found"),
    REQUEST_BAD_ATTRIBUTE(BAD_REQUEST, "Attribute %s is not valid");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
