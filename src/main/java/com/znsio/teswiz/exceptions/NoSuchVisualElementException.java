package com.znsio.teswiz.exceptions;

public class NoSuchVisualElementException extends RuntimeException {
    public NoSuchVisualElementException(String message) {
        super(message);
    }

    public NoSuchVisualElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
