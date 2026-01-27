package ca.empire.util;
import ca.empire.setup.configuration.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DelVsDbValidatorV2 {

    //DEL file vs DB data validation for all rows and 6 columns

    public static void main(String[] args) {

        String filePath = "C:/Users/citmxy/Downloads/file.del";
        String dbUrl = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";

        String tableName = "ipr1dba1.trt";

        int processed = 0;
        int found = 0;
        int notFound = 0;

        try (Connection conn = DriverManager.getConnection(dbUrl,
                Config.getDbUsername(),
                Config.getDbPassword());
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            System.out.println("DB connection successful");
            System.out.println("DEL file opened");

            String line;

            while ((line = reader.readLine()) != null) {

                processed++;

                String[] c = line.split(",", -1);

                System.out.println(
                        "Row " + processed +
                                " | Columns=" + c.length +
                                " | c[0]=" + c[0] +
                                " | c[1]=" + c[1] +
                                " | c[2]=" + c[2] +
                                " | c[3]=" + c[3] +
                                " | c[4]=" + c[4] +
                                " | c[5]=" + c[5]
                );

                String sql =
                        "SELECT 1 FROM " + tableName +
                                " WHERE CO_ID = ? " +
                                " AND RTBL_ID = ? " +
                                " AND RTBL_RT_TYP_CD = ? " +
                                " AND RTBL_SMKR_CD = ? " +
                                " AND RTBL_PAR_CD = ? " +
                                " AND RTBL_SEX_CD = ? ";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, c[0]); // CO_ID
                ps.setString(2, c[1]); // RTBL_ID
                ps.setString(3, c[2]); // RTBL_RT_TYP_CD
                ps.setString(4, c[3]); // RTBL_SMKR_CD
                ps.setString(5, c[4]); // RTBL_PAR_CD
                ps.setString(6, c[5]); // RTBL_SEX_CD

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    found++;
                    System.out.println("✔ FOUND row " + processed);
                } else {
                    notFound++;
                    System.out.println("✘ NOT FOUND row " + processed);
                }

                if (processed % 1000 == 0) {
                    System.out.println("Processed rows: " + processed);
                }
            }

            System.out.println("=================================");
            System.out.println("Rows processed : " + processed);
            System.out.println("Found in DB    : " + found);
            System.out.println("Not found      : " + notFound);
            System.out.println("=================================");



        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
