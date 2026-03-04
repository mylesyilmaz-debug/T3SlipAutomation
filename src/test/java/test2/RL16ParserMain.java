package test2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RL16ParserMain {

    // --- CONFIGURATION ---
    private static final String DOWNLOAD_PATH = "C:\\Selenium_Downloads";
    private static final String FILE_PATTERN = "RL16TAPE";
    private static final String OUTPUT_CSV = DOWNLOAD_PATH + "\\RL16_Parsed_Output.csv";

    public static void main(String[] args) {
        try {
            File rl16File = findLatestRL16File();
            if (rl16File != null) {
                System.out.println("Processing RL16 File: " + rl16File.getName());

                // 1. Read all lines into memory
                List<String> allLines = readAllLines(rl16File);

                // 2. Parse (Skipping First and Last row)
                parseRL16ToCsv(allLines);

            } else {
                System.err.println("FAILURE: No RL16 file found matching '" + FILE_PATTERN + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File findLatestRL16File() {
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

    private static void parseRL16ToCsv(List<String> lines) throws IOException {
        String header = "Year,3_Digit_Code,Recipient Type (15),Policy Num," +
                "SIN (Individual),Surname (Indiv),First Name (Indiv)," +
                "SIN (Business),Business Name," +
                "Address 1,Address 2,City,Postal Code," +
                "Cap Gains (Box 21),Actual Div,Foreign Inc,Other Inc,Taxable Div,Div Tax Credit,Actual Div (Box J)";

        try (PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV))) {
            writer.println(header);

            // LOGIC CHANGE:
            // Start at i = 1 (Skip First Row/Header)
            // End at i < lines.size() - 1 (Skip Last Row/Trailer)

            if (lines.size() < 2) {
                System.out.println("WARNING: File has fewer than 2 lines. No data processed.");
                return;
            }

            for (int i = 1; i < lines.size() - 1; i++) {
                String line = lines.get(i);

                // Safety check: skip empty lines or lines too short to contain data
                if (line.trim().length() < 15) continue;

                // We extract ALL fields for EVERY row regardless of Type
                String row =
                        safeExtract(line, 1, 4) + "," +       // Year
                                safeExtract(line, 11, 13) + "," +     // 3 Digit Code
                                safeExtract(line, 15, 15) + "," +     // Recipient Type (Col 15)
                                safeExtract(line, 16, 25) + "," +     // Policy Num

                                // --- INDIVIDUAL FIELDS ---
                                safeExtract(line, 36, 44) + "," +     // SIN (Individual)
                                safeExtract(line, 45, 74) + "," +     // Surname
                                safeExtract(line, 75, 105) + "," +    // First Name

                                // --- BUSINESS FIELDS ---
                                safeExtract(line, 106, 114) + "," +   // SIN (Business)
                                safeExtract(line, 116, 145) + "," +   // Business Name

                                // --- ADDRESS FIELDS ---
                                safeExtract(line, 146, 175) + "," +   // Address 1
                                safeExtract(line, 176, 205) + "," +   // Address 2
                                safeExtract(line, 206, 235) + "," +   // City
                                safeExtract(line, 236, 241) + "," +   // Postal Code

                                // --- AMOUNTS ---
                                formatDecimal(safeExtract(line, 303, 314)) + "," + // Cap Gains
                                formatDecimal(safeExtract(line, 315, 326)) + "," + // Actual Div (First)
                                formatDecimal(safeExtract(line, 327, 338)) + "," + // Foreign Inc
                                formatDecimal(safeExtract(line, 339, 351)) + "," + // Other Inc
                                formatDecimal(safeExtract(line, 352, 363)) + "," + // Taxable Div
                                formatDecimal(safeExtract(line, 364, 375)) + "," + // Div Tax Credit
                                formatDecimal(safeExtract(line, 543, 554));        // Actual Div (Second)

                writer.println(row);
            }
            System.out.println("SUCCESS: RL16 parsed to: " + OUTPUT_CSV);
        }
    }

    // --- UTILITIES ---

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