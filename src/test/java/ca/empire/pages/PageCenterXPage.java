package ca.empire.pages;

import ca.empire.setup.Hooks;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

public class PageCenterXPage extends PageObject {

    private String targetFile = "STPY0510DET.TXT";

    public void navigateToNonProdReports() {
        // 1. Wait for element, 2. Find and Click
        waitForPresence(By.linkText("Reports"));
        driver.findElement(By.linkText("Reports")).click();

        waitForPresence(By.linkText("NonProd"));
        driver.findElement(By.linkText("NonProd")).click();
    }

    public void setFilters() {
        // Use the ID from your working main class
        waitForPresence(By.name("expldisp"));
        driver.findElement(By.name("expldisp")).click();
    }

    public void searchForReport(String reportName) {
        waitForPresence(By.id("ftext"));
        WebElement input = driver.findElement(By.id("ftext"));
        input.clear();
        input.sendKeys(reportName);

        driver.findElement(By.id("fbutton")).click();
    }

    public String getImportDate(String fileName) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        String rowXpath = "//tr[td/a[text()='" + fileName + "']]";

        // Wait for row visibility
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(rowXpath)));
        WebElement fileRow = driver.findElement(By.xpath(rowXpath));

        return fileRow.findElement(By.xpath("./td[3]")).getText();
    }

    public void downloadFirstResult() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Actions actions = new Actions(driver);

        waitForPresence(By.linkText(targetFile));
        WebElement fileLink = driver.findElement(By.linkText(targetFile));

        // Preserving your 5-second sleep from main class
        try { Thread.sleep(5000); } catch (InterruptedException e) {}

        actions.contextClick(fileLink).perform();

        // Use IDs from your working main class
        wait.until(ExpectedConditions.elementToBeClickable(By.id("pxcdownload")));
        actions.moveToElement(driver.findElement(By.id("pxcdownload"))).click().perform();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("pxcdocdownload")));
        driver.findElement(By.id("pxcdocdownload")).click();
    }

    public void verifyDownloadedFile(String expectedFileName) {
        // 1. Availability: Use the framework's own directory tracker
        File downloadDir = Hooks.getDownloadDirectory();
        File file = new File(downloadDir, expectedFileName);

        // Wait up to 10 seconds for the file to finish writing to disk
        int attempts = 0;
        while (!file.exists() && attempts < 5) {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            attempts++;
        }

        Assert.assertTrue("FAILURE: File " + expectedFileName + " is not visible in " + downloadDir.getAbsolutePath(), file.exists());
        System.out.println("Verification: File is visible.");

        // 2. Readability: Check if OS allows reading
        Assert.assertTrue("FAILURE: File exists but is not readable!", file.canRead());
        System.out.println("Verification: File opens without error.");

        // 3. Integrity: Check that file is not empty (size > 0)
        long size = file.length();
        Assert.assertTrue("FAILURE: File integrity check failed! Size is 0 bytes.", size > 0);
        System.out.println("Verification: File contains data (Size: " + size + " bytes).");
    }
}
