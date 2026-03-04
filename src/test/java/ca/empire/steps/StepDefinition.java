package ca.empire.steps;

import ca.empire.setup.Hooks;
import ca.empire.util.TestEnvironment;
import io.cucumber.java.Before;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.io.File;

public class StepDefinition {

        protected WebDriver driver;

        //@Before
        public void initDriver() {
            driver = Hooks.getDriver(); // ✅ AFTER driver is created
        }
    }

