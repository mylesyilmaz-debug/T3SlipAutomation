@itax
Feature: TRT data validation

  Scenario: Validate total record count between DEL file and DB
    Given DEL file is loaded
    And DB connection is available
    When I count total records in DEL file
    And I count total records in DB table
    Then DEL record count should match DB record count

  Scenario: Validate distinct RTBL_ID distribution in DB
    Given DB connection is available
    When I fetch distinct RTBL_ID values with record counts
    Then RTBL_ID distribution should be displayed

  Scenario: Analyze RTBL_ID distribution in DEL file
    Given I analyze RTBL_ID distribution from DEL file

  Scenario: Select up to 50 random rows per RTBL_ID from DEL file
    Given I select up to 50 random rows per RTBL_ID from DEL file

  Scenario: Random sampled DEL rows should exist in DB
    Given random sampled DEL rows exist in DB

