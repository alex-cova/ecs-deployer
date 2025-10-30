package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;

public class ConfigureStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        System.out.println("\n\n");

        System.out.println("> Enter the name of the ECS cluster:");

        String clusterName = context.getScanner().nextLine().trim();

        if (!clusterName.matches("^[a-zA-Z0-9-_]+$")) {
            throw new IllegalArgumentException("Cluster name '" + clusterName + "' is invalid");
        }

        System.out.println("> Enter the name of the ECS service:");

        String serviceName = context.getScanner().nextLine().trim();

        if (!serviceName.matches("^[a-zA-Z0-9-_]+$")) {
            throw new IllegalArgumentException("Service name '" + serviceName + "' is invalid");
        }

        context.doLogin();
        context.setClusterName(clusterName);
        context.setServiceName(serviceName);
        context.checkEurekaService();
        context.fetchWaitTime();
    }
}
