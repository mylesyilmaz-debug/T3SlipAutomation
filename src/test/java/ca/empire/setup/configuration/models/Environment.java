package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Environment implements Model {
    public final String name;
    public final String filepath;

    private static final Logger logger = LogManager.getLogger(Environment.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Environment(
            @JsonProperty("name") String name, @JsonProperty("filepath") String filepath) {
        logger.traceEntry(() -> name, () -> filepath);
        this.name = name;
        this.filepath = filepath;
        logger.traceExit();
    }
}
