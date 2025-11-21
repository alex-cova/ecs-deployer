package com.alexcova.ecs;

import software.amazon.awssdk.services.ecr.model.*;

import java.util.ArrayList;
import java.util.Scanner;

public class ImageTagger {

    public static void main(String[] args) {
        Context context = new Context();

        context.doLogin();

        var scanner = new Scanner(System.in);

        System.out.println("Enter service name: ");
        var serviceName = scanner.nextLine();

        var listResponse = context.getEcrClient()
                .listImages(ListImagesRequest.builder()
                        .filter(ListImagesFilter.builder()
                                .tagStatus(TagStatus.TAGGED).build())
                        .repositoryName(serviceName)
                        .build());

        var imageList = new ArrayList<String>();

        var count = 0;

        for (ImageIdentifier imageId : listResponse.imageIds()) {
            System.out.println(count + " > " + imageId.imageDigest() + " - " + imageId.imageTag());
            imageList.add(imageId.imageDigest());
            count++;
        }

        System.out.println("Enter image diggest index: 0 - " + (count - 1));
        var currentDiggest = scanner.nextLine();

        if (!currentDiggest.matches("[0-9]+")) {
            System.out.println("Invalid index");
            return;
        }

        currentDiggest = imageList.get(Integer.parseInt(currentDiggest));
        final String i = currentDiggest;

        var found = listResponse.imageIds()
                .stream()
                .filter(it -> it.imageDigest().equals(i))
                .findAny()
                .orElseThrow();

        System.out.println(found.imageDigest());

        var response = context.getEcrClient().batchGetImage(
                BatchGetImageRequest.builder()
                        .repositoryName(serviceName)
                        .imageIds(
                                ImageIdentifier.builder()
                                        .imageDigest(found.imageDigest())
                                        .build()
                        )
                        .build()
        );

        var manifest = response.images().getFirst().imageManifest();

        System.out.println("image tag:");
        var tag = scanner.nextLine();

        if (tag.isEmpty()) {
            System.out.println("Invalid tag value");
            return;
        }

        context.getEcrClient()
                .putImage(PutImageRequest.builder()
                        .repositoryName(serviceName)
                        .imageManifest(manifest)
                        .imageTag(tag)
                        .build());

        System.out.println("Tagged image as: " + tag);

    }
}
