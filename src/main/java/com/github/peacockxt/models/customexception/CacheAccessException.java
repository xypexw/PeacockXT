package com.github.peacockxt.models.customexception;

public class CacheAccessException extends RuntimeException {
    public CacheAccessException(String message) {
        super(message);
    }
    public CacheAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
