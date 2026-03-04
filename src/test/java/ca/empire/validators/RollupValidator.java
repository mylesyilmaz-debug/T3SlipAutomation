package ca.empire.validators;

import ca.empire.util.DatabaseManager;
import org.junit.Assert;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RollupValidator {

    private DatabaseManager dbManager;

    private static final String[] AMOUNT_COLS = {
            "CAP_GAINS_AMT", "CAP_LOSS_AMT", "ACTL_ELIG_DIV_AMT",
            "TAX_ELIG_DIV_AMT", "DIV_TAX_CR_ELIG_DIV_AMT",
            "OTHR_INCM_AMT", "FRGN_NON_BUS_INCM_AMT"
    };

    // Constructor Injection
    public RollupValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void validateRollups(String loadDate) {
        System.out.println("--- Starting Ticket 7: T3 Rollup Validation ---");
        int processedCount = 0;
        int failCount = 0;

        // Grab the existing connection from your framework's DatabaseManager
        Connection conn = dbManager.getConnection();
        Assert.assertNotNull("Database connection is null! Check your @Before hook.", conn);

        String slipQuery =
                "SELECT ts.ID, ts.POL_ID, ts.CRA_PROV_ID, " +
                        "       t3.CAP_GAINS_AMT, t3.CAP_LOSS_AMT, t3.ACTL_ELIG_DIV_AMT, " +
                        "       t3.TAX_ELIG_DIV_AMT, t3.DIV_TAX_CR_ELIG_DIV_AMT, " +
                        "       t3.OTHR_INCM_AMT, t3.FRGN_NON_BUS_INCM_AMT " +
                        "FROM TAX_SLIP ts " +
                        "JOIN T3_SLIP t3 ON ts.ID = t3.TAX_SLIP_ID " +
                        "WHERE ts.ADMIN_SYS_ID = 1 " +
                        "AND ts.TAX_SLIP_TYPE_ID = 1 " +
                        "AND ts.CRA_TAX_YR_ID = 17 " +
                        "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = ? " +
                        "ORDER BY ts.POL_ID";

        try (PreparedStatement slipStmt = conn.prepareStatement(slipQuery)) {
            slipStmt.setString(1, loadDate);

            try (ResultSet rsSlip = slipStmt.executeQuery()) {
                while (rsSlip.next()) {
                    processedCount++;
                    long taxSlipId = rsSlip.getLong("ID");
                    String polId = rsSlip.getString("POL_ID");
                    int provId = rsSlip.getInt("CRA_PROV_ID");
                    String provCode = getProvCode(provId);

                    System.out.println("\n--------------------------------------------------");
                    System.out.println("Validating Rollup for Policy: " + polId + " (Slip ID: " + taxSlipId + ") [Prov: " + provCode + "]");

                    if (!validateSlipRollup(conn, taxSlipId, rsSlip)) {
                        failCount++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Assert.fail("SQL Exception during rollup validation: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("Rollup Validation Summary");
        System.out.println("Policies Checked: " + processedCount);
        System.out.println("Policies Failed : " + failCount);
        System.out.println("==================================================");

        // This assertion tells Cucumber if the test passed or failed!
        Assert.assertEquals("Rollup validation failed for " + failCount + " policies.", 0, failCount);
    }

    private boolean validateSlipRollup(Connection conn, long taxSlipId, ResultSet rsSlip) throws SQLException {
        String fundQuery =
                "SELECT CAP_GAINS_AMT, CAP_LOSS_AMT, ACTL_ELIG_DIV_AMT, " +
                        "       TAX_ELIG_DIV_AMT, DIV_TAX_CR_ELIG_DIV_AMT, " +
                        "       OTHR_INCM_AMT, FRGN_NON_BUS_INCM_AMT " +
                        "FROM T3_FUND WHERE TAX_SLIP_ID = ?";

        try (PreparedStatement fundStmt = conn.prepareStatement(fundQuery)) {
            fundStmt.setLong(1, taxSlipId);

            try (ResultSet rsFund = fundStmt.executeQuery()) {
                List<Map<String, Double>> fundRows = new ArrayList<>();
                while (rsFund.next()) {
                    Map<String, Double> rowData = new HashMap<>();
                    for (String col : AMOUNT_COLS) {
                        rowData.put(col, rsFund.getDouble(col));
                    }
                    fundRows.add(rowData);
                }

                if (fundRows.isEmpty()) {
                    System.out.println("   [FAIL] No Fund records found for Slip ID: " + taxSlipId);
                    return false;
                }

                boolean slipPassed = true;
                for (String col : AMOUNT_COLS) {
                    double slipVal = rsSlip.getDouble(col);
                    double calculatedSum = 0.0;
                    StringBuilder breakdown = new StringBuilder("(");

                    for (int i = 0; i < fundRows.size(); i++) {
                        double val = fundRows.get(i).get(col);
                        calculatedSum += val;
                        breakdown.append(String.format(" %.2f", val));
                        if (i < fundRows.size() - 1) breakdown.append(" +");
                    }
                    breakdown.append(" )");

                    if (Math.abs(slipVal - calculatedSum) > 0.05) {
                        System.out.println(String.format("   [FAIL] %-25s : %.2f %s != Slip[%.2f]", col, calculatedSum, breakdown.toString(), slipVal));
                        slipPassed = false;
                    } else {
                        System.out.println(String.format("   [PASS] %-25s : %.2f %s", col, slipVal, breakdown.toString()));
                    }
                }
                return slipPassed;
            }
        }
    }

    private String getProvCode(int id) {
        switch (id) {
            case 1: return "AB"; case 2: return "BC"; case 3: return "MB";
            case 4: return "NB"; case 5: return "NL"; case 7: return "NS";
            case 9: return "ON"; case 10: return "PE"; case 11: return "QC";
            case 12: return "SK"; default: return "Unknown (" + id + ")";
        }
    }
}