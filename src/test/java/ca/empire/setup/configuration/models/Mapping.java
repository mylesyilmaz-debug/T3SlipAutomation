package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Mapping implements Model {
    public final String key;
    public final String value;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Mapping(@JsonProperty("key") String name, @JsonProperty("value") String value) {
        this.key = name;
        this.value = value;
    }
}
