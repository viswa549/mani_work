package com.ecvs.overrideload.exception;

import lombok.Getter;

@Getter
public class OverrideLoadException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public OverrideLoadException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public OverrideLoadException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
