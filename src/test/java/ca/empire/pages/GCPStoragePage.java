package ca.empire.pages;

import ca.empire.setup.Hooks;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import java.io.File;
import java.util.List;

public class GCPStoragePage extends PageObject {

    // --- 1. Locators ---

    // UPDATED: Uses the stable 'data-icon-name' attribute you found in the HTML
    // This finds the Download button anywhere it appears (likely the top action bar)
    private By topDownloadButton = By.xpath("//*[@data-icon-name='downloadIcon']/ancestor::button | //*[@data-icon-name='downloadIcon']/ancestor::div[@role='button']");
    private By detailsDownloadButton = By.xpath("//span[contains(@class, 'mdc-button__label')]/span[contains(text(), 'Download')] | //span[text()='Download']/ancestor::button");
    // --- 2. Verification Actions ---

    public void verifyBucketName(String bucketName) {
        // Uses the data-test-id from your previous screenshot
        By headerLocator = By.cssSelector("h2[data-test-id='bucket-name-header']");

        try {
            waitForPresence(headerLocator);
            String headerText = driver.findElement(headerLocator).getText();
            Assert.assertTrue("Bucket Header [" + headerText + "] does not contain [" + bucketName + "]",
                    headerText.contains(bucketName));
        } catch (Exception e) {
            System.err.println("Could not find bucket header. Page might not have loaded correctly.");
            throw e;
        }
    }

    public void verifyFolderPath(String folderName) {
        By breadcrumbLocator = By.xpath("//span[contains(@class, 'breadcrumbs') and contains(text(), '" + folderName + "')]");
        waitForPresence(breadcrumbLocator);
        Assert.assertTrue("Folder path '" + folderName + "' not displayed!",
                driver.findElement(breadcrumbLocator).isDisplayed());
    }

    // --- 3. Download Action (Checkbox Strategy) ---

    public void downloadFile(String fullFileName) {
        // --- STEP 1: Find and Click the File Link ---

        By tableLocator = By.cssSelector("table");
        waitForPresence(tableLocator);

        List<WebElement> rows = driver.findElements(By.cssSelector("tr"));
        boolean found = false;

        System.out.println("DEBUG: Scanning " + rows.size() + " rows for file: " + fullFileName);

        for (WebElement row : rows) {
            if (row.getText().contains(fullFileName)) {
                found = true;
                System.out.println("Target file found! Clicking file name to open Details Page...");

                // Find the link (anchor tag) that contains the text and click it
                // This navigates us away from the list to the details page
                try {
                    WebElement fileLink = row.findElement(By.partialLinkText(fullFileName));
                    fileLink.click();
                } catch (Exception e) {
                    // Fallback: If partialLinkText fails, try clicking the cell containing the text
                    WebElement textCell = row.findElement(By.xpath(".//*[contains(text(), '" + fullFileName + "')]"));
                    textCell.click();
                }
                break;
            }
        }

        Assert.assertTrue("File [" + fullFileName + "] not found in the list!", found);

        // --- STEP 2: Click 'Download' on the Details Page ---

        System.out.println("Waiting for Details Page to load...");
        // Wait for the Download button to appear on the new page
        waitForPresence(detailsDownloadButton);

        System.out.println("Clicking 'Download' button on Details Page...");
        try {
            WebElement downloadBtn = driver.findElement(detailsDownloadButton);
            downloadBtn.click();

            // Wait for download to actually start
            Thread.sleep(5000);

        } catch (Exception e) {
            System.err.println("Standard click failed. Trying JS Click...");
            WebElement downloadBtn = driver.findElement(detailsDownloadButton);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", downloadBtn);
        }
    }

    // --- 4. Verification Logic ---

    public void verifyDownloadedFile(String expectedFileName) {
        // Location 1: Framework Temp Folder
        File tempDir = Hooks.getDownloadDirectory();
        // Location 2: System Downloads
        File systemDownloadsDir = new File(System.getProperty("user.home"), "Downloads");

        System.out.println("--- SMART VERIFICATION START ---");
        System.out.println("Looking for file containing: " + expectedFileName);

        File foundFile = null;
        int attempts = 0;

        // Wait up to 30 seconds
        while (attempts < 15 && foundFile == null) {

            // Check Location 1 (Temp Dir)
            if (tempDir.exists() && tempDir.isDirectory()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        // MATCH LOGIC: Check if the actual filename contains our expected string
                        if (f.getName().contains(expectedFileName)) {
                            foundFile = f;
                            break;
                        }
                    }
                }
            }

            // Check Location 2 (System Downloads) - Only if not found yet
            if (foundFile == null && systemDownloadsDir.exists()) {
                File[] files = systemDownloadsDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        // Check for match AND ensure it's a recent file (last 2 mins) to avoid old duplicates
                        if (f.getName().contains(expectedFileName) && (System.currentTimeMillis() - f.lastModified() < 120000)) {
                            foundFile = f;
                            break;
                        }
                    }
                }
            }

            if (foundFile == null) {
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                attempts++;
            }
        }

        if (foundFile != null) {
            System.out.println("SUCCESS: Found file: " + foundFile.getName());
            System.out.println("Full Path: " + foundFile.getAbsolutePath());
            Assert.assertTrue("File size is 0 bytes!", foundFile.length() > 0);
        } else {
            // Fail the test if we still can't find it
            Assert.fail("FAILURE: Could not find any file containing '" + expectedFileName + "' in Temp or Downloads folder after 30 seconds.");
        }
    }
}