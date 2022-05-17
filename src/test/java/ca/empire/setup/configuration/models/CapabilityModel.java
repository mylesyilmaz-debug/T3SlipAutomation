package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CapabilityModel {
    @JsonProperty private String name;
    @JsonProperty private String value;

    public CapabilityModel() {
        setName(null);
        setValue(null);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public CapabilityModel setName(String name) {
        this.name = name;
        return this;
    }

    public CapabilityModel setValue(String value) {
        this.value = value;
        return this;
    }

}
