package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class DriverOptions implements Model {
    public final String name;
    public final String driverVersion;
    public final String browserVersion;
    public final Map<String, Object> capabilities;
    public final List<String> arguments;
    public final Map<String, String> preferences;
    public final Map<String, Object> experimentalOptions;

    private static final Logger logger = LogManager.getLogger(DriverOptions.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DriverOptions(
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty("driverVersion") String driverVersion,
            @JsonProperty("browserVersion") String browserVersion,
            @JsonProperty("capabilities") Map<String, Object> capabilities,
            @JsonProperty("arguments") List<String> arguments,
            @JsonProperty("experimentalOptions") Map<String, Object> experimentalOptions,
            @JsonProperty("preferences") Map<String, String> preferences) {
        logger.traceEntry(
                () -> name,
                () -> driverVersion,
                () -> browserVersion,
                () -> capabilities,
                () -> arguments,
                () -> experimentalOptions,
                () -> preferences);
        this.name = name;
        this.driverVersion = driverVersion;
        this.browserVersion = browserVersion;
        this.capabilities = capabilities;
        this.arguments = arguments;
        this.experimentalOptions = experimentalOptions;
        this.preferences = preferences;
        logger.traceExit(this);
    }
}
