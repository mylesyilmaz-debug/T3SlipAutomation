package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class FundValidatorMain {

    // --- CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";

    // We only need the T3 CSV now
    private static final String T3_CSV = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";

    public static void main(String[] args) {
        System.out.println("--- Starting Step 5: Fund Level Validation (T3 Validation + RL16 Display) ---");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(T3_CSV), "ISO-8859-1"))) {

            System.out.println("Database Connected!");

            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim(), i);
            }

            int rowCount = 0;
            int passCount = 0;
            int failCount = 0;
            String line;

            // QUERY: Fetches T3 Data + RL16 Columns (for display)
            String query =
                    "SELECT ts.POL_ID, ts.RECIP_SIN, ts.RECIP_SUR_TXT, ts.CLI_ID, " +
                            "       tf.TAX_SLIP_ID, tf.TRUST_ID, tr.TRUST_ACCT_NUM, td.TRUST_DESC_TXT, " +
                            "       tf.CAP_GAINS_AMT, " +
                            "       tf.CAP_LOSS_AMT, " +
                            "       tf.ACTL_ELIG_DIV_AMT, " +
                            "       tf.TAX_ELIG_DIV_AMT, " +
                            "       tf.DIV_TAX_CR_ELIG_DIV_AMT, " +
                            "       tf.OTHR_INCM_AMT, " +
                            "       tf.FRGN_NON_BUS_INCM_AMT, " +
                            "       tf.RL16_DIV_TAX_CR_AMT, " +
                            "       tf.RL16_TAX_DIV_AMT " +
                            "FROM TAX_SLIP ts " +
                            "JOIN T3_FUND tf ON ts.ID = tf.TAX_SLIP_ID " +
                            "JOIN TRUST tr ON tf.TRUST_ID = tr.ID " +
                            "JOIN TRUST_DESCRIPTION td ON tr.ID = td.TRUST_ID " +
                            "WHERE ts.POL_ID = ? " +
                            "AND tr.TRUST_ACCT_NUM = ? " +
                            "AND td.LANG_ID = 1 " +
                            "AND ts.ADMIN_SYS_ID = 1 " +
                            "AND ts.TAX_SLIP_TYPE_ID = 1 " +
                            "AND ts.CRA_TAX_YR_ID = 17 " +
                            "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {

                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",", -1);

                    String policyNum = safeGet(row, colMap.get("Policy Num"));
                    String trustAcct = safeGet(row, colMap.get("Trust Account"));

                    if (policyNum.isEmpty() || trustAcct.isEmpty()) continue;

                    rowCount++;
                    System.out.println("\n--------------------------------------------------");
                    System.out.println("Checking Row " + rowCount + ": Policy [" + policyNum + "] Fund [" + trustAcct + "]");

                    stmt.setString(1, policyNum);
                    stmt.setString(2, trustAcct);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // --- IDENTITY INFO ---
                            long dbSlipId      = rs.getLong("TAX_SLIP_ID");
                            long dbTrustId     = rs.getLong("TRUST_ID");
                            String dbDesc      = rs.getString("TRUST_DESC_TXT");
                            String dbSurname   = rs.getString("RECIP_SUR_TXT");
                            String dbSin       = rs.getString("RECIP_SIN");
                            String dbClient    = rs.getString("CLI_ID");

                            System.out.println(String.format("   >> DB Info: [SlipID: %d] [TrustID: %d] [Desc: %s]", dbSlipId, dbTrustId, dbDesc));
                            System.out.println(String.format("   >> Recipient: [Surname: %s] [SIN: %s] [Client: %s]", dbSurname, dbSin, dbClient));

                            // --- T3 VALIDATION (PASS/FAIL) ---
                            boolean rowPassed = true;

                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Capital Gains (21)"))), rs.getDouble("CAP_GAINS_AMT"), "CAP_GAINS_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Capital Losses (37)"))), rs.getDouble("CAP_LOSS_AMT"), "CAP_LOSS_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Actual Div (49/C1)"))), rs.getDouble("ACTL_ELIG_DIV_AMT"), "ACTL_ELIG_DIV_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Taxable Div (50)"))), rs.getDouble("TAX_ELIG_DIV_AMT"), "TAX_ELIG_DIV_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Div Tax Credit (51)"))), rs.getDouble("DIV_TAX_CR_ELIG_DIV_AMT"), "DIV_TAX_CR_ELIG_DIV_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Other Income (26/G)"))), rs.getDouble("OTHR_INCM_AMT"), "OTHR_INCM_AMT");
                            rowPassed &= check(parseDouble(safeGet(row, colMap.get("Foreign Income (25/F)"))), rs.getDouble("FRGN_NON_BUS_INCM_AMT"), "FRGN_NON_BUS_INCM_AMT");

                            // --- RL16 DISPLAY (INFO ONLY) ---
                            double dbRl16Cred = rs.getDouble("RL16_DIV_TAX_CR_AMT");
                            double dbRl16Tax  = rs.getDouble("RL16_TAX_DIV_AMT");

                            if (dbRl16Cred > 0 || dbRl16Tax > 0) {
                                System.out.println(String.format("                RL16_DIV_TAX_CR_AMT : %.2f", dbRl16Cred));
                                System.out.println(String.format("                RL16_TAX_DIV_AMT    : %.2f", dbRl16Tax));
                            }

                            if (rowPassed) {
                                passCount++;
                            } else {
                                failCount++;
                            }

                        } else {
                            System.out.println("   >> RESULT: [FAIL] No DB Record found for Policy/Trust");
                            failCount++;
                        }
                    }
                }
            }

            System.out.println("\n--- Final Summary ---");
            System.out.println("Total Rows: " + rowCount);
            System.out.println("Passed: " + passCount);
            System.out.println("Failed: " + failCount);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HELPER METHODS ---

    private static boolean check(double csvVal, double dbVal, String label) {
        if (Math.abs(csvVal - dbVal) < 0.02) {
            System.out.println(String.format("   [PASS] %-23s : %8.2f", label, csvVal));
            return true;
        } else {
            System.out.println(String.format("   [FAIL] %-23s : CSV[%8.2f] != DB[%8.2f]", label, csvVal, dbVal));
            return false;
        }
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