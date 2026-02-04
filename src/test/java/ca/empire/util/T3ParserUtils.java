package ca.empire.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class T3ParserUtils {

    private static final int INDIVIDUAL_ROWS_LIMIT = 314;

    public void processT3File(String path, String pattern) throws IOException {
        File t3File = findLatestFile(path, pattern);
        if (t3File == null) throw new FileNotFoundException("No T3 file found");

        List<String> allLines = readAllLines(t3File);

        // Define Output Paths
        String outputIndiv = path + "\\T3_Parsed_Output.csv";
        String outputFund = path + "\\T3_FUND_Parsed_Output.csv";

        parseIndividualRecords(allLines, outputIndiv);
        parseFundRecords(allLines, outputFund);
    }

    private void parseIndividualRecords(List<String> lines, String outputPath) throws IOException {
        String header = "Account Number,Surname 1,Name 1,Surname 2,Name 2,Business Name,Address 1,Address 2,City,Prov,Country,Postal,SIN2,Trust Account,CRA_PRT_CODE_ID,Recipient Type,Capital Gains (21),Other Income (26/G),Foreign Income (25/F),Capital Losses (37),Actual Div (49/C1),Taxable Div (50),Div Tax Credit (51),Policy Num,SIN,Status,CLI_ID";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(header);
            int limit = Math.min(lines.size(), INDIVIDUAL_ROWS_LIMIT);

            for (int i = 0; i < limit; i++) {
                String line = lines.get(i);
                if (line.trim().length() < 10) continue;

                String row =
                        safeExtract(line, 2, 9) + "," +
                                safeExtract(line, 10, 29) + "," +
                                safeExtract(line, 30, 42) + "," +
                                safeExtract(line, 43, 62) + "," +
                                safeExtract(line, 63, 75) + "," +
                                safeExtract(line, 76, 135) + "," +
                                safeExtract(line, 136, 165) + "," +
                                safeExtract(line, 166, 195) + "," +
                                safeExtract(line, 196, 223) + "," +
                                safeExtract(line, 224, 225) + "," +
                                safeExtract(line, 226, 228) + "," +
                                safeExtract(line, 229, 235) + "," +
                                safeExtract(line, 239, 247) + "," + // SIN2
                                safeExtract(line, 272, 280) + "," +
                                safeExtract(line, 281, 281) + "," + // CRA_PRT_CODE_ID
                                safeExtract(line, 282, 283) + "," +
                                formatDecimal(safeExtract(line, 284, 294)) + "," +
                                formatDecimal(safeExtract(line, 328, 338)) + "," +
                                formatDecimal(safeExtract(line, 460, 470)) + "," +
                                formatDecimal(safeExtract(line, 515, 525)) + "," +
                                formatDecimal(safeExtract(line, 570, 580)) + "," +
                                formatDecimal(safeExtract(line, 581, 591)) + "," +
                                formatDecimal(safeExtract(line, 592, 602)) + "," +
                                safeExtract(line, 603, 612) + "," +
                                safeExtract(line, 613, 621) + "," +
                                safeExtract(line, 622, 622) + "," +
                                safeExtract(line, 623, 632);

                writer.println(row);
            }
        }
    }

    private void parseFundRecords(List<String> lines, String outputPath) throws IOException {
        String header = "FUND Account,TRUST Account,FUND NAME,Total Records,Date From,Date To,Total Cap Gains,Total Foreign Inc,Total Other Inc,Total Cap Losses,Total Actual Div,Total Taxable Div,Total Div Tax Credit";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println(header);
            int startIndex = INDIVIDUAL_ROWS_LIMIT;
            int endIndex = lines.size() - 1; // Assuming trailer exists

            for (int i = startIndex; i < endIndex; i++) {
                String line = lines.get(i);
                if (line.trim().length() < 10) continue;

                String row =
                        safeExtract(line, 2, 9) + "," +
                                safeExtract(line, 10, 18) + "," +
                                safeExtract(line, 19, 23) + "," +
                                safeExtract(line, 255, 261) + "," +
                                safeExtract(line, 262, 269) + "," +
                                safeExtract(line, 270, 277) + "," +
                                formatDecimal(safeExtract(line, 278, 290)) + "," +
                                formatDecimal(safeExtract(line, 330, 342)) + "," +
                                formatDecimal(safeExtract(line, 343, 355)) + "," +
                                formatDecimal(safeExtract(line, 434, 446)) + "," +
                                formatDecimal(safeExtract(line, 512, 524)) + "," +
                                formatDecimal(safeExtract(line, 525, 537)) + "," +
                                formatDecimal(safeExtract(line, 538, 550));

                writer.println(row);
            }
        }
    }

    // Shared Utilities
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
