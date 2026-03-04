package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class Configuration implements Model {
    public final Environment environment;
    public final DownloadManagement downloadManagement;
    public final String defaultProfile;
    public final Map<String, String> systemProperties;
    public final List<Profile> profiles;

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Configuration(
            @JsonProperty("environment") Environment environment,
            @JsonProperty(value = "downloadManagement") DownloadManagement downloadManagement,
            @JsonProperty("defaultProfile") String defaultProfile,
            @JsonProperty("systemProperties") Map<String, String> systemProperties,
            @JsonProperty(value = "profiles", required = true) List<Profile> profiles) {
        logger.traceEntry(
                () -> environment, () -> downloadManagement, () -> defaultProfile, () -> systemProperties, () -> profiles);

        DownloadManagement derivedDownloadManagement = downloadManagement;

        // Default the download management if a configuration was not provided
        if (derivedDownloadManagement == null) {
            derivedDownloadManagement = new DownloadManagement("afterEach", "failure");
        }

        this.environment = environment;
        this.downloadManagement = derivedDownloadManagement;
        this.defaultProfile = defaultProfile;
        this.systemProperties = systemProperties;
        this.profiles = profiles;
        logger.traceExit(this);
    }
}
