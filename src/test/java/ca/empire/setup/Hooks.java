package ca.empire.setup;

import ca.empire.setup.configuration.Config;
import ca.empire.setup.configuration.models.DownloadManagement;
import ca.empire.setup.configuration.models.Environment;
import ca.empire.util.FileOperations;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

public class Hooks {
    private static final ThreadLocal<Scenario> scenarios = new ThreadLocal<>();
    private static final ThreadLocal<TestEnvironment> testEnvironments = new ThreadLocal<>();
    private static final ThreadLocal<DriverDecorator> driverDecorators = new ThreadLocal<>();

    private static final Config config;
    private static final Logger logger = LogManager.getLogger(Hooks.class);

    static {
        logger.traceEntry();

        // This should help clean up the logs.
        java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(Level.WARNING);

        String configFilepath = System.getProperty("config.filepath", "");
        String configProfile = System.getProperty("config.profile", "");

        logger.info("config.filepath: " + configFilepath);
        logger.info("config.profile: " + configProfile);

        if (configFilepath.isEmpty()) {
            logger.fatal("config.filepath was empty.");
            throw new IllegalArgumentException();
        }

        try {
            config = new Config(configFilepath, configProfile);
        } catch (Exception e) {
            logger.fatal("Exception occurred when loading config:", e);
            throw new RuntimeException(e.getCause());
        }

        DownloadManagement.DeletionCondition deletionCondition =
                config.getConfigurationModel().downloadManagement.deletionCondition;

        if (deletionCondition == DownloadManagement.DeletionCondition.afterAll
                || deletionCondition == DownloadManagement.DeletionCondition.afterEach) {
            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(
                                    () -> {
                                        logger.traceEntry();
                                        FileOperations.deleteDir(
                                                new File(
                                                        System.getProperty("user.dir") + "/.temp"));
                                        logger.traceExit();
                                        LogManager.shutdown(true);
                                    }));
        }

