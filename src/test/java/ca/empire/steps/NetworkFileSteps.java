package ca.empire.steps;

import ca.empire.util.CsvProcessor;
import io.cucumber.java.en.Then;
import java.io.File;

public class NetworkFileSteps extends StepDefinition {

    private File sourceFile;

    @Then("I verify the network file {string} exists in {string}")
    public void verifyNetworkFile(String fileName, String path) {
        // Use quadruple backslashes for the network path in your Feature file
        this.sourceFile = CsvProcessor.validateNetworkFile(path, fileName);
        System.out.println("Network file validated successfully: " + sourceFile.getName());
    }

    @Then("I parse the file and split the POLICY and COV columns")
    public void parseNetworkFile() throws Exception {
        // Saving to a 'Processed' folder so it isn't deleted by Hooks
        String outputDir = System.getProperty("user.dir") + "/Processed_Data";
        String outputPath = CsvProcessor.processCsv(sourceFile, outputDir);
        System.out.println("Parsing Complete. New file created at: " + outputPath);
    }
}
