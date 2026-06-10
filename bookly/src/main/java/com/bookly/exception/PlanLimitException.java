package com.bookly.exception;

public class PlanLimitException extends RuntimeException {
    public PlanLimitException(String message) {
        super(message);
    }
}
