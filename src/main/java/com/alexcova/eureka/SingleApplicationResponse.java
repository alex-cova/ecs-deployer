package com.alexcova.eureka;

public class SingleApplicationResponse {

    private Application application;

    public Application getApplication() {
        return application;
    }

    public SingleApplicationResponse setApplication(Application application) {
        this.application = application;
        return this;
    }

    public SingleApplicationResponse() {
    }
}
