package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EnvironmentModel {
    @JsonProperty private String name;
    @JsonProperty private String filepath;

    public EnvironmentModel() {
        setName(null);
        setFilepath(null);
    }

    public String getName() {
        return name;
    }

    public String getFilepath() {
        return filepath;
    }

    public EnvironmentModel setName(String name) {
        this.name = name;
        return this;
    }

    public EnvironmentModel setFilepath(String filepath) {
        this.filepath = filepath;
        return this;
    }
}
