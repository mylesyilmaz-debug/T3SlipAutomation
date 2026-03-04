    /*
Scenario 1  verifying that Sum(Funds) == Slip Total.
Scenario 2  verifying if a fund has a value (e.g., $50.00) and another fund has NULL/Empty for the same box.
the aggregated total in T3_SLIP is $50.00 (not NULL or error).
Scenario 3  verifying if the system rounds before summing or sums before rounding ($10.004 + 10.006 = 20.01$)
Scenario 4  verifying that negative value is moved to the Capital Loss (Box 37) field in T3_SLIP
and not left as a negative number in Box 21
*/

    package test2;

    import java.sql.*;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class RollupValidatorMain {

        // --- CONFIGURATION ---
        private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
        private static final String DB_USER = "itaxusr";
        private static final String DB_PASS = "1t@xUsare";

        // Columns to validate
        private static final String[] AMOUNT_COLS = {
                "CAP_GAINS_AMT",           // Box 21
                "CAP_LOSS_AMT",            // Box 37
                "ACTL_ELIG_DIV_AMT",       // Box 49
                "TAX_ELIG_DIV_AMT",        // Box 50
                "DIV_TAX_CR_ELIG_DIV_AMT", // Box 51
                "OTHR_INCM_AMT",           // Box 26
                "FRGN_NON_BUS_INCM_AMT"    // Box 25
        };

        public static void main(String[] args) {
            System.out.println("--- Starting Ticket 7: T3 Rollup Validation (With Province) ---");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                System.out.println("Database Connected!");

                // 1. Get all Slips (Parents)
                // ADDED: ts.CRA_PROV_ID to get the province
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
                                "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10' " +
                                "ORDER BY ts.POL_ID";

                try (PreparedStatement slipStmt = conn.prepareStatement(slipQuery);
                     ResultSet rsSlip = slipStmt.executeQuery()) {

                    int processedCount = 0;
                    int failCount = 0;

                    while (rsSlip.next()) {
                        processedCount++;
                        long taxSlipId = rsSlip.getLong("ID");
                        String polId = rsSlip.getString("POL_ID");
                        int provId = rsSlip.getInt("CRA_PROV_ID");
                        String provCode = getProvCode(provId); // Translate ID to Code

                        System.out.println("\n--------------------------------------------------");
                        System.out.println("Validating Rollup for Policy: " + polId + " (Slip ID: " + taxSlipId + ") [Prov: " + provCode + "]");

                        if (!validateSlipRollup(conn, taxSlipId, rsSlip)) {
                            failCount++;
                        }
                    }

                    System.out.println("\n==================================================");
                    System.out.println("Rollup Validation Summary");
                    System.out.println("Policies Checked: " + processedCount);
                    System.out.println("Policies Failed : " + failCount);
                    System.out.println("==================================================");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static boolean validateSlipRollup(Connection conn, long taxSlipId, ResultSet rsSlip) throws SQLException {
            String fundQuery =
                    "SELECT CAP_GAINS_AMT, CAP_LOSS_AMT, ACTL_ELIG_DIV_AMT, " +
                            "       TAX_ELIG_DIV_AMT, DIV_TAX_CR_ELIG_DIV_AMT, " +
                            "       OTHR_INCM_AMT, FRGN_NON_BUS_INCM_AMT " +
                            "FROM T3_FUND " +
                            "WHERE TAX_SLIP_ID = ?";

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

                    System.out.println("   >> Aggregating " + fundRows.size() + " Fund Record(s)...");
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

        // Helper: Map CRA Province IDs to Codes
        private static String getProvCode(int id) {
            switch (id) {
                case 1: return "AB";
                case 2: return "BC";
                case 3: return "MB";
                case 4: return "NB";
                case 5: return "NL";
                case 7: return "NS";
                case 9: return "ON";
                case 10: return "PE";
                case 11: return "QC"; // Quebec
                case 12: return "SK";
                default: return "Unknown (" + id + ")";
            }
        }
    }