package ca.empire.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RL16ParserUtils {

    public void processRL16File(String path, String pattern) throws IOException {
        File rl16File = findLatestFile(path, pattern);
        if (rl16File == null) throw new FileNotFoundException("No RL16 file found");

        List<String> allLines = readAllLines(rl16File);
        String outputCsv = path + "\\RL16_Parsed_Output.csv";

        parseRL16ToCsv(allLines, outputCsv);
    }

    private void parseRL16ToCsv(List<String> lines, String outputPath) throws IOException {
        String header = "Year,3_Digit_Code,Recipient Type (15),Policy Num," +
                "SIN (Individual),Surname (Indiv),First Name (Indiv)," +
                "SIN (Business),Business Name," +
                "Address 1,Address 2,City,Postal Code," +
                "Cap Gains (Box 21),Actual Div,Foreign Inc,Other Inc,Taxable Div,Div Tax Credit,Actual Div (Box J)";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(header);

            if (lines.size() < 2) return;

            // Skip First Row (1) and Last Row (Size-1)
            for (int i = 1; i < lines.size() - 1; i++) {
                String line = lines.get(i);
                if (line.trim().length() < 15) continue;

                String row =
                        safeExtract(line, 1, 4) + "," +
                                safeExtract(line, 11, 13) + "," +
                                safeExtract(line, 15, 15) + "," +
                                safeExtract(line, 16, 25) + "," +

                                // Individual
                                safeExtract(line, 36, 44) + "," +
                                safeExtract(line, 45, 74) + "," +
                                safeExtract(line, 75, 105) + "," +

                                // Business
                                safeExtract(line, 106, 114) + "," +
                                safeExtract(line, 116, 175) + "," +

                                // Address
                                safeExtract(line, 146, 175) + "," +
                                safeExtract(line, 176, 205) + "," +
                                safeExtract(line, 206, 235) + "," +
                                safeExtract(line, 236, 241) + "," +

                                // Amounts
                                formatDecimal(safeExtract(line, 303, 314)) + "," +
                                formatDecimal(safeExtract(line, 315, 326)) + "," +
                                formatDecimal(safeExtract(line, 327, 338)) + "," +
                                formatDecimal(safeExtract(line, 339, 351)) + "," +
                                formatDecimal(safeExtract(line, 352, 363)) + "," +
                                formatDecimal(safeExtract(line, 364, 375)) + "," +
                                formatDecimal(safeExtract(line, 543, 554));

                writer.println(row);
            }
        }
    }

    // --- SHARED UTILS (Ideally, move these to a common FileHelper class) ---
    // For now, copied to ensure this class works standalone

    private File findLatestFile(String path, String pattern) {
        File dir = new File(path);
        if (!dir.exists()) return null;
        File[] files = dir.listFiles((d, name) -> name.contains(pattern) && name.endsWith(".txt"));
        if (files == null || files.length == 0) return null;
        File latest = files[0];
        for (File f : files) { if (f.lastModified() > latest.lastModified()) latest = f; }
        return latest;
    }

    private List<String> readAllLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        }
        return lines;
    }

    private String safeExtract(String line, int start, int end) {
        int actualStart = start - 1;
        if (line.length() < actualStart) return "";
        int actualEnd = Math.min(line.length(), end);
        return line.substring(actualStart, actualEnd).trim().replace(",", " ");
    }

    private String formatDecimal(String value) {
        if (value.isEmpty() || !value.matches("-?\\d+")) return "0.00";
        try {
            double amount = Double.parseDouble(value) / 100.0;
            return String.format("%.2f", amount);
        } catch (NumberFormatException e) { return "0.00"; }
    }
}
