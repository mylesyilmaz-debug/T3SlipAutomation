package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class LookupValidatorMain {

    // --- CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";
    private static final String CSV_PATH = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv"; // Update if needed
    private static final String LOAD_DATE = "2025-12-10"; // The specific date of your ETL load

    public static void main(String[] args) {
        System.out.println("--- Starting Story 8: Supporting Lookup Tables Validation ---");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.println("Database Connected!\n");

            // 1. Extract both Trust Accounts and Policies from the CSV
            Set<String> csvTrusts = new HashSet<>();
            Set<String> csvPolicies = new HashSet<>();
            extractCsvData(CSV_PATH, csvTrusts, csvPolicies);

            if (csvTrusts.isEmpty() || csvPolicies.isEmpty()) {
                System.out.println("[ERROR] Failed to extract data from CSV. Halting validation.");
                return;
            }

            System.out.println("   Extracted " + csvTrusts.size() + " Trust Accounts and " + csvPolicies.size() + " Policies from CSV.\n");

            // 2. Execute Validations Separately
            boolean trustsPassed = validateTrustLookups(conn, csvTrusts);
            boolean slipTypesPassed = validateTaxSlipTypes(conn, csvPolicies);
            boolean adminSysPassed = validateAdminSystems(conn, csvPolicies);

            System.out.println("\n==================================================");
            System.out.println("Story 8 Validation Summary");
            System.out.println("TRUST & DESC DICTIONARY  : " + (trustsPassed ? "PASS" : "FAIL"));
            System.out.println("TAX_SLIP_TYPE DICTIONARY : " + (slipTypesPassed ? "PASS" : "FAIL"));
            System.out.println("ADMIN_SYSTEM DICTIONARY  : " + (adminSysPassed ? "PASS" : "FAIL"));
            System.out.println("==================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- DATA EXTRACTION ---
    private static void extractCsvData(String csvPath, Set<String> trusts, Set<String> policies) {
        System.out.println(">>> Parsing input CSV file...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "ISO-8859-1"))) {
            String headerLine = reader.readLine();
            int trustCol = getColumnIndex(headerLine, "Trust Account");
            int polCol = getColumnIndex(headerLine, "Policy Num");

            if (trustCol == -1 || polCol == -1) {
                System.out.println("[ERROR] Missing required columns in CSV header.");
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length > Math.max(trustCol, polCol)) {
                    String trust = row[trustCol].trim();
                    String pol = row[polCol].trim();

                    if (!trust.isEmpty()) trusts.add(trust);
                    if (!pol.isEmpty()) policies.add(pol);
                }
            }
        } catch (Exception e) {
            System.err.println("CSV Read Error: " + e.getMessage());
        }
    }

    // --- SCENARIO 1: TRUST LOOKUPS ---
    private static boolean validateTrustLookups(Connection conn, Set<String> csvTrusts) {
        System.out.println(">>> Validating TRUST and TRUST_DESCRIPTION mappings...");
        boolean passed = true;

        String query =
                "SELECT t.ID, td.TRUST_DESC_TXT " +
                        "FROM TRUST t " +
                        "LEFT JOIN TRUST_DESCRIPTION td ON t.ID = td.TRUST_ID AND td.LANG_ID = 1 " +
                        "WHERE t.TRUST_ACCT_NUM = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            System.out.println(String.format("   %-15s | %-8s | %s", "Trust Acct No", "DB ID", "DB Description"));
            System.out.println("   --------------------------------------------------------------");

            for (String trustAcct : csvTrusts) {
                stmt.setString(1, trustAcct);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long dbId = rs.getLong("ID");
                        String desc = rs.getString("TRUST_DESC_TXT");

                        if (desc == null || desc.trim().isEmpty()) {
                            System.out.println(String.format("   [FAIL] %-15s | %-8d | MISSING DESCRIPTION", trustAcct, dbId));
                            passed = false;
                        } else {
                            System.out.println(String.format("   [PASS] %-15s | %-8d | %s", trustAcct, dbId, desc));
                        }
                    } else {
                        System.out.println(String.format("   [FAIL] %-15s | %-8s | ACCOUNT NOT FOUND IN DB", trustAcct, "NULL"));
                        passed = false;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return passed;
    }

    // --- SCENARIO 2: TAX_SLIP_TYPE LOOKUPS ---
    private static boolean validateTaxSlipTypes(Connection conn, Set<String> csvPolicies) {
        System.out.println("\n>>> Validating TAX_SLIP_TYPE (Expect ID=1) for all " + csvPolicies.size() + " policies...");
        boolean passed = true;
        int successCount = 0;

        // Added Date Filter and JOIN to dictionary table
        String query =
                "SELECT ts.TAX_SLIP_TYPE_ID, tst.TAX_SLIP_TYPE_CD " +
                        "FROM TAX_SLIP ts " +
                        "LEFT JOIN TAX_SLIP_TYPE tst ON ts.TAX_SLIP_TYPE_ID = tst.ID " +
                        "WHERE ts.POL_ID = ? AND ts.CRA_TAX_YR_ID = 17 " +
                        "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            System.out.println(String.format("   %-12s | %-12s | %-15s | %s", "Policy Num", "DB Type ID", "DB Type Code", "Result"));
            System.out.println("   -------------------------------------------------------------------------");

            for (String polId : csvPolicies) {
                stmt.setString(1, polId);
                stmt.setString(2, LOAD_DATE);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean foundRecord = false;
                    while (rs.next()) {
                        foundRecord = true;
                        int slipTypeId = rs.getInt("TAX_SLIP_TYPE_ID");
                        String slipTypeCode = rs.getString("TAX_SLIP_TYPE_CD");

                        if (slipTypeId == 1) {
                            System.out.println(String.format("   [PASS] %-12s | %-12d | %-15s | Valid T3_RL16 Match", polId, slipTypeId, slipTypeCode));
                            successCount++;
                        } else {
                            System.out.println(String.format("   [FAIL] %-12s | %-12d | %-15s | Invalid Lookup ID", polId, slipTypeId, slipTypeCode));
                            passed = false;
                        }
                    }

                    if (!foundRecord) {
                        System.out.println(String.format("   [FAIL] %-12s | %-12s | %-15s | No DB Record Found on Date", polId, "N/A", "N/A"));
                        passed = false;
                    }
                }
            }
            System.out.println("   -------------------------------------------------------------------------");
            System.out.println("   TAX_SLIP_TYPE Checked -> Valid Records: " + successCount);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return passed;
    }

    // --- SCENARIO 3: ADMIN_SYSTEM LOOKUPS ---
    private static boolean validateAdminSystems(Connection conn, Set<String> csvPolicies) {
        System.out.println("\n>>> Validating ADMIN_SYSTEM (Expect ID=1) for all " + csvPolicies.size() + " policies...");
        boolean passed = true;
        int successCount = 0;

        // Added Date Filter and JOIN to dictionary table
        String query =
                "SELECT ts.ADMIN_SYS_ID, sys.ADMIN_SYS_CD " +
                        "FROM TAX_SLIP ts " +
                        "LEFT JOIN ADMIN_SYSTEM sys ON ts.ADMIN_SYS_ID = sys.ID " +
                        "WHERE ts.POL_ID = ? AND ts.CRA_TAX_YR_ID = 17 " +
                        "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            System.out.println(String.format("   %-12s | %-12s | %-15s | %s", "Policy Num", "DB Sys ID", "DB Sys Code", "Result"));
            System.out.println("   -------------------------------------------------------------------------");

            for (String polId : csvPolicies) {
                stmt.setString(1, polId);
                stmt.setString(2, LOAD_DATE);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean foundRecord = false;
                    while (rs.next()) {
                        foundRecord = true;
                        int adminSysId = rs.getInt("ADMIN_SYS_ID");
                        String adminSysCode = rs.getString("ADMIN_SYS_CD");

                        if (adminSysId == 1) {
                            System.out.println(String.format("   [PASS] %-12s | %-12d | %-15s | Valid Ingenium Match", polId, adminSysId, adminSysCode));
                            successCount++;
                        } else {
                            System.out.println(String.format("   [FAIL] %-12s | %-12d | %-15s | Invalid Lookup ID", polId, adminSysId, adminSysCode));
                            passed = false;
                        }
                    }

                    if (!foundRecord) {
                        System.out.println(String.format("   [FAIL] %-12s | %-12s | %-15s | No DB Record Found on Date", polId, "N/A", "N/A"));
                        passed = false;
                    }
                }
            }
            System.out.println("   -------------------------------------------------------------------------");
            System.out.println("   ADMIN_SYSTEM Checked -> Valid Records: " + successCount);

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return passed;
    }

    // --- HELPER METHOD ---
    private static int getColumnIndex(String headerLine, String colName) {
        if (headerLine == null) return -1;
        String[] headers = headerLine.split(",");
        for (int i = 0; i < headers.length; i++) {
            String cleanHeader = headers[i].replace("\"", "").trim();
            if (cleanHeader.equalsIgnoreCase(colName)) return i;
        }
        return -1;
    }
}