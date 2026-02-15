package com.seba.tasks.dto;

import com.seba.tasks.error.exceptions.InvalidArgumentException;
import com.seba.tasks.model.TaskStatus;

import static com.seba.tasks.error.ErrorCode.REQUEST_BAD_ATTRIBUTE;

public record UpdateTaskRequest(
        String title,
        TaskStatus status,
        String updatedBy) {

    public UpdateTaskRequest {
        if (updatedBy == null || updatedBy.isBlank())
            throw new InvalidArgumentException(REQUEST_BAD_ATTRIBUTE, "updatedBy");
    }
}
