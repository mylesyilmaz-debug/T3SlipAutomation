package ca.empire.setup;

import ca.empire.setup.configuration.models.Mapping;
import ca.empire.setup.configuration.models.Driver;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.Scenario;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
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

    private enum SupportedBrowsers {
        chrome,
        firefox,
        edge
    }

    private static final String factoryStartTime =
            new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss").format(new Date());

    static {
        for (SupportedBrowsers browser : SupportedBrowsers.values()) {
            driverSetups.put(browser, false);
        }
    }

    /**
     * Creates a WebDriver based on the provided driver profile.
     *
     * @param driver - The driver profile that will be used to create the WebDriver.
     * @return - The WebDriver
     */
    public static DriverDecorator createDriver(Driver driver) {
        String driverFramework = driver.framework;

        if (driverFramework == null) {
            throw new NullPointerException("driverFramework must be specified in the DriverModel.");
        }

        if (driverFramework.equalsIgnoreCase("browserstack")) {
            return createBrowserStackDriver(driver);
        } else if (driverFramework.equalsIgnoreCase("appium")) {
            return createAppiumDriver(driver);
        } else {
            if (!driverFramework.equalsIgnoreCase("selenium")) {
                System.out.println(
                        driverFramework + " is not an expected value. Defaulting to 'selenium'...");
            }

            return createDesktopBrowserDriver(driver);
        }
    }

    /**
     * @param driver - The driver profile that represents a BrowserStack driver.
     * @return - The resulting BrowserStack driver.
     */
    private static DriverDecorator createBrowserStackDriver(Driver driver) {
        TestEnvironment environment = Hooks.getTestEnvironment();
        String browserStackUrl = environment.get("BROWSERSTACK_AUTOMATE_URL");

        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability("build", "build-" + factoryStartTime);

        for (Mapping capability : driver.capabilities) {
            caps.setCapability(capability.key.toLowerCase(), capability.key.toLowerCase());
        }

        DriverDecorator driverDecorator = new DriverDecorator().setUuid(UUID.randomUUID());

        // We want to have the final say on what the name will be
        Scenario scenario = Hooks.getScenario();
        String threadName =
                scenario.getName().toLowerCase().replaceAll("\\s", "-")
                        + "-line-"
                        + scenario.getLine()
                        + "-uuid-"
                        + driverDecorator.getUuid();
        caps.setCapability("name", threadName);

        if (browserStackUrl == null || browserStackUrl.isEmpty()) {
            throw new IllegalArgumentException(
                    "BROWSERSTACK_AUTOMATE_URL is either null or empty.");
        }

        try {
            return driverDecorator.setDriver(new RemoteWebDriver(new URL(browserStackUrl), caps));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param driver - The driver profile that represents an Appium driver.
     * @return - The resulting AppiumDriver.
     */
    private static DriverDecorator createAppiumDriver(Driver driver) {
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
            throw new NullPointerException(
                    "browser.name must not be null for the default selenium DriverModel...");
        }

        switch (SupportedBrowsers.valueOf(browserName)) {
            case chrome:
                return createChromeDriver(driver);
            case firefox:
                return createFirefoxDriver(driver);
            case edge:
                return createEdgeDriver(driver);
            default:
                throw new IllegalArgumentException("");
        }
    }

    /**
     * @param driverProfile - The driver profile that represents a ChromeDriver.
     * @return - The resulting ChromeDriver.
     */
    private static DriverDecorator createChromeDriver(Driver driverProfile) {
        HashMap<String, String> caps = new HashMap<>();
        for (Mapping capability : driverProfile.capabilities) {
            caps.put(capability.key.toLowerCase(), capability.value.toLowerCase());
        }

        HashMap<String, Object> prefs = new HashMap<>();
        for (Mapping preference : driverProfile.preferences) {
            prefs.put(preference.key, preference.value);
        }

        DriverDecorator driverDecorator =
                new DriverDecorator()
                        .setUuid(UUID.randomUUID());

        try {
            driverDecorator.setDownloadDirectory(
                    generateTempDownloadDirectory(driverDecorator.getUuid()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        prefs.put("download.default_directory", driverDecorator.getDownloadDirectory().getAbsolutePath());

        lock.readLock().lock();
        Boolean isSetup = driverSetups.get(SupportedBrowsers.chrome);
        lock.readLock().unlock();

        if (!isSetup) {
            lock.writeLock().lock();
            System.out.println("Attempting to setup ChromeDriver...");
            // Double check that it hasn't already been setup while waiting to acquire the write
            // lock
            if (!driverSetups.get(SupportedBrowsers.chrome)) {
                try {
                    WebDriverManager manager = WebDriverManager.chromedriver();

                    if (caps.containsKey("browser.version")) {
                        manager = manager.browserVersion(caps.get("browser.version"));
                    }

                    manager.setup();
                    driverSetups.put(SupportedBrowsers.chrome, true);
                    System.out.println("ChromeDriver is done setup.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("ChromeDriver was setup while waiting for write lock.");
            }

            lock.writeLock().unlock();
        }

        ChromeOptions options = new ChromeOptions();

        options.addArguments(driverProfile.arguments);
        options.setExperimentalOption("prefs", prefs);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        return driverDecorator.setDriver(new ChromeDriver(options));
    }

    /**
     * @param driver - The driver profile that represents a FirefoxDriver.
     * @return - The resulting FirefoxDriver.
     */
    private static DriverDecorator createFirefoxDriver(Driver driver) {
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driver - The driver profile that represents a EdgeDriver.
     * @return - The resulting EdgeDriver.
     */
    private static DriverDecorator createEdgeDriver(Driver driver) {
        throw new UnsupportedOperationException("edge drivers have not been implemented yet.");
    }

    /*
    ====================================================================================================================
                                                        Util Methods
    ====================================================================================================================
     */

    private static File generateTempDownloadDirectory(UUID uuid) throws IOException {
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

        return directory;
    }
}
