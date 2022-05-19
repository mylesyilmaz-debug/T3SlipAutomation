package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Configuration implements Model {
    public final Environment environment;
    public final String defaultProfile;
    public final List<Mapping> systemProperties;
    public final List<Profile> profiles;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Configuration(
            @JsonProperty("environment") Environment environment,
            @JsonProperty("defaultProfile") String defaultProfile,
            @JsonProperty("systemProperties") List<Mapping> systemProperties,
            @JsonProperty("profiles") List<Profile> profiles) {
        this.environment = environment;
        this.defaultProfile = defaultProfile;
        this.systemProperties = systemProperties;
        this.profiles = profiles;
    }
}
