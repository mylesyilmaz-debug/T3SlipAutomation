package ca.empire.setup;

import ca.empire.setup.configuration.Config;
import ca.empire.setup.configuration.models.Environment;
import ca.empire.setup.configuration.models.Profile;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

public class Hooks {
    private static final ThreadLocal<Scenario> scenarios = new ThreadLocal<>();
    private static final ThreadLocal<TestEnvironment> testEnvironments = new ThreadLocal<>();
    private static final ThreadLocal<DriverDecorator> driverDecorators = new ThreadLocal<>();

    private static final Config config;

    static {
        // This should help clean up the logs.
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING);

        String configFilepath = System.getProperty("config.filepath", null);
        String configProfile = System.getProperty("config.profile", null);

        if (configFilepath == null) {
            throw new NullPointerException();
        }

        try {
            config = new Config(configFilepath, configProfile);
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }

        Profile profile = null;
        List<Profile> profileList = config.getConfigurationModel().profiles;

        for (Profile model : profileList) {
            if (model.name.equals(configProfile)) {
                profile = model;
                break;
            }
        }

        if (profile == null) {
            throw new IllegalArgumentException(
                    configProfile + " is not declared in " + configFilepath);
        }
    }

    public Hooks() {}

    /*
    ====================================================================================================================
                                                        Before Hooks
    ====================================================================================================================
     */

    /* Standard Hooks */

    /**
     * Creates the test environment that will be used for this test. If a .env file is declared in
     * the config file, then that will be used as the starting state for the environment.
     *
     * @throws IOException when there was an issue loading in the provided .env file.
     */
    @Before(order = 0)
    public void loadEnvironment() throws IOException {
        Environment environment = getConfig().getConfigurationModel().environment;
        String envFilepath = environment.filepath;
        TestEnvironment testEnvironment;

        if (envFilepath != null) {
            testEnvironment = new TestEnvironment(new File(envFilepath), false);
        } else {
            testEnvironment = new TestEnvironment(true);
        }

        testEnvironments.set(testEnvironment);
    }

    /**
     * Sets the scenario of this test
     *
     * @param scenario - The cucumber scenario for this test
     */
    @Before(order = 1)
    public void setScenario(Scenario scenario) {
        scenarios.set(scenario);
    }

    /** Creates the driver that will be used for this test if it is not an API test */
    @Before(value = "not @api", order = 2)
    public void createDriver() {
        setDriverDecorator(DriverFactory.createDriver(config.getProfile().driver));

        Scenario scenario = getScenario();
        System.out.printf(
                "[Thread %d (%s)] Running -> [Scenario: %s (%s:%d)]\n",
                Thread.currentThread().getId(),
                driverDecorators.get().getUuid(),
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getLine());
    }

    /* Conditional Hooks */

    /*
    ====================================================================================================================
                                                        After Hooks
    ====================================================================================================================
     */

    /* Standard Hooks */

    /**
     * Takes a screenshot if the scenario failed.
     *
     * @param scenario - The test scenario
     */
    @After(order = 4)
    public void screenCapture(Scenario scenario) {
        /* ... */
    }

    /**
     * marks the the test result in BrowserStack
     *
     * @param scenario - The test scenario
     */
    @After(order = 3)
    public void markBrowserStackTestResult(Scenario scenario) {
        JavascriptExecutor jse;
        String jsScript;
        Status testStatus;

        if (!config.getProfile().driver.framework.equals("browserstack")) {
            return;
        }

        jse = (JavascriptExecutor) getDriver();
        testStatus = scenario.getStatus();

        if (testStatus.equals(Status.PASSED)) {
            jsScript =
                    "browserstack_executor: {"
                            + "\"action\": \"setSessionStatus\", "
                            + "\"arguments\": {"
                            + "\"status\": \"passed\", "
                            + "\"reason\": "
                            + "\"Test passed.\""
                            + "}}";
        } else if (testStatus.equals(Status.FAILED)) {
            jsScript =
                    "browserstack_executor: {"
                            + "\"action\": \"setSessionStatus\", "
                            + "\"arguments\": {"
                            + "\"status\": \"failed\", "
                            + "\"reason\": "
                            + "\"Test failed.\""
                            + "}}";
        } else {
            jsScript =
                    "browserstack_executor: {"
                            + "\"action\": \"setSessionStatus\", "
                            + "\"arguments\": {"
                            + "\"status\": \"failed\", "
                            + "\"reason\": "
                            + "\"Test ended with status "
                            + testStatus.toString()
                            + "\""
                            + "}}";
        }

        jse.executeScript(jsScript);
    }

    /** Attaches downloaded files to the scenario and then deletes them */
    @After(order = 2)
    public void deleteDownloadDirectory() {
        Scenario scenario = getScenario();
        File downloadDir = getDownloadDirectory();
        File[] downloadedFiles = downloadDir.listFiles();
        long maxSize = (long) Math.pow(2, 23); // roughly 8MB

        for (File downloadedFile : downloadedFiles) {
            long size = downloadedFile.length();

            if (size > maxSize) {
                double convertedSize = size / Math.pow(2, 20); // convert to MB
                System.out.println(
                        downloadedFile.getAbsolutePath()
                                + " exceeds max size ("
                                + convertedSize
                                + "MB)");
            } else {
                try {
                    byte[] fileBytes = Files.readAllBytes(downloadedFile.toPath());

                    scenario.attach(
                            fileBytes, "application/octet-stream", downloadedFile.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!downloadedFile.delete()) {
                System.out.println("Unable to delete " + downloadedFile.getAbsolutePath());
            } else {
                System.out.println("Deleted " + downloadedFile.getAbsolutePath());
            }
        }

        if (!downloadDir.delete()) {
            System.out.println("Unable to delete " + downloadDir.getAbsolutePath());
        } else {
            System.out.println("Deleted " + downloadDir.getAbsolutePath());
        }
    }

    /** Clears the HAR file in the proxy */
    @After(order = 1)
    public void clearHarFile() {
        /* ... */
    }

    /** Cleans up all resources created for this thread */
    @After(order = 0)
    public void threadCleanup() {
        Scenario scenario = getScenario();
        System.out.printf(
                "[Thread %2d] Finished [Scenario: %s (%s:%d)] - %s\n",
                Thread.currentThread().getId(),
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getLine(),
                scenario.getStatus());

        scenarios.remove();
        testEnvironments.remove();

        if (driverDecorators.get() != null) {
            getDriver().close();
            getDriver().quit();
            driverDecorators.remove();
        }
    }

    /* Conditional Hooks */

    /*
    ====================================================================================================================
                                                    Accessors and Modifiers
    ====================================================================================================================
     */

    public static File getDownloadDirectory() {
        return driverDecorators.get().getDownloadDirectory();
    }

    public static WebDriver getDriver() {
        return driverDecorators.get().getDriver();
    }

    public static TestEnvironment getTestEnvironment() {
        return testEnvironments.get();
    }

    public static Scenario getScenario() {
        return scenarios.get();
    }

    public Config getConfig() {
        return config;
    }

    private static void setDriverDecorator(DriverDecorator driver) {
        driverDecorators.set(driver);
    }
}
