package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.ecr.model.*;

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
        var imagesResponse = ecrClient.listImages(ListImagesRequest.builder()
                .repositoryName(currentFamily)
                .filter(ListImagesFilter.builder()
                        .tagStatus(TagStatus.TAGGED)
                        .build())
                .maxResults(250)
                .build());

        System.out.println("----");

        for (ImageIdentifier imageId : imagesResponse.imageIds()) {
            if (imageId.imageTag() == null) {
                continue;
            }

            if ("latest".equals(imageId.imageTag())) {
                context.setLatestImageDigest(imageId.imageDigest());
                continue;
            }

            if ("stable".equals(imageId.imageTag())) {
                context.setStableDigest(imageId.imageDigest());
            }

            System.out.println("🔍 Found image: " + imageId.imageDigest() + " with tag: " + imageId.imageTag());
        }

        System.out.println("----");

        if (context.getStableDigest().isEmpty()) {
            System.err.println("⚠️ WARNING no stable image found!");
        } else {
            System.out.println("🤙 Stable digest: " + context.getStableDigest());
        }
    }
}
