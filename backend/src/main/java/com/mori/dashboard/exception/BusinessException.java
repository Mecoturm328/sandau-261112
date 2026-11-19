package com.mori.dashboard.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：由 Service 层抛出，GlobalExceptionHandler 统一转换为 Result。
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
