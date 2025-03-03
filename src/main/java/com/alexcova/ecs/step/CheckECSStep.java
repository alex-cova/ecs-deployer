package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import com.alexcova.ecs.TaskDefinition;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecr.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecs.model.*;
import com.alexcova.eureka.Instance;

import java.util.List;
import java.util.stream.Collectors;

public class CheckECSStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        var servicesResponse = context.getEcsClient().describeServices(
                DescribeServicesRequest.builder()
                        .services(context.getServiceName())
                        .cluster(context.getClusterName())
                        .build()
        );

        if (!servicesResponse.hasServices() || servicesResponse.services().isEmpty()) {
            throw new IllegalStateException("Service '" + context.getServiceName() + "' not found");
        }

        var networkConfiguration = servicesResponse.services()
                .getFirst()
                .networkConfiguration();

        context.setNetworkConfiguration(networkConfiguration);

        System.out.println("------------------------");
        System.out.println("🛜 Current network configuration:\n ");
        AwsVpcConfiguration vpcConfiguration = networkConfiguration.awsvpcConfiguration();
        System.out.println("\tSecurity Groups: " + vpcConfiguration.securityGroups());
        System.out.println("\tSubnets: " + vpcConfiguration.subnets());
        System.out.println("\tAssign Public IP: " + vpcConfiguration.assignPublicIp());
        System.out.println("------------------------");

        var taskDefinition = TaskDefinition.of(
                servicesResponse.services().getFirst()
                        .taskDefinition().substring(servicesResponse.services().getFirst().taskDefinition().indexOf("/") + 1)
        );

        context.setCurrentFamily(taskDefinition.family());

        System.out.println("⭐️ Task definition: " + taskDefinition);

        var taskListResponse = context.getEcsClient().listTasks(
                ListTasksRequest.builder()
                        .cluster(context.getClusterName())
                        .serviceName(context.getServiceName())
                        .build()
        );

        var currentTasksArns = taskListResponse.taskArns()
                .stream()
                .map(ARN::new)
                .toList();

        for (ARN arn : currentTasksArns) {
            String id = arn.lastToken();
            System.out.println("Task: " + arn + " id: " + id);
            context.addOldTask(id);
        }

        if (!context.isUsingECSID()) {
            context.clearOldTasks();
            context.addOldTask(context.getCurrentEurekaInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList()));

            System.out.println("------------------------ USING EUREKA IDS ------------------------");
            for (Instance instance : context.getCurrentEurekaInstances()) {
                System.out.print("\t" + instance.getInstanceId() + " ");
            }

            System.out.println("😰 Is it correct? (y/n)");

            String correct = context.getScanner().nextLine();

            if (!correct.equals("y")) {
                throw new IllegalStateException("Aborted by user");
            }
        }

        var currentTask = context.getEcsClient().describeTaskDefinition(
                DescribeTaskDefinitionRequest.builder()
                        .taskDefinition(taskDefinition.family())
                        .build()
        );

        context.setCurrentTaskDefinition(currentTask);

        var describeTaskResponse = context.getEcsClient().describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(context.getClusterName())
                        .tasks(currentTasksArns.stream().map(ARN::value).collect(Collectors.toList()))
                        .build()
        );

        for (Task task : describeTaskResponse.tasks()) {
            for (Container container : task.containers()) {
                context.addContainer(container.imageDigest());
            }
        }

        context.setNewRevision(currentTask.taskDefinition().revision());

        if (currentTask.taskDefinition().containerDefinitions().isEmpty()) {
            throw new IllegalStateException("🤡 No containers found in task definition");
        }

        var stopTimeout = currentTask.taskDefinition().containerDefinitions().getFirst().stopTimeout() != null
                ? currentTask.taskDefinition().containerDefinitions().getFirst().stopTimeout()
                : 30;

        context.setStopTimeout(stopTimeout);

        if (stopTimeout <= 30) {
            System.out.println("----------------------------------------------------------------------------");
            System.out.println("⚠️ WARNING: Stop timeout is " + stopTimeout + ". You should set this to at least 60 seconds");
            System.out.println("----------------------------------------------------------------------------");
        } else {
            System.out.println("Stop timeout: " + stopTimeout + " 🕒");
        }

        var currentContainer = currentTask.taskDefinition().containerDefinitions().getFirst();

        context.setCurrentImage(currentContainer.image());

        var description = context.getEcsClient()
                .describeTasks(DescribeTasksRequest.builder()
                        .cluster(context.getClusterName())
                        .tasks(List.of(currentTasksArns.getFirst().value()))
                        .build());

        System.out.println("👉 Current container image: " + context.getCurrentImage());

        System.out.println("----------------------------------------------------------------------------");
        System.out.println("👉 Current container image digest: " + description.tasks().getFirst().containers().getFirst().imageDigest());
        context.setCurrentImageDiggest(description.tasks().getFirst().containers().getFirst().imageDigest());
        System.out.println("----------------------------------------------------------------------------");

        var currentImage = context.getCurrentImage();

        if (currentImage.contains(":")) {
            context.setCurrentTag(currentImage.substring(currentImage.indexOf(":") + 1));
        }

        System.out.println("👉 Current task definition image: " + currentContainer.image() + ":" + context.getCurrentTag());

        if (context.getNewRevision() == taskDefinition.revision()) {
            System.out.println("👉 Task definition is already the latest revision (" + taskDefinition + ")");
        } else {
            System.out.println("👉 Task definition is not the latest revision (" + taskDefinition + ")");
            context.setNeedsDefinitionUpdate(true);
        }

        context.setTaskDefinition(taskDefinition);
    }
}
