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
            WebElement userField = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("usid")));
            userField.click();
            userField.clear();
            userField.sendKeys("citmxy");

// 2. Small "Safety" pause to let the browser process the text
            Thread.sleep(500);

// 3. Handle Password: Click specifically to move focus away from Username
            WebElement passField = driver.findElement(By.id("pwidtemp"));
            passField.click();
            passField.clear();
            passField.sendKeys("PakTurk78%");

// 4. Click Logon
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

            // --- STEP 5: VALIDATE BATCH CYCLE DATE ---
            System.out.println("Verifying batch cycle date...");

            // 1. Locate the row that contains our specific file name
            // Using XPath to find the row (tr) that has a link with our filename
            String rowXpath = "//tr[td/a[text()='STPY0510DET.TXT']]";
            WebElement fileRow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(rowXpath)));

            // 2. Find the 'Import Date' cell within that row
            // Based on your UI, the Import Date is usually the 4th cell (td) in the row
            WebElement importDateCell = fileRow.findElement(By.xpath("./td[3]"));
            String actualDate = importDateCell.getText();

            System.out.println("Found File with Import Date: " + actualDate);

            // 3. Compare with expected cycle date (2025/12/10)
            String expectedCycleDate = "2025/12/10";  //if the cycle run after midnight the expected date should range
            //for 24 hours which means , it may be Dec 10 to Dec 11

            if (actualDate.startsWith(expectedCycleDate)) {
                System.out.println("SUCCESS: Batch cycle date verified. Proceeding to download...");
            } else {
                System.err.println("FAILURE: Date mismatch! Expected " + expectedCycleDate + " but found " + actualDate);
                // Stop the test here if the date is wrong
                driver.quit();
                return;
            }

            // STEP 5: RIGHT-CLICK DOWNLOAD
            System.out.println("Attempting to download...");
            WebElement fileLink = wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText(targetFile)));

            Thread.sleep(5000);

            // Perform Right-Click
            actions.contextClick(fileLink).perform();

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
                System.out.println("SUCCESS: File " + targetFile + " is downloaded!");
                System.out.println("******************************************");

                // 1. Parsing and Cleaning call (using the File object from earlier in the try block)
                System.out.println("Starting file parsing and cleaning...");
                String csvOutput = downloadDir + "STPY0510DET_Cleaned.csv";

                // We call the method using the absolute path of the file we verified
                parseAndCleanFile(downloadedFile.getAbsolutePath(), csvOutput);

            } else {
                System.err.println("FAILURE: File not found in " + downloadDir);
            }

        } catch (Exception e) {
            System.err.println("Automation Error encountered.");
            e.printStackTrace();
        } finally {
            System.out.println("Automation session complete.");
            // driver.quit();
        }



    }

    public static void parseAndCleanFile(String inputPath, String outputPath) {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(inputPath));
             java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(outputPath))) {

            // 1. Write the Header
            writer.println("FUND_CODE,CL,SIN_NUM,POLICY_NUM,COV_NUM,YR_BEG_ACB,INT_ELIG,DIV_ELIG,DIV_FOREIGN,GAINS_DISP,GAINS_CAN,TOTAL_INC,YR_END_ACB");

            String line;
            while ((line = reader.readLine()) != null) {
                // Clean up the line: turn multiple spaces into a single space for easy splitting
                String cleanLine = line.trim().replaceAll(" +", " ");
                String[] tokens = cleanLine.split(" ");

                // 2. Dynamic Filter: Data rows must have at least 10 pieces of information
                // Total rows must have at least 7 pieces
                if (tokens.length < 7) continue;

                String fund = tokens[0];
                String cl   = tokens[1];

                // Validate that 'cl' looks like a Province (2 capital letters)
                if (!cl.matches("[A-Z]{2}")) continue;

                // --- CASE A: DATA ROW (SIN exists at tokens[2]) ---
                if (tokens.length >= 11 && tokens[2].matches("\\d{9}")) {
                    String sin = tokens[2];
                    String rawPolicy = tokens[3];
                    String policy = rawPolicy.substring(0, rawPolicy.length() - 2);
                    String cov = rawPolicy.substring(rawPolicy.length() - 2);

                    // Check for non-zero financial data (tokens 4 to end)
                    boolean hasData = false;
                    StringBuilder financials = new StringBuilder();
                    for (int i = 4; i < tokens.length; i++) {
                        String val = tokens[i].replace(",", "");
                        if (!val.equals("0.00")) hasData = true;
                        financials.append(",").append(val);
                    }

                    // Only write if there is actual money in the row
                    if (hasData) {
                        writer.println(fund + "," + cl + "," + sin + "," + policy + "," + cov + financials.toString());
                    }
                }

                // --- CASE B: TOTAL ROW (No SIN, fewer tokens, but has financials) ---
                else if (tokens.length >= 7 && tokens.length < 11) {
                    // We know it's a total row if it starts with Fund/CL but doesn't have a SIN
                    StringBuilder totals = new StringBuilder();
                    for (int i = 2; i < tokens.length; i++) {
                        totals.append(",").append(tokens[i].replace(",", ""));
                    }
                    // Leave SIN, Policy, and Cov empty in the CSV for totals
                    writer.println(fund + "," + cl + ",,,," + totals.toString());
                }
            }
            System.out.println("Dynamic parsing complete. Output saved to: " + outputPath);

        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
