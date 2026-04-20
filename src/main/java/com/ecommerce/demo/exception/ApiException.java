package com.ecommerce.demo.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends BaseException {
    public ApiException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public ApiException(String message, HttpStatus status) {
        super(message, status);
    }
}
