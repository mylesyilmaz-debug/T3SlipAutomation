package ca.empire.util;

import ca.empire.setup.configuration.Config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CsvDbRandomSampleValidator {

    //Get 1000 rows randomly from DEL file and check if exist in DB

    public static void main(String[] args) {

        int SAMPLE_SIZE = 1000;

        List<String[]> sampleRows = new ArrayList<>(SAMPLE_SIZE);
        Random random = new Random();

        int rowNumber = 0;
        int found = 0;
        int notFound = 0;

        String filePath = "C:/Users/citmxy/Downloads/file.del";
        String dbUrl = "jdbc:db2://kgnmfdbmlt01.empire.ca:50111/DBIUA1";

        String tableName = "ipr1dba1.trt";

        String sql =
                "SELECT 1 FROM ipr1dba1.trt WHERE " +
                        "CO_ID = ? " +
                        "AND RTBL_ID = ? " +
                        "AND RTBL_RT_TYP_CD = ? " +
                        "AND RTBL_SMKR_CD = ? " +
                        "AND RTBL_PAR_CD = ? " +
                        "AND RTBL_SEX_CD = ? " ;

        try (Connection conn = DriverManager.getConnection(dbUrl,
                Config.getDbUsername(),
                Config.getDbPassword());
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                String[] c = line.split(",");

                if (sampleRows.size() < SAMPLE_SIZE) {
                    sampleRows.add(c);
                } else {
                    int j = random.nextInt(rowNumber);
                    if (j < SAMPLE_SIZE) {
                        sampleRows.set(j, c);
                    }
                }
            }

            System.out.println("Total rows read = " + rowNumber);
            System.out.println("Random sample size = " + sampleRows.size());

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                for (int i = 0; i < sampleRows.size(); i++) {
                    String[] c = sampleRows.get(i);

                    ps.setString(1, c[0]);               // CO_ID
                    ps.setString(2, c[1]);               // RTBL_ID
                    ps.setString(3, c[2]);               // RTBL_RT_TYP_CD
                    ps.setString(4, c[3]);               // RTBL_SMKR_CD
                    ps.setString(5, c[4]);               // RTBL_PAR_CD
                    ps.setString(6, c[5]);               // RTBL_SEX_CD

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            found++;
                            System.out.println("✔ FOUND row " + (i + 1));
                        } else {
                            notFound++;
                            System.out.println("✘ NOT FOUND row " + (i + 1)
                                    + " -> CO_ID=" + c[0]
                                    + ", RTBL_ID=" + c[1]);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
