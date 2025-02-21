package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;

public class StopBackupStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        System.out.println("Stop backup tasks (" + context.getBackupTasksArns().size() + ")? (y/n)");

        String stopLine = context.getScanner().nextLine();

        if (stopLine.equals("y")) {
            for (ARN backup : context.getBackupTasksArns()) {
                context.putOutOfService(backup.lastToken());
            }

            System.out.println("waiting stop time (" + context.getStopTimeout() + " seconds)");

            waitTime(context.getStopTimeout() * 1000L);

            for (ARN arn : context.getBackupTasksArns()) {
                context.getEcsClient().stopTask(StopTaskRequest.builder()
                        .cluster(context.getClusterName())
                        .task(arn.lastToken())
                        .build());

                System.out.println("Stop backup task: " + arn);
            }

            System.out.println("waiting propagation time (30 sec)");

            waitTime(30000);
        } else {
            System.err.println("No backup task will not be stopped");
        }
    }
}
