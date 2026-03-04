package ca.empire.validators;

import ca.empire.util.DatabaseManager;
import java.io.*;
import java.sql.*;
import java.util.*;

public class FundValidator {

    private DatabaseManager dbManager;

    public FundValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void validateFunds(String t3CsvPath) {
        System.out.println("--- Starting Fund Level Validation (Detailed) ---");

        // UPDATED QUERY: Now includes Identity columns and Trust Description
        String query =
                "SELECT ts.POL_ID, ts.RECIP_SIN, ts.RECIP_SUR_TXT, ts.CLI_ID, " +
                        "       tf.TAX_SLIP_ID, tf.TRUST_ID, tr.TRUST_ACCT_NUM, td.TRUST_DESC_TXT, " +
                        "       tf.CAP_GAINS_AMT, tf.CAP_LOSS_AMT, tf.ACTL_ELIG_DIV_AMT, " +
                        "       tf.TAX_ELIG_DIV_AMT, tf.DIV_TAX_CR_ELIG_DIV_AMT, tf.OTHR_INCM_AMT, " +
                        "       tf.FRGN_NON_BUS_INCM_AMT, tf.RL16_DIV_TAX_CR_AMT, tf.RL16_TAX_DIV_AMT " +
                        "FROM TAX_SLIP ts " +
                        "JOIN T3_FUND tf ON ts.ID = tf.TAX_SLIP_ID " +
                        "JOIN TRUST tr ON tf.TRUST_ID = tr.ID " +
                        "JOIN TRUST_DESCRIPTION td ON tr.ID = td.TRUST_ID " +
                        "WHERE ts.POL_ID = ? AND tr.TRUST_ACCT_NUM = ? " +
                        "AND td.LANG_ID = 1 " + // Ensure we get the English description
                        "AND ts.ADMIN_SYS_ID = 1 AND ts.TAX_SLIP_TYPE_ID = 1 " +
                        "AND ts.CRA_TAX_YR_ID = 17 AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(t3CsvPath), "ISO-8859-1"));
             PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {

            String headerLine = reader.readLine();
            Map<String, Integer> colMap = mapHeaders(headerLine);
            String line;
            int rowCount = 0;

            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                String policy = safeGet(row, colMap.get("Policy Num"));
                String trust = safeGet(row, colMap.get("Trust Account"));

                if (policy.isEmpty() || trust.isEmpty()) continue;
                rowCount++;

                System.out.println("\n--------------------------------------------------");
                System.out.println("Checking Fund Row " + rowCount + ": Policy [" + policy + "] Fund [" + trust + "]");

                stmt.setString(1, policy);
                stmt.setString(2, trust);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        validateFundRow(row, colMap, rs);
                    } else {
                        System.out.println("   [FAIL] Fund Record Not Found in DB: Policy=" + policy + ", Trust=" + trust);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateFundRow(String[] row, Map<String, Integer> colMap, ResultSet rs) throws SQLException {
        // --- 1. Print Detailed Identity Info (Restored) ---
        long dbSlipId      = rs.getLong("TAX_SLIP_ID");
        long dbTrustId     = rs.getLong("TRUST_ID");
        String dbDesc      = rs.getString("TRUST_DESC_TXT");
        String dbSurname   = rs.getString("RECIP_SUR_TXT");
        String dbSin       = rs.getString("RECIP_SIN");
        String dbClient    = rs.getString("CLI_ID");

        System.out.println(String.format("   >> DB Info: [SlipID: %d] [TrustID: %d] [Desc: %s]", dbSlipId, dbTrustId, dbDesc));
        System.out.println(String.format("   >> Recipient: [Surname: %s] [SIN: %s] [Client: %s]", dbSurname, dbSin, dbClient));

        // --- 2. Validate Amounts ---
        check(parseDouble(safeGet(row, colMap.get("Capital Gains (21)"))), rs.getDouble("CAP_GAINS_AMT"), "CAP_GAINS_AMT");
        check(parseDouble(safeGet(row, colMap.get("Capital Losses (37)"))), rs.getDouble("CAP_LOSS_AMT"), "CAP_LOSS_AMT");
        check(parseDouble(safeGet(row, colMap.get("Actual Div (49/C1)"))), rs.getDouble("ACTL_ELIG_DIV_AMT"), "ACTL_ELIG_DIV_AMT");
        check(parseDouble(safeGet(row, colMap.get("Taxable Div (50)"))), rs.getDouble("TAX_ELIG_DIV_AMT"), "TAX_ELIG_DIV_AMT");
        check(parseDouble(safeGet(row, colMap.get("Div Tax Credit (51)"))), rs.getDouble("DIV_TAX_CR_ELIG_DIV_AMT"), "DIV_TAX_CR_ELIG_DIV_AMT");
        check(parseDouble(safeGet(row, colMap.get("Other Income (26/G)"))), rs.getDouble("OTHR_INCM_AMT"), "OTHR_INCM_AMT");
        check(parseDouble(safeGet(row, colMap.get("Foreign Income (25/F)"))), rs.getDouble("FRGN_NON_BUS_INCM_AMT"), "FRGN_NON_BUS_INCM_AMT");

        // --- 3. Display RL16 Amounts (Info Only) ---
        double rl16Cred = rs.getDouble("RL16_DIV_TAX_CR_AMT");
        double rl16Tax = rs.getDouble("RL16_TAX_DIV_AMT");

        if (rl16Cred > 0 || rl16Tax > 0) {
            System.out.println(String.format("          RL16_DIV_TAX_CR_AMT : %.2f", rl16Cred));
            System.out.println(String.format("          RL16_TAX_DIV_AMT    : %.2f", rl16Tax));
        }
    }

    private void check(double csvVal, double dbVal, String label) {
        if (Math.abs(csvVal - dbVal) < 0.02) {
            System.out.println(String.format("   [PASS] %-23s : %.2f", label, csvVal));
        } else {
            System.out.println(String.format("   [FAIL] %-23s : CSV[%.2f] != DB[%.2f]", label, csvVal, dbVal));
        }
    }

    // --- Helpers ---
    private Map<String, Integer> mapHeaders(String line) {
        Map<String, Integer> map = new HashMap<>();
        String[] headers = line.split(",");
        for (int i = 0; i < headers.length; i++) map.put(headers[i].trim(), i);
        return map;
    }
    private String safeGet(String[] row, Integer index) {
        if (index == null || index >= row.length) return "";
        return row[index].trim();
    }
    private double parseDouble(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }
}