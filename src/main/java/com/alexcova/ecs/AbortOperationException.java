package com.alexcova.ecs;

public class AbortOperationException extends RuntimeException {

    public AbortOperationException(String message) {
        super(message);
    }
}
