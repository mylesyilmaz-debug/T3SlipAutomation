package ca.empire.setup.configuration;

import ca.empire.setup.configuration.models.Configuration;
import ca.empire.setup.configuration.models.Mapping;
import ca.empire.setup.configuration.models.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/** Represents a YAML configuration file */
public class Config {
    private Configuration configurationModel;
    private Profile profile;

    private static final Logger logger = LogManager.getLogger(Config.class);

    /**
     * Loads in the provided YAML file.
     *
     * @param configYaml - the desired YAML file.
     * @throws IOException if an IOException occurs when loading the provided YAML file.
     */
    public Config(@NotNull File configYaml, String profileName) throws IOException {
        logger.traceEntry(() -> configYaml, () -> profileName);

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

        for (Profile profile : configurationModel.profiles) {
            if (profile.name.equalsIgnoreCase(profileName)) {
                this.profile = profile;
                break;
            }
        }

        if (profile == null) {
            throw new NullPointerException("No profile named " + profileName + " exists.");
        }

        logger.info("Profile found! " + profile.name + " - " + profile.description);

        ArrayList<Mapping> systemProperties = new ArrayList<>();

        if (configurationModel.systemProperties != null && profile.systemProperties != null) {
            systemProperties.addAll(configurationModel.systemProperties);
            systemProperties.addAll(profile.systemProperties);

        } else if (configurationModel.systemProperties != null) {
            systemProperties.addAll(configurationModel.systemProperties);
        } else if (profile.systemProperties != null) {
            systemProperties.addAll(profile.systemProperties);
        }

        for (Mapping mapping : systemProperties) {
            logger.info("Setting system property - " + mapping.key + ":" + mapping.value);
            System.setProperty(mapping.key, mapping.value);
        }

        logger.traceExit();
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
        logger.traceEntry();
        logger.traceExit(configurationModel);
        return configurationModel;
    }

    public Profile getProfile() {
        logger.traceEntry();
        logger.traceExit(profile);
        return profile;
    }
}
