package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.AbortOperationException;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.HealthStatus;
import com.alexcova.eureka.Instance;
import software.amazon.awssdk.services.ecs.model.ListTasksRequest;
import software.amazon.awssdk.services.ecs.model.UpdateServiceRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeployStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        final var clusterName = context.getClusterName();
        final var serviceName = context.getServiceName();
        final var ecsClient = context.getEcsClient();

        if (!confirm("☢️ Deploy new version?", context)) {
            System.out.println("Aborting...");

            new StopBackupStep()
                    .stopBackupInstances(context);

            System.exit(0);
        }

        waitTime(1000 * 60);

        if (!isStableRunning(context)) {
            if (confirm("Stables are not running, exit?", context)) {
                throw new AbortOperationException("Stables are not running");
            }
        }

        if (context.isNeedsDefinitionUpdate()) {
            if (confirm("🤠 Update task definition to revision " + context.getNewRevision() + "?", context)) {
                System.out.println("Updating task definition to " + context.getTaskDefinition().family() + ":" + context.getNewRevision());

                ecsClient.updateService(UpdateServiceRequest.builder()
                        .cluster(clusterName)
                        .service(serviceName)
                        .taskDefinition(context.getTaskDefinition().family() + ":" + context.getNewRevision())
                        .forceNewDeployment(true)
                        .build());
            } else {
                System.out.println("Skipping task definition update");

                ecsClient.updateService(UpdateServiceRequest.builder()
                        .cluster(clusterName)
                        .service(serviceName)
                        .forceNewDeployment(true)
                        .build());
            }
        } else {
            System.out.println("Updating " + context.getCurrentTasksArns().size() + " tasks to new deployment");

            ecsClient.updateService(UpdateServiceRequest.builder()
                    .cluster(clusterName)
                    .service(serviceName)
                    .forceNewDeployment(true)
                    .build());
        }


        System.out.println("Waiting (20 sec) for service to be updated...");

        waitTime(20000);

        System.out.println("Shutting down old tasks...");

        for (String oldTask : context.getOldTasks()) {
            context.putOutOfService(oldTask);
        }

        Set<String> newTargets = new HashSet<>();

        while (true) {
            waitTime(10000);

            System.out.println("Checking for new task...");

            var newTasksSet = ecsClient.listTasks(ListTasksRequest.builder()
                    .cluster(clusterName)
                    .serviceName(serviceName)
                    .build());

            if (newTasksSet.taskArns().isEmpty()) continue;

            for (String taskArn : newTasksSet.taskArns()) {
                if (context.getCurrentTasksArns().stream().noneMatch(i -> i.equalsArn(taskArn))) {
                    newTargets.add(taskArn);
                    System.out.println("🤠 New task: " + taskArn + " " + newTargets.size() + " of " + context.getCurrentTasksArns().size());
                    context.addNewTaskArn(new ARN(taskArn));
                }
            }

            if (newTargets.size() == context.getCurrentTasksArns().size()) break;
        }

        waitTime(context.getWaitTime() * 1000L);

        boolean serviceUpdated = false;
        int checkCount = 0;

        while (!serviceUpdated) {
            if (checkCount > 50) {
                System.out.println("🤡 No eureka registry found");
                break;
            }

            checkCount++;
            List<Instance> instances = context.getEurekaClient().getInstances(serviceName, clusterName);

            Instance instance = instances.stream()
                    .filter(inst -> newTargets.contains(inst.getInstanceId()))
                    .findFirst()
                    .orElse(null);

            if (instance != null) {
                System.out.println("😎 Eureka registry found! " + LocalDateTime
                        .ofEpochSecond(instance.getLeaseInfo().getRegistrationTimestamp(), 0, java.time.ZoneOffset.UTC));
                serviceUpdated = true;
            } else {
                System.out.println("🤡 No eureka registry found");
                waitTime(2000);
            }

            var response = ecsClient.describeTasks(DescribeTasksRequest.builder()
                    .cluster(clusterName)
                    .tasks(newTargets.iterator().next())
                    .build());

            var task = response.tasks().stream()
                    .filter(t -> context.getCurrentTasksArns().stream().noneMatch(i -> i.equalsArn(t.taskArn())))
                    .findFirst()
                    .orElse(null);

            if (task != null) {
                String substring = task.taskArn().substring(task.taskArn().indexOf("/"));

                if (task.healthStatus() == HealthStatus.HEALTHY) {
                    System.out.println("🎉 new task " + substring + " is healthy at " + LocalTime.now() + " other task may stop in less than " + context.getStopTimeout() + " seconds");
                    break;
                } else {
                    System.out.printf("Waiting (5 sec) for service %s to update task %s, current status: %s%n",
                            serviceName, substring, task.lastStatus());

                    waitTime(5000);
                }
            } else {
                System.out.println("Waiting (5 sec) for service " + serviceName);
                waitTime(5000);
            }
        }

        System.out.println("waiting propagation time (30 sec)");

        waitTime(30000);

        List<Instance> registeredInstances = context.getEurekaInstances();

        for (Instance instance : registeredInstances) {
            ARN arn = context.getArn(instance.getInstanceId());
            if (arn != null) {
                var task = getECSTask(context, arn);

                if (task != null) {
                    System.out.printf("Instance: %s %s:%s %s ECS Status: %s %s %n",
                            instance.getInstanceId(), instance.getIpAddr(), instance.getPort(), instance.getStatus(), task.group(), task.lastStatus());

                    if (task.stoppedAt() != null) {
                        System.err.println(task.stoppedReason());
                    }

                    continue;
                }
            }

            System.out.printf("Instance: %s %s:%s %s%n", instance.getInstanceId(), instance.getIpAddr(), instance.getPort(), instance.getStatus());
        }

        System.out.println("Finished!");
    }


}
