package com.seba.tasks.dto;

import com.seba.tasks.error.exceptions.InvalidArgumentException;

import static com.seba.tasks.error.ErrorCode.REQUEST_BAD_ATTRIBUTE;

public record CreateTaskRequest(
        String title,
        String createdBy) {

    public CreateTaskRequest {
        if (title == null || title.isBlank())
            throw new InvalidArgumentException(REQUEST_BAD_ATTRIBUTE, "title");

        if (createdBy == null || createdBy.isBlank())
            throw new InvalidArgumentException(REQUEST_BAD_ATTRIBUTE, "createdBy");
    }
}
