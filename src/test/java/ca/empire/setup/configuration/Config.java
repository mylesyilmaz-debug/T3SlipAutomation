package ca.empire.setup.configuration;

import ca.empire.setup.configuration.models.Configuration;
import ca.empire.setup.configuration.models.Mapping;
import ca.empire.setup.configuration.models.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/** Represents a YAML configuration file */
public class Config {
    private Configuration configurationModel;
    private Profile profile;

    /**
     * Loads in the provided YAML file.
     *
     * @param configYaml - the desired YAML file.
     * @throws IOException if an IOException occurs when loading the provided YAML file.
     */
    public Config(@NotNull File configYaml, String profileName) throws IOException {
        if (!configYaml.exists()) {
            throw new IllegalArgumentException(
                    "The provided file " + configYaml.getPath() + " does not exist.");
        } else if (!configYaml.isFile()) {
            throw new IllegalArgumentException(
                    "The provided file " + configYaml.getPath() + " is a directory.");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        configurationModel = mapper.readValue(configYaml, Configuration.class);
        profile = null;

        if (profileName == null) {
            profileName = "DEFAULT";
        }

        for (Profile profile : configurationModel.profiles) {
            if (profile.name.equalsIgnoreCase(profileName)) {
                this.profile = profile;
                break;
            }
        }

        if (profile == null) {
            throw new NullPointerException("No profile named " + profileName + " exists.");
        }

        System.out.printf("Profile found!\nName: %s\nDescription: %s\n", profile.name, profile.description);

        ArrayList<Mapping> systemProperties  = new ArrayList<>();

        if (configurationModel.systemProperties != null && profile.systemProperties != null) {
            systemProperties.addAll(configurationModel.systemProperties);
            systemProperties.addAll(profile.systemProperties);

        } else if (configurationModel.systemProperties != null) {
            systemProperties.addAll(configurationModel.systemProperties);
        } else if (profile.systemProperties != null) {
            systemProperties.addAll(profile.systemProperties);
        }

        for (Mapping mapping : systemProperties) {
            System.setProperty(mapping.key, mapping.value);
        }
    }

    /**
     * Loads in the YAML file at the provided filepath.
     *
     * @param configYamlFilepath - the filepath that points to the desired YAML file.
     * @throws IOException if an IOException occurs when loading the provided YAML file.
     */
    public Config(@NotNull String configYamlFilepath, String profile) throws IOException {
        this(new File(configYamlFilepath), profile);
    }

    public Configuration getConfigurationModel() {
        return configurationModel;
    }

    public Profile getProfile() {
        return profile;
    }
}
