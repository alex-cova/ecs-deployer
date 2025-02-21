package com.alexcova.ecs;

public class Configuration {

    private String eurekaProductionIp;
    private String eurekaDevelopmentIp;
    private String eurekaBasicAuthentication;
    private String prometheusProduction;
    private String prometheusDevelopment;

    public Configuration() {
    }

    public String getPrometheusDevelopment() {
        return prometheusDevelopment;
    }

    public Configuration setPrometheusDevelopment(String prometheusDevelopment) {
        this.prometheusDevelopment = prometheusDevelopment;
        return this;
    }

    public String getPrometheusProduction() {
        return prometheusProduction;
    }

    public Configuration setPrometheusProduction(String prometheusProduction) {
        this.prometheusProduction = prometheusProduction;
        return this;
    }

    public String getEurekaProductionIp() {
        return eurekaProductionIp;
    }

    public Configuration setEurekaProductionIp(String eurekaProductionIp) {
        this.eurekaProductionIp = eurekaProductionIp;
        return this;
    }

    public String getEurekaDevelopmentIp() {
        return eurekaDevelopmentIp;
    }

    public Configuration setEurekaDevelopmentIp(String eurekaDevelopmentIp) {
        this.eurekaDevelopmentIp = eurekaDevelopmentIp;
        return this;
    }

    public String getEurekaBasicAuthentication() {
        return eurekaBasicAuthentication;
    }

    public Configuration setEurekaBasicAuthentication(String eurekaBasicAuthentication) {
        this.eurekaBasicAuthentication = eurekaBasicAuthentication;
        return this;
    }
}
