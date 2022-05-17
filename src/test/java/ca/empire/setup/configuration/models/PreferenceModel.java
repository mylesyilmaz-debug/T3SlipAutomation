package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PreferenceModel {
    @JsonProperty private String name;
    @JsonProperty private Object value;

    public PreferenceModel() {
        setName(null);
        setValue(null);
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public PreferenceModel setName(String name) {
        this.name = name;
        return this;
    }

    public PreferenceModel setValue(Object value) {
        this.value = value;
        return this;
    }
}
