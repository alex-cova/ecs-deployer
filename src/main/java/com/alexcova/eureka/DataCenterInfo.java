package com.alexcova.eureka;

public class DataCenterInfo {
    private String dataCenterInfoClass;
    private String name;

    public String getDataCenterInfoClass() {
        return dataCenterInfoClass;
    }

    public DataCenterInfo setDataCenterInfoClass(String dataCenterInfoClass) {
        this.dataCenterInfoClass = dataCenterInfoClass;
        return this;
    }

    public String getName() {
        return name;
    }

    public DataCenterInfo setName(String name) {
        this.name = name;
        return this;
    }
}