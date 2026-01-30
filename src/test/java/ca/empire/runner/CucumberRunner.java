package ca.empire.runner;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/feature",
        glue = "ca.empire",
        tags = "@regression or @GCP",
        plugin = {"pretty", "html:target/cucumber-reports.html"}
)
public class CucumberRunner {

   }
