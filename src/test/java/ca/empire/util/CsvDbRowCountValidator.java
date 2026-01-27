package ca.empire.util;

import ca.empire.setup.configuration.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CsvDbRowCountValidator {

    //Check if row counts matching DEL file vs DB table

    public static void main(String[] args) {
        String csvPath = "C:/Users/citmxy/Downloads/file.del";
        String dbUrl = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";
        String tableName = "ipr1dba1.trt";

        int rowCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {

            String line;

            while ((line = reader.readLine()) != null) {
                 rowCount++;
            }

            System.out.println("Total file rows : " + (rowCount));

        } catch (IOException e) {
            System.err.println("Error reading DEL file");
            e.printStackTrace();
        }
        int dbRowCount = 0;

        String sql = "SELECT COUNT(*) FROM ipr1dba1.trt where prev_updt_user_id = 'S24211'";

        try (Connection conn = DriverManager.getConnection(dbUrl,
                Config.getDbUsername(),
                Config.getDbPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                dbRowCount = rs.getInt(1);
            }

            System.out.println("Total DB rows: " + dbRowCount);

        } catch (Exception e) {
            System.err.println("Error counting DB rows");
            e.printStackTrace();
        }

        System.out.println("--------------------------------------------------");

        if (rowCount == dbRowCount) {
            System.out.println("✅ ROW COUNT MATCHES");
            System.out.println("FILE rows = " + rowCount + ", DB rows = " + dbRowCount);
        } else {
            System.out.println("❌ ROW COUNT MISMATCH");
            System.out.println("FILE rows = " + rowCount + ", DB rows = " + dbRowCount);
        }


    }
}
