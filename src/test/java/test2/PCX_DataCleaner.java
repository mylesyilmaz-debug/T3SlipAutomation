package test2;

import java.io.*;
import java.util.*;

public class PCX_DataCleaner {

    public static void main(String[] args) {
        // --- CONFIGURATION ---
        String userName = System.getProperty("user.name");
        String inputPath = "C:\\Users\\" + userName + "\\Downloads\\STPY0510DET.TXT";
        String outputPath = "C:\\Users\\" + userName + "\\Downloads\\STPY0510DET_Cleaned.csv";

        System.out.println("Starting standalone parsing for: " + inputPath);
        parseAndCleanFile(inputPath, outputPath);
    }

    public static void parseAndCleanFile(String inputPath, String outputPath) {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            System.err.println("Error: Input file not found at " + inputPath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath));
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

            // 1. Write the Header ONCE at the top
            writer.println("FUND_CODE,CL,SIN_NUM,POLICY_NUM,COV_NUM,YR_BEG_ACB,INT_ELIG,DIV_ELIG,DIV_FOREIGN,GAINS_DISP,GAINS_CAN,TOTAL_INC,YR_END_ACB");

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // 2. Filter out non-data lines (Titles, Headers, Dates)
                if (trimmed.isEmpty() || trimmed.contains("T3 YEAREND") || trimmed.contains("PAGE") ||
                        trimmed.contains("FUND CL") || trimmed.contains("WORK ITEM") ||
                        trimmed.matches("^\\d{2}/\\d{2}/\\d{2}.*") || trimmed.startsWith("---")) {
                    continue;
                }

                // 3. Tokenize the line (Split by one or more spaces)
                String cleanLine = trimmed.replaceAll(" +", " ");
                String[] tokens = cleanLine.split(" ");

                // A valid row must at least have Fund, CL, and one value
                if (tokens.length < 3) continue;

                String fund = tokens[0];
                String cl   = tokens[1];

                // 4. Validate Province Code (ensure it's 2 letters like AB, BC, ON)
                if (!cl.matches("[A-Z]{2}")) continue;

                StringBuilder rowBuilder = new StringBuilder();
                rowBuilder.append(fund).append(",").append(cl);

                // 5. Logic to distinguish Individual Data Rows vs. Summary Total Rows
                // Data rows usually have a SIN/Policy string as the 3rd or 4th element
                boolean isDataRow = tokens[2].length() >= 9 && !tokens[2].contains(".");

                if (isDataRow && tokens.length >= 10) {
                    // --- DATA ROW PROCESSING (Captures all records for ON, BC, etc.) ---
                    String sin = tokens[2];
                    String rawPolicy = tokens[3];

                    // Split Policy and Cov (Last 2 chars are Cov)
                    String policy = rawPolicy.length() > 2 ? rawPolicy.substring(0, rawPolicy.length() - 2) : rawPolicy;
                    String cov = rawPolicy.length() > 2 ? rawPolicy.substring(rawPolicy.length() - 2) : "";

                    rowBuilder.append(",").append(sin).append(",").append(policy).append(",").append(cov);

                    boolean hasNonZeroData = false;
                    for (int i = 4; i < tokens.length; i++) {
                        String val = tokens[i].replace(",", "");
                        if (!val.equals("0.00")) hasNonZeroData = true;
                        rowBuilder.append(",").append(val);
                    }

                    // Only write if there is actual money in the record (Skips CA, FL, FN, etc.)
                    if (hasNonZeroData) {
                        writer.println(rowBuilder.toString());
                    }
                }
                else {
                    // --- TOTAL ROW PROCESSING ---
                    rowBuilder.append(",,,,"); // Leave SIN, Policy, and Cov empty

                    boolean hasNonZeroTotal = false;
                    for (int i = 2; i < tokens.length; i++) {
                        String val = tokens[i].replace(",", "");
                        if (!val.equals("0.00")) hasNonZeroTotal = true;
                        rowBuilder.append(",").append(val);
                    }

                    // Captures the Total rows for AMWAL AB, AMWAL BC, etc.
                    if (hasNonZeroTotal) {
                        writer.println(rowBuilder.toString());
                    }
                }
            }
            System.out.println("Success! Cleaned file created at: " + outputPath);

        } catch (IOException e) {
            System.err.println("An error occurred during file parsing: " + e.getMessage());
        }
    }
}
