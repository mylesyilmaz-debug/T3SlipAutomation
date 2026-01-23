Feature: T3 Microfocus Job Validation - Story 1

 @network
 Scenario: STPY0510 cleaned CSV exists on network folder
  Given the network file "\\cho_main_server\data\Pilot\Tax_Data\Balancing\INGENIUM_STPY0510DET_2025-12-10.csv" exists
  Then the network file is readable and not empty
  And I generate a transformed T3 CSV with separated policy and coverage numbers

 @pcx_ui
 Scenario: Login to PCX and open PageCenterX
  Given I am logged into PCX and PageCenterX is opened
