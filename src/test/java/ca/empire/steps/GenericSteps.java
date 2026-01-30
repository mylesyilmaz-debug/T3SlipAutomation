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
    private void performGoogleLogin(WebDriver driver) throws InterruptedException {
        logger.info("Detected GCP URL. Checking if login is required...");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            // A. Check for Email Field
            // If we are already logged in, this waits 10s then fails, skipping to catch block (which is good)
            WebElement emailField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")));

            logger.info("Google Sign-In detected. Entering email...");
            emailField.sendKeys(Config.getGcpEmail());

            // Click "Next"
            driver.findElement(By.xpath("//span[text()='Next']/ancestor::button")).click();
            Thread.sleep(3000); // Wait for the screen to change

            // --- MANUAL INTERVENTION BLOCK ---
            // If CAPTCHA appears, we pause for 60 seconds to let YOU solve it.
            if (driver.getPageSource().contains("Type the text you hear or see")) {
                logger.warn("⚠️ CAPTCHA DETECTED! Please manually type the characters and click Next within 60 seconds...");
                Thread.sleep(60000);
            }

            // B. Wait for Password Field
            logger.info("Waiting for password field...");
            WebElement passField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")));
            passField.sendKeys(Config.getGcpPassword());

            // Click "Next"
            driver.findElement(By.xpath("//span[text()='Next']/ancestor::button")).click();

            // --- 2FA INTERVENTION BLOCK ---
            logger.warn("⚠️ Pausing 15 seconds for potential 2FA/Phone verification...");
            Thread.sleep(15000);

            // C. Final Verification
            logger.info("Waiting for redirection to Bucket...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[contains(text(), 'Bucket details')]")));
            logger.info("Successfully logged into GCP Storage!");

        } catch (Exception e) {
            logger.info("Google Login skipped. Either already logged in or page structure changed. Message: " + e.getMessage());
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