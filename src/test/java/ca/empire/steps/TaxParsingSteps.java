package ca.empire.steps;

import io.cucumber.java.en.*;
import org.junit.Assert;
import ca.empire.util.T3ParserUtils;
import ca.empire.util.RL16ParserUtils;
import java.io.File;

public class TaxParsingSteps {

    // --- CONFIG ---
    private static final String DOWNLOAD_PATH = "C:\\Selenium_Downloads";

    // Instantiate the Utility classes (The Logic)
    T3ParserUtils t3Parser = new T3ParserUtils();
    RL16ParserUtils rl16Parser = new RL16ParserUtils();

    // --- GIVEN ---
    @Given("the download directory contains a file matching {string}")
    public void verifySourceFileExists(String filePattern) {
        File dir = new File(DOWNLOAD_PATH);
        File[] files = dir.listFiles((d, name) -> name.contains(filePattern) && name.endsWith(".txt"));

        Assert.assertNotNull("Directory does not exist", files);
        Assert.assertTrue("No file found matching: " + filePattern, files.length > 0);
    }

    // --- WHEN ---
    @When("I run the T3 file parser")
    public void runT3Parser() {
        try {
            t3Parser.processT3File(DOWNLOAD_PATH, "T39580-ITAX");
        } catch (Exception e) {
            Assert.fail("T3 Parsing Failed: " + e.getMessage());
        }
    }

    @When("I run the RL16 file parser")
    public void runRL16Parser() {
        try {
            rl16Parser.processRL16File(DOWNLOAD_PATH, "RL16TAPE");
        } catch (Exception e) {
            Assert.fail("RL16 Parsing Failed: " + e.getMessage());
        }
    }

    // --- THEN ---
    @Then("the {string} file should be generated")
    public void verifyFileGenerated(String fileName) {
        File file = new File(DOWNLOAD_PATH + "\\" + fileName);

        // 1. Check file exists
        Assert.assertTrue("Output file was not created: " + fileName, file.exists());

        // 2. Check file is not empty (Basic validation)
        Assert.assertTrue("Output file is empty: " + fileName, file.length() > 0);
    }
}
