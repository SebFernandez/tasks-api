package com.seba.tasks.error.exceptions;

import com.seba.tasks.error.ApiException;
import com.seba.tasks.error.ErrorCode;

public class DependencyNotFoundException extends ApiException {
    public DependencyNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}