        logger.traceExit();
    }

    public Hooks() {
        logger.traceEntry();
        logger.traceExit();
    }

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
        logger.traceEntry();

        Environment environment = getConfig().getConfigurationModel().environment;
        String envFilepath = environment.filepath;
        TestEnvironment testEnvironment;

        if (envFilepath != null) {
            testEnvironment = new TestEnvironment(new File(envFilepath), false);
        } else {
            testEnvironment = new TestEnvironment(true);
        }

        testEnvironments.set(testEnvironment);
        logger.traceExit();
    }

    /**
     * Sets the scenario of this test
     *
     * @param scenario - The cucumber scenario for this test
     */
    @Before(order = 1)
    public void setScenario(Scenario scenario) {
        logger.traceEntry();
        scenarios.set(scenario);
        logger.traceExit();
    }

    /** Creates the driver that will be used for this test if it is not an API test */
    @Before(value = "not @api", order = 2)
    public void createDriver() {
        logger.traceEntry();
        setDriverDecorator(DriverFactory.createDriver(config.getProfile().driverOptions));

        Scenario scenario = getScenario();
        logger.info(
                "[Thread {} ({})] Running -> [Scenario: {} ({}:{})]",
                Thread.currentThread().getId(),
                driverDecorators.get().getUuid(),
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getLine());

        logger.traceExit();
    }

    /**
     * API tests still need access to a download directory in case they need to save a file for
     * later use. This will create said download directory and the required DriverDecorator to store
     * it.
     *
     * @throws IOException - If the download directory could not be created.
     */
    @Before(value = "@api", order = 3)
    public void createApiDownloadDirectory() throws IOException {
        logger.traceEntry();
        UUID uuid = UUID.randomUUID();
        DriverDecorator decorator =
                new DriverDecorator()
                        .setUuid(uuid)
                        .setDownloadDirectory(FileOperations.generateTempDownloadDirectory(uuid));
        setDriverDecorator(decorator);
        logger.traceExit();
    }

    /* Conditional Hooks */

    /*
    ====================================================================================================================
                                                        After Hooks
    ====================================================================================================================
     */

    /* Standard Hooks */

    /**
     * Takes a screenshot if the scenario did not pass.
     *
     * @param scenario - The test scenario
     */
    @After(order = 4)
    public void screenCapture(Scenario scenario) {
        logger.traceEntry();

        if (getDriver() == null) {
            logger.traceExit("Driver is null");
            return;
        } else if (scenario.getStatus() == Status.PASSED) {
            logger.traceExit("Scenario has passed");
            return;
        }

        TakesScreenshot screenshotDriver;

        try {
            screenshotDriver = (TakesScreenshot) getDriver();
        } catch (ClassCastException e) {
            logger.traceExit("Driver is unable to take screenshots");
            return;
        }

        String filename = scenario.getId() + ".png";
        File screenshotFile = screenshotDriver.getScreenshotAs(OutputType.FILE);

        // Save screenshot locally
        try {
            File savedFile =
                    new File("./build/testResults/" + scenario.getStatus() + "/" + filename);
            FileUtils.copyFile(screenshotFile, savedFile);
        } catch (IOException e) {
            logger.warn(
                    "An exception occurred while trying to save the screenshot: {}",
                    e.getMessage());
        }

        // Attach screenshot to the scenario
        try {
            byte[] screenshotBytes = Files.readAllBytes(screenshotFile.toPath());
            scenario.attach(screenshotBytes, "image/png", filename);
        } catch (IOException e) {
            logger.warn(
                    "An exception occurred while trying to attach the screenshot: {}",
                    e.getMessage());
        }

        logger.traceExit();
    }

    /**
     * marks the test result in BrowserStack
     *
     * @param scenario - The test scenario
     */
    @After(order = 3)
    public void markBrowserStackTestResult(Scenario scenario) {
        logger.traceEntry();

        JavascriptExecutor jse;
        String jsScript;
        Status testStatus;

        if (!config.getProfile().driverOptions.name.equals("browserstack")) {
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
                            + testStatus
                            + "\""
                            + "}}";
        }

        jse.executeScript(jsScript);
        logger.traceExit();
    }

    /** Attaches downloaded files to the scenario and then deletes them */
    @After(order = 2)
    public void deleteDownloadDirectory() {
        logger.traceEntry();
        File downloadDir = getDownloadDirectory();
        DownloadManagement downloadManagement = config.getConfigurationModel().downloadManagement;

        if (downloadDir == null) {
            logger.info("This driver does not have any download directories associated with it.");
            logger.traceExit();
            return;
        }

        Scenario scenario = getScenario();
        File[] downloadedFiles = downloadDir.listFiles();
        long maxSize = (long) Math.pow(2, 23); // roughly 8MB

        if (downloadedFiles == null) {
            logger.warn("Attempting to retrieve the list of downloaded files returned a null value.");
            logger.traceExit();
            return;
        }

        DownloadManagement.AttachmentCondition condition = downloadManagement.attachmentCondition;
        boolean attachFiles =
                (condition == DownloadManagement.AttachmentCondition.failure && scenario.isFailed())
                        || (condition == DownloadManagement.AttachmentCondition.unsuccessful
                                && scenario.getStatus() != Status.PASSED)
                        || condition == DownloadManagement.AttachmentCondition.always;

        if (attachFiles) {
            for (File downloadedFile : downloadedFiles) {
                long size = downloadedFile.length();

                if (size > maxSize) {
                    double convertedSize = size / Math.pow(2, 20); // convert to MB
                    logger.warn(
                            downloadedFile.getAbsolutePath()
                                    + " exceeds max size ("
                                    + convertedSize
                                    + "MB)");
                } else {
                    try {
                        byte[] fileBytes = Files.readAllBytes(downloadedFile.toPath());
                        String mimeType = Files.probeContentType(downloadedFile.toPath());

                        scenario.attach(fileBytes, mimeType, downloadedFile.getName());
                    } catch (Exception e) {
                        logger.warn(e);
                    }
                }
            }
        }

        if (downloadManagement.deletionCondition
                == DownloadManagement.DeletionCondition.afterEach) {
            FileOperations.deleteDir(downloadDir);
        }
        logger.traceExit();
    }

    /** Clears the HAR file in the proxy */
    @After(order = 1)
    public void clearHarFile() {
        logger.traceEntry();
        /* ... */
        logger.traceExit();
    }

    /** Cleans up all resources created for this thread */
    @After(order = 0)
    public void threadCleanup() {
        logger.traceEntry();

        Scenario scenario = getScenario();
        logger.info(
                "[Thread {} ({})] Finished [Scenario: {} ({}:{})] - {}",
                Thread.currentThread().getId(),
                driverDecorators.get().getUuid(),
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getLine(),
                scenario.getStatus());

        scenarios.remove();
        testEnvironments.remove();

        if (driverDecorators.get() != null) {
            try {
                getDriver().close();
            } catch (Exception e) {
                logger.warn(
                        "An exception occurred while trying to close the driver: {}",
                        e.getMessage());
            }
            getDriver().quit();
            driverDecorators.remove();
        }

        logger.traceExit();
    }

    /* Conditional Hooks */

    /*
    ====================================================================================================================
                                                    Accessors and Modifiers
    ====================================================================================================================
     */

    /**
     * @return the download directory assigned to this test runner.
     */
    public static File getDownloadDirectory() {
        logger.traceEntry();
        File dir = driverDecorators.get().getDownloadDirectory();
        logger.traceExit(dir);
        return dir;
    }

    /**
     * @return the driver assigned to this test runner.
     */
    public static WebDriver getDriver() {
        logger.traceEntry();
        WebDriver driver = driverDecorators.get().getDriver();
        logger.traceExit(driver);
        return driver;
    }

    /**
     * @return the test environment assigned to this test runner.
     */
    public static TestEnvironment getTestEnvironment() {
        logger.traceEntry();
        logger.traceExit(testEnvironments.get());
        return testEnvironments.get();
    }

    /**
     * @return the scenario assigned to this test runner.
     */
    public static Scenario getScenario() {
        logger.traceEntry();
        logger.traceExit(scenarios.get());
        return scenarios.get();
    }

    /**
     * @return the parsed configuration that is used by this program.
     */
    public Config getConfig() {
        logger.traceEntry();
        logger.traceExit(config);
        return config;
    }

    /**
     * Sets the DriverDecorator that this test runner will use.
     *
     * @param driver The driver decorator that this test runner will use.
     */
    private static void setDriverDecorator(DriverDecorator driver) {
        logger.traceEntry(() -> driver);
        driverDecorators.set(driver);
        logger.traceExit();
    }
}
