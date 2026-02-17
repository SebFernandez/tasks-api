package com.seba.tasks.error.exceptions;

import com.seba.tasks.error.ApiException;
import com.seba.tasks.error.ErrorCode;

public class TaskBlockedException extends ApiException {
    public TaskBlockedException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
