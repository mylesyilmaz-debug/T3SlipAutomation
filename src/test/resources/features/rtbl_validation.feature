Feature: RTBL Data Validation between DEL file and Database


  Scenario: Total record count should match
    Given the DEL file is loaded
    And the database connection is available
    When I count total records in DEL and DB
    Then the total record count should match

  Scenario: Distinct RTBL_ID distribution should be valid
    Given the DEL file is loaded
    And the database connection is available
    When I calculate distinct RTBL_ID counts
    Then each RTBL_ID should have matching record counts in DEL and DB

  Scenario: Validate up to 100 records per RTBL_ID
    Given the DEL file is loaded
    And the database connection is available
    When I randomly validate up to 100 records for each RTBL_ID
    Then all selected records should be found in DB

  Scenario: Validate 1000 random records overall
    Given the DEL file is loaded
    And the database connection is available
    When I randomly validate 1000 records
    Then all selected records should be found in DB