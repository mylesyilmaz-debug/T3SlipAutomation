package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Mapping implements Model {
    public final String key;
    public final String value;

    private static final Logger logger = LogManager.getLogger(Mapping.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Mapping(@JsonProperty("key") String key, @JsonProperty("value") String value) {
        logger.traceEntry(() -> key, () -> value);
        this.key = key;
        this.value = value;
        logger.traceExit();
    }
}
