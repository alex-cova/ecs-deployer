package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecr.model.BatchGetImageRequest;
import software.amazon.awssdk.services.ecr.model.ImageAlreadyExistsException;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;
import software.amazon.awssdk.services.ecr.model.PutImageRequest;

public class ConfigureImageStep extends Step {
    @Override
    public void execute(@NotNull Context context) {
        if (context.getStableDigest().isEmpty()) {
            System.err.println("🤡 No stable image found, creating one...");
            createStableImage(context);
        } else if (!context.getStableDigest().equals(context.getContainers().getFirst())) {
            System.out.println("> Containers first: " + context.getContainers().getFirst());
            System.out.println("> Latest image digest: " + context.getStableDigest());
            System.out.println("😏 Update the stable image to the latest image? (y/n/cancel) " + context.getContainers().getFirst() + " ");

            String updateStableImage = context.getScanner().nextLine();

            if (updateStableImage.equals("y")) {
                createStableImage(context);
            }

            if (updateStableImage.equals("cancel")) {
                throw new IllegalStateException("Aborted by user");
            }
        }

        if (context.getLatestImageDigest().equals(context.getContainers().getFirst())) {
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("Service " + context.getServiceName() + " already using the latest image: " + context.getLatestImageDigest());
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------");

            System.out.println("⚠️ Force new deployment? (y/n)");

            String forceNewDeployment = context.getScanner().nextLine();

            if (!forceNewDeployment.equals("y")) {
                throw new IllegalStateException("Ok, good bye");
            }
        } else {
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("Service " + context.getServiceName() + " will be deployed using the latest image: " + context.getLatestImageDigest());
            System.out.println("-------------------------------------------------------------------------------------------------------------------------------");
        }
    }

    void createStableImage(@NotNull Context context) {
        if (context.getCurrentImage().isEmpty() || context.getContainers().isEmpty()) {
            throw new IllegalStateException("Current image is empty or no containers found");
        }

        String currentDigest = context.getContainers().getFirst();

        if (!currentDigest.startsWith("sha256:")) {
            throw new IllegalStateException("Current digest is not a sha256 digest: " + currentDigest);
        }

        var response = context.getEcrClient().batchGetImage(
                BatchGetImageRequest.builder()
                        .repositoryName(context.getServiceName())
                        .imageIds(
                                ImageIdentifier.builder()
                                        .imageDigest(currentDigest)
                                        .build()
                        )
                        .build()
        );

        if (response.images().isEmpty()) {
            throw new IllegalStateException("No image " + currentDigest + " found for " + context.getServiceName());
        }

        var manifest = response.images().getFirst().imageManifest();

        try {
            context.getEcrClient().putImage(
                    PutImageRequest.builder()
                            .repositoryName(context.getServiceName())
                            .imageManifest(manifest)
                            .imageTag("stable")
                            .build()
            );

            System.out.println("🔥 Image for " + context.getServiceName() + " tagged as stable, digest: " + currentDigest);
            context.setStableDigest(currentDigest);
        } catch (ImageAlreadyExistsException ex) {
            System.out.println("🤡 Image already exists: " + ex.getMessage());
            context.setStableDigest(currentDigest);
        }
    }
}
