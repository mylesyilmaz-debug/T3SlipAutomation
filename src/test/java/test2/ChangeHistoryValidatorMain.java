package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class ChangeHistoryValidatorMain {

    // --- CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";
    private static final String CSV_PATH = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv"; // Update if your path is different

    public static void main(String[] args) {
        System.out.println("--- Starting Ticket 9: CHANGE_HISTORY Audit Trail Validation ---");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("Database Connected!\n");
            validateChangeHistoryForLoad(conn, CSV_PATH);
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void validateChangeHistoryForLoad(Connection conn, String csvPath) {
        System.out.println(">>> Extracting target policies from CSV to validate Audit Trail...");
        Set<String> csvPolicies = new HashSet<>();
        int passCount = 0;
        int failCount = 0;

        // 1. Read the input CSV
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "ISO-8859-1"))) {
            String headerLine = reader.readLine();
            int polColIndex = getColumnIndex(headerLine, "Policy Num");

            if (polColIndex == -1) {
                System.err.println("[ERROR] Could not find column 'Policy Num' in CSV headers.");
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length > polColIndex) {
                    String polId = row[polColIndex].trim();
                    if (!polId.isEmpty()) {
                        csvPolicies.add(polId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read CSV: " + e.getMessage());
            return;
        }

        System.out.println("   Found " + csvPolicies.size() + " unique Policies in CSV. Checking CHANGE_HISTORY...\n");
        System.out.println(String.format("   %-12s | %-8s | %-4s | %-4s | %s", "POLICY_ID", "USER", "STAT", "RSN", "RESULT / ERRORS"));
        System.out.println("   -----------------------------------------------------------------------------------------");

        // 2. Query CHANGE_HISTORY filtered by the EXACT load date.
        // Removed FETCH FIRST 1 ROWS ONLY so we get all events for that day.
        String query =
                "SELECT ch.CHNG_USER_ID, ch.STAT_ID, ch.CHNG_REASN_ID, ch.CHNG_COMNT_TXT, ch.CHNG_DT " +
                        "FROM TAX_SLIP ts " +
                        "JOIN CHANGE_HISTORY ch ON ts.ID = ch.RLT_ID " +
                        "WHERE ts.POL_ID = ? AND ts.CRA_TAX_YR_ID = 17 " +
                        "AND ch.RLT_TBL_NM = 'TAX_SLIP' " +
                        "AND TO_CHAR(ch.CHNG_DT, 'YYYY-MM-DD') = '2025-12-10' " + // ** Update this date if your load was on a different day! **
                        "ORDER BY ch.CHNG_DT ASC";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {

            for (String polId : csvPolicies) {
                stmt.setString(1, polId);

                try (ResultSet rs = stmt.executeQuery()) {

                    boolean foundNewEvent = false;
                    boolean recordPassed = false;
                    String validationErrors = "No 'NEW' (Reason=1) event found on this date.";

                    // Loop through ALL history records for this policy on this date
                    while (rs.next()) {
                        int reasonId = rs.getInt("CHNG_REASN_ID");

                        // We ONLY want to validate the event where Reason = 1 (NEW)
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

                            // Validation B: Correct Status (IGO or Completed)
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

                                // Translate the ID for the console output
                                String statusName = (statusId == 2) ? "IGO" : "COMPLETE";

                                System.out.println(String.format("   [PASS] %-12s | %-8s | %-4d | %-4d | Valid Audit Trail (%s / NEW / SYSTEM)",
                                        polId, user, statusId, reasonId, statusName));
                                break; // We found the perfect record, stop looping!
                            }
                        }
                    }

                    // After checking all rows for this policy, did we find a passing one?
                    if (recordPassed) {
                        passCount++;
                    } else if (foundNewEvent) {
                        // Found Reason=1, but it failed the user/status/comment checks
                        System.out.println(String.format("   [FAIL] %-12s | %-8s | %-4s | %-4s | ERRORS: %s",
                                polId, "N/A", "N/A", "1", validationErrors));
                        failCount++;
                    } else {
                        // Never even found a Reason=1 record on that date
                        System.out.println(String.format("   [FAIL] %-12s | %-8s | %-4s | %-4s | %s",
                                polId, "N/A", "N/A", "N/A", validationErrors));
                        failCount++;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("   -----------------------------------------------------------------------------------------");
        System.out.println("\n==================================================");
        System.out.println("Ticket 9 Validation Summary");
        System.out.println("Policies Checked : " + csvPolicies.size());
        System.out.println("Records Passed   : " + passCount);
        System.out.println("Records Failed   : " + failCount);
        System.out.println("==================================================");
    }

    // Helper method to dynamically find CSV columns
    private static int getColumnIndex(String headerLine, String colName) {
        if (headerLine == null) return -1;
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            // Trim quotes and whitespace just in case
            String cleanHeader = headers[i].replace("\"", "").trim();
            if (cleanHeader.equalsIgnoreCase(colName)) return i;
        }
        return -1;
    }
}