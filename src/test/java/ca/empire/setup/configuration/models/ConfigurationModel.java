package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigurationModel {
    @JsonProperty private EnvironmentModel environmentModel;
    @JsonProperty private List<ProfileModel> profiles;

    public EnvironmentModel getEnvironmentModel() {
        return environmentModel;
    }

    public List<ProfileModel> getProfiles() {
        return profiles;
    }
}
