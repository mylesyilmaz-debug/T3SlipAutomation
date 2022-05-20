package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Driver implements Model {
    public final String framework;
    public final List<Mapping> capabilities;
    public final List<String> arguments;
    public final List<Mapping> preferences;

    private static final Logger logger = LogManager.getLogger(Driver.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Driver(
            @JsonProperty("framework") String framework,
            @JsonProperty("capabilities") List<Mapping> capabilities,
            @JsonProperty("arguments") List<String> arguments,
            @JsonProperty("preferences") List<Mapping> preferences) {
        logger.traceEntry(() -> framework, () -> capabilities, () -> arguments, () -> preferences);
        this.framework = framework;
        this.capabilities = capabilities;
        this.arguments = arguments;
        this.preferences = preferences;
    }
}
