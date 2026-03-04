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
    public void navigateTo(String urlKey) throws InterruptedException {
        WebDriver driver = Hooks.getDriver();
        String targetUrl = "";
        boolean isPcx = false;
        boolean isGcp = false;  // <--- NEW FLAG

        // 1. Traffic Cop Logic: Decide where to go and what to do
        if (urlKey.contains("pcx.url") || urlKey.contains("pcx-url")) {
            // Case A: PageCenterX
            targetUrl = Config.getPcxUrl();
            isPcx = true;
        } else if (urlKey.contains("gcp-bucket-url")) {
            // Case B: Google Cloud
            targetUrl = Config.getGcpUrl();
            isGcp = true;      // <--- MARK AS GCP
        } else {
            // Case C: Raw URL
            targetUrl = urlKey;
            isPcx = false;
        }

        // 2. Perform the Navigation
        logger.info("Navigating to: " + targetUrl);
        driver.get(targetUrl);

        // 3. Conditional Logic: specific login for specific site
        if (isPcx) {
            performPcxLogin(driver);
        } else if (isGcp) {
            performGoogleLogin(driver); // <--- CALL THE NEW HELPER
        }
    }

    /*
     * Handles the Google Cloud login with a PAUSE for manual CAPTCHA solving.
     */
    /*
     * Handles the Google Cloud login by yielding control to the user if a login screen is detected.
     */
    private void performGoogleLogin(WebDriver driver) throws InterruptedException {
        logger.info("Detected GCP URL. Checking if manual login is required...");

        // Wait 3 seconds to let any Google redirects settle
        Thread.sleep(3000);

        // If Google redirects us to their login authentication server...
        if (driver.getCurrentUrl().contains("accounts.google.com")) {
            logger.warn("⚠️ Google Sign-In screen detected!");
            logger.warn("⚠️ YOU HAVE 60 SECONDS TO MANUALLY LOG IN, SOLVE CAPTCHAS, AND APPROVE 2FA...");

            try {
                // Let's try to be helpful and pre-fill the email to save you time
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
                WebElement emailField = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")));
                emailField.sendKeys(Config.getGcpEmail());
                driver.findElement(By.xpath("//span[text()='Next']/ancestor::button")).click();
                logger.info("Auto-filled email successfully. Please handle the password/CAPTCHA manually.");
            } catch (Exception e) {
                logger.info("Could not auto-fill email (You might be on a 'Choose Account' screen). Please proceed entirely manually.");
            }

            // THE MAGIC PAUSE: Wait up to 60 seconds for the URL to change AWAY from the login screen
            WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            try {
                longWait.until(ExpectedConditions.not(ExpectedConditions.urlContains("accounts.google.com")));
                logger.info("✅ Successfully navigated past the Google Login screen!");

                // Wait for the actual bucket UI to render before giving control back to Cucumber
                longWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[contains(text(), 'Bucket details')]")));
            } catch (Exception e) {
                throw new RuntimeException("❌ Timed out waiting 60 seconds for manual Google Login / CAPTCHA solving.");
            }
        } else {
            logger.info("✅ Already logged in! No Google Sign-In screen detected. Proceeding to Bucket.");
        }
    }

    /**
     * Helper method to keep the main logic clean.
     * Only runs when 'isPcx' is true.
     */
    private void performPcxLogin(WebDriver driver) throws InterruptedException {
        logger.info("Detected PCX URL. Attempting auto-login...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid")))
                    .sendKeys(Config.getPcxUsername());

            driver.findElement(By.id("pwidtemp"))
                    .sendKeys(Config.getPcxPassword());

            Thread.sleep(5000); // Stability pause

            driver.findElement(By.id("LogonID")).click();

            logger.info("PCX Credentials entered and Logon clicked.");
            Thread.sleep(5000); // Wait for search screen

        } catch (Exception e) {
            logger.warn("PCX Login skipped. You might be already logged in via SSO: " + e.getMessage());
        }
    }

    @io.cucumber.java.en.And("I wait for {int} seconds")
    public void iWaitForSeconds(int seconds) throws InterruptedException {
        logger.info("Pausing test execution for " + seconds + " seconds...");
        // Convert seconds to milliseconds
        Thread.sleep(seconds * 1000L);
    }




//    @Given("I navigate to {string}")
//    public void navigateTo(String url) throws InterruptedException {
//        WebDriver driver = Hooks.getDriver();
//        //String finalUrl = "https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx?trid=logonx&tridsfx=&cssover=&srvid=DEVPCX01&svrlst=&logoff=1T";
//
//        // 1. Navigate to the PCX URL
//        driver.get(Config.getPcxUrl());
//        logger.info("Navigated to PageCenterX URL.");
//
//        // 2. Setup a wait for the login elements
//        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
//
//        try {
//            // 3. Find and fill the login form (assuming standard PCX field names)
//            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid"))).sendKeys(Config.getPcxUsername());
//            driver.findElement(By.id("pwidtemp")).sendKeys(Config.getPcxPassword());
//            Thread.sleep(5000);
//            driver.findElement(By.id("LogonID")).click();
//
//            logger.info("Credentials entered into PCX form.");
//            logger.info("Logon button clicked.");
//
//        } catch (Exception e) {
//            logger.warn("Login form not detected. You might have been logged in automatically via SSO: " + e.getMessage());
//        }
//
//        // 4. Pause to ensure the Search Screen is fully loaded for the next step
//        Thread.sleep(5000);
//    }
}