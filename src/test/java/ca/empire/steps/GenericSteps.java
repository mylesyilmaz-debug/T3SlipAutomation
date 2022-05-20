package ca.empire.steps;

import io.cucumber.java.en.Given;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common steps that can be used during any test. Steps should only go here if they are generic and
 * involve interaction with core components of the repo (i.e. the WebDriver) and are not specific to
 * any site or project.
 */
public class GenericSteps extends StepDefinition {
    private static final Logger logger = LogManager.getLogger(StepDefinition.class);

    public GenericSteps() {
        super();
    }

    @Given("I navigate to {string}")
    public void navigateTo(String url) {
        logger.traceEntry(() -> url);
        driver.navigate().to(url);
        logger.traceExit();
    }
}
