@GCP
Feature: GCP Storage Verification

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