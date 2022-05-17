package ca.empire.setup.configuration;

import ca.empire.setup.configuration.models.ConfigurationModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/** Represents a YAML configuration file */
public class Config {
    private ConfigurationModel configurationModel;

    /**
     * Creates a Config with the provided configuration model.
     *
     * @param configurationModel - the configuration model to use.
     */
    public Config(@NotNull ConfigurationModel configurationModel)
    {
        setConfigurationModel(configurationModel);
    }

    /**
     * Loads in the provided YAML file.
     *
     * @param configYaml - the desired YAML file.
     * @throws IOException if an IOException occurs when loading the provided YAML file.
     */
    public Config(@NotNull File configYaml) throws IOException {
        if (!configYaml.exists()) {
            throw new IllegalArgumentException(
                    "The provided file " + configYaml.getPath() + " does not exist.");
        } else if (!configYaml.isFile()) {
            throw new IllegalArgumentException(
                    "The provided file " + configYaml.getPath() + " is a directory.");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        configurationModel = mapper.readValue(configYaml, ConfigurationModel.class);
    }

    /**
     * Loads in the YAML file at the provided filepath.
     *
     * @param configYamlFilepath - the filepath that points to the desired YAML file.
     * @throws IOException if an IOException occurs when loading the provided YAML file.
     */
    public Config(@NotNull String configYamlFilepath) throws IOException {
        this(new File(configYamlFilepath));
    }

    public ConfigurationModel getConfigurationModel() {
        return configurationModel;
    }

    public Config setConfigurationModel(ConfigurationModel configurationModel) {
        this.configurationModel = configurationModel;
        return this;
    }
}
