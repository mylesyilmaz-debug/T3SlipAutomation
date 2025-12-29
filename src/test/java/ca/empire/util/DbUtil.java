package ca.empire.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbUtil {

    static String url = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";
    static String user = "citmxy@empire.corp";
    static String password = "PakTurk78%";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, user, password);
    }
}
