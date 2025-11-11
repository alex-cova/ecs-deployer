package com.alexcova.ecs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public final class HealthChecker implements Runnable {

    private final String service;
    private final Configuration configuration;
    @NotNull
    private final Context context;
    private final List<TimestampedResponse> responseCodes = new ArrayList<>();
    private volatile boolean stop = false;

    public HealthChecker(@NotNull Context context) {
        this.service = context.getServiceName();
        this.configuration = context.getConfiguration();
        this.context = context;
    }

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (!stop) {
            HttpResponse<String> response = sendRequest(service);

            if (response != null) {
                responseCodes.add(new TimestampedResponse(response.statusCode()));
            } else {
                responseCodes.add(new TimestampedResponse(-1));
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        stop = true;
    }


    @Nullable
    private HttpResponse<String> sendRequest(@NotNull String serviceName) {
        var prometheus = context.isProduction() ? configuration.getPrometheusProduction() : configuration.getPrometheusDevelopment();

        var url = "https://%s/%s/v1/actuator/prometheus"
                .formatted(prometheus, serviceName);

        if (serviceName.equals("graph")) {
            url = "https://%s/%s/actuator/prometheus"
                    .formatted(prometheus, serviceName);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();

        try {
            return Context.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            System.out.println("Failed to get prometheus metrics (" + ex.getMessage() + ")");
        }

        return null;
    }

    public List<TimestampedResponse> getResponseCodes() {
        return responseCodes;
    }
}