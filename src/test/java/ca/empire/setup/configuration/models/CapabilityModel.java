package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CapabilityModel {
    @JsonProperty private String name;
    @JsonProperty private String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
