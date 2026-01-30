package ca.empire.setup.configuration;

import ca.empire.setup.configuration.models.Configuration;
import ca.empire.setup.configuration.models.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.Properties;
import java.io.FileInputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/** Represents a YAML configuration file */
public class Config {
    private Configuration configurationModel;
    private Profile profile;
    private static Properties envProperties = new Properties();

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

        // --- NEW LOGIC STARTS HERE ---
        // We ignore the profile name and look for the file the framework wants: pilot.env
        String projectRoot = System.getProperty("user.dir");
        String envPath = projectRoot + File.separator + "environments" + File.separator + "pilot.env";

        logger.info("Config is loading URL from: " + envPath);
        loadEnvFile(envPath);
        // --- NEW LOGIC ENDS HERE ---

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

    /**
     * Loads the .env file into the static Properties object.
     */
    private static void loadEnvFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                envProperties.load(fis);
                logger.info("Successfully loaded environment properties from: " + filePath);
            } catch (IOException e) {
                logger.error("Failed to load .env file: " + filePath, e);
            }
        } else {
            logger.warn("Environment file not found at: " + filePath + ". Skipping load.");
        }
    }

    /**
     * Gets a property from the loaded .env file.
     */
    public static String getEnvProperty(String key) {
        return envProperties.getProperty(key);
    }

    /**
     * Returns the PageCenterX URL from the .env file.
     */
    public static String getPcxUrl() {
        return getEnvProperty("pcx-url");
    }

    /**
     * Returns the GCP Bucket URL from the .env file.
     */
    public static String getGcpUrl() {
        return getEnvProperty("gcp-bucket-url");
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

    /**
     * Helper to get GCP Email from System Environment.
     */
    public static String getGcpEmail() {
        String email = System.getenv("EDGE_EMAIL");
        if (email == null) {
            throw new RuntimeException("EDGE_EMAIL environment variable is missing!");
        }
        return email;
    }

    /**
     * Helper to get GCP Password from System Environment.
     */
    public static String getGcpPassword() {
        String pass = System.getenv("EDGE_PASSWORD");
        if (pass == null) {
            throw new RuntimeException("EDGE_PASSWORD environment variable is missing!");
        }
        return pass;
    }




}
