package test2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class T3ParserMain {

    // --- CONFIGURATION ---
    private static final String DOWNLOAD_PATH = "C:\\Selenium_Downloads";
    private static final String FILE_PATTERN = "T39580-ITAX";

    // Output Files
    private static final String OUTPUT_CSV_INDIVIDUAL = DOWNLOAD_PATH + "\\T3_Parsed_Output.csv";
    private static final String OUTPUT_CSV_FUND = DOWNLOAD_PATH + "\\T3_FUND_Parsed_Output.csv";

    // --- CONSTANTS ---
    private static final int INDIVIDUAL_ROWS_LIMIT = 314;

    public static void main(String[] args) {
        try {
            File t3File = findLatestT3File();
            if (t3File != null) {
                System.out.println("Processing File: " + t3File.getName());

                // Read all lines into memory first for easier index handling
                List<String> allLines = readAllLines(t3File);

                // 1. Parse Individual Records (Rows 1 to 314)
                parseIndividualRecords(allLines);

                // 2. Parse Fund Records (Row 315 to End - 1)
                parseFundRecords(allLines);

            } else {
                System.err.println("FAILURE: No T3 file found matching '" + FILE_PATTERN + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseIndividualRecords(List<String> lines) throws IOException {
        String header = "Account Number,Surname 1,Name 1,Surname 2,Name 2,Business Name,Address 1,Address 2,City,Prov,Country,Postal,Trust Account,Capital Gains (21),Other Income (26/G),Foreign Income (25/F),Capital Losses (37),Actual Div (49/C1),Taxable Div (50),Div Tax Credit (51),Policy Num,SIN,CLI_ID";

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV_INDIVIDUAL))) {
            writer.println(header);

            // Limit loop to the first 314 rows OR file size, whichever is smaller
            int limit = Math.min(lines.size(), INDIVIDUAL_ROWS_LIMIT);

            for (int i = 0; i < limit; i++) {
                String line = lines.get(i);

                // Validation: Individual rows usually start with '2'
                if (line.trim().length() < 10) continue;

                // UPDATED: Account Number is now 2-9 (Start index 2, End index 9)
                String row =
                        safeExtract(line, 2, 9) + "," +      // UPDATED: Account Number (2-9)
                                safeExtract(line, 10, 29) + "," +    // Surname 1
                                safeExtract(line, 30, 42) + "," +    // Name 1
                                safeExtract(line, 43, 62) + "," +    // Surname 2
                                safeExtract(line, 63, 75) + "," +    // Name 2
                                safeExtract(line, 76, 135) + "," +   // Business Name
                                safeExtract(line, 136, 165) + "," +  // Address Line 1
                                safeExtract(line, 166, 195) + "," +  // Address Line 2
                                safeExtract(line, 196, 223) + "," +  // City
                                safeExtract(line, 224, 225) + "," +  // Prov
                                safeExtract(line, 226, 228) + "," +  // Country
                                safeExtract(line, 229, 235) + "," +  // Postal Code
                                safeExtract(line, 272, 280) + "," +  // Trust Account
                                formatDecimal(safeExtract(line, 284, 294)) + "," + // Box 21
                                formatDecimal(safeExtract(line, 328, 338)) + "," + // Box 26/G
                                formatDecimal(safeExtract(line, 460, 470)) + "," + // Box 25/F
                                formatDecimal(safeExtract(line, 515, 525)) + "," + // Box 37
                                formatDecimal(safeExtract(line, 570, 580)) + "," + // Box 49/C1
                                formatDecimal(safeExtract(line, 581, 591)) + "," + // Box 50
                                formatDecimal(safeExtract(line, 592, 602)) + "," + // Box 51
                                safeExtract(line, 603, 612) + "," +  // Policy Num
                                safeExtract(line, 613, 621) + "," +  // SIN
                                safeExtract(line, 623, 632);         // CLI_ID

                writer.println(row);
            }
            System.out.println("SUCCESS: Individual records parsed to: " + OUTPUT_CSV_INDIVIDUAL);
        }
    }

    private static void parseFundRecords(List<String> lines) throws IOException {
        String header = "FUND Account,TRUST Account,FUND NAME,Total Records,Date From,Date To,Total Cap Gains,Total Foreign Inc,Total Other Inc,Total Cap Losses,Total Actual Div,Total Taxable Div,Total Div Tax Credit";

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV_FUND))) {
            writer.println(header);

            // Start from row 315 (index 314)
            // End before the last row (size - 1)
            int startIndex = INDIVIDUAL_ROWS_LIMIT;
            int endIndex = lines.size() - 1;

            for (int i = startIndex; i < endIndex; i++) {
                String line = lines.get(i);

                if (line.trim().length() < 10) continue;

                // Fund rows typically start with '3', but we rely on the row index here.

                String row =
                        safeExtract(line, 2, 9) + "," +      // FUND Account Number
                                safeExtract(line, 10, 18) + "," +    // TRUST Account Number
                                safeExtract(line, 19, 23) + "," +    // FUND NAME
                                safeExtract(line, 255, 261) + "," +  // Total Records
                                safeExtract(line, 262, 269) + "," +  // Date From
                                safeExtract(line, 270, 277) + "," +  // Date To
                                formatDecimal(safeExtract(line, 278, 290)) + "," + // Fund Level Capital Gains
                                formatDecimal(safeExtract(line, 330, 342)) + "," + // Fund Level Foreign Income
                                formatDecimal(safeExtract(line, 343, 355)) + "," + // Fund Level Other Income
                                formatDecimal(safeExtract(line, 434, 446)) + "," + // Fund Level Capital Losses
                                formatDecimal(safeExtract(line, 512, 524)) + "," + // Fund Level Actual Div
                                formatDecimal(safeExtract(line, 525, 537)) + "," + // Fund Level Taxable Div
                                formatDecimal(safeExtract(line, 538, 550));        // Fund Level Div Tax Credit

                writer.println(row);
            }
            System.out.println("SUCCESS: Fund records parsed to: " + OUTPUT_CSV_FUND);
        }
    }

    // --- UTILITIES ---

    private static File findLatestT3File() {
        File dir = new File(DOWNLOAD_PATH);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles((d, name) -> name.contains(FILE_PATTERN) && name.endsWith(".txt"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File f : files) { if (f.lastModified() > latest.lastModified()) latest = f; }
        return latest;
    }

    private static List<String> readAllLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String safeExtract(String line, int start, int end) {
        int actualStart = start - 1;
        if (line.length() < actualStart) return "";
        int actualEnd = Math.min(line.length(), end);
        return line.substring(actualStart, actualEnd).trim().replace(",", " ");
    }

    private static String formatDecimal(String value) {
        if (value.isEmpty() || !value.matches("-?\\d+")) return "0.00";
        try {
            double amount = Double.parseDouble(value) / 100.0;
            return String.format("%.2f", amount);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }
}