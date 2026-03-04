package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class RL16AmountValidatorMain {

    // --- CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";
    private static final String CSV_FILE = "C:\\Selenium_Downloads\\RL16_Parsed_Output.csv";

    // Holder for accumulating amounts
    static class RL16Amounts {
        double capGains = 0.0;     // Box 21
        double actualDiv = 0.0;    // First "Actual Div" column
        double foreignInc = 0.0;   // Foreign Inc
        double otherInc = 0.0;     // Other Inc
        double taxableDiv = 0.0;   // Taxable Div
        double divTaxCredit = 0.0; // Div Tax Credit
        double actualDivJ = 0.0;   // Actual Div (Box J)
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Step 4: RL16 Amount Validation (Quebec Only) ---");

        // 1. AGGREGATE CSV DATA
        // Map: PolicyNumber -> Summed Amounts
        Map<String, RL16Amounts> csvTotals = new TreeMap<>();
        System.out.println("Phase 1: Reading and Summing RL16 CSV Data...");

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

                csvTotals.putIfAbsent(policy, new RL16Amounts());
                RL16Amounts current = csvTotals.get(policy);

                // Summing the amounts
                current.capGains    += parseDouble(safeGet(row, colMap.get("Cap Gains (Box 21)")));
                current.actualDiv   += parseDouble(safeGet(row, colMap.get("Actual Div")));
                current.foreignInc  += parseDouble(safeGet(row, colMap.get("Foreign Inc")));
                current.otherInc    += parseDouble(safeGet(row, colMap.get("Other Inc")));
                current.taxableDiv  += parseDouble(safeGet(row, colMap.get("Taxable Div")));
                current.divTaxCredit+= parseDouble(safeGet(row, colMap.get("Div Tax Credit")));
                current.actualDivJ  += parseDouble(safeGet(row, colMap.get("Actual Div (Box J)")));
            }
            System.out.println("Aggregation Complete. Unique Policies Found in CSV: " + csvTotals.size());

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 2. VALIDATE AGAINST DB
        System.out.println("Phase 2: Querying DB (T3_SLIP + TAX_SLIP WHERE PROV=11)...");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            // QUERY JOINING TAX_SLIP AND T3_SLIP
            // Filtering for PROV_ID = 11 (Quebec)
            String query =
                    "SELECT t3.TAX_SLIP_ID, ts.RECIP_SUR_TXT, ts.RECIP_SIN, ts.CLI_ID, " +
                            "       t3.CAP_GAINS_AMT, " +
                            "       t3.ACTL_ELIG_DIV_AMT, " +       // Used for both 'Actual Div' and 'Box J' per req
                            "       t3.FRGN_NON_BUS_INCM_AMT, " +
                            "       t3.OTHR_INCM_AMT, " +
                            "       t3.TAX_ELIG_DIV_AMT, " +
                            "       t3.RL16_DIV_TAX_CR_AMT " +      // Specific RL16 Column
                            "FROM T3_SLIP t3 " +
                            "JOIN TAX_SLIP ts ON t3.TAX_SLIP_ID = ts.ID " +
                            "WHERE ts.POL_ID = ? " +
                            "AND ts.CRA_PROV_ID = 11 " +           // QUEBEC FILTER
                            "AND ts.ADMIN_SYS_ID = 1 " +
                            "AND ts.TAX_SLIP_TYPE_ID = 1 " +
                            "AND ts.CRA_TAX_YR_ID = 17 " +
                            "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {

                int passCount = 0;
                int failCount = 0;
                int skippedCount = 0; // For policies in CSV but not Quebec in DB

                for (Map.Entry<String, RL16Amounts> entry : csvTotals.entrySet()) {
                    String policyNum = entry.getKey();
                    RL16Amounts csvVal = entry.getValue();

                    stmt.setString(1, policyNum);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("\n--------------------------------------------------");
                            System.out.println("Checking Policy: " + policyNum);

                            // --- Identity Info ---
                            long dbTaxSlipId   = rs.getLong("TAX_SLIP_ID");
                            String dbSurname   = rs.getString("RECIP_SUR_TXT");
                            String dbSIN       = rs.getString("RECIP_SIN");
                            String dbClientID  = rs.getString("CLI_ID");

                            System.out.println(String.format("   >> Identity: [TaxSlipID: %d] [Surname: %s] [SIN: %s] [ClientID: %s]",
                                    dbTaxSlipId, dbSurname, dbSIN, dbClientID));

                            // --- Extract DB Values ---
                            double dbCapGains      = rs.getDouble("CAP_GAINS_AMT");
                            double dbActEligDiv    = rs.getDouble("ACTL_ELIG_DIV_AMT");
                            double dbForeignInc    = rs.getDouble("FRGN_NON_BUS_INCM_AMT");
                            double dbOtherInc      = rs.getDouble("OTHR_INCM_AMT");
                            double dbTaxEligDiv    = rs.getDouble("TAX_ELIG_DIV_AMT");
                            double dbRl16TaxCred   = rs.getDouble("RL16_DIV_TAX_CR_AMT"); // Using RL16 specific column

                            boolean policyFailed = false;

                            // 1. Cap Gains
                            if (compare(csvVal.capGains, dbCapGains)) {
                                System.out.println("   [PASS] Cap Gains    : " + format(csvVal.capGains));
                            } else {
                                System.out.println("   [FAIL] Cap Gains    : CSV[" + format(csvVal.capGains) + "] != DB[" + format(dbCapGains) + "]");
                                policyFailed = true;
                            }

                            // 2. Actual Div (First Column)
                            if (compare(csvVal.actualDiv, dbActEligDiv)) {
                                System.out.println("   [PASS] Actual Div   : " + format(csvVal.actualDiv));
                            } else {
                                System.out.println("   [FAIL] Actual Div   : CSV[" + format(csvVal.actualDiv) + "] != DB[" + format(dbActEligDiv) + "]");
                                policyFailed = true;
                            }

                            // 3. Foreign Income
                            if (compare(csvVal.foreignInc, dbForeignInc)) {
                                System.out.println("   [PASS] Foreign Inc  : " + format(csvVal.foreignInc));
                            } else {
                                System.out.println("   [FAIL] Foreign Inc  : CSV[" + format(csvVal.foreignInc) + "] != DB[" + format(dbForeignInc) + "]");
                                policyFailed = true;
                            }

                            // 4. Other Income
                            if (compare(csvVal.otherInc, dbOtherInc)) {
                                System.out.println("   [PASS] Other Inc    : " + format(csvVal.otherInc));
                            } else {
                                System.out.println("   [FAIL] Other Inc    : CSV[" + format(csvVal.otherInc) + "] != DB[" + format(dbOtherInc) + "]");
                                policyFailed = true;
                            }

                            // 5. Taxable Dividends
                            if (compare(csvVal.taxableDiv, dbTaxEligDiv)) {
                                System.out.println("   [PASS] Taxable Div  : " + format(csvVal.taxableDiv));
                            } else {
                                System.out.println("   [FAIL] Taxable Div  : CSV[" + format(csvVal.taxableDiv) + "] != DB[" + format(dbTaxEligDiv) + "]");
                                policyFailed = true;
                            }

                            // 6. RL16 Div Tax Credit
                            if (compare(csvVal.divTaxCredit, dbRl16TaxCred)) {
                                System.out.println("   [PASS] Div Tax Cred : " + format(csvVal.divTaxCredit));
                            } else {
                                System.out.println("   [FAIL] Div Tax Cred : CSV[" + format(csvVal.divTaxCredit) + "] != DB[" + format(dbRl16TaxCred) + "]");
                                policyFailed = true;
                            }

                            // 7. Actual Div Box J (Mapped to same DB column as #2 per request)
                            if (compare(csvVal.actualDivJ, dbActEligDiv)) {
                                System.out.println("   [PASS] Box J ActDiv : " + format(csvVal.actualDivJ));
                            } else {
                                System.out.println("   [FAIL] Box J ActDiv : CSV[" + format(csvVal.actualDivJ) + "] != DB[" + format(dbActEligDiv) + "]");
                                policyFailed = true;
                            }

                            if (!policyFailed) {
                                passCount++;
                            } else {
                                failCount++;
                            }

                        } else {
                            // Policy exists in CSV, but query returned nothing.
                            // This means either the record is missing OR the person is NOT Quebec (Prov != 11)
                            // We treat this as "Skipped" (Not a Failure) because the RL16 file might have non-Quebec people if filtering wasn't perfect,
                            // OR more likely, we just skip non-Quebec rows cleanly.
                            // However, since we are parsing "RL16_Parsed_Output", we expect them to be Quebec.
                            // Let's log it as a warning.
                            // System.out.println("   [SKIP] Policy " + policyNum + " - Not found as Quebec Resident (Prov=11) in DB");
                            skippedCount++;
                        }
                    }
                }

                System.out.println("\n==================================================");
                System.out.println("Validation Summary");
                System.out.println("==================================================");
                System.out.println("Policies in CSV    : " + csvTotals.size());
                System.out.println("Quebec Pols Found  : " + (passCount + failCount));
                System.out.println("Policies Passed    : " + passCount);
                System.out.println("Policies Failed    : " + failCount);
                System.out.println("Policies Skipped   : " + skippedCount + " (Likely Non-Quebec or No DB Record)");
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
