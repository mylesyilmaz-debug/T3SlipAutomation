package test2;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Arrays; // Added for Set initialization

public class DBValidatorMain {

    // --- DATABASE CONFIGURATION ---
    private static final String DB_URL = "jdbc:db2://kgndbconnpl.empire.ca:55012/ITAX";
    private static final String DB_USER = "itaxusr";
    private static final String DB_PASS = "1t@xUsare";

    private static final String CSV_FILE = "C:\\Selenium_Downloads\\T3_Parsed_Output.csv";
    private static final int EXPECTED_UNIQUE_RECIPIENTS = 73;

    // --- WHITELISTED DUMMY SINS ---
    private static final Set<String> WHITELISTED_SINS = new HashSet<>(Arrays.asList(
            "999999998", "111111118", "222222226", "333333334", "444444442",
            "555555556", "666666664", "000000000","777777772", "888888880"
    ));

    public static void main(String[] args) {
        System.out.println("--- Starting Step 2.2: Demographics + Address + Whitelisted SIN Logic ---");

        Set<String> processedPolicies = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(CSV_FILE), "ISO-8859-1"))) {

            System.out.println("Database Connected!");

            String headerLine = reader.readLine();
            String[] headers = headerLine.split(",");
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim(), i);
            }

            int totalPoliciesChecked = 0;
            int policiesPassed = 0;
            String line;

            String query = "SELECT ts.RECIP_SUR_TXT, ts.CRA_RPT_CODE_ID, ts.RECIP_SIN, ts.CLI_ID, " +
                    "       ts.RECIP_ADDR_1_TXT, ts.RECIP_CITY_TXT, ts.RECIP_PSTL_CD, " +
                    "       ts.BAD_ADDR_IND, " +
                    "       t3.RECIP_BUS_NUM, " +
                    "       t3.CRA_RECIP_TYPE_ID " +
                    "FROM TAX_SLIP ts " +
                    "JOIN T3_SLIP t3 ON ts.ID = t3.TAX_SLIP_ID " +
                    "WHERE ts.POL_ID = ? " +
                    "AND ts.ADMIN_SYS_ID = 1 " +
                    "AND ts.TAX_SLIP_TYPE_ID = 1 " +
                    "AND ts.CRA_TAX_YR_ID = 17 " +
                    "AND TO_CHAR(ts.CREAT_DT, 'YYYY-MM-DD') = '2025-12-10'";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {

                while ((line = reader.readLine()) != null) {
                    String[] row = line.split(",", -1);
                    String csvPolicyNum = safeGet(row, colMap.get("Policy Num"));

                    if (processedPolicies.contains(csvPolicyNum)) {
                        continue;
                    }
                    processedPolicies.add(csvPolicyNum);
                    totalPoliciesChecked++;

                    // --- EXTRACT CSV DATA ---
                    String csvName = safeGet(row, colMap.get("Surname 1"));
                    String csvBusinessName = safeGet(row, colMap.get("Business Name"));
                    String nameToValidate = csvName.isEmpty() ? csvBusinessName : csvName;

                    String csvRawCode = safeGet(row, colMap.get("CRA_PRT_CODE_ID"));
                    String expectedDbCode = mapReportCode(csvRawCode);

                    String csvSIN = safeGet(row, colMap.get("SIN"));
                    String csvClientID = safeGet(row, colMap.get("CLI_ID"));

                    String csvAddress = safeGet(row, colMap.get("Address 1"));
                    String csvCity    = safeGet(row, colMap.get("City"));
                    String csvPostal  = safeGet(row, colMap.get("Postal"));
                    String csvStatus  = safeGet(row, colMap.get("Status"));

                    System.out.println("\n--------------------------------------------------");
                    System.out.println("Checking Policy: " + csvPolicyNum);

                    stmt.setString(1, csvPolicyNum);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // --- EXTRACT DB DATA ---
                            String dbSurname = rs.getString("RECIP_SUR_TXT");
                            String dbReportCode = rs.getString("CRA_RPT_CODE_ID");
                            String dbSIN = rs.getString("RECIP_SIN");
                            String dbBusNum = rs.getString("RECIP_BUS_NUM");
                            String dbRecipType = rs.getString("CRA_RECIP_TYPE_ID");
                            String dbClientID = rs.getString("CLI_ID");

                            String dbAddress = rs.getString("RECIP_ADDR_1_TXT");
                            String dbCity    = rs.getString("RECIP_CITY_TXT");
                            String dbPostal  = rs.getString("RECIP_PSTL_CD");
                            int dbBadAddrInd = rs.getInt("BAD_ADDR_IND");

                            boolean allFieldsPass = true;

                            // 1. Name Check
                            if (compareStrings(nameToValidate, dbSurname)) {
                                System.out.println("   [PASS] Surname    : " + nameToValidate);
                            } else {
                                System.out.println("   [FAIL] Surname    : CSV [" + nameToValidate + "] != DB [" + dbSurname + "]");
                                allFieldsPass = false;
                            }

                            // 2. SIN / Business Number Check
                            // A. Check if Invalid Format (Luhn)
                            if (!isValidSIN(csvSIN)) {
                                System.out.println("   [PASS] SIN        : Skipped (Invalid Format/Dummy: " + csvSIN + ")");
                            }
                            // B. Check if Whitelisted (NEW LOGIC)
                            else if (WHITELISTED_SINS.contains(csvSIN)) {
                                System.out.println("   [PASS] SIN        : Skipped (Whitelisted Test SIN: " + csvSIN + ")");
                            }
                            // C. Perform DB Validation
                            else {
                                if ("3".equals(dbRecipType) || "4".equals(dbRecipType)) {
                                    if (compareStrings(csvSIN, dbBusNum)) {
                                        System.out.println("   [PASS] Bus Num    : " + csvSIN + " (Matched T3_SLIP.RECIP_BUS_NUM)");
                                    } else {
                                        System.out.println("   [FAIL] Bus Num    : CSV [" + csvSIN + "] != DB [" + dbBusNum + "]");
                                        allFieldsPass = false;
                                    }
                                } else {
                                    if (compareStrings(csvSIN, dbSIN)) {
                                        System.out.println("   [PASS] SIN        : " + csvSIN);
                                    } else {
                                        System.out.println("   [FAIL] SIN        : CSV [" + csvSIN + "] != DB [" + dbSIN + "]");
                                        allFieldsPass = false;
                                    }
                                }
                            }

                            // 3. Client ID Check
                            if (compareStrings(csvClientID, dbClientID)) {
                                System.out.println("   [PASS] Client ID  : " + csvClientID);
                            } else {
                                System.out.println("   [FAIL] Client ID  : CSV [" + csvClientID + "] != DB [" + dbClientID + "]");
                                allFieldsPass = false;
                            }

                            // 4. Report Code Check
                            if (compareStrings(expectedDbCode, dbReportCode)) {
                                System.out.println("   [PASS] Report Code: " + csvRawCode + " (DB ID: " + dbReportCode + ")");
                            } else {
                                System.out.println("   [FAIL] Report Code: CSV [" + expectedDbCode + "] != DB [" + dbReportCode + "]");
                                allFieldsPass = false;
                            }

                            // 5. Address Check
                            if (compareStrings(csvAddress, dbAddress)) {
                                System.out.println("   [PASS] Address    : " + csvAddress);
                            } else {
                                if ("E".equalsIgnoreCase(csvStatus) && dbBadAddrInd == 1) {
                                    System.out.println("   [PASS] Address    : Ignored (BadAddr=1) - CSV[" + csvAddress + "] vs DB[" + dbAddress + "]");
                                } else {
                                    System.out.println("   [FAIL] Address    : CSV [" + csvAddress + "] != DB [" + dbAddress + "]");
                                    allFieldsPass = false;
                                }
                            }

                            // 6. City Check
                            if (compareStrings(csvCity, dbCity)) {
                                System.out.println("   [PASS] City       : " + csvCity);
                            } else {
                                if ("E".equalsIgnoreCase(csvStatus) && dbBadAddrInd == 1) {
                                    System.out.println("   [PASS] City       : Ignored (BadAddr=1) - CSV[" + csvCity + "] vs DB[" + dbCity + "]");
                                } else {
                                    System.out.println("   [FAIL] City       : CSV [" + csvCity + "] != DB [" + dbCity + "]");
                                    allFieldsPass = false;
                                }
                            }

                            // 7. Postal Code Check
                            if (compareStrings(csvPostal, dbPostal)) {
                                System.out.println("   [PASS] Postal     : " + csvPostal);
                            } else {
                                if ("E".equalsIgnoreCase(csvStatus) && dbBadAddrInd == 1) {
                                    System.out.println("   [PASS] Postal     : Ignored (BadAddr=1) - CSV[" + csvPostal + "] vs DB[" + dbPostal + "]");
                                } else {
                                    System.out.println("   [FAIL] Postal     : CSV [" + csvPostal + "] != DB [" + dbPostal + "]");
                                    allFieldsPass = false;
                                }
                            }

                            if (allFieldsPass) {
                                policiesPassed++;
                            }

                        } else {
                            System.out.println("   [FAIL] No Record Found in DB with filters: Year=17, Date=2025-12-10");
                        }
                    }
                }
            }

            System.out.println("\n==================================================");
            System.out.println("Validation Summary");
            System.out.println("==================================================");
            System.out.println("Unique Policies Checked : " + totalPoliciesChecked);
            System.out.println("Policies Passed Fully   : " + policiesPassed);
            System.out.println("Policies Failed         : " + (totalPoliciesChecked - policiesPassed));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- UTILITIES ---

    private static boolean isValidSIN(String sin) {
        if (sin == null) return false;
        String cleanSin = sin.replaceAll("[^0-9]", "");
        if (cleanSin.length() != 9) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = cleanSin.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cleanSin.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private static String safeGet(String[] row, Integer index) {
        if (index == null || index >= row.length) return "";
        return row[index].trim();
    }

    private static String mapReportCode(String code) {
        if ("O".equalsIgnoreCase(code)) return "1";
        if ("A".equalsIgnoreCase(code)) return "2";
        if ("C".equalsIgnoreCase(code)) return "3";
        return code;
    }

    private static boolean compareStrings(String csvVal, String dbVal) {
        if (csvVal == null) csvVal = "";
        if (dbVal == null) dbVal = "";
        csvVal = csvVal.trim();
        dbVal = dbVal.trim();
        if (csvVal.equalsIgnoreCase(dbVal)) return true;
        String cleanCsv = csvVal.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String cleanDb = dbVal.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (cleanCsv.equals(cleanDb)) return true;
        if (cleanCsv.startsWith(cleanDb) && cleanDb.length() > 5) return true;
        if (cleanDb.startsWith(cleanCsv) && cleanCsv.length() > 5) return true;
        return false;
    }
}