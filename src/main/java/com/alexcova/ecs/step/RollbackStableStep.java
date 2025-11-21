package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;

public class RollbackStableStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        if (confirm("Stop stables?", context)) {
            for (ARN arn : context.getBackupTasksArns()) {
                context.putOutOfService(arn.lastToken());
                context.getEcsClient()
                        .stopTask(StopTaskRequest.builder()
                                .cluster(context.getClusterName())
                                .task(arn.lastToken())
                                .reason("Rollback stable")
                                .build());
            }
        }
    }
}
