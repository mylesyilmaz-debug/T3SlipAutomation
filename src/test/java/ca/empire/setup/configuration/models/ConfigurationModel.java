package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigurationModel {
    @JsonProperty private EnvironmentModel environment;
    @JsonProperty private List<ProfileModel> profiles;
    private ProfileModel profile;

    public ConfigurationModel() {
        setEnvironment(null);
        setProfiles(null);
        setProfile(null);
    }

    public EnvironmentModel getEnvironment() {
        return environment;
    }

    public List<ProfileModel> getProfiles() {
        return profiles;
    }

    public ConfigurationModel setEnvironment(EnvironmentModel environment) {
        this.environment = environment;
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
