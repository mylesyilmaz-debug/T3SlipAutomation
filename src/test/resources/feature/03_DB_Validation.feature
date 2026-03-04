@DB_Validation
Feature: Database Data Integrity Verification

  Background:
    Given I have the parsed T3 CSV file "T3_Parsed_Output.csv"
    And the database connection is established

  @CSVvsDB
  Scenario: Validate Recipient Demographics and Fund Amounts
    Then I validate the T3 recipient demographics and address against the database
    And I validate the T3 fund level amounts and RL16 display against the database

  @Rollup
  Scenario: Validate T3 Fund-to-Slip Rollup and Aggregation Logic
    Then I validate that the sum of T3 funds matches the T3 slip totals

  @Lookups
  Scenario: Validate Static Lookup Tables Mapping
    Then I validate the Trust Account definitions in the database
    And I verify the TAX_SLIP_TYPE and ADMIN_SYSTEM lookup IDs for the loaded policies

  @AuditTrail
  Scenario: Validate CHANGE_HISTORY Audit Trail Generation
    Then I validate that a SYSTEM generated NEW audit record exists for each loaded policy

