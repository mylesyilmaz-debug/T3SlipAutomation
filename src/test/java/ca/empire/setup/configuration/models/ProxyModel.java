package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyModel {
    @JsonProperty private int minPort;
    @JsonProperty private int maxPort;
    @JsonProperty private boolean enabled;

    public ProxyModel() {
        setMinPort(-1);
        setMaxPort(-1);
        setEnabled(false);
    }

    public int getMinPort() {
        return minPort;
    }

    public int getMaxPort() {
        return maxPort;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public ProxyModel setMinPort(int minPort) {
        this.minPort = minPort;
        return this;
    }

    public ProxyModel setMaxPort(int maxPort) {
        this.maxPort = maxPort;
        return this;
    }

    public ProxyModel setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
