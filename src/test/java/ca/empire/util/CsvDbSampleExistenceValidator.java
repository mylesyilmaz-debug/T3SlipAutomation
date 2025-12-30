package ca.empire.util;
import jdk.internal.org.jline.terminal.TerminalBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;

public class CsvDbSampleExistenceValidator {

    //Check if data in DEL file exists in DB table , first 5 rows and first 6 columns

    private static void setStringOrNull(
            PreparedStatement ps,
            int index,
            String value
    ) throws SQLException {
        String v = normalize(value);
        if (v == null) {
            ps.setNull(index, java.sql.Types.CHAR);
        } else {
            ps.setString(index, v);
        }
    }

    private static String normalize(String value) {
        if (value == null) return null;

        // Remove surrounding quotes if present
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        // Trim again after removing quotes
        value = value.trim();

        return value.isEmpty() ? null : value;
    }

    public static void main(String[] args) throws SQLException {

        String delFilePath = "C:/Users/citmxy/Downloads/file.del";
        String dbUrl = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";
        String dbUser = "citmxy@empire.corp";
        String dbPass = "PakTurk78%";
        String tableName = "ipr1dba1.trt";

        int sampleLimit = 5;
        int processed = 0;

        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
        String sql =
                "SELECT 1 FROM " + tableName +
                        " WHERE CO_ID = ? " +
                        " AND RTBL_ID = ? " +
                        " AND RTBL_RT_TYP_CD = ? " +
                        " AND RTBL_SMKR_CD = ? " +
                        " AND RTBL_PAR_CD = ? " +
                        " AND RTBL_SEX_CD = ? " +
                        " AND (\n" +
                        "      RTBL_STBL_1_CD = ?\n" +
                        "   OR (RTBL_STBL_1_CD IS NULL AND ? IS NULL)\n" +
                        "   OR (RTRIM(RTBL_STBL_1_CD) = '' AND ? IS NULL)\n" +
                        ") " ;


        try (BufferedReader reader = new BufferedReader(new FileReader(delFilePath))) {

            String line;

            while ((line = reader.readLine()) != null && processed < sampleLimit) {

                if (processed >= 5) break;

                String[] c = line.split(",");

                System.out.println("Sample row " + (processed + 1));
                System.out.println("CO_ID                = " + c[0]);
                System.out.println("RTBL_ID              = " + c[1]);
                System.out.println("RTBL_RT_TYP_CD       = " + c[2]);
                System.out.println("RTBL_SMKR_CD         = " + c[3]);
                System.out.println("RTBL_PAR_CD          = " + c[4]);
                System.out.println("RTBL_SEX_CD          = " + c[5]);
                System.out.println("RTBL_STBL_1_CD       = " + c[6]);
                System.out.println("RTBL_STBL_2_CD       = " + c[7]);
                System.out.println("RTBL_STBL_3_CD       = " + c[8]);
                System.out.println("RTBL_STBL_4_CD       = " + c[9]);
                System.out.println("LOC_GR_ID            = " + c[10]);
                System.out.println("RTBL_DB_OPT_CD       = " + c[11]);
                System.out.println("RTBL_PNSN_QUALF_CD   = " + c[12]);
                System.out.println("RTBL_JNT_LIFE_CD     = " + c[13]);
                System.out.println("DPOS_TRM_MO_DUR      = " + c[14]);
                System.out.println("DPOS_TRM_DY_DUR      = " + c[15]);
                System.out.println("RTBL_AGE             = " + c[16]);
                System.out.println("RTBL_IDT_NUM         = " + c[17]);
                System.out.println("-------------------------------------");


                PreparedStatement ps = conn.prepareStatement(sql);

                ps.setString(1, c[0]); // CO_ID
                ps.setString(2, c[1]); // RTBL_ID
                ps.setString(3, c[2]); // RTBL_RT_TYP_CD
                ps.setString(4, c[3]); // RTBL_SMKR_CD
                ps.setString(5, c[4]); // RTBL_PAR_CD
                ps.setString(6, c[5]); // RTBL_SEX_CD

                String p7 = normalize(c[6]);

                ps.setString(7, p7);   // RTBL_STBL_1_CD = ?
                ps.setString(8, p7);   // ? IS NULL
                ps.setString(9, p7);   // RTRIM(col)='' AND ? IS NULL


                ResultSet rs = ps.executeQuery();
                boolean existsInDb = rs.next();
                if (!existsInDb) {
                    System.out.println("❌ NOT FOUND IN DB → CO_ID=" + c[0] + ", RTBL_ID=" + c[1]);
                }
                if (existsInDb) {
                    System.out.println("FOUND IN DB → CO_ID=" + c[0] + ", RTBL_ID=" + c[1]);



                    }


                processed++;
            }

        } catch (Exception e) {
            System.err.println("Error reading DEL file");
            e.printStackTrace();
        }

    }

}