package com.alexcova.eureka;

public class AppsResponse {
    private Applications applications;

    public Applications getApplications() {
        return applications;
    }

    public AppsResponse setApplications(Applications applications) {
        this.applications = applications;
        return this;
    }
}