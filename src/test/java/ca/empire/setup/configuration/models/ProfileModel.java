package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ProfileModel {
    @JsonProperty private String name;
    @JsonProperty private String description;
    @JsonProperty private String driverFramework;
    @JsonProperty private List<CapabilityModel> capabilities;
    @JsonProperty private List<String> arguments;
    @JsonProperty private List<PreferenceModel> preferences;

    public ProfileModel() {
        setName(null);
        setDescription(null);
        setDriverFramework(null);
        setCapabilities(null);
        setArguments(null);
        setPreferences(null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDriverFramework() {
        return driverFramework;
    }

    public List<CapabilityModel> getCapabilities() {
        return capabilities;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public List<PreferenceModel> getPreferences() {
        return preferences;
    }

    public ProfileModel setName(String name) {
        this.name = name;
        return this;
    }

    public ProfileModel setDescription(String description) {
        this.description = description;
        return this;
    }

    public ProfileModel setDriverFramework(String driverFramework) {
        this.driverFramework = driverFramework;
        return this;
    }

    public ProfileModel setCapabilities(List<CapabilityModel> capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public ProfileModel setArguments(List<String> arguments) {
        this.arguments = arguments;
        return this;
    }

    public ProfileModel setPreferences(List<PreferenceModel> preferences) {
        this.preferences = preferences;
        return this;
    }
}
