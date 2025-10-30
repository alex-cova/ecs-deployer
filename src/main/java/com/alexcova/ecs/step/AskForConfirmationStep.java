package com.alexcova.ecs.step;

import com.alexcova.ecs.AbortOperationException;
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

        if (!confirm("🔥 Ready to deploy " + context.getServiceName() + " to " + context.getClusterName(), context)) {
            throw new AbortOperationException("User aborted deployment");
        }

    }
}
