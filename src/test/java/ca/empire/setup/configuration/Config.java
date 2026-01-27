package ca.empire.setup.configuration;

import ca.empire.setup.configuration.models.Configuration;
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
                    "The provided file " + configYaml.getPath() + " is not a file.");
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        configurationModel = mapper.readValue(configYaml, Configuration.class);
        profile = null;

        String derivedProfileName =
                profileName == null || profileName.isEmpty()
                        || profileName.equalsIgnoreCase("defaultProfile")
                        ? configurationModel.defaultProfile
                        : profileName;

        if (derivedProfileName == null || derivedProfileName.isEmpty()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(
                            "No profile was provided and the defaultProfile is null or empty.");
            logger.fatal(e);
            throw e;
        }

        for (Profile profile : configurationModel.profiles) {
            if (profile.name.equalsIgnoreCase(derivedProfileName)) {
                this.profile = profile;
                break;
            }
        }

        if (profile == null) {
            throw new NullPointerException("No profile named " + derivedProfileName + " exists.");
        }

        logger.info("Profile found! " + profile.name + " - " + profile.description);

        if (configurationModel.systemProperties != null) {
            configurationModel.systemProperties.forEach(System::setProperty);
        }

        if (profile.systemProperties != null) {
            profile.systemProperties.forEach(System::setProperty);
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

    /**
     * Retrieves the PageCenterX username from Environment Variables.
     * @return String username
     */
    public static String getPcxUsername() {
        String user = System.getenv("PCX_USERNAME");
        if (user == null || user.isEmpty()) {
            logger.error("PCX_USERNAME Environment Variable is NOT set!");
            throw new RuntimeException("Missing environment variable: PCX_USERNAME");
        }
        logger.info("PCX_USERNAME loaded successfully from environment.");
        return user;
    }

    /**
     * Retrieves the PageCenterX password from Environment Variables.
     * @return String password
     */
    public static String getPcxPassword() {
        String pass = System.getenv("PCX_PASSWORD");
        if (pass == null || pass.isEmpty()) {
            logger.error("PCX_PASSWORD Environment Variable is NOT set!");
            throw new RuntimeException("Missing environment variable: PCX_PASSWORD");
        }
        // We log that it was found, but we NEVER log the actual password string.
        logger.info("PCX_PASSWORD loaded successfully from environment.");
        return pass;
    }

    /** Retrieves the Database username */
    public static String getDbUsername() {
        String user = System.getenv("DB_USERNAME");
        if (user == null) throw new RuntimeException("DB_USERNAME env variable is missing!");
        return user;
    }

    /** Retrieves the Database password */
    public static String getDbPassword() {
        String pass = System.getenv("DB_PASSWORD");
        if (pass == null) throw new RuntimeException("DB_PASSWORD env variable is missing!");
        return pass;
    }
}
