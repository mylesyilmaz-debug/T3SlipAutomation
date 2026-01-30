package ca.empire.steps;

import ca.empire.pages.GCPStoragePage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
// We don't need 'driver' here anymore!

public class GCPSteps extends StepDefinition {

    private GCPStoragePage gcpPage;

    public GCPSteps() {
        // MATCHING YOUR PATTERN:
        // No arguments passed. The Page Object handles the driver internally.
        this.gcpPage = new GCPStoragePage();
    }

    @Then("I verify I am in the {string} bucket")
    public void verifyBucket(String bucketName) {
        gcpPage.verifyBucketName(bucketName);
    }

    @Then("I verify the current folder path is {string}")
    public void verifyFolder(String folderName) {
        gcpPage.verifyFolderPath(folderName);
    }

    @When("I download the file {string} from the list")
    public void downloadFile(String fileName) throws InterruptedException {
        gcpPage.downloadFile(fileName);
        Thread.sleep(5000);
    }

    @Then("I verify the GCP document {string} is successfully downloaded and valid")
    public void verifyDocumentDownload(String fileName) {
        gcpPage.verifyDownloadedFile(fileName);
    }
}