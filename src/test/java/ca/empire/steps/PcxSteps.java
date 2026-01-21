package ca.empire.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.junit.Assert;

import java.time.Duration;
import java.util.Set;

public class PcxSteps {

    WebDriver driver;
    WebDriverWait wait;
    String originalWindow;

    @Given("I navigate to {string}")
    public void openPortal() {
        // Initialize driver just like your successful Amazon test
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        driver.get("https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx");
        originalWindow = driver.getWindowHandle();
    }
}
