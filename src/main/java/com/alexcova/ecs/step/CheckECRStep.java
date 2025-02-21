package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecr.model.ImageIdentifier;

import java.util.Objects;

public class CheckECRStep extends Step {
    @Override
    public void execute(@NotNull Context context) {
        Objects.requireNonNull(context.getServiceName());
        Objects.requireNonNull(context.getClusterName());

        if (context.getCurrentFamily().isEmpty()) {
            throw new IllegalStateException("Service name or family is empty");
        }

        var ecrClient = context.getEcrClient();
        var currentFamily = context.getCurrentFamily();

        var imagesResponse = ecrClient.listImages(r -> r.repositoryName(currentFamily));

        for (ImageIdentifier imageId : imagesResponse.imageIds()) {
            if ("latest".equals(imageId.imageTag())) {
                context.setLatestImageDigest(imageId.imageDigest());
                continue;
            }

            if ("stable".equals(imageId.imageTag())) {
                context.setStableDigest(imageId.imageDigest());
            }
        }

        if (context.getStableDigest().isEmpty()) {
            System.err.println("⚠️ WARNING no stable image found!");
        } else {
            System.out.println("🤙 Stable digest: " + context.getStableDigest());
        }
    }
}
