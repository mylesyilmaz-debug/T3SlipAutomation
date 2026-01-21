package test2;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;
import java.time.Duration;

public class PCX_Final_Automation {

    public static void main(String[] args) {
        // --- CONFIGURATION ---
        String driverPath = "C:\\temp\\msedgedriver.exe";
        String targetFile = "STPY0510DET.TXT";
        String downloadDir = System.getProperty("user.home") + "\\Downloads\\";

        System.setProperty("webdriver.edge.driver", driverPath);
        EdgeDriverService service = new EdgeDriverService.Builder()
                .usingDriverExecutable(new File(driverPath))
                .build();

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new EdgeDriver(service, options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        Actions actions = new Actions(driver);

        try {
            // STEP 1: LOGIN
            System.out.println("Navigating to PCX Portal...");
            driver.get("https://kgnpcxpl01.empire.corp/lrs/nlrswc2.exe/pcx?trid=logonx&tridsfx=&cssover=&srvid=DEVPCX01&svrlst=&logoff=1T");

            System.out.println("Entering credentials...");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid"))).sendKeys("citmxy");
            driver.findElement(By.id("pwidtemp")).sendKeys("PakTurk78%");
            Thread.sleep(5000);
            driver.findElement(By.id("LogonID")).click();

            // STEP 2: NAVIGATION (ROOT -> REPORTS -> NONPROD)
            System.out.println("Navigating through folders...");
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Reports"))).click();
            Thread.sleep(5000);
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("NonProd"))).click();
            Thread.sleep(5000);

            // STEP 3: SET FILTERS
            System.out.println("Setting Show/Locate filters...");
            new Select(wait.until(ExpectedConditions.presenceOfElementLocated(By.name("expldisp"))))
                    .selectByVisibleText("Folders/Documents");

            Thread.sleep(5000);

            new Select(driver.findElement(By.name("ftype")))
                    .selectByVisibleText("Document");

            Thread.sleep(5000);

            // STEP 4: SEARCH
            System.out.println("Searching for file: " + targetFile);
            // Locates the search box using id="ftext"
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ftext")));
            searchInput.clear();
            searchInput.sendKeys(targetFile);

            Thread.sleep(5000);

            // Locates the 'Go' button using id="fbutton"
            WebElement goButton = driver.findElement(By.id("fbutton"));
            goButton.click();

            // STEP 5: RIGHT-CLICK DOWNLOAD
            System.out.println("Attempting to download...");
            WebElement fileLink = wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(targetFile)));

            Thread.sleep(5000);

            // Perform Right-Click
            actions.contextClick(fileLink).perform();

//            // Select 'Download' from context menu
//            WebElement downloadOpt = wait.until(ExpectedConditions.elementToBeClickable(By.id("pxcdownload")));
//            actions.moveToElement(downloadOpt).click().perform();
//
//            Thread.sleep(2000);
//
//            // Click 'Document' sub-menu
//            WebElement docSubOpt = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Document']")));
//            docSubOpt.click();

            //Click the 'Download' main menu item (using id="pxcdownload")
            WebElement downloadOpt = wait.until(ExpectedConditions.elementToBeClickable(By.id("pxcdownload")));
            actions.moveToElement(downloadOpt).click().perform();

            // 3. UPDATED: Click the 'Document' sub-menu item using its specific ID
            // Based on your image, the ID is "pxcdocdownload"
            WebElement docSubOpt = wait.until(ExpectedConditions.elementToBeClickable(By.id("pxcdocdownload")));
            docSubOpt.click();

            System.out.println("Document download triggered via pxcdocdownload ID.");

            // STEP 6: VERIFY LOCAL DOWNLOAD
            System.out.println("Verifying download in local folder...");
            File downloadedFile = new File(downloadDir + targetFile);

            boolean success = false;
            for (int i = 0; i < 15; i++) { // Wait up to 15 seconds for download to finish
                if (downloadedFile.exists() && downloadedFile.length() > 0) {
                    success = true;
                    break;
                }
                Thread.sleep(1000);
            }

            if (success) {
                System.out.println("******************************************");
                System.out.println("SUCCESS: File " + targetFile + " is available, readable, and downloaded!");
                System.out.println("File Size: " + downloadedFile.length() + " bytes");
                System.out.println("******************************************");
            } else {
                System.err.println("FAILURE: File not found in " + downloadDir);
            }

        } catch (Exception e) {
            System.err.println("Automation Error encountered.");
            e.printStackTrace();
        } finally {
            System.out.println("Automation session complete.");
            // driver.quit(); // Uncomment this to close browser automatically
        }
    }
}