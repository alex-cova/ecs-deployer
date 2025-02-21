package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;

public class StartHealthCheckerStep extends Step {
    @Override
    public void execute(@NotNull Context context) {
        System.out.println("Starting availability checker");
        context.startHealthChecker();
    }
}
