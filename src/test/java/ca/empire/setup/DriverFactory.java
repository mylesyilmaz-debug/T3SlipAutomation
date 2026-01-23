package ca.empire.setup;

import ca.empire.exceptions.AutomationException;
import ca.empire.setup.configuration.models.DriverOptions;
import ca.empire.util.FileOperations;
import ca.empire.util.TestEnvironment;
import com.lambdatest.tunnel.Tunnel;
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

import java.io.File;
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
    private static final ReentrantReadWriteLock tunnelSetupLock = new ReentrantReadWriteLock(true);
    private static final Logger logger = LogManager.getLogger(DriverFactory.class);

    private static Tunnel tunnel = null;

    static {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    logger.traceEntry();
                                    if (tunnel != null) {
                                        try {
                                            logger.info("Attempting to shutdown tunnel...");
                                            tunnel.stopTunnel();
                                        } catch (Exception e) {
                                            logger.error(e);
                                            e.printStackTrace();
                                        }
                                    }
                                    logger.traceExit();
                                }));
    }

    private enum SupportedDriver {
        chrome,
        firefox,
        edge,
        browserstack,
        lambda,
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

        SupportedDriver supportedDriver = SupportedDriver.valueOf(driverName);

        switch (supportedDriver) {
            case browserstack:
                driverDecorator = createBrowserStackDriver(driverOptions);
                break;
            case lambda:
                driverDecorator = createLambdaDriver(driverOptions);
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
        String driverUrl = environment.get("BROWSERSTACK_AUTOMATE_URL");

        if (driverUrl == null || driverUrl.isEmpty()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(
                            "BROWSERSTACK_AUTOMATE_URL is either null, empty or missing from the environment.");
            logger.error(e);
            throw e;
        }

        DriverDecorator decorator =
                createRemoteDriver(driverUrl, new DesiredCapabilities(driverOptions.capabilities));

        logger.traceExit(decorator);
        return decorator;
    }

    /**
     * @param driverOptions - The driver profile that represents a LambdaTest driver.
     * @return - The resulting LambdaTest driver.
     */
    private static DriverDecorator createLambdaDriver(DriverOptions driverOptions) {
        logger.traceEntry(() -> driverOptions);

        TestEnvironment environment = Hooks.getTestEnvironment();
        String driverUrl = environment.get("LAMBDA_TEST_AUTOMATION_URL");

        if (driverUrl == null || driverUrl.isEmpty()) {
            IllegalArgumentException e =
                    new IllegalArgumentException(
                            "LAMBDA_TEST_AUTOMATION_URL is either null, empty or missing from the environment.");
            logger.error(e);
            throw e;
        }

        if (driverOptions.capabilities.containsKey("tunnel")
                && driverOptions.capabilities.get("tunnel").toString().equalsIgnoreCase("true")) {
            String tunnelUser = environment.get("LAMBDA_TUNNEL_USER");
            String tunnelKey = environment.get("LAMBDA_TUNNEL_KEY");

            if (tunnelUser == null || tunnelUser.isEmpty()) {
                IllegalArgumentException e =
                        new IllegalArgumentException(
                                "LAMBDA_TUNNEL_USER is either null, empty or missing from the environment.");
                logger.error(e);
                throw e;
            }

            if (tunnelKey == null || tunnelKey.isEmpty()) {
                IllegalArgumentException e =
                        new IllegalArgumentException(
                                "LAMBDA_TUNNEL_KEY is either null, empty or missing from the environment.");
                logger.error(e);
                throw e;
            }

            tryTunnelSetup(tunnelUser, tunnelKey);
        }

        DriverDecorator decorator =
                createRemoteDriver(driverUrl, new DesiredCapabilities(driverOptions.capabilities));

        logger.traceExit(decorator);
        return decorator;
    }

    private static DriverDecorator createRemoteDriver(String driverUrl, DesiredCapabilities caps) {
        logger.traceEntry(() -> "***", () -> caps);

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());
        caps.setCapability("build", "build-" + factoryStartTime);

        // We want to have the final say on what the runner name will be
        Scenario scenario = Hooks.getScenario();
        String threadName =
                scenario.getName().toLowerCase().replaceAll("\\s", "-")
                        + "-line-"
                        + scenario.getLine()
                        + "-uuid-"
                        + driverDecorator.getUuid();
        caps.setCapability("name", threadName);
        logger.debug("Test name: {}", threadName);

        try {
            driverDecorator.setDriver(new RemoteWebDriver(new URL(driverUrl), caps));
            logger.traceExit(driverDecorator);
            return driverDecorator;
        } catch (MalformedURLException e) {
            logger.error("Error occurred when trying to connect to remote driver: {}", e.getMessage());
        }

        logger.traceExit(null);
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
            logger.error("Error occurred when trying to connect to remote appium driver: {}", e.getMessage());
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

        // Skip the trySetupDriver call entirely because it tries to use the internet
        // trySetupDriver(SupportedDriver.edge, driverOptions.driverVersion, driverOptions.browserVersion);

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());
        EdgeOptions options = new EdgeOptions();

        // Add arguments from YAML (like --inprivate and --start-maximized)
        if (driverOptions.arguments != null) {
            options.addArguments(driverOptions.arguments);
        }

        try {
            driverDecorator.setDownloadDirectory(
                    FileOperations.generateTempDownloadDirectory(driverDecorator.getUuid()));

            Map<String, Object> experimentalOptions =
                    tryAddChromiumDownloadDirectory(
                            driverOptions.experimentalOptions,
                            driverDecorator.getDownloadDirectory().getAbsolutePath());
            experimentalOptions.forEach(options::setExperimentalOption);
        } catch (IOException e) {
            logger.error("Failed to setup download directory: {}", e.getMessage());
        }

        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        // This will now use the "webdriver.edge.driver" path you set in the frameworkConfig.yaml
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
     * Attempts to set up the tunnel for LambdaTest.
     *
     * @param username The username for the LambdaTest account
     * @param key The key for the LambdaTest account
     */
    private static void tryTunnelSetup(String username, String key) {
        logger.traceEntry(() -> username, () -> key);

        tunnelSetupLock.readLock().lock();
        boolean isSetup = tunnel != null;
        tunnelSetupLock.readLock().unlock();

        if (!isSetup) {
            tunnelSetupLock.writeLock().lock();
            logger.info("Attempting to setup tunnel...");

            if (tunnel == null) {
                try {
                    tunnel = new Tunnel();

                    HashMap<String, String> options = new HashMap<>();
                    options.put("user", username);
                    options.put("key", key);

                    tunnel.start(options);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                logger.info("Tunnel was successfully setup.");
            } else {
                logger.info("Tunnel was setup while waiting for write lock.");
            }
            tunnelSetupLock.writeLock().unlock();
        }
    }

    /**
     * Attempts to set up the driver using DriverManager. Will only do so if it hasn't already been
     * set up.
     *
     * @param supportedDriver - The driver that we will be setting up.
     * @param driverVersion - The desired version.
     */
    private static void trySetupDriver(
            SupportedDriver supportedDriver, String driverVersion, String browserVersion) {

        if (supportedDriver == SupportedDriver.edge) return;
        logger.traceEntry(() -> supportedDriver, () -> driverVersion, () -> browserVersion);

        driverSetupLock.readLock().lock();
        boolean isSetup = driverSetups.get(supportedDriver);
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
