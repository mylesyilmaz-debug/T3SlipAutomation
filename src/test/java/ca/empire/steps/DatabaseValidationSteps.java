package ca.empire.steps;

import ca.empire.util.DatabaseManager;
import ca.empire.validators.FundValidator;
import ca.empire.validators.RecipientValidator;
import ca.empire.validators.RollupValidator; // <-- Import your new validator
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.junit.Assert;
import java.sql.SQLException;
import ca.empire.validators.LookupValidator;
import ca.empire.validators.ChangeHistoryValidator;

public class DatabaseValidationSteps {

    private DatabaseManager dbManager;
    private RecipientValidator recipientValidator;
    private FundValidator fundValidator;
    private RollupValidator rollupValidator;
    private LookupValidator lookupValidator;
    private ChangeHistoryValidator changeHistoryValidator;



    // Picocontainer or Constructor Injection usually handles this
    public DatabaseValidationSteps() {
        this.dbManager = new DatabaseManager();
        this.recipientValidator = new RecipientValidator(dbManager);
        this.fundValidator = new FundValidator(dbManager);
        this.rollupValidator = new RollupValidator(dbManager);
        this.lookupValidator = new LookupValidator(dbManager);
        this.changeHistoryValidator = new ChangeHistoryValidator(dbManager);
    }

    @Before("@DB_Validation")
    public void setupDB() throws SQLException {
        dbManager.connect();
    }

    @After("@DB_Validation")
    public void tearDownDB() {
        dbManager.close();
    }

    @Given("I have the parsed T3 CSV file {string}")
    public void i_have_the_parsed_t3_csv_file(String fileName) {
       System.out.println("Using T3 CSV File: " + fileName);

    }

    @And("the database connection is established")
    public void the_database_connection_is_established() {
        // Since @Before("@DB_Validation") already called dbManager.connect(),
        // we just assert that it was successful so the step passes cleanly.
        Assert.assertNotNull("Database connection failed!", dbManager.getConnection());
        System.out.println("Verified Database Connection is active.");
    }

    @Then("I validate the T3 recipient demographics and address against the database")
    public void validateT3Demographics() {
        String csvPath = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
        recipientValidator.validateRecipients(csvPath);
    }

    @Then("I validate the T3 fund level amounts and RL16 display against the database")
    public void validateT3Funds() {
        String csvPath = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
        fundValidator.validateFunds(csvPath);
    }

    @Then("I validate that the sum of T3 funds matches the T3 slip totals")
    public void validate_sum_of_t3_funds_matches_slip_totals() {
        // We pass the expected load date. You could also pass the CSV path here
        // if you want to extract specific policies later!
        String loadDate = "2025-12-10";
        rollupValidator.validateRollups(loadDate);
    }

    @Then("I validate the Trust Account definitions in the database")
    public void i_validate_the_trust_account_definitions_in_the_database() {
        String csvPath = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
        lookupValidator.validateTrustAccounts(csvPath);
    }

    @And("I verify the TAX_SLIP_TYPE and ADMIN_SYSTEM lookup IDs for the loaded policies")
    public void i_verify_the_tax_slip_type_and_admin_system_lookup_ids_for_the_loaded_policies() {
        String csvPath = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
        String loadDate = "2025-12-10"; // The specific load date
        lookupValidator.validatePolicyLookups(csvPath, loadDate);
    }

    @Then("I validate that a SYSTEM generated NEW audit record exists for each loaded policy")
    public void i_validate_that_a_system_generated_new_audit_record_exists_for_each_loaded_policy() {
        String csvPath = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
        String loadDate = "2025-12-10"; // The specific load date
        changeHistoryValidator.validateAuditTrail(csvPath, loadDate);
    }

}
