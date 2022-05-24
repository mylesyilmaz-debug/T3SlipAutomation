package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Profile implements Model {
    public final String name;
    public final String description;
    public final Driver driver;
    public final List<Mapping> systemProperties;

    private static final Logger logger = LogManager.getLogger(Profile.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Profile(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("driver") Driver driver,
            @JsonProperty("systemProperties") List<Mapping> systemProperties) {
        logger.traceEntry(() -> name, () -> description, () -> driver, () -> systemProperties);
        this.name = name;
        this.description = description;
        this.driver = driver;
        this.systemProperties = systemProperties;
        logger.traceExit();
    }
}
