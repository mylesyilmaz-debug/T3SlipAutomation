package ca.empire.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "ca.empire.steps",
                "ca.empire.hooks"
        },
        tags = "@network",
        plugin = {
                "pretty",
                "html:target/t3-story1-report.html"
        },
        monochrome = true
)
public class T3Story1Runner {
}
