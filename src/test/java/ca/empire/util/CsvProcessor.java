package ca.empire.util;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CsvProcessor {

    public static File validateNetworkFile(String path, String fileName) {
        File file = new File(path, fileName);
        if (!file.exists()) throw new RuntimeException("File not found at: " + file.getAbsolutePath());
        if (!file.canRead()) throw new RuntimeException("File is not readable!");
        if (file.length() == 0) throw new RuntimeException("File is empty!");
        return file;
    }

    public static String processCsv(File sourceFile, String outputDir) throws IOException {
        String outputFileName = "PARSED_" + sourceFile.getName();
        File directory = new File(outputDir);
        if (!directory.exists()) directory.mkdirs();

        File outputFile = new File(directory, outputFileName);

        try (BufferedReader br = new BufferedReader(new FileReader(sourceFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            boolean isHeader = true;
            int colIndex = -1;

            while ((line = line = br.readLine()) != null) {
                // Assuming comma-separated. Adjust to ";" if needed.
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                List<String> newRow = new ArrayList<>();

                if (isHeader) {
                    for (int i = 0; i < columns.length; i++) {
                        if (columns[i].equalsIgnoreCase("POLICY/COV NUM")) {
                            colIndex = i;
                            newRow.add("POLICY NUM");
                            newRow.add("COV NUM");
                        } else {
                            newRow.add(columns[i]);
                        }
                    }
                    isHeader = false;
                } else {
                    for (int i = 0; i < columns.length; i++) {
                        if (i == colIndex) {
                            String fullVal = columns[i].replace("\"", "").trim();
                            if (fullVal.length() > 2) {
                                String policy = fullVal.substring(0, fullVal.length() - 2);
                                String cov = fullVal.substring(fullVal.length() - 2);
                                newRow.add(policy);
                                newRow.add(cov);
                            } else {
                                newRow.add(fullVal); // Fallback
                                newRow.add("");
                            }
                        } else {
                            newRow.add(columns[i]);
                        }
                    }
                }
                bw.write(String.join(",", newRow));
                bw.newLine();
            }
        }
        return outputFile.getAbsolutePath();
    }
}