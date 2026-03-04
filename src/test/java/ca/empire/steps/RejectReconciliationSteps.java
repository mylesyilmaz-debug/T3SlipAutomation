package ca.empire.steps;

import ca.empire.pages.GCPStoragePage;
import ca.empire.setup.Hooks; // <-- This should light up now!
import ca.empire.util.DatabaseManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.junit.Assert;
import io.cucumber.java.en.And;

public class RejectReconciliationSteps {

    private DatabaseManager dbManager;
    private GCPStoragePage gcpPage;

    // State variables to pass data between steps
    private int inputCsvCount = 0;
    private int itaxDbCount = 0;
    private int balancingDiff = 0;
    private String loadDate = "2025-12-10";
    private String downloadDir = "C:\\Selenium_Downloads\\";
    private String targetRejectFileName = "2025_Ingenium_T3_RL16_Rejected_Records_20251210_222618.csv";

    // 1. Clean Constructor (No WebDriver calls here!)
    public RejectReconciliationSteps() {
        this.dbManager = new DatabaseManager();
    }

    @Given("I calculate the balancing difference between the Input CSV and the ITAX database")
    public void calculateBalancingDifference() throws Exception {
        System.out.println(">>> STEP 1: Calculating Balancing Difference...");

        // 1. Get CSV Count (excluding header)
        String csvPath = downloadDir + "T3_Parsed_Output.csv";
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            while (reader.readLine() != null) inputCsvCount++;
            inputCsvCount--; // Subtract header
        }

        // 2. Get DB Count
        dbManager.connect();
        Connection conn = dbManager.getConnection();
        String query2 = "SELECT COUNT(*) AS total FROM TAX_SLIP WHERE CRA_TAX_YR_ID = 17 and ADMIN_SYS_ID = 1 and TAX_SLIP_TYPE_ID = 1 AND TO_CHAR(CREAT_DT, 'YYYY-MM-DD') = ?";
        String query = "SELECT COUNT(F.TAX_SLIP_ID) AS total FROM TAX_SLIP S JOIN T3_FUND F ON S.ID = F.TAX_SLIP_ID WHERE S.CRA_TAX_YR_ID = 17 AND S.ADMIN_SYS_ID = 1 AND S.TAX_SLIP_TYPE_ID = 1 AND TO_CHAR(S.CREAT_DT, 'YYYY-MM-DD') = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loadDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) itaxDbCount = rs.getInt("total");
        }

        // 3. Calculate Math
        balancingDiff = inputCsvCount - itaxDbCount;

        System.out.println("   Input CSV Count: " + inputCsvCount);
        System.out.println("   ITAX Load Count: " + itaxDbCount);
        System.out.println("   Calculated Diff: " + balancingDiff);
    }

    @When("I navigate to the GCP Rejects folder for the current run date")
    public void navigateToRejects() throws InterruptedException {
        System.out.println(">>> STEP 2: Navigating to GCP Rejects...");

        // 2. Initialize the Page Object safely inside the step!
        this.gcpPage = new GCPStoragePage();

        String rejectsUrl = "https://console.cloud.google.com/storage/browser/corp-taxreports-pilot-microfocus/data/Pilot/Tax_Data/Intellitax_Loads/T3/Rejects";
        Hooks.getDriver().get(rejectsUrl);

        Thread.sleep(5000);
    }

    @Then("I verify the reject file state matches the calculated balancing difference")
    public void verifyRejectFileState() {
        System.out.println(">>> STEP 3: Verifying File State for Diff = " + balancingDiff);

        // Pass the target filename into our new specific check
        boolean isVisible = gcpPage.isSpecificRejectFileVisible(targetRejectFileName);

        if (balancingDiff == 0) {
            if (isVisible) {
                String rowText = gcpPage.getSpecificRejectFileSizeInUI(targetRejectFileName);
                System.out.println("   [INFO] UI Size scraped as: " + rowText + " (Bypassing strict UI text check)");
                System.out.println("   [PASS] Scenario B: Diff is 0, file exists. Downloading to verify physical 0 Byte size...");

                // WE MUST DOWNLOAD IT HERE so Step 4 can physically verify it!
                gcpPage.downloadFile(targetRejectFileName);
            } else {
                System.out.println("   [PASS] Scenario B: Diff is 0, and no Reject file was generated.");
            }
        } else {
            Assert.assertTrue("Diff is " + balancingDiff + " but NO Reject file was found in GCP!", isVisible);
            System.out.println("   [PASS] Scenario A: Diff > 0, and populated Reject file is visible.");

            gcpPage.downloadFile(targetRejectFileName);
        }
    }
    @And("I validate the content of the downloaded reject file equals the balancing difference")
    public void validateDownloadedContent() {
        System.out.println(">>> STEP 4: Validating downloaded file content...");
        File folder = new File(downloadDir);

        // Wait up to 15 seconds for the file to finish downloading
        Wait<File> wait = new FluentWait<>(folder)
                .withTimeout(Duration.ofSeconds(15))
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(Exception.class);

        try {
            File downloadedReject = wait.until(dir -> {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().contains("reject"));
                if (files != null && files.length > 0) {
                    File latestFile = files[files.length - 1];
                    // Ensure it's not a temporary downloading file
                    if (!latestFile.getName().endsWith(".crdownload") && !latestFile.getName().endsWith(".tmp")) {
                        return latestFile;
                    }
                }
                return null;
            });

            // Measure the physical file size
            long fileSize = downloadedReject.length();

            // SCENARIO 1: Diff is 0, so the file MUST be 0 bytes
            if (balancingDiff == 0) {
                Assert.assertEquals("Expected a 0-byte file since diff is 0, but file had data!", 0, fileSize);
                System.out.println("   [PASS] It passed because the Diff is 0 and the downloaded file size is exactly 0 Bytes.");
                return;
            }

            // SCENARIO 2: Diff > 0, so we count the rows
            int rowCount = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(downloadedReject))) {
                while (reader.readLine() != null) rowCount++;
            }

            Assert.assertEquals("Reject file row count does not match the Balancing Difference!", Math.abs(balancingDiff), rowCount);
            System.out.println("   [PASS] Reject file row count (" + rowCount + ") perfectly matches Balancing Diff.");

        } catch (org.openqa.selenium.TimeoutException e) {
            Assert.fail("FAIL: No fully downloaded reject file was found in the download directory.");
        } catch (Exception e) {
            Assert.fail("Technical error during validation: " + e.getMessage());
        }
    }
}