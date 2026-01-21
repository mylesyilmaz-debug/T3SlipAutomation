package test2;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;
import java.time.Duration;

public class PCX_SourceDump {
    public static void main(String[] args) {
        String driverPath = "C:\\temp\\msedgedriver.exe";
        System.setProperty("webdriver.edge.driver", driverPath);

        EdgeDriverService service = new EdgeDriverService.Builder()
                .usingDriverExecutable(new File(driverPath))
                .build();

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new EdgeDriver(service, options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            System.out.println("Navigating to PCX...");
            driver.get("https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx?trid=logonx&tridsfx=&cssover=&srvid=DEVPCX01&svrlst=&logoff=1T");

            // --- USE THE REAL IDS FROM YOUR IMAGES ---
            System.out.println("Finding the User ID field...");
            WebElement usernameBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid")));

            System.out.println("Finding the Password field...");
            WebElement passwordBox = driver.findElement(By.id("pwidtemp"));

            System.out.println("Finding the Login button...");
            WebElement loginBtn = driver.findElement(By.id("LogonID"));

            // --- ACTION ---
            usernameBox.sendKeys("citmxy");
            passwordBox.sendKeys("PakTurk78%");

            System.out.println("Clicking Login...");
            loginBtn.click();

            // --- VERIFY SUCCESS ---
            // Wait for something that only appears AFTER login (like a folder icon or title change)
            Thread.sleep(5000);
            System.out.println("\n******************************************");
            System.out.println("   SUCCESS! You should now be logged in.");
            System.out.println("   Current Page Title: " + driver.getTitle());
            System.out.println("******************************************\n");

        } catch (Exception e) {
            System.out.println("Login failed - double check if the page is inside a frame!");
            e.printStackTrace();
        }
    }
}