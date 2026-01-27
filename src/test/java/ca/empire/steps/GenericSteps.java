package ca.empire.steps;

import ca.empire.setup.Hooks;
import ca.empire.setup.configuration.Config;
import io.cucumber.java.en.Given;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class GenericSteps extends StepDefinition {

    private static final Logger logger = LogManager.getLogger(GenericSteps.class);

    public GenericSteps() {
        super();
    }

    @Given("I navigate to {string}")
    public void navigateTo(String url) throws InterruptedException {
        WebDriver driver = Hooks.getDriver();
        String finalUrl = "https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx?trid=logonx&tridsfx=&cssover=&srvid=DEVPCX01&svrlst=&logoff=1T";

        // 1. Navigate to the PCX URL
        driver.get(finalUrl);
        logger.info("Navigated to PageCenterX URL.");

        // 2. Setup a wait for the login elements
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            // 3. Find and fill the login form (assuming standard PCX field names)
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid"))).sendKeys(Config.getPcxUsername());
            driver.findElement(By.id("pwidtemp")).sendKeys(Config.getPcxPassword());
            Thread.sleep(5000);
            driver.findElement(By.id("LogonID")).click();

            logger.info("Credentials entered into PCX form.");
            logger.info("Logon button clicked.");

        } catch (Exception e) {
            logger.warn("Login form not detected. You might have been logged in automatically via SSO: " + e.getMessage());
        }

        // 4. Pause to ensure the Search Screen is fully loaded for the next step
        Thread.sleep(5000);
    }
}