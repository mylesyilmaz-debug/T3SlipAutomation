package ca.empire.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class T3NetworkFileSteps {

    private Path networkFile;

    @Given("the network file {string} exists")
    public void the_network_file_exists(String filePath) {
        networkFile = Paths.get(filePath);

        if (!Files.exists(networkFile)) {
            throw new AssertionError("❌ File not found: " + networkFile);
        }

        System.out.println("✅ FOUND network file: " + networkFile);
    }

    @Then("the network file is readable and not empty")
    public void the_network_file_is_readable_and_not_empty() throws IOException {

        if (!Files.isReadable(networkFile)) {
            throw new AssertionError("❌ File is not readable: " + networkFile);
        }

        long sizeBytes = Files.size(networkFile);
        if (sizeBytes <= 0) {
            throw new AssertionError("❌ File is empty (0 bytes): " + networkFile);
        }

        try (BufferedReader br = Files.newBufferedReader(networkFile)) {

            // Read header
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new AssertionError("❌ Missing header line: " + networkFile);
            }

            // Read first data row
            String firstDataRow = br.readLine();
            if (firstDataRow == null) {
                throw new AssertionError("❌ No data rows found (only header exists): " + networkFile);
            }

            String[] headers = headerLine.split(",", -1);
            String[] values = firstDataRow.split(",", -1);

            System.out.println("---- First data row verification ----");
            for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                System.out.println(headers[i].trim() + " = " + values[i].trim());
            }
            System.out.println("-------------------------------------");
        }

        System.out.println("✅ STPY0510 job output verified → considered SUCCESS");
        System.out.println("   File: " + networkFile + " | size=" + sizeBytes + " bytes");
    }

    @Then("I generate a transformed T3 CSV with separated policy and coverage numbers")
    public void generate_transformed_t3_csv() throws IOException {

        Path outputDir = Paths.get("build", "t3-output");
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve("STPY0510_transformed.csv");

        try (BufferedReader br = Files.newBufferedReader(networkFile);
             BufferedWriter bw = Files.newBufferedWriter(outputFile)) {

            // --- Read header ---
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new AssertionError("Input CSV has no header");
            }

            String[] headers = headerLine.split(",", -1);

            int policyCovIdx = -1;
            int yrBegAcbIdx = -1;
            int yrEndAcbIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim();
                if (h.equalsIgnoreCase("POLICY/COV NUM")) policyCovIdx = i;
                if (h.equalsIgnoreCase("YR-BEG ACB")) yrBegAcbIdx = i;
                if (h.equalsIgnoreCase("YR-END ACB")) yrEndAcbIdx = i;
            }

            if (policyCovIdx == -1) {
                throw new AssertionError("POLICY/COV NUM column not found");
            }

            // --- Write new header ---
            List<String> outHeaders = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                if (i == yrBegAcbIdx || i == yrEndAcbIdx) continue;

                if (i == policyCovIdx) {
                    outHeaders.add("POLICY_NUM");
                    outHeaders.add("COV_NUM");
                } else {
                    outHeaders.add(headers[i].trim());
                }
            }

            bw.write(String.join(",", outHeaders));
            bw.newLine();

            // --- Process rows ---
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1);
                List<String> outValues = new ArrayList<>();

                for (int i = 0; i < values.length; i++) {
                    if (i == yrBegAcbIdx || i == yrEndAcbIdx) continue;

                    if (i == policyCovIdx) {
                        String raw = values[i].replaceAll("\\s+", "");
                        if (raw.length() < 2) {
                            throw new AssertionError("Invalid POLICY/COV value: " + values[i]);
                        }
                        String policy = raw.substring(0, raw.length() - 2);
                        String cov = raw.substring(raw.length() - 2);

                        outValues.add(policy);
                        outValues.add(cov);
                    } else {
                        outValues.add(values[i].trim());
                    }
                }

                bw.write(String.join(",", outValues));
                bw.newLine();
            }
        }

        System.out.println("✅ Transformed CSV generated at: " + outputFile.toAbsolutePath());
    }
}
