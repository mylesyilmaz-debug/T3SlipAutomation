package ca.empire.util;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class CsvVsDbValidator {
    public static void main(String[] args) {

        try {
            runValidation();
        } catch (Exception e) {
            System.out.println("Validation finished with errors (expected for negative scenario)");
            e.printStackTrace();
        }

    }

    private static void runValidation() throws Exception {

        String csvPath = "src/test/resources/testdata/data.csv";

        String url = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";
        String user = "citmxy@empire.corp";
        String password = "PakTurk78%";

        Connection connection = DriverManager.getConnection(url, user, password);
        Statement statement = connection.createStatement();

        FileReader fileReader = new FileReader(csvPath);
        CSVReader reader = new CSVReader(fileReader);

        List<String[]> rows = reader.readAll();

        boolean allMatched = true;

        for (int i = 1; i < rows.size(); i++) { // skip header

            String[] csvRow = rows.get(i);

            String csvId = csvRow[0];
            String csvName = csvRow[1];
            String csvRate = csvRow[2];

            String sql = "SELECT name, rate FROM YOUR_TABLE WHERE id = " + csvId;
            ResultSet rs = statement.executeQuery(sql);

            if (rs.next()) {

                String dbName = rs.getString("name");
                String dbRate = rs.getString("rate");

                System.out.println("ID: " + csvId);
                System.out.println("CSV -> name=" + csvName + ", rate=" + csvRate);
                System.out.println("DB  -> name=" + dbName + ", rate=" + dbRate);

                if (!csvName.equals(dbName) || !csvRate.equals(dbRate)) {
                    System.out.println("❌ MISMATCH FOUND");
                    allMatched = false;
                } else {
                    System.out.println("✅ MATCH");
                }

            } else {
                System.out.println("❌ No DB record found for ID: " + csvId);
                allMatched = false;
            }

            System.out.println("----------------------------------");
        }
        if (!allMatched) {
            System.out.println("=== VALIDATION COMPLETED: MISMATCHES FOUND (NEGATIVE SCENARIO) ===");
        } else {
            System.out.println("=== VALIDATION COMPLETED: ALL RECORDS MATCH ===");
        }

        reader.close();
        connection.close();
    }
}
