package com.znsio.teswiz.exceptions;

public class VisualSubsystemDisabledException extends RuntimeException {
    public VisualSubsystemDisabledException(String message) {
        super(message);
    }

    public VisualSubsystemDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
