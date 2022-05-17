package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigurationModel {
    @JsonProperty private EnvironmentModel environmentModel;
    @JsonProperty private List<ProfileModel> profiles;
    private ProfileModel profile;

    public ConfigurationModel() {
        setEnvironmentModel(null);
        setProfiles(null);
        setProfile(null);
    }

    public EnvironmentModel getEnvironmentModel() {
        return environmentModel;
    }

    public List<ProfileModel> getProfiles() {
        return profiles;
    }

    public ConfigurationModel setEnvironmentModel(EnvironmentModel environmentModel) {
        this.environmentModel = environmentModel;
        return this;
    }

    public ConfigurationModel setProfiles(List<ProfileModel> profiles) {
        this.profiles = profiles;
        return this;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public ConfigurationModel setProfile(ProfileModel profile) {
        this.profile = profile;
        return this;
    }
}
