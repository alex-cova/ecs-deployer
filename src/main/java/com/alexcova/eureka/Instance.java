package com.alexcova.eureka;

public class Instance {
    private String instanceId;
    private String hostName;
    private String app;
    private String ipAddr;
    private String status;
    private String overriddenStatus;
    private Port port;
    private Port securePort;
    private long countryId;
    private DataCenterInfo dataCenterInfo;
    private LeaseInfo leaseInfo;
    private Metadata metadata;
    private String appGroupName;
    private String homePageUrl;
    private String statusPageUrl;
    private String healthCheckUrl;
    private String vipAddress;
    private String secureVipAddress;
    private String isCoordinatingDiscoveryServer;
    private String lastUpdatedTimestamp;
    private String lastDirtyTimestamp;
    private String actionType;

    public String getInstanceId() {
        return instanceId;
    }

    public Instance setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    public String getHostName() {
        return hostName;
    }

    public Instance setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public String getApp() {
        return app;
    }

    public Instance setApp(String app) {
        this.app = app;
        return this;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public Instance setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Instance setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getOverriddenStatus() {
        return overriddenStatus;
    }

    public Instance setOverriddenStatus(String overriddenStatus) {
        this.overriddenStatus = overriddenStatus;
        return this;
    }

    public Port getPort() {
        return port;
    }

    public Instance setPort(Port port) {
        this.port = port;
        return this;
    }

    public Port getSecurePort() {
        return securePort;
    }

    public Instance setSecurePort(Port securePort) {
        this.securePort = securePort;
        return this;
    }

    public long getCountryId() {
        return countryId;
    }

    public Instance setCountryId(long countryId) {
        this.countryId = countryId;
        return this;
    }

    public DataCenterInfo getDataCenterInfo() {
        return dataCenterInfo;
    }

    public Instance setDataCenterInfo(DataCenterInfo dataCenterInfo) {
        this.dataCenterInfo = dataCenterInfo;
        return this;
    }

    public LeaseInfo getLeaseInfo() {
        return leaseInfo;
    }

    public Instance setLeaseInfo(LeaseInfo leaseInfo) {
        this.leaseInfo = leaseInfo;
        return this;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Instance setMetadata(Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getAppGroupName() {
        return appGroupName;
    }

    public Instance setAppGroupName(String appGroupName) {
        this.appGroupName = appGroupName;
        return this;
    }

    public String getHomePageUrl() {
        return homePageUrl;
    }

    public Instance setHomePageUrl(String homePageUrl) {
        this.homePageUrl = homePageUrl;
        return this;
    }

    public String getStatusPageUrl() {
        return statusPageUrl;
    }

    public Instance setStatusPageUrl(String statusPageUrl) {
        this.statusPageUrl = statusPageUrl;
        return this;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public Instance setHealthCheckUrl(String healthCheckUrl) {
        this.healthCheckUrl = healthCheckUrl;
        return this;
    }

    public String getVipAddress() {
        return vipAddress;
    }

    public Instance setVipAddress(String vipAddress) {
        this.vipAddress = vipAddress;
        return this;
    }

    public String getSecureVipAddress() {
        return secureVipAddress;
    }

    public Instance setSecureVipAddress(String secureVipAddress) {
        this.secureVipAddress = secureVipAddress;
        return this;
    }

    public String getIsCoordinatingDiscoveryServer() {
        return isCoordinatingDiscoveryServer;
    }

    public Instance setIsCoordinatingDiscoveryServer(String isCoordinatingDiscoveryServer) {
        this.isCoordinatingDiscoveryServer = isCoordinatingDiscoveryServer;
        return this;
    }

    public String getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public Instance setLastUpdatedTimestamp(String lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        return this;
    }

    public String getLastDirtyTimestamp() {
        return lastDirtyTimestamp;
    }

    public Instance setLastDirtyTimestamp(String lastDirtyTimestamp) {
        this.lastDirtyTimestamp = lastDirtyTimestamp;
        return this;
    }

    public String getActionType() {
        return actionType;
    }

    public Instance setActionType(String actionType) {
        this.actionType = actionType;
        return this;
    }
}