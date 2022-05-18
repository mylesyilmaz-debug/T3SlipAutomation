package ca.empire.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {"src/test/resources/feature"},
        glue = {"ca.empire"},
        plugin = {"com.epam.reportportal.cucumber.ScenarioReporter"},
        monochrome = true)
public class CucumberRunner {}
