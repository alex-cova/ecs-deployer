package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecs.model.HealthStatus;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;

public class StopBackupStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        if (confirm("Stop backup tasks (" + context.getBackupTasksArns().size() + ")?", context)) {
            stopBackupInstances(context);
        } else {
            System.err.println("No backup task will not be stopped");
        }
    }

    public void stopBackupInstances(@NotNull Context context) {
        System.out.println("Stopping backup tasks, lets check if the new task are healthy");

        for (ARN newTasksArn : context.getNewTasksArns()) {
            var task = getECSTask(context, newTasksArn);

            if (task != null) {
                if (task.healthStatus() != HealthStatus.HEALTHY) {
                    System.out.println("🚨 Task " + newTasksArn + " is not healthy");

                    if (confirm("Stop the stables?", context)) {
                        break;
                    } else {
                        System.out.println("No stable task was stopped");
                        return;
                    }
                }
            }
        }

        for (ARN backup : context.getBackupTasksArns()) {
            context.putOutOfService(backup.lastToken());
        }

        System.out.println("Waiting stop time (" + context.getStopTimeout() + " seconds)");

        waitTime(context.getStopTimeout() * 1000L);

        for (ARN arn : context.getBackupTasksArns()) {
            context.getEcsClient().stopTask(StopTaskRequest.builder()
                    .cluster(context.getClusterName())
                    .task(arn.lastToken())
                    .build());

            System.out.println("Stopping backup task: " + arn);
        }

        System.out.println("waiting propagation time (30 sec)");

        waitTime(30000);
    }
}
