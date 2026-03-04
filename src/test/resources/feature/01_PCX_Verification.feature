@regression
Feature: PageCenterX Report Cycle Verification

  Scenario: Verify and download the STPY0510DET report for a specific batch cycle
    Given I navigate to "{{pcx.url}}"
    When I search for the report "STPY0510DET.TXT" in PageCenterX
    Then the report "STPY0510DET.TXT" should have an import date starting with "2025/12/10"
    And I download the document "STPY0510DET.TXT" from the results
    And I verify the document "STPY0510DET.TXT" is successfully downloaded and valid

  @Story3
  Scenario: Validate and Parse Network CSV File
    Then I verify the network file "INGENIUM_STPY0510DET_2025-12-10.csv" exists in "\\\\cho_main_server\\data\\Pilot\\Tax_Data\\Balancing\\"
    And I parse the file and split the POLICY and COV columns