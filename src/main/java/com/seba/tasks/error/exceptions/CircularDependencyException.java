package com.seba.tasks.error.exceptions;

import com.seba.tasks.error.ApiException;
import com.seba.tasks.error.ErrorCode;

public class CircularDependencyException extends ApiException {
    public CircularDependencyException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
