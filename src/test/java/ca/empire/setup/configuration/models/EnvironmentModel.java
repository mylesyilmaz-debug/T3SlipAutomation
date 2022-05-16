package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnvironmentModel {
    @JsonProperty private String name;
    @JsonProperty private String filepath;

    public String getName() {
        return name;
    }

    public String getFilepath() {
        return filepath;
    }
}
