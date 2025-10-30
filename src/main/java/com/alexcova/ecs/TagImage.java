package com.alexcova.ecs;

import software.amazon.awssdk.services.ecr.model.*;


public class TagImage {

    public static void main(String[] args) {
        Context context = new Context();
        context.doLogin();

        var response = context.getEcrClient().batchGetImage(
                BatchGetImageRequest.builder()
                        .repositoryName("graph")
                        .imageIds(
                                ImageIdentifier.builder()
                                        .imageDigest("sha256:")
                                        .build()
                        )
                        .build()
        );

        context.getEcrClient().putImage(
                PutImageRequest.builder()
                        .repositoryName("graph")
                        .imageManifest(response.images().getFirst().imageManifest())
                        .imageTag("latest")
                        .build()
        );
    }
}
