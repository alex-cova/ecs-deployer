package com.alexcova.eureka;

import java.util.List;

public class Applications {
    private String versionsDelta;
    private String appsHashcode;
    private List<Application> application;

    public String getVersionsDelta() {
        return versionsDelta;
    }

    public Applications setVersionsDelta(String versionsDelta) {
        this.versionsDelta = versionsDelta;
        return this;
    }

    public String getAppsHashcode() {
        return appsHashcode;
    }

    public Applications setAppsHashcode(String appsHashcode) {
        this.appsHashcode = appsHashcode;
        return this;
    }

    public List<Application> getApplication() {
        return application;
    }

    public Applications setApplication(List<Application> application) {
        this.application = application;
        return this;
    }
}
