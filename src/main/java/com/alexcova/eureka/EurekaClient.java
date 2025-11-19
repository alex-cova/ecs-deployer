package com.alexcova.eureka;

import com.alexcova.ecs.Configuration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EurekaClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Configuration config;

    public EurekaClient(Configuration config) {
        this.config = config;
    }

    public boolean outOfService(String application, String instanceId, String environment) {
        var request = HttpRequest.newBuilder(URI.create("http://" + getHost(environment) + "/eureka/apps/%s/%s/status?value=OUT_OF_SERVICE"
                        .formatted(application, instanceId)))
                .header("Authorization", "Basic " + config.getEurekaBasicAuthentication())
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

            return send.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(EurekaClient.class.getName())
                    .log(Level.SEVERE, null, e);
        }

        return false;
    }

    public boolean delete(String application, String environment) {
        var request = HttpRequest.newBuilder(URI.create("http://" + getHost(environment) + "/eureka/apps/%s"
                        .formatted(application)))
                .header("Authorization", "Basic " + config.getEurekaBasicAuthentication())
                .DELETE()
                .build();

        try {
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

            return send.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            Logger.getLogger(EurekaClient.class.getName())
                    .log(Level.SEVERE, null, e);
        }

        return false;
    }

    public boolean delete(String application, String instanceId, String environment) {
        var request = HttpRequest.newBuilder(URI.create("http://" + getHost(environment) + "/eureka/apps/%s/%s"
                        .formatted(application, instanceId)))
                .header("Authorization", "Basic " + config.getEurekaBasicAuthentication())
                .DELETE()
                .build();

        try {
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

            return send.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            Logger.getLogger(EurekaClient.class.getName())
                    .log(Level.SEVERE, null, e);
        }

        return false;
    }

    public String getHost(@NotNull String environment) {
        if (environment.equals("production")) {
            return config.getEurekaProductionIp();
        }

        return config.getEurekaDevelopmentIp();
    }

    public List<Instance> getInstances(String service, String environment) {
        var request = HttpRequest.newBuilder(URI.create("http://" + getHost(environment) + "/eureka/apps/" + service))
                .GET()
                .header("Authorization", "Basic " + config.getEurekaBasicAuthentication())
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() == 200) {
                return mapper.readValue(send.body(), SingleApplicationResponse.class)
                        .getApplication()
                        .getInstance();
            } else {
                System.out.println("Error: " + send.statusCode() + " " + send.body());
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }

    public List<Application> getApplications(String environment) {

        var request = HttpRequest.newBuilder(URI.create("http://" + getHost(environment) + "/eureka/apps"))
                .GET()
                .header("Authorization", "Basic " + config.getEurekaBasicAuthentication())
                .header("Accept", "application/json")
                .build();

        try {
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() == 200) {
                return mapper.readValue(send.body(), AppsResponse.class)
                        .getApplications()
                        .getApplication();
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }

}
