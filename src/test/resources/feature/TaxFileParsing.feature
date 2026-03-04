Feature: Tax File Parsing and Validation
  I want to parse raw T3 and RL16 text files
  So that I can verify the extracted data in CSV format

  @T3Parser
  Scenario: Parse T39580-ITAX file into Individual and Fund CSVs
    Given the download directory contains a file matching "T39580-ITAX"
    When I run the T3 file parser
    Then the "T3_Parsed_Output.csv" file should be generated
    And the "T3_FUND_Parsed_Output.csv" file should be generated


  @RL16Parser
  Scenario: Parse RL16TAPE file into a consolidated CSV
    Given the download directory contains a file matching "RL16TAPE"
    When I run the RL16 file parser
    Then the "RL16_Parsed_Output.csv" file should be generated
