package ca.empire.validators;

import ca.empire.util.DatabaseManager;
import org.junit.Assert;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ChangeHistoryValidator {

    private DatabaseManager dbManager;
    private Set<String> csvPolicies = new HashSet<>();
    private boolean isDataLoaded = false;

    // Constructor Injection
    public ChangeHistoryValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- 1. CSV DATA EXTRACTION ---
    public void loadCsvDataIfNeeded(String csvPath) {
        if (isDataLoaded) return;

        System.out.println(">>> Parsing input CSV file for Audit Trail Validation...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "ISO-8859-1"))) {
            String headerLine = reader.readLine();
            int polCol = getColumnIndex(headerLine, "Policy Num");

            Assert.assertTrue("Missing 'Policy Num' column in CSV header.", polCol != -1);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length > polCol) {
                    String polId = row[polCol].trim();
                    if (!polId.isEmpty()) csvPolicies.add(polId);
                }
            }
            isDataLoaded = true;
            System.out.println("   Extracted " + csvPolicies.size() + " unique Policies from CSV.");
        } catch (Exception e) {
            Assert.fail("CSV Read Error: " + e.getMessage());
        }
    }

    // --- 2. CHANGE HISTORY VALIDATION ---
    public void validateAuditTrail(String csvPath, String loadDate) {
        loadCsvDataIfNeeded(csvPath);
        System.out.println("\n>>> Validating CHANGE_HISTORY Audit Trail for " + csvPolicies.size() + " policies...");

        int passCount = 0;
        int failCount = 0;
        Connection conn = dbManager.getConnection();

        String query =
                "SELECT ch.CHNG_USER_ID, ch.STAT_ID, ch.CHNG_REASN_ID, ch.CHNG_COMNT_TXT, ch.CHNG_DT " +
                        "FROM TAX_SLIP ts " +
                        "JOIN CHANGE_HISTORY ch ON ts.ID = ch.RLT_ID " +
                        "WHERE ts.POL_ID = ? AND ts.CRA_TAX_YR_ID = 17 " +
                        "AND ch.RLT_TBL_NM = 'TAX_SLIP' " +
                        "AND TO_CHAR(ch.CHNG_DT, 'YYYY-MM-DD') = ? " +
                        "ORDER BY ch.CHNG_DT ASC";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (String polId : csvPolicies) {
                stmt.setString(1, polId);
                stmt.setString(2, loadDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean foundNewEvent = false;
                    boolean recordPassed = false;
                    String validationErrors = "No 'NEW' (Reason=1) event found on this date.";

                    // Loop through ALL history records for this policy on this date
                    while (rs.next()) {
                        int reasonId = rs.getInt("CHNG_REASN_ID");

                        // We ONLY validate the 'NEW' event
                        if (reasonId == 1) {
                            foundNewEvent = true;

                            String user = rs.getString("CHNG_USER_ID");
                            int statusId = rs.getInt("STAT_ID");
                            String comment = rs.getString("CHNG_COMNT_TXT");

                            boolean isRowValid = true;
                            StringBuilder errors = new StringBuilder();

                            // Validation A: Correct User/System ID
                            if (user == null || !user.toUpperCase().contains("SYSTEM")) {
                                isRowValid = false;
                                errors.append("User is '").append(user).append("' (Expected SYSTEM). ");
                            }

                            // Validation B: Correct Status (IGO=2 or COMPLETED=7)
                            if (statusId != 2 && statusId != 7) {
                                isRowValid = false;
                                errors.append("Status=").append(statusId).append(" (Expected 2 or 7). ");
                            }

                            // Validation C: Comment Check
                            if (comment == null || !comment.toLowerCase().contains("load")) {
                                isRowValid = false;
                                errors.append("Comment missing 'load' keyword. ");
                            }

                            if (isRowValid) {
                                recordPassed = true;
                                String statusName = (statusId == 2) ? "IGO" : "COMPLETED";
                                System.out.println(String.format("   [PASS] %-12s | %-8s | %-4d | Valid Audit Trail (%s/NEW/SYSTEM)",
                                        polId, user, statusId, statusName));
                                break; // Found the perfect record, stop looping!
                            } else {
                                validationErrors = errors.toString();
                            }
                        }
                    }

                    if (recordPassed) {
                        passCount++;
                    } else if (foundNewEvent) {
                        System.out.println(String.format("   [FAIL] %-12s | ERRORS: %s", polId, validationErrors));
                        failCount++;
                    } else {
                        System.out.println(String.format("   [FAIL] %-12s | %s", polId, validationErrors));
                        failCount++;
                    }
                }
            }
        } catch (SQLException e) {
            Assert.fail("SQL Exception during Audit Trail validation: " + e.getMessage());
        }

        System.out.println("   -----------------------------------------------------------------------------------------");
        System.out.println("   Audit Trail Summary -> Passed: " + passCount + ", Failed: " + failCount);

        // Assert to fail the Cucumber test if any records fail
        Assert.assertEquals("CHANGE_HISTORY validation failed for " + failCount + " policies!", 0, failCount);
    }

    private int getColumnIndex(String headerLine, String colName) {
        if (headerLine == null) return -1;
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].replace("\"", "").trim().equalsIgnoreCase(colName)) return i;
        }
        return -1;
    }
}