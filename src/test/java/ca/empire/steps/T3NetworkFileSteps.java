package ca.empire.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        // Professional validation: confirm header + at least 1 data row exist
        try (BufferedReader br = Files.newBufferedReader(networkFile)) {
            String header = br.readLine();
            String firstDataRow = br.readLine();

            if (header == null) {
                throw new AssertionError("❌ Missing header line: " + networkFile);
            }
            if (firstDataRow == null) {
                throw new AssertionError("❌ No data rows (only header exists): " + networkFile);
            }
        }

        System.out.println("✅ PASS readable + not empty: " + networkFile + " | bytes=" + sizeBytes);
    }
}
