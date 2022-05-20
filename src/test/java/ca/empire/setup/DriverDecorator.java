package ca.empire.setup;

import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.util.UUID;

/**
 * While this isn't quite a decorator, it does allow us to stored some additional information that is associated with
 * the driver.
 */
public class DriverDecorator {
    private WebDriver driver;
    private File downloadDirectory;
    private UUID uuid;

    public DriverDecorator() {}

    public WebDriver getDriver() {
        return driver;
    }

    public File getDownloadDirectory() {
        return downloadDirectory;
    }

    public UUID getUuid() {
        return uuid;
    }

    public DriverDecorator setDriver(WebDriver driver) {
        this.driver = driver;
        return this;
    }

    public DriverDecorator setDownloadDirectory(File downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
        return this;
    }

    public DriverDecorator setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }
}
