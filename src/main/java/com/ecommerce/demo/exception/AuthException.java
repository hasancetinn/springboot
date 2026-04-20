package com.ecommerce.demo.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends BaseException {
    public AuthException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public AuthException(String message, HttpStatus status) {
        super(message, status);
    }
}
