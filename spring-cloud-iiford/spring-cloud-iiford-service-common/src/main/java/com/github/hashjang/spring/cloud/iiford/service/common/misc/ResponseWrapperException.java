package com.github.hashjang.spring.cloud.iiford.service.common.misc;

import lombok.Data;

@Data
public class ResponseWrapperException extends RuntimeException {
    private Object response;
    private String message;

    public ResponseWrapperException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public ResponseWrapperException(String message, Object response) {
        super(message);
        this.message = message;
        this.response = response;
    }
}