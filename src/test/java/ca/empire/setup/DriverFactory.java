package ca.empire.setup;

import ca.empire.setup.configuration.models.CapabilityModel;
import ca.empire.setup.configuration.models.PreferenceModel;
import ca.empire.setup.configuration.models.ProfileModel;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Factory for supported web drivers.
 */
public class DriverFactory {
    private static final HashMap<SupportedBrowsers, Boolean> driverSetups = new HashMap<>();
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private enum SupportedBrowsers {chrome, firefox, edge}

    static {
        for (SupportedBrowsers browser : SupportedBrowsers.values())
        {
            driverSetups.put(browser, false);
        }
    }

    /**
     * Creates a WebDriver based on the provided driver profile.
     * @param driverProfile - The driver profile that will be used to create the WebDriver.
     * @param downloadDir - The directory where downloaded files will be placed. Can be null if using a mobile device.
     * @return - The WebDriver
     */
    public static WebDriver createDriver(ProfileModel driverProfile, File downloadDir) {
        String driverFramework = driverProfile.getDriverFramework();

        if (driverFramework == null) {
            throw new NullPointerException("driverFramework must be specified in the ProfileModel.");
        }

        if (driverFramework.equalsIgnoreCase("browserstack")) {
            return createBrowserStackDriver(driverProfile);
        }
        else if (driverFramework.equalsIgnoreCase("appium")) {
            return createAppiumDriver(driverProfile);
        }
        else {
            if (!driverFramework.equalsIgnoreCase("selenium"))
            {
                System.out.println(driverFramework + " is not an expected value. Defaulting to 'selenium'...");
            }

            return createDesktopBrowserDriver(driverProfile, downloadDir);
        }
    }

    /**
     * @param driverProfile - The driver profile that represents a BrowserStack driver.
     * @return - The resulting BrowserStack driver.
     */
    private static WebDriver createBrowserStackDriver(ProfileModel driverProfile) {
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driverProfile - The driver profile that represents an Appium driver.
     * @return - The resulting AppiumDriver.
     */
    private static WebDriver createAppiumDriver(ProfileModel driverProfile) {
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driverProfile - The driver profile that represents a standard desktop browser.
     * @param downloadDir - The directory where downloaded files will be placed.
     * @return - The resulting WebDriver.
     */
    private static WebDriver createDesktopBrowserDriver(ProfileModel driverProfile, File downloadDir) {
        HashMap<String, String> caps = new HashMap<>();

        for (CapabilityModel model : driverProfile.getCapabilities()) {
            caps.put(model.getName().toLowerCase(), model.getValue().toLowerCase());
        }

        String browserName = caps.get("browser.name");

        if (browserName == null)
        {
            throw new NullPointerException("browser.name must not be null for the default selenium ProfileModel...");
        }

        switch (SupportedBrowsers.valueOf(browserName))
        {
            case chrome: return createChromeDriver(driverProfile, downloadDir);
            case firefox: return createFirefoxDriver(driverProfile, downloadDir);
            case edge: return createEdgeDriver(driverProfile, downloadDir);
            default: throw new IllegalArgumentException("");
        }
    }

    /**
     * @param driverProfile - The driver profile that represents a ChromeDriver.
     * @param downloadDir - The directory where downloaded files will be placed.
     * @return - The resulting ChromeDriver.
     */
    private static ChromeDriver createChromeDriver(ProfileModel driverProfile, File downloadDir) {
        HashMap<String, String> caps = new HashMap<>();
        for (CapabilityModel model : driverProfile.getCapabilities()) {
            caps.put(model.getName().toLowerCase(), model.getValue().toLowerCase());
        }

        HashMap<String, Object> prefs = new HashMap<>();
        for (PreferenceModel model : driverProfile.getPreferences()) {
            prefs.put(model.getName().toLowerCase(), model.getValue());
        }

        prefs.put("download.default_directory", downloadDir.getAbsolutePath());

        lock.readLock().lock();
        Boolean isSetup = driverSetups.get(SupportedBrowsers.chrome);
        lock.readLock().unlock();

        if (!isSetup)
        {
            lock.writeLock().lock();
            System.out.println("Attempting to setup ChromeDriver...");
            // Double check that it hasn't already been setup while waiting to acquire the write lock
            if (!driverSetups.get(SupportedBrowsers.chrome)) {
                try {
                    WebDriverManager manager = WebDriverManager.chromedriver();

                    if (caps.containsKey("browser.version")) {
                        manager = manager.browserVersion(caps.get("browser.version"));
                    }

                    manager.setup();
                    driverSetups.put(SupportedBrowsers.chrome, true);
                    System.out.println("ChromeDriver is done setup.");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                System.out.println("ChromeDriver was setup while waiting for write lock.");
            }

            lock.writeLock().unlock();
        }

        ChromeOptions options = new ChromeOptions();

        options.addArguments(driverProfile.getArguments());
        options.setExperimentalOption("prefs", prefs);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        return new ChromeDriver(options);
    }

    /**
     * @param driverProfile - The driver profile that represents a FirefoxDriver.
     * @param downloadDir - The directory where downloaded files will be placed.
     * @return - The resulting FirefoxDriver.
     */
    private static FirefoxDriver createFirefoxDriver(ProfileModel driverProfile, File downloadDir) {
        throw new UnsupportedOperationException("firefox drivers have not been implemented yet.");
    }

    /**
     * @param driverProfile - The driver profile that represents a EdgeDriver.
     * @param downloadDir - The directory where downloaded files will be placed.
     * @return - The resulting EdgeDriver.
     */
    private static EdgeDriver createEdgeDriver(ProfileModel driverProfile, File downloadDir) {
        throw new UnsupportedOperationException("edge drivers have not been implemented yet.");
    }
}
