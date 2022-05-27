package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class Profile implements Model {
    public final String name;
    public final String description;
    public final DriverOptions driverOptions;
    public final Map<String, String> systemProperties;

    private static final Logger logger = LogManager.getLogger(Profile.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Profile(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("driver") DriverOptions driverOptions,
            @JsonProperty("systemProperties") Map<String, String> systemProperties) {
        logger.traceEntry(() -> name, () -> description, () -> driverOptions, () -> systemProperties);
        this.name = name;
        this.description = description;
        this.driverOptions = driverOptions;
        this.systemProperties = systemProperties;
        logger.traceExit();
    }
}
