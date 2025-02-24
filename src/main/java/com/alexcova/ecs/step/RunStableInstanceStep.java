package com.alexcova.ecs.step;

import com.alexcova.ecs.ARN;
import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.PutImageRequest;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.List;
import java.util.Objects;

public class RunStableInstanceStep extends Step {

    @Override
    public void execute(@NotNull Context context) {
        String backupFamily;
        int backupRevision;

        try {
            var definitionResponse = context.getEcsClient()
                    .describeTaskDefinition(a -> a.taskDefinition(context.getServiceName() + "-stable"));

            backupFamily = definitionResponse.taskDefinition().family();
            backupRevision = definitionResponse.taskDefinition().revision();
        } catch (Exception ex) {
            System.err.println("🤡 No stable image found: " + context.getServiceName() + "-stable, creating a new one");

            var c = context.getCurrentTaskDefinition()
                    .taskDefinition();

            var definitions = c.containerDefinitions();

            var stableDefinition = definitions.getFirst().toBuilder()
                    .image(context.getCurrentImage() + ":stable")
                    .build();

            try {
                context.getEcsClient()
                        .registerTaskDefinition(b -> b.family(context.getServiceName() + "-stable")
                                .networkMode(c.networkMode())
                                .cpu(c.cpu())
                                .memory(c.memory())
                                .ipcMode(c.ipcMode())
                                .executionRoleArn(c.executionRoleArn())
                                .runtimePlatform(c.runtimePlatform())
                                .taskRoleArn(c.taskRoleArn())
                                .ephemeralStorage(c.ephemeralStorage())
                                .containerDefinitions(List.of(stableDefinition))
                        );

                backupFamily = context.getServiceName() + "-stable";
                backupRevision = 1;

                context.setStableDigest(context.getCurrentImage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (backupFamily.endsWith("-stable")) {
            if (context.getStableDigest().isEmpty()) {
                throw new IllegalStateException("No stable image found");
            }

            if (!context.getStableDigest().equals(context.getLatestImageDigest())) {
                System.out.println("Update the stable image to the latest image? (y/n) " + context.getLatestImageDigest());

                String updateStableImage = context.getScanner().nextLine();

                if (updateStableImage.equals("y")) {
                    System.out.println("Updating stable image to " + context.getLatestImageDigest());

                    var response = context.getEcrClient()
                            .batchGetImage(a -> a.repositoryName(context.getServiceName())
                                    .imageIds(ImageIdentifier.builder()
                                            .imageDigest(context.getLatestImageDigest())
                                            .build()));

                    if (response.images().isEmpty()) {
                        throw new IllegalStateException("No image found for digest: " + context.getLatestImageDigest());
                    }

                    var manifest = response.images().getFirst()
                            .imageManifest();

                    context.getEcrClient().putImage(PutImageRequest.builder()
                            .repositoryName(context.getServiceName())
                            .imageManifest(manifest)
                            .imageTag("stable")
                            .build());

                    System.out.println("Image (Stable) updated to " + context.getLatestImageDigest());
                    context.setStableDigest(context.getLatestImageDigest());
                }
            }

            System.out.println("Image (Stable) for backup task will be " + context.getStableDigest());
        } else {
            System.out.println("Image for backup task will be " + context.getCurrentImage());
        }

        Objects.requireNonNull(backupFamily, "Backup family is null");

        var tasksRunning = context.getEcsClient()
                .listTasks(ListTasksRequest.builder()
                        .cluster(context.getClusterName())
                        .family(context.getServiceName())
                        .build());

        context.setCurrentTasksArns(tasksRunning.taskArns()
                .stream()
                .map(ARN::new)
                .toList());

        List<String> runningStables = context.getEcsClient().listTasks(
                ListTasksRequest.builder()
                        .cluster(context.getClusterName())
                        .family(context.getServiceName() + "-stable")
                        .build()).taskArns();

        if (!runningStables.isEmpty()) {
            System.out.println("⚠️ WARNING there is already a backup task running (" + runningStables.size() + ")");
            System.out.println("😎 Run anyway? (y/n)");

            String runAnyway = context.getScanner().nextLine();

            if (!runAnyway.equals("y")) {
                for (String runningStable : runningStables) {
                    context.addBackupTask(new ARN(runningStable));
                }
                return;
            }
        }

        System.out.println("🐵 Starting (" + tasksRunning.taskArns().size() + ") backup service for " + context.getServiceName() + " with task definition " + backupFamily + ":" + backupRevision);

        System.out.println("is it correct? (y/n)");

        String correct = context.getScanner().nextLine();

        if (!correct.equals("y")) {
            System.out.println("Aborting...");
            System.exit(0);
        }

        var netConfig = context.getNetworkConfiguration();

        if (netConfig == null) {
            throw new IllegalStateException("Network configuration is null");
        }

        AwsVpcConfiguration config = netConfig.awsvpcConfiguration();

        System.out.println("-----------------------------------------------------------");
        System.out.println("    Using network configuration:");
        System.out.println("    Security Group: " + config.securityGroups());
        System.out.println("    Subnets: " + config.subnets());
        System.out.println("    Assign public IP: " + config.assignPublicIp());
        System.out.println("-----------------------------------------------------------");

        RunTaskResponse response = context.getEcsClient().runTask(
                RunTaskRequest.builder()
                        .cluster(context.getClusterName())
                        .taskDefinition(backupFamily + ":" + backupRevision)
                        .group(context.getServiceName() + "-backup")
                        .launchType(LaunchType.FARGATE)
                        .networkConfiguration(
                                NetworkConfiguration.builder()
                                        .awsvpcConfiguration(
                                                AwsVpcConfiguration.builder()
                                                        .securityGroups(config.securityGroups())
                                                        .subnets(config.subnets())
                                                        .assignPublicIp(AssignPublicIp.DISABLED)
                                                        .build())
                                        .build())
                        .count(tasksRunning.taskArns().size())
                        .build());

        System.out.println("------------- 🐵 Backup task started -------------");

        for (Task task : response.tasks()) {
            System.out.println("🐵 Backup task: " + task.taskArn());
        }

        context.addBackupTask(response.tasks().stream()
                .map(Task::taskArn)
                .map(ARN::new)
                .toList());

        waitForTask(context, new ARN(response.tasks().getFirst().taskArn()));

        waitTime(context.getWaitTime() * 1000L);

        System.out.println("waiting 30 sec for backup propagation");

        waitTime(30000);
    }
}
