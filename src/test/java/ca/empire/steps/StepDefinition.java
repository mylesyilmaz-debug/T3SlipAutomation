package ca.empire.steps;

import ca.empire.setup.Hooks;
import ca.empire.util.TestEnvironment;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class StepDefinition {
    protected WebDriver driver;
    protected TestEnvironment testEnvironment;
    protected File downloadDirectory;

    public StepDefinition() {
        driver = Hooks.getDriver();
        testEnvironment = Hooks.getTestEnvironment();
        downloadDirectory = Hooks.getDownloadDirectory();
    }
}
