package test2;//taskkill /F /IM chrome.exe /T
//start chrome.exe --remote-debugging-port=9222 --user-data-dir="C:\Temp\AutomationSession" --incognito
//"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" --remote-debugging-port=9222 --user-data-dir="C:\Temp\EdgePortal"

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class PcxProfileSpike {
    public static void main(String[] args) {
        try {
            // STEP 1: Launch Chrome as a "Trusted" standalone process
            // This is the "Secret Sauce" to bypass the Zscaler kill
            System.out.println("Launching Trusted Browser Process...");
            String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
            String userData = "C:\\Temp\\AutomationSession";

            Runtime.getRuntime().exec(new String[]{
                    chromePath,
                    "--remote-debugging-port=9222",
                    "--user-data-dir=" + userData,
                    "--incognito",
                    "https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx?trid=logonx&tridsfx=&cssover=&srvid=DEVPCX01&svrlst=&logoff=1T"
            });

            // Give the browser 5 seconds to actually start up
            Thread.sleep(5000);

            // STEP 2: Hook Selenium into the process we just started
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
            WebDriver driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            System.out.println("Hooked Successfully! Entering Credentials...");

            // STEP 3: Automated Login (No manual typing!)
            // Replace these IDs with the actual IDs from your login page
            WebElement userField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("USERID")));
            WebElement passField = driver.findElement(By.name("PASSWORD"));
            WebElement loginBtn = driver.findElement(By.id("btnLogin")); // or whatever the button ID is

            userField.sendKeys("citmxy");
            passField.sendKeys("PakTurk78%");
            loginBtn.click();

            // STEP 4: Wait for the Dashboard
            System.out.println("Waiting for Dashboard load...");
            wait.until(ExpectedConditions.titleContains("Folder Explorer"));

            System.out.println("Full Automation Complete. Now Scraping Data...");

            // YOUR SCRAPING LOGIC GOES HERE

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}