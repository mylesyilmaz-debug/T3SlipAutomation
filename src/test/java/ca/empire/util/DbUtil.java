package ca.empire.util;

import ca.empire.setup.configuration.Config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbUtil {

    static String url = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url,
                Config.getDbUsername(),
                Config.getDbPassword());
    }
}
