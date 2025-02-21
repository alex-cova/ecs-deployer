package com.alexcova.eureka;

public class EurekaResponse {
    private Application application;

    public Application getApplication() {
        return application;
    }

    public EurekaResponse setApplication(Application application) {
        this.application = application;
        return this;
    }
}