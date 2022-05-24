package ca.empire.setup;

import ca.empire.setup.configuration.models.Mapping;
import ca.empire.setup.configuration.models.Driver;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.Scenario;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Factory for supported web drivers. */
public class DriverFactory {
    private static final HashMap<SupportedBrowsers, Boolean> driverSetups = new HashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private static final Logger logger = LogManager.getLogger(DriverFactory.class);

    private enum SupportedBrowsers {
        chrome,
        firefox,
        edge
    }

    private static final String factoryStartTime =
            new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());

    static {
        logger.traceEntry();

        for (SupportedBrowsers browser : SupportedBrowsers.values()) {
            driverSetups.put(browser, false);
        }

        logger.traceExit();
    }

    /**
     * Creates a WebDriver based on the provided driver profile.
     *
     * @param driver - The driver profile that will be used to create the WebDriver.
     * @return - The WebDriver
     */
    public static DriverDecorator createDriver(Driver driver) {
        logger.traceEntry(() -> driver);

        String driverFramework = driver.framework;
        DriverDecorator driverDecorator;

        if (driverFramework == null) {
            NullPointerException e =
                    new NullPointerException(
                            "driverFramework must be specified in the DriverModel.");
            logger.error(e);
            throw e;
        }

        if (driverFramework.equalsIgnoreCase("browserstack")) {
            driverDecorator = createBrowserStackDriver(driver);
        } else if (driverFramework.equalsIgnoreCase("appium")) {
            driverDecorator = createAppiumDriver(driver);
        } else {
            if (!driverFramework.equalsIgnoreCase("selenium")) {
                logger.warn(
                        driverFramework + " is not an expected value. Defaulting to 'selenium'...");
            }

            driverDecorator = createDesktopBrowserDriver(driver);
        }

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driver - The driver profile that represents a BrowserStack driver.
     * @return - The resulting BrowserStack driver.
     */
    private static DriverDecorator createBrowserStackDriver(Driver driver) {
        logger.traceEntry(() -> driver);

        TestEnvironment environment = Hooks.getTestEnvironment();
        String browserStackUrl = environment.get("BROWSERSTACK_AUTOMATE_URL");
        DesiredCapabilities caps = new DesiredCapabilities();
        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        caps.setCapability("build", "build-" + factoryStartTime);

        for (Mapping capability : driver.capabilities) {
            logger.debug("Setting capability - " + capability.key + ":" + capability.value);
            caps.setCapability(capability.key, capability.value);
        }

        // We want to have the final say on what the name will be
        Scenario scenario = Hooks.getScenario();
        String threadName =
                scenario.getName().toLowerCase().replaceAll("\\s", "-")
                        + "-line-"
                        + scenario.getLine()
                        + "-uuid-"
                        + driverDecorator.getUuid();
        caps.setCapability("name", threadName);
        logger.info("BrowserStack test name: {}", threadName);

        if (browserStackUrl == null || browserStackUrl.isEmpty()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(
                            "BROWSERSTACK_AUTOMATE_URL is either null or empty.");
            logger.error(e);
            throw e;
        }

        try {
            driverDecorator.setDriver(new RemoteWebDriver(new URL(browserStackUrl), caps));
            logger.traceExit(driverDecorator);
            return driverDecorator;
        } catch (MalformedURLException e) {
            logger.error(e);
        }

        logger.traceExit();
        return null;
    }

    /**
     * @param driver - The driver profile that represents an Appium driver.
     * @return - The resulting AppiumDriver.
     */
    private static DriverDecorator createAppiumDriver(Driver driver) {
        logger.traceEntry(() -> driver);
        /* ... */
        logger.traceExit();
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driver - The driver profile that represents a standard desktop browser.
     * @return - The resulting WebDriver.
     */
    private static DriverDecorator createDesktopBrowserDriver(Driver driver) {
        HashMap<String, String> caps = new HashMap<>();

        for (Mapping capability : driver.capabilities) {
            caps.put(capability.key.toLowerCase(), capability.value.toLowerCase());
        }

        String browserName = caps.get("browser.name");

        if (browserName == null) {
            NullPointerException e =
                    new NullPointerException(
                            "browser.name must not be null for the default selenium DriverModel...");
            logger.error(e);
            throw e;
        }

        DriverDecorator driverDecorator;

        switch (SupportedBrowsers.valueOf(browserName)) {
            case chrome:
                driverDecorator = createChromeDriver(driver);
                break;
            case firefox:
                driverDecorator = createFirefoxDriver(driver);
                break;
            case edge:
                driverDecorator = createEdgeDriver(driver);
                break;
            default:
                IllegalArgumentException e =
                        new IllegalArgumentException(browserName + " is not a valid option");
                logger.error(e);
                throw e;
        }

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverProfile - The driver profile that represents a ChromeDriver.
     * @return - The resulting ChromeDriver.
     */
    private static DriverDecorator createChromeDriver(Driver driverProfile) {
        logger.traceEntry(() -> driverProfile);

        final SupportedBrowsers browserName = SupportedBrowsers.chrome;
        HashMap<String, String> caps = new HashMap<>();
        HashMap<String, Object> prefs = new HashMap<>();

        for (Mapping capability : driverProfile.capabilities) {
            logger.debug("Setting capability - " + capability.key + ":" + capability.value);
            caps.put(capability.key.toLowerCase(), capability.value.toLowerCase());
        }

        for (Mapping preference : driverProfile.preferences) {
            logger.debug("Setting preference - " + preference.key + ":" + preference.value);
            prefs.put(preference.key, preference.value);
        }

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        try {
            driverDecorator.setDownloadDirectory(
                    generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        prefs.put(
                "download.default_directory",
                driverDecorator.getDownloadDirectory().getAbsolutePath());

        lock.readLock().lock();
        Boolean isSetup = driverSetups.get(browserName);
        lock.readLock().unlock();

        if (!isSetup) {
            lock.writeLock().lock();
            logger.info("Attempting to setup ChromeDriver...");
            // Double check that it hasn't already been setup while waiting to acquire the write
            // lock
            if (!driverSetups.get(browserName)) {
                try {
                    WebDriverManager manager = WebDriverManager.chromedriver();

                    if (caps.containsKey("browser.version")) {
                        manager = manager.browserVersion(caps.get("browser.version"));
                    }

                    manager.setup();
                    driverSetups.put(browserName, true);
                    logger.info("ChromeDriver is done setup.");
                } catch (Exception e) {
                    logger.error(e);
                }
            } else {
                logger.info("ChromeDriver was setup while waiting for write lock.");
            }

            lock.writeLock().unlock();
        }

        ChromeOptions options =
                new ChromeOptions()
                        .addArguments(driverProfile.arguments)
                        .setExperimentalOption("prefs", prefs);

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        driverDecorator.setDriver(new ChromeDriver(options));

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverProfile - The driver profile that represents a FirefoxDriver.
     * @return - The resulting FirefoxDriver.
     */
    private static DriverDecorator createFirefoxDriver(Driver driverProfile) {
        logger.traceEntry(() -> driverProfile);
        /* ... */
        logger.traceExit();
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driverProfile - The driver profile that represents a EdgeDriver.
     * @return - The resulting EdgeDriver.
     */
    private static DriverDecorator createEdgeDriver(Driver driverProfile) {
        logger.traceEntry(() -> driverProfile);

        HashMap<String, String> caps = new HashMap<>();
        HashMap<String, Object> prefs = new HashMap<>();
        final SupportedBrowsers browserName = SupportedBrowsers.edge;

        for (Mapping capability : driverProfile.capabilities) {
            logger.debug("Setting capability - " + capability.key + ":" + capability.value);
            caps.put(capability.key.toLowerCase(), capability.value.toLowerCase());
        }

        for (Mapping preference : driverProfile.preferences) {
            logger.debug("Setting preference - " + preference.key + ":" + preference.value);
            prefs.put(preference.key, preference.value);
        }

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        try {
            driverDecorator.setDownloadDirectory(
                    generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        prefs.put(
                "download.default_directory",
                driverDecorator.getDownloadDirectory().getAbsolutePath());

        lock.readLock().lock();
        Boolean isSetup = driverSetups.get(browserName);
        lock.readLock().unlock();

        if (!isSetup) {
            lock.writeLock().lock();
            logger.info("Attempting to setup ChromeDriver...");
            // Double check that it hasn't already been setup while waiting to acquire the write
            // lock
            if (!driverSetups.get(browserName)) {
                try {
                    WebDriverManager manager = WebDriverManager.chromedriver();

                    if (caps.containsKey("browser.version")) {
                        manager = manager.browserVersion(caps.get("browser.version"));
                    }

                    manager.setup();
                    driverSetups.put(browserName, true);
                    logger.info("ChromeDriver is done setup.");
                } catch (Exception e) {
                    logger.error(e);
                }
            } else {
                logger.info("ChromeDriver was setup while waiting for write lock.");
            }

            lock.writeLock().unlock();
        }

        EdgeOptions options =
                new EdgeOptions()
                        .addArguments(driverProfile.arguments)
                        .setExperimentalOption("prefs", prefs);

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        driverDecorator.setDriver(new EdgeDriver(options));

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /*
    ====================================================================================================================
                                                        Util Methods
    ====================================================================================================================
     */

    private static File generateTempDownloadDirectory(UUID uuid) throws IOException {
        logger.traceEntry(() -> uuid);
        String separator = File.separator;
        String basePath =
                System.getProperty("user.dir")
                        + separator
                        + ".temp"
                        + separator
                        + "downloads"
                        + separator;

        File directory = new File(basePath + uuid.toString());

        if (!directory.mkdirs()) {
            throw new IOException(
                    "Unable to create download directory " + directory.getAbsolutePath());
        }

        logger.traceExit(directory);
        return directory;
    }
}
