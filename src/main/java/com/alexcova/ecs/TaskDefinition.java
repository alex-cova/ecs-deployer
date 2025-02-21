package com.alexcova.ecs;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record TaskDefinition(String family, int revision) {

    @Contract("_ -> new")
    public static @NotNull TaskDefinition of(@NotNull String version) {
        String[] parts = version.split(":");
        return new TaskDefinition(parts[0], Integer.parseInt(parts[1]));
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return family + ":" + revision;
    }
}