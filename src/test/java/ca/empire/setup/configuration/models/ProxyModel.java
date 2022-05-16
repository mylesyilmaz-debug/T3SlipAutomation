package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProxyModel {
    @JsonProperty private int port;
    @JsonProperty private boolean enabled;

    public int getPort() {
        return port;
    }

    public boolean getEnabled() {
        return enabled;
    }
}
