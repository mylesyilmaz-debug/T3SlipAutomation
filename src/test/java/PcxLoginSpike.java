import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import java.nio.file.Files;
import java.nio.file.Path;

public class PcxLoginSpike {

    public static void main(String[] args) throws Exception {

        System.out.println("Starting PCX login spike...");

        // Create a fresh, temporary Chrome profile folder for this run
        Path tempProfile = Files.createTempDirectory("pcx-chrome-profile-");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-data-dir=" + tempProfile.toAbsolutePath());
        options.addArguments("--no-first-run");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--start-maximized");


       WebDriver driver = new ChromeDriver(options);

       driver.get("https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        WebElement link = wait.until(d -> d.findElement(By.linkText("PageCenterX")));

// Print what we are clicking
        System.out.println("PageCenterX outerHTML = " + link.getAttribute("outerHTML"));

        String href = link.getAttribute("href");
        System.out.println("PageCenterX href = " + href);

// If href is empty or javascript, force navigation via JS
        if (href == null || href.isBlank() || href.startsWith("javascript")) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
            System.out.println("Clicked PageCenterX via JS click()");
        } else {
            driver.navigate().to(href);
            System.out.println("Navigated to PageCenterX href directly");
        }

        Thread.sleep(5000);
        System.out.println("After navigation, URL = " + driver.getCurrentUrl());

        System.out.println("Opened URL successfully");

        Thread.sleep(5000); // just to visually confirm it stays open

        // don't quit yet while testing stability
        // driver.quit();
    }
}