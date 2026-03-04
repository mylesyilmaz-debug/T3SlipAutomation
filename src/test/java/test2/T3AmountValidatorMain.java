package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class T3AmountValidatorMain {

    // --- CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";
    private static final String CSV_FILE = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";

    // Holder for accumulating amounts
    static class T3Amounts {
        double capGains = 0.0;     // Box 21
        double otherIncome = 0.0;  // Box 26
        double foreignInc = 0.0;   // Box 25
        double capLoss = 0.0;      // Box 37 (NEW)
        double actEligDiv = 0.0;   // Box 49 (Updated to Eligible)
        double taxEligDiv = 0.0;   // Box 50 (Updated to Eligible)
        double divTaxCredElig = 0.0;// Box 51 (Updated to Eligible)
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Step 3: T3 Amount Validation (Corrected Mapping) ---");

        // 1. AGGREGATE CSV DATA
        Map<String, T3Amounts> csvTotals = new TreeMap<>();
        System.out.println("Phase 1: Reading and Summing CSV Data...");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(CSV_FILE), "ISO-8859-1"))) {
            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                String policy = safeGet(row, colMap.get("Policy Num"));

                if (policy.isEmpty()) continue;

                csvTotals.putIfAbsent(policy, new T3Amounts());
                T3Amounts current = csvTotals.get(policy);

                // Add this row's amounts to the running total for that Policy
                current.capGains      += parseDouble(safeGet(row, colMap.get("Capital Gains (21)")));
                current.otherIncome   += parseDouble(safeGet(row, colMap.get("Other Income (26/G)")));
                current.foreignInc    += parseDouble(safeGet(row, colMap.get("Foreign Income (25/F)")));
                current.capLoss       += parseDouble(safeGet(row, colMap.get("Capital Losses (37)"))); // New Field
                current.actEligDiv    += parseDouble(safeGet(row, colMap.get("Actual Div (49/C1)")));
                current.taxEligDiv    += parseDouble(safeGet(row, colMap.get("Taxable Div (50)")));
                current.divTaxCredElig+= parseDouble(safeGet(row, colMap.get("Div Tax Credit (51)")));
            }
            System.out.println("Aggregation Complete. Unique Policies Found: " + csvTotals.size());

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 2. VALIDATE AGAINST DB
        System.out.println("Phase 2: Querying DB and Validating...");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            // UPDATED QUERY: Fetching the _ELIG_ columns and CAP_LOSS_AMT
            String query =
                    "SELECT t3.TAX_SLIP_ID, ts.RECIP_SUR_TXT, ts.RECIP_SIN, ts.CLI_ID, " +
                            "       t3.CAP_GAINS_AMT, t3.OTHR_INCM_AMT, t3.FRGN_NON_BUS_INCM_AMT, " +
                            "       t3.CAP_LOSS_AMT, " +                // NEW
                            "       t3.ACTL_ELIG_DIV_AMT, " +           // UPDATED
                            "       t3.TAX_ELIG_DIV_AMT, " +            // UPDATED
                            "       t3.DIV_TAX_CR_ELIG_DIV_AMT " +      // UPDATED
                            "FROM T3_SLIP t3 " +
                            "JOIN TAX_SLIP ts ON t3.TAX_SLIP_ID = ts.ID " +
                            "WHERE ts.POL_ID = ? " +
                            "AND ts.ADMIN_SYS_ID = 1 " +
                            "AND ts.TAX_SLIP_TYPE_ID = 1 " +
                            "AND ts.CRA_TAX_YR_ID = 17 " +
                            "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {

                int passCount = 0;
                int failCount = 0;

                for (Map.Entry<String, T3Amounts> entry : csvTotals.entrySet()) {
                    String policyNum = entry.getKey();
                    T3Amounts csvVal = entry.getValue();

                    System.out.println("\n--------------------------------------------------");
                    System.out.println("Checking Policy: " + policyNum);

                    stmt.setString(1, policyNum);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // --- Identity Info ---
                            long dbTaxSlipId   = rs.getLong("TAX_SLIP_ID");
                            String dbSurname   = rs.getString("RECIP_SUR_TXT");
                            String dbSIN       = rs.getString("RECIP_SIN");
                            String dbClientID  = rs.getString("CLI_ID");

                            System.out.println(String.format("   >> Identity: [TaxSlipID: %d] [Surname: %s] [SIN: %s] [ClientID: %s]",
                                    dbTaxSlipId, dbSurname, dbSIN, dbClientID));

                            // --- Extract DB Values (Using New Columns) ---
                            double dbCapGains       = rs.getDouble("CAP_GAINS_AMT");
                            double dbOtherInc       = rs.getDouble("OTHR_INCM_AMT");
                            double dbForeignInc     = rs.getDouble("FRGN_NON_BUS_INCM_AMT");
                            double dbCapLoss        = rs.getDouble("CAP_LOSS_AMT");              // New
                            double dbActEligDiv     = rs.getDouble("ACTL_ELIG_DIV_AMT");         // New Mapping
                            double dbTaxEligDiv     = rs.getDouble("TAX_ELIG_DIV_AMT");          // New Mapping
                            double dbDivCredElig    = rs.getDouble("DIV_TAX_CR_ELIG_DIV_AMT");   // New Mapping

                            boolean policyFailed = false;

                            // 1. Cap Gains
                            if (compare(csvVal.capGains, dbCapGains)) {
                                System.out.println("   [PASS] Cap Gains  : " + format(csvVal.capGains));
                            } else {
                                System.out.println("   [FAIL] Cap Gains  : CSV[" + format(csvVal.capGains) + "] != DB[" + format(dbCapGains) + "]");
                                policyFailed = true;
                            }

                            // 2. Other Income
                            if (compare(csvVal.otherIncome, dbOtherInc)) {
                                System.out.println("   [PASS] Other Inc  : " + format(csvVal.otherIncome));
                            } else {
                                System.out.println("   [FAIL] Other Inc  : CSV[" + format(csvVal.otherIncome) + "] != DB[" + format(dbOtherInc) + "]");
                                policyFailed = true;
                            }

                            // 3. Foreign Income
                            if (compare(csvVal.foreignInc, dbForeignInc)) {
                                System.out.println("   [PASS] Foreign Inc: " + format(csvVal.foreignInc));
                            } else {
                                System.out.println("   [FAIL] Foreign Inc: CSV[" + format(csvVal.foreignInc) + "] != DB[" + format(dbForeignInc) + "]");
                                policyFailed = true;
                            }

                            // 4. Capital Losses (NEW)
                            if (compare(csvVal.capLoss, dbCapLoss)) {
                                System.out.println("   [PASS] Cap Losses : " + format(csvVal.capLoss));
                            } else {
                                System.out.println("   [FAIL] Cap Losses : CSV[" + format(csvVal.capLoss) + "] != DB[" + format(dbCapLoss) + "]");
                                policyFailed = true;
                            }

                            // 5. Actual Eligible Dividends (Updated Mapping)
                            if (compare(csvVal.actEligDiv, dbActEligDiv)) {
                                System.out.println("   [PASS] Act EligDiv: " + format(csvVal.actEligDiv));
                            } else {
                                System.out.println("   [FAIL] Act EligDiv: CSV[" + format(csvVal.actEligDiv) + "] != DB[" + format(dbActEligDiv) + "]");
                                policyFailed = true;
                            }

                            // 6. Taxable Eligible Dividends (Updated Mapping)
                            if (compare(csvVal.taxEligDiv, dbTaxEligDiv)) {
                                System.out.println("   [PASS] Tax EligDiv: " + format(csvVal.taxEligDiv));
                            } else {
                                System.out.println("   [FAIL] Tax EligDiv: CSV[" + format(csvVal.taxEligDiv) + "] != DB[" + format(dbTaxEligDiv) + "]");
                                policyFailed = true;
                            }

                            // 7. Div Tax Credit Eligible (Updated Mapping)
                            if (compare(csvVal.divTaxCredElig, dbDivCredElig)) {
                                System.out.println("   [PASS] Tax CredElg: " + format(csvVal.divTaxCredElig));
                            } else {
                                System.out.println("   [FAIL] Tax CredElg: CSV[" + format(csvVal.divTaxCredElig) + "] != DB[" + format(dbDivCredElig) + "]");
                                policyFailed = true;
                            }

                            if (!policyFailed) {
                                passCount++;
                            } else {
                                failCount++;
                            }

                        } else {
                            System.out.println("   [FAIL] No Record Found in DB (Filters: Year=17, Date=2025-12-10)");
                            failCount++;
                        }
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("Validation Summary");
                System.out.println("==================================================");
                System.out.println("Policies Passed: " + passCount);
                System.out.println("Policies Failed: " + failCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- UTILITIES ---

    private static boolean compare(double csv, double db) {
        return Math.abs(csv - db) < 0.02;
    }

    private static String format(double val) {
        return String.format("%.2f", val);
    }

    private static double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    private static String safeGet(String[] row, Integer index) {
        if (index == null || index >= row.length) return "";
        return row[index].trim();
    }
}
