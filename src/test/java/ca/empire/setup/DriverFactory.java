package ca.empire.setup;

import ca.empire.exceptions.AutomationException;
import ca.empire.setup.configuration.models.DriverOptions;
import ca.empire.util.FileOperations;
import ca.empire.util.TestEnvironment;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.cucumber.java.Scenario;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Factory for supported web drivers. */
public class DriverFactory {
    private static final HashMap<SupportedDriver, Boolean> driverSetups = new HashMap<>();
    private static final ReentrantReadWriteLock driverSetupLock = new ReentrantReadWriteLock(true);
    private static final Logger logger = LogManager.getLogger(DriverFactory.class);

    private enum SupportedDriver {
        chrome,
        firefox,
        edge,
        browserstack,
        appium
    }

    private static final String factoryStartTime =
            new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());

    static {
        logger.traceEntry();

        for (SupportedDriver driver : SupportedDriver.values()) {
            driverSetups.put(driver, false);
        }

        logger.traceExit();
    }

    /**
     * Creates a WebDriver based on the provided driver profile.
     *
     * @param driverOptions - The driver profile that will be used to create the WebDriver.
     * @return - The WebDriver
     */
    public static DriverDecorator createDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);

        String driverName = driverOptions.name;
        DriverDecorator driverDecorator;

        if (driverName == null) {
            NullPointerException e =
                    new NullPointerException("driverName must be specified in the DriverModel.");
            logger.error(e);
            throw e;
        }

        switch (SupportedDriver.valueOf(driverName)) {
            case browserstack:
                driverDecorator = createBrowserStackDriver(driverOptions);
                break;
            case appium:
                driverDecorator = createAppiumDriver(driverOptions);
                break;
            case chrome:
                driverDecorator = createChromeDriver(driverOptions);
                break;
            case edge:
                driverDecorator = createEdgeDriver(driverOptions);
                break;
            case firefox:
                driverDecorator = createFirefoxDriver(driverOptions);
                break;
            default:
                // We really shouldn't get here
                IllegalArgumentException e =
                        new IllegalArgumentException(driverName + " is not a valid option");
                logger.error(e);
                throw e;
        }

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverOptions - The driver profile that represents a BrowserStack driver.
     * @return - The resulting BrowserStack driver.
     */
    private static DriverDecorator createBrowserStackDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);

        TestEnvironment environment = Hooks.getTestEnvironment();
        String browserStackUrl = environment.get("BROWSERSTACK_AUTOMATE_URL");
        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        DesiredCapabilities caps = new DesiredCapabilities(driverOptions.capabilities);
        caps.setCapability("build", "build-" + factoryStartTime);

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
     * @param driverOptions - The driver profile that represents an Appium driver.
     * @return - The resulting AppiumDriver.
     */
    private static DriverDecorator createAppiumDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);

        DesiredCapabilities caps = new DesiredCapabilities(driverOptions.capabilities);
        Platform platformName = caps.getPlatformName();
        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());
        URL appiumUrl;

        try {
            appiumUrl = new URL(Hooks.getTestEnvironment().get("APPIUM_SERVER_URL"));
        } catch (MalformedURLException e) {
            logger.error(e);
            return null;
        }

        switch (platformName) {
            case ANDROID:
                driverDecorator.setDriver(new AndroidDriver(appiumUrl, caps));
                break;
            case IOS:
                driverDecorator.setDriver(new IOSDriver(appiumUrl, caps));
                break;
        }

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverOptions - The driver profile that represents a ChromeDriver.
     * @return - The resulting ChromeDriver.
     */
    private static DriverDecorator createChromeDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);
        trySetupDriver(
                SupportedDriver.chrome, driverOptions.driverVersion, driverOptions.browserVersion);

        ChromeOptions options = new ChromeOptions().addArguments(driverOptions.arguments);
        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        try {
            driverDecorator.setDownloadDirectory(
                    FileOperations.generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        Map<String, Object> experimentalOptions =
                tryAddChromiumDownloadDirectory(
                        driverOptions.experimentalOptions,
                        driverDecorator.getDownloadDirectory().getAbsolutePath());
        experimentalOptions.forEach(options::setExperimentalOption);

        if (driverOptions.capabilities != null) {
            driverOptions.capabilities.forEach(options::setCapability);
        }

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        driverDecorator.setDriver(new ChromeDriver(options));

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverOptions - The driver profile that represents a FirefoxDriver.
     * @return - The resulting FirefoxDriver.
     */
    private static DriverDecorator createFirefoxDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);
        trySetupDriver(
                SupportedDriver.firefox, driverOptions.driverVersion, driverOptions.browserVersion);

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        try {
            driverDecorator.setDownloadDirectory(
                    FileOperations.generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference(
                "browser.download.dir", driverDecorator.getDownloadDirectory().getAbsolutePath());
        driverOptions.preferences.forEach(profile::setPreference);

        FirefoxOptions options =
                new FirefoxOptions().addArguments(driverOptions.arguments).setProfile(profile);

        if (driverOptions.capabilities != null) {
            driverOptions.capabilities.forEach(options::setCapability);
        }

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        driverDecorator.setDriver(new FirefoxDriver(options));

        logger.traceExit(driverDecorator);
        return driverDecorator;
    }

    /**
     * @param driverOptions - The driver profile that represents a EdgeDriver.
     * @return - The resulting EdgeDriver.
     */
    private static DriverDecorator createEdgeDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);
        trySetupDriver(
                SupportedDriver.edge, driverOptions.driverVersion, driverOptions.browserVersion);

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());
        EdgeOptions options = new EdgeOptions().addArguments(driverOptions.arguments);

        try {
            driverDecorator.setDownloadDirectory(
                    FileOperations.generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        Map<String, Object> experimentalOptions =
                tryAddChromiumDownloadDirectory(
                        driverOptions.experimentalOptions,
                        driverDecorator.getDownloadDirectory().getAbsolutePath());
        experimentalOptions.forEach(options::setExperimentalOption);

        if (driverOptions.capabilities != null) {
            driverOptions.capabilities.forEach(options::setCapability);
        }

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

    /**
     * Attempts to set up the driver using DriverManager. Will only do so if it hasn't already been
     * set up.
     *
     * @param supportedDriver - The driver that we will be setting up.
     * @param driverVersion - The desired version.
     */
    private static void trySetupDriver(
            SupportedDriver supportedDriver, String driverVersion, String browserVersion) {
        logger.traceEntry(() -> supportedDriver, () -> driverVersion, () -> browserVersion);

        driverSetupLock.readLock().lock();
        Boolean isSetup = driverSetups.get(supportedDriver);
        driverSetupLock.readLock().unlock();

        if (!isSetup) {
            driverSetupLock.writeLock().lock();
            logger.info("Attempting to setup " + supportedDriver + "...");
            // Double check that it hasn't already been set up while waiting
            if (!driverSetups.get(supportedDriver)) {
                try {
                    WebDriverManager manager;

                    switch (supportedDriver) {
                        case chrome:
                            manager = WebDriverManager.chromedriver();
                            break;
                        case edge:
                            manager = WebDriverManager.edgedriver();
                            break;
                        case firefox:
                            manager = WebDriverManager.firefoxdriver();
                            break;
                        default:
                            IllegalArgumentException e =
                                    new IllegalArgumentException(
                                            supportedDriver + " is not a valid option");
                            logger.error(e);
                            throw e;
                    }

                    if (driverVersion != null) {
                        logger.info("Desired driver version: {}", driverVersion);
                        manager = manager.driverVersion(driverVersion);
                    }

                    if (browserVersion != null) {
                        logger.info("Desired browser version: {}", browserVersion);
                        manager = manager.browserVersion(browserVersion);
                    }

                    manager.setup();
                    driverSetups.put(supportedDriver, true);
                    logger.info(supportedDriver + " is done setup.");
                } catch (Exception e) {
                    logger.error(e);
                }
            } else {
                logger.info(supportedDriver + " was setup while waiting for write lock.");
            }

            driverSetupLock.writeLock().unlock();
        }

        logger.traceExit();
    }

    /**
     * Attempts to add the desired download directory to the experimental options. If a download
     * directory is already declared, then it will not be overwritten.
     *
     * @param experimentalOptions - The experimental options for the Chromium browser.
     * @param downloadPath - The path to the desired download directory.
     * @return - The updated or the same experimental options.
     */
    private static Map<String, Object> tryAddChromiumDownloadDirectory(
            Map<String, Object> experimentalOptions, String downloadPath) {
        logger.traceEntry(() -> experimentalOptions, () -> downloadPath);

        Map<String, Object> copiedOptions = new HashMap<>();
        Map<Object, Object> copiedPrefs = new HashMap<>();

        if (experimentalOptions != null) {
            copiedOptions.putAll(experimentalOptions);
        }

        if (copiedOptions.containsKey("prefs")) {
            if (!(copiedOptions.get("prefs") instanceof HashMap<?, ?>)) {
                AutomationException e = new AutomationException("prefs must be a map.");
                logger.error(e);
                throw e;
            }

            copiedPrefs.putAll((HashMap<?, ?>) copiedOptions.get("prefs"));

            if (!copiedPrefs.containsKey("download.default_directory")) {
                copiedPrefs.put("download.default_directory", downloadPath);
            } else {
                logger.warn(
                        "Manually assigning default download directory might lead to issues if using "
                                + "parallel execution. Use at your own risk.");
                return experimentalOptions;
            }
        } else {
            copiedPrefs.put("download.default_directory", downloadPath);
        }

        copiedOptions.put("prefs", copiedPrefs);
        logger.traceExit(copiedOptions);
        return copiedOptions;
    }
}
