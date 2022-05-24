package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Proxy implements Model {
    public final int minPort;
    public final int maxPort;
    public final boolean enabled;

    private static final Logger logger = LogManager.getLogger(Proxy.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Proxy(
            @JsonProperty("minPort") int minPort,
            @JsonProperty("maxPort") int maxPort,
            @JsonProperty("enabled") boolean enabled) {
        logger.traceEntry(() -> minPort, () -> maxPort, () -> enabled);
        this.minPort = minPort;
        this.maxPort = maxPort;
        this.enabled = enabled;
        logger.traceExit();
    }
}
