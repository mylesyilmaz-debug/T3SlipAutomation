package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreferenceModel {
    @JsonProperty private String name;
    @JsonProperty private Object value;

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
