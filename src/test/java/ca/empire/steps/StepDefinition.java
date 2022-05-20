package ca.empire.steps;

import ca.empire.setup.Hooks;
import ca.empire.util.TestEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class StepDefinition {
    protected WebDriver driver;
    protected TestEnvironment testEnvironment;
    protected File downloadDirectory;

    private static final Logger logger = LogManager.getLogger(StepDefinition.class);

    public StepDefinition() {
        logger.traceEntry();
        driver = Hooks.getDriver();
        testEnvironment = Hooks.getTestEnvironment();
        downloadDirectory = Hooks.getDownloadDirectory();
        logger.traceExit();
    }
}
