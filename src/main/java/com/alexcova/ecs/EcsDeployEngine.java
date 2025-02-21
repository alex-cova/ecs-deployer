package com.alexcova.ecs;

import org.jetbrains.annotations.NotNull;

public class EcsDeployEngine {

    private Step head;
    private final Context context;

    public EcsDeployEngine() {
        System.out.println("""
                                      _            _                      \s
                   ___  ___ ___    __| | ___ _ __ | | ___  _   _  ___ _ __\s
                  / _ \\/ __/ __|  / _` |/ _ \\ '_ \\| |/ _ \\| | | |/ _ \\ '__|
                 |  __/ (__\\__ \\ | (_| |  __/ |_) | | (_) | |_| |  __/ |  \s
                  \\___|\\___|___/  \\__,_|\\___| .__/|_|\\___/ \\__, |\\___|_|  \s
                    by Alex                 |_|            |___/          \s
                """);
        this.context = new Context();
    }

    public void execute() {
        Step current = head;
        while (current != null) {
            current.execute(context);
            current = current.getNext();
        }
    }

    EcsDeployEngine and(@NotNull Step step) {
        System.out.println("- " + step.getClass().getSimpleName().substring(0, step.getClass().getSimpleName().length() - 4));
        if (head == null) {
            head = step;
        } else {
            Step current = head;
            while (current.getNext() != null) {
                current = current.getNext();
            }
            current.setNext(step);
        }
        return this;
    }

}
