package com.seba.tasks.error.exceptions;

import com.seba.tasks.error.ApiException;
import com.seba.tasks.error.ErrorCode;

public final class InvalidArgumentException extends ApiException {
    public InvalidArgumentException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
