package ca.empire.runner;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/feature",
        glue = "ca.empire",
        tags = "@Rejects",
        plugin = {"pretty", "html:btarget/cucumber-reports.html"}
)
public class CucumberRunner {
   //@regression or @GCP or @T3Parser or @RL16Parser or @DB_Validation or
   //@CSVvsDB or @Rollup or @Lookups or @AuditTrail or @Rejects

   }