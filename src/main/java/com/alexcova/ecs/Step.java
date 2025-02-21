package com.alexcova.ecs;

import com.alexcova.eureka.Instance;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.HealthStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public abstract class Step {

    private Step next;

    void setNext(Step next) {
        this.next = next;
    }

    Step getNext() {
        return next;
    }

    public abstract void execute(@NotNull Context context);

    protected void waitForTask(@NotNull Context context, ARN taskArn) {
        var serviceName = context.getServiceName();
        var clusterName = context.getClusterName();
        var ecsClient = context.getEcsClient();
        var eurekaClient = context.getEurekaClient();

        System.out.println("Waiting for task: " + taskArn + " to be healthy ");

        boolean serviceUpdated = false;

        while (!serviceUpdated) {

            var response = ecsClient.describeTasks(DescribeTasksRequest.builder()
                    .cluster(clusterName)
                    .tasks(taskArn.value())
                    .build());

            var task = response.tasks().stream()
                    .filter(t -> taskArn.equalsArn(t.taskArn()))
                    .findFirst()
                    .orElse(null);

            if (task != null) {
                if (task.healthStatus() == HealthStatus.HEALTHY) {
                    System.out.println("new task " + task.taskArn() + " is healthy at " + LocalTime.now());
                    serviceUpdated = true;
                } else if (task.healthStatus() == HealthStatus.UNHEALTHY) {
                    throw new IllegalStateException("Task " + task.taskArn() + " is unhealthy, stopping deployment");
                } else {
                    System.out.println("Waiting (20 sec) for task " + task.taskArn() + ", current status: " + task.lastStatus());
                    waitTime(20_000);
                }
            } else {
                System.out.println("Waiting (5 sec) for task " + taskArn);
                waitTime(5000);
            }
        }

        serviceUpdated = false;

        String instanceId = taskArn.lastToken();

        System.out.println("😮‍💨 Now waiting for registry in eureka: " + instanceId);

        int counter = 0;

        while (!serviceUpdated) {
            List<Instance> instances = eurekaClient.getInstances(serviceName, clusterName);

            if (instances.stream().anyMatch(instance -> instance.getInstanceId().equals(instanceId))) {
                serviceUpdated = true;
            }

            counter++;

            if (counter > 15) {
                System.out.println("Failed to find instance in eureka, do you want to continue? (y/n)");
                System.out.println("Available tasks: ");
                instances.forEach(instance -> System.out.println("\t- Instance: " + instance.getInstanceId()
                        + " " + instance.getIpAddr() + ":" + instance.getPort() + " " + instance.getStatus()
                        + " " + LocalDateTime
                        .ofEpochSecond(instance.getLeaseInfo().getRegistrationTimestamp(), 0, java.time.ZoneOffset.UTC)));

                if (context.getScanner().nextLine().equals("n")) {
                    break;
                }
            }

            waitTime(5000);
        }

        System.out.println("service " + serviceName + " successfully registered at eureka!");
    }

    protected void waitTime(long time) {
        int progress = 0;
        long timePerProgress = time / 100;
        while (progress < 100) {
            System.out.print("\r[");
            for (int i = 0; i <= progress; i++) {
                System.out.print("=");
            }
            for (int i = progress; i < 100; i++) {
                System.out.print(" ");
            }
            System.out.print("] " + progress + "%");
            progress++;
            try {
                Thread.sleep(timePerProgress);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        System.out.println("\r[====================================================================================================] 100%");
    }
}
