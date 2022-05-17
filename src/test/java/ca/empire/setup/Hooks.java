package ca.empire.setup;

import ca.empire.setup.configuration.Config;
import ca.empire.setup.configuration.models.EnvironmentModel;
import ca.empire.setup.configuration.models.ProfileModel;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Hooks {
    private static final ThreadLocal<Scenario> scenarios = new ThreadLocal<>();
    private static final ThreadLocal<File> downloadDirectories = new ThreadLocal<>();
    private static final ThreadLocal<TestEnvironment> testEnvironments = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();

    private Config config;

    public Hooks() {}

    /*
    ====================================================================================================================
                                                        Before Hooks
    ====================================================================================================================
     */

    /* Standard Hooks */

    @Before(order = 0)
    public void loadConfig() throws IOException {
        String configFilepath = System.getProperty("config.filepath", null);
        String configProfile = System.getProperty("config.profile", null);

        if (configFilepath == null) {
            throw new NullPointerException();
        } else if (configProfile == null) {
            throw new NullPointerException();
        }

        config = new Config(configFilepath);
        ProfileModel profileModel = null;
        List<ProfileModel> profileModelList = config.getConfigurationModel().getProfiles();

        for (ProfileModel model : profileModelList) {
            if (model.getName().equals(configProfile)) {
                profileModel = model;
                break;
            }
        }

        if (profileModel == null) {
            throw new IllegalArgumentException(
                    configProfile + " is not declared in " + configFilepath);
        }

        config.getConfigurationModel().setProfile(profileModel);
    }

    /**
     * Creates the test environment that will be used for this test. If a .env file is declared in
     * the config file, then that will be used as the starting state for the environment.
     *
     * @throws IOException when there was an issue loading in the provided .env file.
     */
    @Before(order = 1)
    public void loadEnvironment() throws IOException {
        EnvironmentModel environmentModel =
                getConfig().getConfigurationModel().getEnvironmentModel();
        String envFilepath = environmentModel.getFilepath();
        TestEnvironment testEnvironment;

        if (envFilepath != null) {
            testEnvironment = new TestEnvironment(new File(envFilepath), false);
        } else {
            testEnvironment = new TestEnvironment(false);
        }

        testEnvironments.set(testEnvironment);
    }

    /**
     * Sets the scenario of this test
     *
     * @param scenario - The cucumber scenario for this test
     */
    @Before(order = 2)
    public void setScenario(Scenario scenario) {
        scenarios.set(scenario);
    }

    /** Creates the driver that will be used for this test if it is not an API test */
    @Before(value = "not @api", order = 3)
    public void createDriver() {

    }

    /**
     * Creates the download directory that will be used by one of the test runners
     *
     * @throws IOException if we are unable to create the required directories.
     */
    @Before(order = 4)
    public void createDownloadDirectory() throws IOException {
        String separator = File.separator;
        String basePath =
                System.getProperty("user.dir")
                        + separator
                        + ".temp"
                        + separator
                        + "downloads"
                        + separator;

        File directory = new File(basePath + UUID.randomUUID().toString());

        if (!directory.mkdirs()) {
            throw new IOException(
                    "Unable to create download directory " + directory.getAbsolutePath());
        }

        downloadDirectories.set(directory);
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

        if (!config.getConfigurationModel().getProfile().getDriverFramework().equals("browserstack")) {
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
                    FileInputStream fio = new FileInputStream(downloadedFile);
                    byte[] fileBytes = fio.readAllBytes();
                    fio.close();

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
        scenarios.remove();
        downloadDirectories.remove();
        testEnvironments.remove();
        drivers.remove();
    }

    /* Conditional Hooks */

    /*
    ====================================================================================================================
                                                    Accessors and Modifiers
    ====================================================================================================================
     */

    public static File getDownloadDirectory() {
        return downloadDirectories.get();
    }

    public static WebDriver getDriver() {
        return drivers.get();
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
}
