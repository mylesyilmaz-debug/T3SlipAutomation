package ca.empire.util;

import ca.empire.setup.configuration.Config;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class CsvVsDbValidator {

    //negative scenario, data.csv vs DB matching or not , 3 rows 3 columns

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

        Connection connection = DriverManager.getConnection(url,
                Config.getDbUsername(),
                Config.getDbPassword());
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

            String sql = "SELECT CO_ID, RTBL_ID, RTBL_RT_TYP_CD from ipr1dba1.trt where prev_updt_user_id = 'S24211'";
            ResultSet rs = statement.executeQuery(sql);

            if (rs.next()) {

                String coId = rs.getString("CO_ID");
                String rtblId = rs.getString("RTBL_ID");
                String rtblRtTypCd = rs.getString("RTBL_RT_TYP_CD");

                System.out.println("CSV -> ID: " + csvId);
                System.out.println("CSV -> name=" + csvName + ", CSV -> rate=" + csvRate);
                System.out.println("DB  -> CoID=" + coId + ", DB -> rtbID =" + rtblId + ", DB -> rtblRtTypCd =" + rtblRtTypCd);

                if (!csvName.equals(rtblId) || !csvRate.equals(rtblRtTypCd)) {
                    System.out.println("❌ MISMATCH FOUND");
                    allMatched = false;
                } else {
                    System.out.println("✅ MATCH");
                }

            }
            else {
                System.out.println("❌ No DB record found for ID: " + csvId);
                allMatched = false;
            }

            System.out.println("----------------------------------");
        }
        if (!allMatched) {
            System.out.println("=== VALIDATION COMPLETED: MISMATCHES FOUND (NEGATIVE SCENARIO) ===");
        }
        else {
            System.out.println("=== VALIDATION COMPLETED: ALL RECORDS MATCH ===");
        }

        reader.close();
        connection.close();
    }
}
