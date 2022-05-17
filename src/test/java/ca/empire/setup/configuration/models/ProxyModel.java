package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyModel {
    @JsonProperty private int minPort;
    @JsonProperty private int maxPort;
    @JsonProperty private boolean enabled;

    public int getMinPort() {
        return minPort;
    }

    public int getMaxPort() {
        return maxPort;
    }

    public boolean getEnabled() {
        return enabled;
    }
}
