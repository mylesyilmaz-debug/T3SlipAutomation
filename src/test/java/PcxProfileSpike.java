import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Set;

public class PcxProfileSpike {
    // Change only these 2 if needed
    private static final String PCX_URL = "https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx";
    private static final String USER_DATA_DIR = "C:/Users/citmxy/AppData/Local/Google/Chrome/User Data";
    private static final String PROFILE_DIR = "Default"; // if you created a new profile, it might be "Profile 1", "Profile 2", etc.

    public static void main(String[] args) throws Exception {
        System.out.println("Starting PCX profile spike...");

        ChromeOptions options = new ChromeOptions();

        // Use your real Chrome profile
        options.addArguments("--user-data-dir=" + USER_DATA_DIR);
        options.addArguments("--profile-directory=" + PROFILE_DIR);

        // Stability options
        options.addArguments("--start-maximized");
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--remote-allow-origins=*");

        // Keep browser open for debugging (so it doesn't close right away)
        options.setExperimentalOption("detach", true);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            driver.get(PCX_URL);
            System.out.println("Opened URL successfully: " + driver.getCurrentUrl());

            // Wait for the PageCenterX link
            WebElement pageCenterLink = wait.until(
                    ExpectedConditions.elementToBeClickable(By.linkText("PageCenterX"))
            );

            // IMPORTANT: do NOT click (click seems to kill session in your case)
            String href = pageCenterLink.getAttribute("href");
            System.out.println("PageCenterX href = " + href);

            if (href == null || href.trim().isEmpty()) {
                System.out.println("No href found. Trying click + window switch as fallback...");
                String before = driver.getWindowHandle();
                Set<String> oldWindows = driver.getWindowHandles();
                pageCenterLink.click();
                switchToNewWindow(driver, oldWindows, before);
            } else {
                driver.navigate().to(href);
            }

            waitForPageReady(driver);
            System.out.println("Now at: " + driver.getCurrentUrl());

            System.out.println("✅ If you see the PageCenterX main page, step 1 is DONE.");
            System.out.println("Press ENTER to quit (browser will stay open because detach=true)...");
            new BufferedReader(new InputStreamReader(System.in)).readLine();

        } finally {
            // If detach=true, Chrome stays open even after quit.
            driver.quit();
        }
    }

    private static void waitForPageReady(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            try {
                return "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"));
            } catch (Exception e) {
                return true;
            }
        });
    }

    private static void switchToNewWindow(WebDriver driver, Set<String> oldWindows, String beforeHandle) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> d.getWindowHandles().size() > oldWindows.size());
        for (String w : driver.getWindowHandles()) {
            if (!oldWindows.contains(w)) {
                driver.switchTo().window(w);
                return;
            }
        }
        driver.switchTo().window(beforeHandle);
    }
}
