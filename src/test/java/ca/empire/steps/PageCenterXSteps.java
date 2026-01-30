package ca.empire.steps;

import ca.empire.pages.PageCenterXPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;

public class PageCenterXSteps extends StepDefinition {

    private PageCenterXPage pcxPage;

    public PageCenterXSteps() {

        this.pcxPage = new PageCenterXPage();
    }

    @When("I search for the report {string} in PageCenterX")
    public void i_search_for_the_report_in_page_center_x(String reportName) {
        pcxPage.navigateToNonProdReports();
        pcxPage.setFilters();
        pcxPage.searchForReport(reportName);
    }

    @Then("the report {string} should have an import date starting with {string}")
    public void verifyReportDate(String fileName, String expectedCycleDate) {
        // Get the date and remove any hidden spaces
        String actualDate = pcxPage.getImportDate(fileName).trim();
        System.out.println("Found File with Import Date: " + actualDate);

        // This checks if the system date (with slashes) contains your input
        if (actualDate.contains(expectedCycleDate)) {
            System.out.println("SUCCESS: Batch cycle date verified.");
        } else {
            org.junit.Assert.fail("FAILURE: Date mismatch! Expected [" + expectedCycleDate + "] to be inside [" + actualDate + "]");
        }
    }

    @Then("I download the document {string} from the results")
    public void downloadDocument(String fileName) {
        System.out.println("Attempting to download " + fileName + "...");
        pcxPage.downloadFirstResult();
    }

    @Then("I verify the document {string} is successfully downloaded and valid")
    public void verifyDocumentIsValid(String fileName) {
        pcxPage.verifyDownloadedFile(fileName);
    }


}