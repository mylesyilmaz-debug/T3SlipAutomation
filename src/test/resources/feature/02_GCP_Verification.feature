@GCP
Feature: GCP Storage Verification

  @GCPT3RL16
  Scenario Outline: Verify and download Tax Reports (T3 and RL16)
    Given I navigate to "{{gcp-bucket-url}}"

    # 1. Verify location
    Then I verify I am in the "corp-taxreports-pilot-microfocus" bucket
    And I verify the current folder path is "Input"

    # 2. Download the specific file (Placeholder variable)
    When I download the file "<fileName>" from the list

    # 3. Verify it landed in the downloads folder
    Then I verify the GCP document "<fileName>" is successfully downloaded and valid

    # This table tells Cucumber to run the test twice: once for T3, once for RL16
    Examples:
      | fileName                            |
      | T39580-ITAX.txt_20251210222633.txt  |
      | RL16TAPE.txt_20251210222633.txt     |

  @Rejects
  Scenario: Validate GCP Reject Files based on End-to-End Balancing Counts
    # Step 1 & 2 from the ticket (Pre-condition calculations)
    Given I calculate the balancing difference between the Input CSV and the ITAX database
    # Step 3 & 4 from the ticket (Navigating and UI verification)
    When I navigate to the GCP Rejects folder for the current run date
    Then I verify the reject file state matches the calculated balancing difference
    # Step 5 & 6 from the ticket (Downloading and Content verification)
    And I validate the content of the downloaded reject file equals the balancing difference