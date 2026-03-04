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

public class LookupValidator {

    private DatabaseManager dbManager;
    private Set<String> csvTrusts = new HashSet<>();
    private Set<String> csvPolicies = new HashSet<>();
    private boolean isDataLoaded = false;

    // Constructor Injection
    public LookupValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- 1. CSV DATA EXTRACTION ---
    public void loadCsvDataIfNeeded(String csvPath) {
        if (isDataLoaded) return; // Only load once per test scenario

        System.out.println(">>> Parsing input CSV file for Lookup Validation...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "ISO-8859-1"))) {
            String headerLine = reader.readLine();
            int trustCol = getColumnIndex(headerLine, "Trust Account");
            int polCol = getColumnIndex(headerLine, "Policy Num");

            Assert.assertTrue("Missing required columns in CSV header.", trustCol != -1 && polCol != -1);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length > Math.max(trustCol, polCol)) {
                    String trust = row[trustCol].trim();
                    String pol = row[polCol].trim();

                    if (!trust.isEmpty()) csvTrusts.add(trust);
                    if (!pol.isEmpty()) csvPolicies.add(pol);
                }
            }
            isDataLoaded = true;
            System.out.println("   Extracted " + csvTrusts.size() + " Trust Accounts and " + csvPolicies.size() + " Policies.");
        } catch (Exception e) {
            Assert.fail("CSV Read Error: " + e.getMessage());
        }
    }

    // --- 2. TRUST LOOKUP VALIDATION ---
    public void validateTrustAccounts(String csvPath) {
        loadCsvDataIfNeeded(csvPath);
        System.out.println(">>> Validating TRUST and TRUST_DESCRIPTION mappings...");
        int failCount = 0;
        Connection conn = dbManager.getConnection();

        String query =
                "SELECT t.ID, td.TRUST_DESC_TXT " +
                        "FROM TRUST t " +
                        "LEFT JOIN TRUST_DESCRIPTION td ON t.ID = td.TRUST_ID AND td.LANG_ID = 1 " +
                        "WHERE t.TRUST_ACCT_NUM = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (String trustAcct : csvTrusts) {
                stmt.setString(1, trustAcct);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long dbId = rs.getLong("ID");
                        String desc = rs.getString("TRUST_DESC_TXT");

                        if (desc == null || desc.trim().isEmpty()) {
                            System.out.println(String.format("   [FAIL] %-15s | %-8d | MISSING DESCRIPTION", trustAcct, dbId));
                            failCount++;
                        } else {
                            System.out.println(String.format("   [PASS] %-15s | %-8d | %s", trustAcct, dbId, desc));
                        }
                    } else {
                        System.out.println(String.format("   [FAIL] %-15s | %-8s | ACCOUNT NOT FOUND IN DB", trustAcct, "NULL"));
                        failCount++;
                    }
                }
            }
        } catch (SQLException e) {
            Assert.fail("SQL Exception during Trust validation: " + e.getMessage());
        }

        Assert.assertEquals("Trust Lookup validation failed for " + failCount + " accounts!", 0, failCount);
    }

    // --- 3. POLICY LOOKUPS (SLIP TYPE & ADMIN SYSTEM) ---
    public void validatePolicyLookups(String csvPath, String loadDate) {
        loadCsvDataIfNeeded(csvPath);
        System.out.println("\n>>> Validating TAX_SLIP_TYPE and ADMIN_SYSTEM for " + csvPolicies.size() + " policies...");
        int typeFailures = validateLookupID(csvPolicies, loadDate,
                "TAX_SLIP_TYPE_ID", "TAX_SLIP_TYPE_CD", "TAX_SLIP_TYPE", 1);
        int sysFailures = validateLookupID(csvPolicies, loadDate,
                "ADMIN_SYS_ID", "ADMIN_SYS_CD", "ADMIN_SYSTEM", 1);

        Assert.assertEquals("TAX_SLIP_TYPE validation failed!", 0, typeFailures);
        Assert.assertEquals("ADMIN_SYSTEM validation failed!", 0, sysFailures);
    }

    // --- HELPER: GENERIC DB CHECK FOR POLICY LOOKUPS ---
    private int validateLookupID(Set<String> policies, String loadDate, String idCol, String codeCol, String joinTable, int expectedId) {
        int failCount = 0;
        Connection conn = dbManager.getConnection();

        String query = "SELECT ts." + idCol + ", dict." + codeCol + " " +
                "FROM TAX_SLIP ts " +
                "LEFT JOIN " + joinTable + " dict ON ts." + idCol + " = dict.ID " +
                "WHERE ts.POL_ID = ? AND ts.CRA_TAX_YR_ID = 17 " +
                "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (String polId : policies) {
                stmt.setString(1, polId);
                stmt.setString(2, loadDate);

                try (ResultSet rs = stmt.executeQuery()) {
                    boolean foundRecord = false;
                    while (rs.next()) {
                        foundRecord = true;
                        int actualId = rs.getInt(idCol);
                        String actualCode = rs.getString(codeCol);

                        if (actualId == expectedId) {
                            System.out.println(String.format("   [PASS] %-12s | %s: %-2d | %s", polId, idCol, actualId, actualCode));
                        } else {
                            System.out.println(String.format("   [FAIL] %-12s | %s: %-2d | %s (Expected %d)", polId, idCol, actualId, actualCode, expectedId));
                            failCount++;
                        }
                    }
                    if (!foundRecord) {
                        System.out.println(String.format("   [FAIL] %-12s | %s | No DB Record Found on Date", polId, idCol));
                        failCount++;
                    }
                }
            }
        } catch (SQLException e) {
            Assert.fail("SQL Exception: " + e.getMessage());
        }
        return failCount;
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
