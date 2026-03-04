package ca.empire.runner;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/feature",
        glue = {"ca.empire"},
        tags = "@itax",
        plugin = {
                "pretty",
                "html:target/cucumber-report.html"
        }
)
public class DataValidationRunner {
}
