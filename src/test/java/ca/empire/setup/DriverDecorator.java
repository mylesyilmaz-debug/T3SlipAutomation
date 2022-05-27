package ca.empire.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.UUID;

/**
 * While this isn't quite a decorator, it does allow us to stored some additional information that
 * is associated with the driver.
 */
public class DriverDecorator {
    private WebDriver driver;
    private File downloadDirectory;
    private UUID uuid;

    private static final Logger logger = LogManager.getLogger(DriverDecorator.class);

    public DriverDecorator() {
        logger.traceEntry();
        logger.traceExit();
    }

    public WebDriver getDriver() {
        logger.traceEntry();
        logger.traceExit(driver);
        return driver;
    }

    public File getDownloadDirectory() {
        logger.traceEntry();
        logger.traceExit(downloadDirectory);
        return downloadDirectory;
    }

    public UUID getUuid() {
        logger.traceEntry();
        logger.traceExit(uuid);
        return uuid;
    }

    public DriverDecorator setDriver(WebDriver driver) {
        logger.traceEntry(() -> driver);
        this.driver = driver;
        logger.traceExit(this);
        return this;
    }

    public DriverDecorator setDownloadDirectory(File downloadDirectory) {
        logger.traceEntry(() -> downloadDirectory);
        this.downloadDirectory = downloadDirectory;
        logger.traceExit(this);
        return this;
    }

    public DriverDecorator setUuid(UUID uuid) {
        logger.traceEntry(() -> uuid);
        this.uuid = uuid;
        logger.traceExit(this);
        return this;
    }
}
