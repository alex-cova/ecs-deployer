package com.alexcova.ecs;

import org.jetbrains.annotations.NotNull;

public record ARN(String value) {

    public ARN(@NotNull String value) {
        if (!value.startsWith("arn:aws:")) {
            throw new IllegalArgumentException("ARN must start with 'arn:aws:'");
        }
        this.value = value;
    }

    public boolean equalsArn(String other) {
        return value.equals(other);
    }

    public @NotNull String lastToken() {
        int index = value.lastIndexOf("/");
        if (index == -1) {
            throw new IllegalArgumentException("ARN must contain a '/'");
        }
        return value.substring(index + 1);
    }

    @Override
    public @NotNull String toString() {
        return value;
    }
}