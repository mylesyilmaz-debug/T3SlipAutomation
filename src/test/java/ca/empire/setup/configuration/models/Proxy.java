package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Proxy implements Model {
    public final int minPort;
    public final int maxPort;
    public final boolean enabled;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Proxy(
            @JsonProperty("minPort") int minPort,
            @JsonProperty("maxPort") int maxPort,
            @JsonProperty("enabled") boolean enabled) {
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.enabled = enabled;
    }
}
