package com.seba.tasks.error.exceptions;

import com.seba.tasks.error.ApiException;
import com.seba.tasks.error.ErrorCode;

public final class TaskNotFoundException extends ApiException {

    public TaskNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
