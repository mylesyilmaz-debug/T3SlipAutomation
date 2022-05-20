package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Environment implements Model {
    public final String name;
    public final String filepath;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Environment(
            @JsonProperty("name") String name, @JsonProperty("filepath") String filepath) {
        this.name = name;
        this.filepath = filepath;
    }
}
