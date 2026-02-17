package com.seba.tasks.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode {
    TASK_NOT_FOUND(NOT_FOUND, "Task with id %s not found"),
    REQUEST_BAD_ATTRIBUTE(BAD_REQUEST, "Attribute %s is not valid"),
    CIRCULAR_DEPENDENCY(CONFLICT, "Adding dependency from %s to %s would create a cycle"),
    TASK_BLOCKED(CONFLICT, "Task %s is blocked and cannot be modified"),
    DEPENDENCY_NOT_FOUND(NOT_FOUND, "Task %s does not depend on %s");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
