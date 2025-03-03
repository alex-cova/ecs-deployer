package com.alexcova.ecs;

import java.time.LocalDateTime;

public class TimestampedResponse {
    private final int status;
    private final LocalDateTime timestamp;

    public TimestampedResponse(int status) {
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Status: %s at %s".formatted(status, timestamp);
    }
}
