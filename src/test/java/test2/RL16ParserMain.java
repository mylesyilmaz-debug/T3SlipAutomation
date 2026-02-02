package test2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RL16ParserMain {

    // --- CONFIGURATION ---
    private static final String DOWNLOAD_PATH = "C:\\Selenium_Downloads";
    // Matching "RL16TAPE" or similar based on your previous logs
    private static final String FILE_PATTERN = "RL16TAPE";
    private static final String OUTPUT_CSV = DOWNLOAD_PATH + "\\RL16_Parsed_Output.csv";

    public static void main(String[] args) {
        try {
            File rl16File = findLatestRL16File();
            if (rl16File != null) {
                System.out.println("Processing RL16 File: " + rl16File.getName());
                parseRL16ToCsv(rl16File);
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

    private static void parseRL16ToCsv(File inputFile) throws IOException {
        // We create a SUPER SET of headers to accommodate both Type 1 and Type 3 data cleanly
        String header = "Year,3_Digit_Code,Recipient Type (15),Policy Num,Ref Name (30-42)," +
                "SIN (Individual),Surname (Indiv),First Name (Indiv)," + // Type 1 Specifics
                "SIN (Business),Business Name," +                        // Type 3 Specifics
                "Address 1,Address 2,City," +                            // Shared Address
                "Cap Gains (Box 21),Actual Div,Foreign Inc,Other Inc,Taxable Div,Div Tax Credit,Actual Div (Box J)";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new FileWriter(OUTPUT_CSV))) {

            writer.println(header);

            String line;
            while ((line = reader.readLine()) != null) {
                // 1. SKIP HEADERS/TRAILERS based on length or missing discriminator
                if (line.trim().length() < 15) continue;

                // 2. IDENTIFY RECIPIENT TYPE (Column 15 -> Index 14)
                char recipientType = line.charAt(14);

                // We only process if Type is '1' (Individual) or '3' (Business)
                if (recipientType != '1' && recipientType != '3') {
                    continue;
                }

                // 3. INITIALIZE FIELDS
                String sinIndiv = "", surname = "", firstName = "";
                String sinBus = "", busName = "";

                // 4. CONDITIONAL PARSING
                if (recipientType == '1') {
                    // --- TYPE 1: INDIVIDUAL ---
                    sinIndiv  = safeExtract(line, 36, 44);
                    surname   = safeExtract(line, 45, 74);
                    firstName = safeExtract(line, 75, 105);
                } else {
                    // --- TYPE 3: BUSINESS ---
                    sinBus    = safeExtract(line, 106, 114);
                    busName   = safeExtract(line, 116, 175);
                }

                // 5. CONSTRUCT ROW
                // Note: Address 1 (146-175) is extracted for BOTH.
                // Warning: For Type 3, this might overlap with the end of Business Name.
                String row =
                        safeExtract(line, 1, 4) + "," +       // Year
                                safeExtract(line, 11, 13) + "," +     // 3 Digit Code
                                recipientType + "," +                 // Type (Col 15)
                                safeExtract(line, 16, 25) + "," +     // Policy Num
                                safeExtract(line, 30, 42) + "," +     // Ref Name (Common)

                                sinIndiv + "," +                      // Individual Fields
                                surname + "," +
                                firstName + "," +

                                sinBus + "," +                        // Business Fields
                                busName + "," +

                                safeExtract(line, 146, 175) + "," +   // Address 1 (Both)
                                safeExtract(line, 176, 205) + "," +   // Address 2 (Both)
                                safeExtract(line, 206, 235) + "," +   // City (Both)

                                // AMOUNTS (Both)
                                formatDecimal(safeExtract(line, 303, 314)) + "," + // Cap Gains
                                formatDecimal(safeExtract(line, 315, 326)) + "," + // Actual Div (First one)
                                formatDecimal(safeExtract(line, 327, 338)) + "," + // Foreign Inc
                                formatDecimal(safeExtract(line, 339, 351)) + "," + // Other Inc
                                formatDecimal(safeExtract(line, 352, 363)) + "," + // Taxable Div
                                formatDecimal(safeExtract(line, 364, 375)) + "," + // Div Tax Credit
                                formatDecimal(safeExtract(line, 543, 554));        // Actual Div (Second one)

                writer.println(row);
            }
            System.out.println("SUCCESS: RL16 parsed to: " + OUTPUT_CSV);
        }
    }

    // --- UTILITIES (Same as T3) ---

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