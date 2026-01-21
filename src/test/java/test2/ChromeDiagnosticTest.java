package test2;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.Collections;

public class ChromeDiagnosticTest {

    public static void main(String[] args) {
        ChromeOptions options = new ChromeOptions();

        // 1. Point to your fresh Chrome installation
        options.setBinary("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");

        // 2. Use a brand new temp directory to avoid profile picker screens
        options.addArguments("--user-data-dir=C:\\Temp\\SeleniumWork");

        // 3. STEALTH FLAGS: This hides the "Automated" flag from the corporate server
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.addArguments("--disable-blink-features=AutomationControlled");

        // 4. Standard Corporate Bypass
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        try {
            WebDriver driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            System.out.println("Navigating to Corporate Portal...");
            driver.get("https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx");

            // 5. Wait for the link
            WebElement pcxLink = wait.until(
                    ExpectedConditions.elementToBeClickable(By.linkText("PageCenterX"))
            );

            //pcxLink.click();

            // 7. Success Check
            System.out.println("Wait for Dashboard to load...");
            wait.until(ExpectedConditions.titleContains("PageCenterX"));

            System.out.println("Current Page Title: " + driver.getTitle());

            // Now you can do your data validation!
            // Example: String data = driver.findElement(By.id("some_data")).getText();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
