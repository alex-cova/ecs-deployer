package com.alexcova.eureka;

import java.util.List;

public class Application {
    private String name;
    private List<Instance> instance;

    public String getName() {
        return name;
    }

    public Application setName(String name) {
        this.name = name;
        return this;
    }

    public List<Instance> getInstance() {
        return instance;
    }

    public Application setInstance(List<Instance> instance) {
        this.instance = instance;
        return this;
    }
}