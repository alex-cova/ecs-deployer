package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;

public class AskForConfirmationStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        if (context.getTaskDefinition() == null) {
            throw new IllegalStateException("Task definition is null");
        }

        System.out.println("🕒 Application ready time: " + context.getWaitTime() + " seconds");
        System.err.println("Ready to deploy " + context.getServiceName() + " to " + context.getClusterName() + "? (y/n)");

        String ready = context.getScanner().nextLine();

        if (!ready.equals("y")) {
            throw new IllegalStateException("User aborted deployment");
        }

    }
}
