package com.alexcova.ecs;

import com.alexcova.eureka.Instance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.HealthStatus;
import software.amazon.awssdk.services.ecs.model.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Step implements CmdUtil {

    private Step next;

    void setNext(Step next) {
        this.next = next;
    }

    Step getNext() {
        return next;
    }

    public abstract void execute(@NotNull Context context);


    public boolean isStableRunning(@NotNull Context context) {
        var clusterName = context.getClusterName();
        var ecsClient = context.getEcsClient();

        var response = ecsClient.describeTasks(DescribeTasksRequest.builder()
                .cluster(clusterName)
                .tasks(context.getBackupTasksArns()
                        .stream()
                        .map(ARN::value)
                        .toList())
                .build());

        return response.tasks().stream()
                .allMatch(t -> t.healthStatus() == HealthStatus.HEALTHY);
    }


    public @Nullable Task getECSTask(@NotNull Context context, @NotNull ARN taskArn) {
        var response = context.getEcsClient().describeTasks(DescribeTasksRequest.builder()
                .cluster(context.getClusterName())
                .tasks(taskArn.value())
                .build());

        return response.tasks().stream()
                .filter(t -> taskArn.equalsArn(t.taskArn()))
                .findFirst()
                .orElse(null);
    }

    protected void waitForTask(@NotNull Context context, ARN taskArn) {
        var serviceName = context.getServiceName();
        var clusterName = context.getClusterName();
        var eurekaClient = context.getEurekaClient();

        System.out.println("Waiting for task: " + taskArn + " to be healthy ");

        boolean serviceUpdated = false;

        while (!serviceUpdated) {
            var task = getECSTask(context, taskArn);

            if (task != null) {
                var taskId = task.taskArn().substring(task.taskArn().lastIndexOf("/") + 1);

                if (task.healthStatus() == HealthStatus.HEALTHY) {
                    System.out.println("new task " + taskId + " is healthy at " + LocalTime.now());
                    serviceUpdated = true;
                } else if (task.healthStatus() == HealthStatus.UNHEALTHY) {
                    throw new IllegalStateException("Task " + taskId + " is unhealthy, stopping deployment");
                } else if (task.lastStatus().equalsIgnoreCase("STOPPED")) {
                    throw new IllegalStateException("Task " + taskId + " is stopped, stopping deployment: " + task.stoppedReason());
                } else {
                    System.out.println("Waiting (20 sec) for task " + taskId + ", current status: " + task.lastStatus());
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
                System.out.println("❌ Failed to find instance in eureka");
                System.out.println("> Available tasks: ");
                instances.forEach(instance -> System.out.println("\t- Instance: " + instance.getInstanceId()
                        + " " + instance.getIpAddr() + ":" + instance.getPort() + " " + instance.getStatus()
                        + " " + LocalDateTime
                        .ofEpochSecond(instance.getLeaseInfo().getRegistrationTimestamp() / 1000, 0, java.time.ZoneOffset.UTC)));

                if (!confirm("Do you want to continue?", context)) {
                    break;
                }
            }

            waitTime(5000);
        }

        System.out.println("service " + serviceName + " successfully registered at eureka!");

        checkGatewayRecord(context, taskArn);
    }

    protected void checkGatewayRecord(@NotNull Context context, ARN taskArn) {
        if (context.getConfiguration().getGatewayInstancesUri() == null) {
            System.out.println("⚠️ Gateway instances uri is null, skipping check");
            return;
        }

        System.out.println("Checking registry in gateway");

        var found = isInGateway(context, taskArn.lastToken());

        if (!found) {
            System.out.println("⚠️ No instance of %s:%s found in gateway"
                    .formatted(context.getServiceName(), taskArn.value()));

            if (!confirm("Do you want to continue?", context)) {
                throw new AbortOperationException("Ok, good bye");
            }
        }

    }

    ObjectMapper mapper = new ObjectMapper();

    protected boolean isInGateway(@NotNull Context context, String id) {
        var url = "https://%s/%s"
                .formatted(context.isProduction() ? context.getConfiguration().getPrometheusProduction() : context.getConfiguration().getPrometheusDevelopment(),
                        context.getConfiguration().getGatewayInstancesUri());
        System.out.println(url);

        var found = false;

        for (int i = 0; i < 10; i++) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                var response = Context.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<com.alexcova.ecs.Instance> instances = mapper.readValue(response.body(), mapper.getTypeFactory()
                            .constructCollectionType(List.class, com.alexcova.ecs.Instance.class));

                    for (com.alexcova.ecs.Instance instance : instances) {
                        if (instance.name().equalsIgnoreCase(context.getServiceName())) {
                            if (instance.instances().contains(id)) {
                                System.out.println("✅ Instance " + instance.name() + ":" + id + " found in gateway");
                                found = true;
                                break;
                            }
                        }
                    }
                } else {
                    System.out.println("⚠️ Gateway response code: " + response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                Logger.getLogger(Step.class.getName())
                        .log(Level.SEVERE, null, e);
            }

            waitTime(5000);
        }

        return found;
    }

    protected void waitTime(long time) {
        int progress = 0;
        long timePerProgress = time / 100;
        while (progress < 100) {
            System.out.print("\r[");
            for (int i = 0; i <= progress; i++) {
                System.out.print("█");
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
        System.out.println("\r[████████████████████████████████████████████████████████████████████████████████████████████████████] 100%");
    }
}
