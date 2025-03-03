package com.alexcova.ecs.step;

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

        final var scanner = context.getScanner();
        final var clusterName = context.getClusterName();
        final var serviceName = context.getServiceName();
        final var ecsClient = context.getEcsClient();

        if (context.isNeedsDefinitionUpdate()) {
            System.out.println("🤠 Update task definition to revision " + context.getNewRevision() + "? (y/n)");

            String updateDefinition = scanner.nextLine();

            if (updateDefinition.equals("y")) {
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
                    .tasks(newTargets.stream().findFirst().get())
                    .build());

            var task = response.tasks().stream()
                    .filter(t -> context.getCurrentTasksArns().stream().noneMatch(i -> i.equalsArn(t.taskArn())))
                    .findFirst()
                    .orElse(null);

            if (task != null) {
                if (task.healthStatus() == HealthStatus.HEALTHY) {
                    System.out.println("🎉 new task " + task.taskArn().substring(task.taskArn().indexOf("/")) + " is healthy at " + LocalTime.now() + " other task may stop in less than " + context.getStopTimeout() + " seconds");
                    break;
                } else {
                    System.out.println("Waiting (5 sec) for service " + serviceName + " to update task " + task.taskArn().substring(task.taskArn().indexOf("/")) + ", current status: " + task.lastStatus());

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
            System.out.println("Instance: " + instance.getInstanceId() + " " + instance.getIpAddr() + ":" + instance.getPort() + " " + instance.getStatus());
        }

        System.out.println("Finished!");
    }


}
