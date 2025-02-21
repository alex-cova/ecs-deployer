package com.alexcova.eureka;

import com.fasterxml.jackson.annotation.JsonAlias;

public class Port {
    private long empty;
    @JsonAlias("@enabled")
    private String enabled;
    @JsonAlias("$")
    private int port;

    public long getEmpty() {
        return empty;
    }

    public int getPort() {
        return port;
    }

    public Port setEmpty(long empty) {
        this.empty = empty;
        return this;
    }

    public String getEnabled() {
        return enabled;
    }

    public Port setEnabled(String enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public String toString() {
        return String.valueOf(port);
    }
}