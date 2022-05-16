package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ProfileModel {
    @JsonProperty private String name;
    @JsonProperty private String description;
    @JsonProperty private String driverFramework;
    @JsonProperty private ProxyModel proxyModel;
    @JsonProperty private List<CapabilityModel> capabilities;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDriverFramework() {
        return driverFramework;
    }

    public ProxyModel getProxyModel() {
        return proxyModel;
    }

    public List<CapabilityModel> getCapabilities() {
        return capabilities;
    }
}
