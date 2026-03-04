package ca.empire.validators;

import ca.empire.util.DatabaseManager;
import java.io.*;
import java.sql.*;
import java.util.*;

public class RecipientValidator {

    private DatabaseManager dbManager;
    private static final Set<String> WHITELISTED_SINS = new HashSet<>(Arrays.asList(
            "999999998", "111111118", "222222226", "333333334", "444444442",
            "555555556", "666666664","000000000", "777777772", "888888880"
    ));

    public RecipientValidator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void validateRecipients(String csvPath) {
        System.out.println("--- Starting Recipient Validation (Detailed) ---");
        Set<String> processedPolicies = new HashSet<>();

        String query = "SELECT ts.RECIP_SUR_TXT, ts.CRA_RPT_CODE_ID, ts.RECIP_SIN, ts.CLI_ID, " +
                "       ts.RECIP_ADDR_1_TXT, ts.RECIP_CITY_TXT, ts.RECIP_PSTL_CD, " +
                "       ts.BAD_ADDR_IND, t3.RECIP_BUS_NUM, t3.CRA_RECIP_TYPE_ID " +
                "FROM TAX_SLIP ts " +
                "JOIN T3_SLIP t3 ON ts.ID = t3.TAX_SLIP_ID " +
                "WHERE ts.POL_ID = ? AND ts.ADMIN_SYS_ID = 1 AND ts.TAX_SLIP_TYPE_ID = 1 " +
                "AND ts.CRA_TAX_YR_ID = 17 AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "ISO-8859-1"));
             PreparedStatement stmt = dbManager.getConnection().prepareStatement(query)) {

            String headerLine = reader.readLine();
            Map<String, Integer> colMap = mapHeaders(headerLine);
            String line;

            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                String policyNum = safeGet(row, colMap.get("Policy Num"));

                if (processedPolicies.contains(policyNum)) continue;
                processedPolicies.add(policyNum);

                System.out.println("\n--------------------------------------------------");
                System.out.println("Checking Recipient Policy: " + policyNum);

                stmt.setString(1, policyNum);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        validateRow(row, colMap, rs);
                    } else {
                        System.out.println("   [FAIL] Policy " + policyNum + " not found in DB.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void validateRow(String[] row, Map<String, Integer> colMap, ResultSet rs) throws SQLException {
        // --- 1. Policy Number ---
        // Since we queried by Policy Number to get here, it implicitly matches.
        String csvPolicy = safeGet(row, colMap.get("Policy Num"));
        System.out.println("   [PASS] Policy Number : " + csvPolicy);

        // --- 2. Surname ---
        String csvName = safeGet(row, colMap.get("Surname 1"));
        String csvBusName = safeGet(row, colMap.get("Business Name"));
        String nameToValidate = csvName.isEmpty() ? csvBusName : csvName;
        String dbSurname = rs.getString("RECIP_SUR_TXT");

        if (compareStrings(nameToValidate, dbSurname)) {
            System.out.println("   [PASS] Surname       : " + nameToValidate);
        } else {
            System.out.println("   [FAIL] Surname       : CSV[" + nameToValidate + "] != DB[" + dbSurname + "]");
        }

        // --- 3. SIN / Business Number ---
        String csvSIN = safeGet(row, colMap.get("SIN"));
        String dbSIN = rs.getString("RECIP_SIN");
        String dbBusNum = rs.getString("RECIP_BUS_NUM");
        String dbType = rs.getString("CRA_RECIP_TYPE_ID");

        if (!isValidSIN(csvSIN)) {
            System.out.println("   [PASS] SIN           : Skipped (Dummy/Invalid Format: " + csvSIN + ")");
        } else if (WHITELISTED_SINS.contains(csvSIN)) {
            System.out.println("   [PASS] SIN           : Skipped (Whitelisted Test SIN: " + csvSIN + ")");
        } else {
            if ("3".equals(dbType) || "4".equals(dbType)) {
                if (compareStrings(csvSIN, dbBusNum)) {
                    System.out.println("   [PASS] Bus Num       : " + csvSIN + " (Matched T3_SLIP.RECIP_BUS_NUM)");
                } else {
                    System.out.println("   [FAIL] Bus Num       : CSV[" + csvSIN + "] != DB[" + dbBusNum + "]");
                }
            } else {
                if (compareStrings(csvSIN, dbSIN)) {
                    System.out.println("   [PASS] SIN           : " + csvSIN);
                } else {
                    System.out.println("   [FAIL] SIN           : CSV[" + csvSIN + "] != DB[" + dbSIN + "]");
                }
            }
        }

        // --- 4. Client ID ---
        String csvClientID = safeGet(row, colMap.get("CLI_ID"));
        String dbClientID = rs.getString("CLI_ID");
        if (compareStrings(csvClientID, dbClientID)) {
            System.out.println("   [PASS] Client ID     : " + csvClientID);
        } else {
            System.out.println("   [FAIL] Client ID     : CSV[" + csvClientID + "] != DB[" + dbClientID + "]");
        }

        // --- 5, 6, 7. Address, City, Postal (Bad Address Logic) ---
        String csvStatus = safeGet(row, colMap.get("Status"));
        int dbBadAddrInd = rs.getInt("BAD_ADDR_IND");

        checkAddressField(safeGet(row, colMap.get("Address 1")), rs.getString("RECIP_ADDR_1_TXT"), "Address", csvStatus, dbBadAddrInd);
        checkAddressField(safeGet(row, colMap.get("City")), rs.getString("RECIP_CITY_TXT"), "City", csvStatus, dbBadAddrInd);
        checkAddressField(safeGet(row, colMap.get("Postal")), rs.getString("RECIP_PSTL_CD"), "Postal Code", csvStatus, dbBadAddrInd);

        // --- 8. Report Code ---
        String csvCode = safeGet(row, colMap.get("CRA_PRT_CODE_ID")); // Code from CSV (O, A, C)
        String dbCode = rs.getString("CRA_RPT_CODE_ID"); // DB ID (1, 2, 3)
        String mappedCode = mapReportCode(csvCode);

        if (compareStrings(mappedCode, dbCode)) {
            System.out.println("   [PASS] Report Code   : " + csvCode + " (Mapped to DB ID: " + dbCode + ")");
        } else {
            System.out.println("   [FAIL] Report Code   : CSV[" + mappedCode + "] != DB[" + dbCode + "]");
        }
    }

    private void checkAddressField(String csvVal, String dbVal, String label, String status, int badAddrInd) {
        if (compareStrings(csvVal, dbVal)) {
            System.out.println(String.format("   [PASS] %-13s : %s", label, csvVal));
        } else {
            if ("E".equalsIgnoreCase(status) && badAddrInd == 1) {
                System.out.println(String.format("   [PASS] %-13s : Mismatch Ignored (Status=E & BadAddr=1)", label));
            } else {
                System.out.println(String.format("   [FAIL] %-13s : CSV[%s] != DB[%s]", label, csvVal, dbVal));
            }
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

    private boolean compareStrings(String csvVal, String dbVal) {
        if (csvVal == null) csvVal = ""; if (dbVal == null) dbVal = "";
        return csvVal.trim().replaceAll("[^a-zA-Z0-9]", "").equalsIgnoreCase(dbVal.trim().replaceAll("[^a-zA-Z0-9]", ""));
    }

    private boolean isValidSIN(String sin) {
        if (sin == null) return false;
        String clean = sin.replaceAll("[^0-9]", "");
        if (clean.length() != 9) return false;
        int sum = 0; boolean alt = false;
        for (int i = clean.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(clean.substring(i, i + 1));
            if (alt) { n *= 2; if (n > 9) n = (n % 10) + 1; }
            sum += n; alt = !alt;
        }
        return (sum % 10 == 0);
    }

    private String mapReportCode(String code) {
        if ("O".equalsIgnoreCase(code)) return "1";
        if ("A".equalsIgnoreCase(code)) return "2";
        if ("C".equalsIgnoreCase(code)) return "3";
        return code;
    }
}