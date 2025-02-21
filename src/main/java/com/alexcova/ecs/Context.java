package com.alexcova.ecs;

import com.alexcova.eureka.EurekaClient;
import com.alexcova.eureka.Instance;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Context {

    static HttpClient httpClient = HttpClient.newHttpClient();

    private final AwsCredentials credentials = new AwsCredentials();
    private final EurekaClient eurekaClient;
    private boolean needsDefinitionUpdate = false;
    private int stopTimeout = 30;
    private int waitTime = 120;

    private String currentTag = "latest";

    private String currentFamily = "";
    private String currentImage = "";

    private int newRevision = 0;

    private String latestImageDigest = "";
    private String stableDigest = "";

    private final List<ARN> currentTasksArns = new ArrayList<>();
    private final List<ARN> backupTasksArns = new ArrayList<>();
    private final List<String> containers = new ArrayList<>();
    private final List<Instance> currentEurekaInstances = new ArrayList<>();
    private final List<String> oldTasks = new ArrayList<>();

    private final Scanner scanner = new Scanner(System.in);

    private TaskDefinition taskDefinition = null;
    private NetworkConfiguration networkConfiguration = null;

    private EcsClient ecsClient = null;
    private EcrClient ecrClient = null;

    private String serviceName;
    private String clusterName;

    private boolean usingECSID;
    private HealthChecker healthChecker;
    private final Configuration configuration;

    public Context() {
        var config = new File("deployerConfig.json");

        if (!config.exists()) {
            throw new IllegalStateException("eurekaConfig.json not found");
        }

        try {
            ObjectMapper mapper = new ObjectMapper()
                    .findAndRegisterModules();
            this.configuration = mapper.readValue(config, Configuration.class);
            eurekaClient = new EurekaClient(configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void startHealthChecker() {
        if (healthChecker != null) {
            throw new IllegalStateException("Health checker already started");
        }

        healthChecker = new HealthChecker(serviceName, clusterName, configuration);
        healthChecker.start();
    }

    public HealthChecker stopHealthChecker() {
        if (healthChecker == null) {
            throw new IllegalStateException("Health checker not started");
        }

        healthChecker.stop();

        return healthChecker;
    }

    public void doLogin() {
        if (ecrClient != null) {
            return;
        }

        System.err.println("Go production? (y/n): ");

        String production = scanner.nextLine();

        String fileName = ".aws/credentials";

        if ("y".equals(production)) {
            fileName = ".aws/prod-credentials";
        }

        File credentialsFile = new File(System.getProperty("user.home"), fileName);

        if (!credentialsFile.exists()) {
            System.out.println("path: " + credentialsFile.getAbsolutePath());
            throw new IllegalStateException("Credentials file not found. Please create a credentials file in the .aws directory in your home directory.");
        }

        try {
            Files.readAllLines(credentialsFile.toPath()).forEach(line -> {
                if (line.startsWith("aws_access_key_id")) {
                    credentials.setAccessKeyId(line.split("=")[1].trim());
                } else if (line.startsWith("aws_secret_access_key")) {
                    credentials.setSecretAccessKey(line.split("=")[1].trim());
                }
            });
        } catch (IOException e) {
            Logger.getLogger(Context.class.getName())
                    .log(Level.SEVERE, null, e);
        }

        System.out.println("🔑 AWS credential key id: " + credentials.getAccessKeyId() + "\n");

        ecsClient = EcsClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(() -> AwsBasicCredentials.create(
                        credentials.getAccessKeyId(),
                        credentials.getSecretAccessKey()
                ))
                .build();

        ecrClient = EcrClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(() -> AwsBasicCredentials.create(
                        credentials.getAccessKeyId(),
                        credentials.getSecretAccessKey()
                ))
                .build();
    }

    public boolean putOutOfService(String arn) {
        List<Instance> instances = eurekaClient.getInstances(serviceName, clusterName);

        Instance instance = instances.stream()
                .filter(inst -> inst.getInstanceId().equals(arn))
                .findFirst()
                .orElse(null);

        if (instance != null) {
            if (eurekaClient.outOfService(serviceName, instance.getInstanceId(), clusterName)) {
                System.out.println("✅ Task " + instance.getInstanceId() + " is out of service");
                return true;
            } else {
                System.out.println("⚠️ Failed to put task " + instance.getInstanceId() + " out of service");
            }
        } else {
            System.out.println("⚠️ Instance " + arn + " not found in eureka");

            instances.forEach(inst -> System.out.println("\t- Instance: " +
                    inst.getInstanceId() + " " + inst.getIpAddr() + ":" + inst.getPort() + " " +
                    inst.getStatus() + " " + LocalDateTime
                    .ofEpochSecond(inst.getLeaseInfo().getRegistrationTimestamp(), 0, java.time.ZoneOffset.UTC)));

            System.out.println("Which task do you want to stop? (instance id)");
            String taskId = scanner.nextLine();

            if (!taskId.isEmpty()) {
                return putOutOfService(taskId.trim());
            }
        }

        return false;
    }

    public void checkEurekaService() {
        currentEurekaInstances.clear();
        currentEurekaInstances.addAll(eurekaClient.getInstances(serviceName, clusterName));

        if (currentEurekaInstances.isEmpty()) {
            throw new IllegalStateException("No instances found in eureka for service " + serviceName + " in " + clusterName + ", are you sure the service is registered?");
        } else {
            usingECSID = true;

            System.out.println("Found " + currentEurekaInstances.size() + " instances in eureka for service " + serviceName + " in " + clusterName);
            for (Instance currentInstance : currentEurekaInstances) {
                System.out.println("\t " + currentInstance.getInstanceId());

                if (!currentInstance.getInstanceId().matches("[a-z0-9]{32}")) {
                    System.err.println("⚠️ Invalid instance id " + currentInstance.getInstanceId() + " instance not supported");
                    usingECSID = false;
                }
            }
        }
    }

    public void fetchWaitTime() {
        var prometheus = clusterName.equals("development") ? configuration.getPrometheusDevelopment() : configuration.getPrometheusProduction();

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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Failed to get prometheus metrics (" + response.statusCode() + ")");
                return;
            }

            String body = response.body();
            String[] lines = body.split("\n");
            for (String line : lines) {
                if (line.startsWith("application_ready_time_seconds")) {
                    String[] parts = line.split(" ");
                    waitTime = (int) Double.parseDouble(parts[1]) + 10;
                }
            }
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(Context.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    public EcrClient getEcrClient() {
        return ecrClient;
    }

    public boolean isUsingECSID() {
        return usingECSID;
    }

    public EurekaClient getEurekaClient() {
        return eurekaClient;
    }

    public boolean isNeedsDefinitionUpdate() {
        return needsDefinitionUpdate;
    }

    public int getStopTimeout() {
        return stopTimeout;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public String getCurrentTag() {
        return currentTag;
    }

    public String getCurrentFamily() {
        return currentFamily;
    }


    public String getCurrentImage() {
        return currentImage;
    }


    public int getNewRevision() {
        return newRevision;
    }

    public String getLatestImageDigest() {
        return latestImageDigest;
    }

    public String getStableDigest() {
        return stableDigest;
    }

    public List<ARN> getCurrentTasksArns() {
        return currentTasksArns;
    }

    public void addBackupTask(ARN arn) {
        backupTasksArns.add(arn);
    }

    public void addBackupTask(List<ARN> arns) {
        backupTasksArns.addAll(arns);
    }

    public List<ARN> getBackupTasksArns() {
        return backupTasksArns;
    }

    public List<Instance> getCurrentEurekaInstances() {
        return currentEurekaInstances;
    }

    public List<String> getContainers() {
        return containers;
    }

    public Scanner getScanner() {
        return scanner;
    }

    public TaskDefinition getTaskDefinition() {
        return taskDefinition;
    }

    public NetworkConfiguration getNetworkConfiguration() {
        return networkConfiguration;
    }

    public List<String> getOldTasks() {
        return oldTasks;
    }

    public EcsClient getEcsClient() {
        return ecsClient;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setNeedsDefinitionUpdate(boolean needsDefinitionUpdate) {
        this.needsDefinitionUpdate = needsDefinitionUpdate;
    }

    public void setStopTimeout(int stopTimeout) {
        this.stopTimeout = stopTimeout;
    }


    public void setCurrentTag(String currentTag) {
        this.currentTag = currentTag;
    }

    public void setCurrentFamily(String currentFamily) {
        this.currentFamily = currentFamily;
    }

    public void setCurrentImage(String currentImage) {
        this.currentImage = currentImage;
    }

    public void setNewRevision(int newRevision) {
        this.newRevision = newRevision;
    }

    public void setLatestImageDigest(String latestImageDigest) {
        this.latestImageDigest = latestImageDigest;
    }

    public void setStableDigest(String stableDigest) {
        this.stableDigest = stableDigest;
    }

    public void setCurrentTasksArns(List<ARN> currentTasksArns) {
        this.currentTasksArns.clear();
        this.currentTasksArns.addAll(currentTasksArns);
    }

    public void setTaskDefinition(TaskDefinition taskDefinition) {
        this.taskDefinition = taskDefinition;
    }

    public void setNetworkConfiguration(NetworkConfiguration networkConfiguration) {
        this.networkConfiguration = networkConfiguration;
    }

    public void setServiceName(String serviceName) {
        if (this.serviceName != null) return;
        this.serviceName = serviceName;
    }

    public void setClusterName(String clusterName) {
        if (this.clusterName != null) return;
        this.clusterName = clusterName;
    }

    public void addOldTask(String id) {
        oldTasks.add(id);
    }

    public void addOldTask(List<String> ids) {
        oldTasks.addAll(ids);
    }

    public void clearOldTasks() {
        oldTasks.clear();
    }

    public void addContainer(String imageDigest) {
        containers.add(imageDigest);
    }


    public List<Instance> getEurekaInstances() {
        return eurekaClient.getInstances(serviceName, clusterName);
    }
}
