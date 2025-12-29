package ca.empire.hooks;

import ca.empire.util.DbUtil;
import io.cucumber.java.After;
import io.cucumber.java.Before;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;

public class TestHooks {

    public static Connection connection;
    public static BufferedReader delReader;

    private static final String DEL_FILE_PATH ="C:/Users/citmxy/Downloads/file.del";


    @Before
    public void setUp() throws Exception {
        // DB connection
        connection = DbUtil.getConnection();

        System.out.println("✅ DB connection established");

        delReader = new BufferedReader(new FileReader(DEL_FILE_PATH));
        System.out.println("📂 DEL file opened");
    }

    @After
    public void tearDown() throws Exception {
        if (delReader != null) {
            delReader.close();
            System.out.println("📂 DEL file closed");
        }

        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("🧹 DB connection closed");
        }
    }
}
