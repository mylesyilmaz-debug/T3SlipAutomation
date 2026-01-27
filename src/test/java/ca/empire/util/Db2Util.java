package ca.empire.util;

import ca.empire.setup.configuration.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Db2Util {

    public static int getRowCount() {

        String url = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";

        String query = "SELECT COUNT(*) FROM ipr1dba1.trt";

        try (Connection conn = DriverManager.getConnection(
                url,
                Config.getDbUsername(),
                Config.getDbPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            rs.next();
            return rs.getInt(1);

        } catch (Exception e) {
            throw new RuntimeException("DB2 connection failed", e);
        }
    }

}
