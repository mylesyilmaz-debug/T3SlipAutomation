package ca.empire.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ca.empire.hooks.TestHooks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class RtblValidationSteps {

        Connection conn = TestHooks.connection;
        private int delRowCount;
        private int dbRowCount;
        public static String TABLE_NAME = "ipr1dba1.trt";
        public static String delPath = "C:/Users/citmxy/Downloads/file.del";

        private Map<String, Integer> rtblIdCounts = new HashMap<>();

        @Given("the DEL file is loaded")
        public void the_del_file_is_loaded() {
            // later
        }

        @Given("the database connection is available")
        public void the_database_connection_is_available() {
            // later
        }

        @When("I count total records in DEL and DB")
        public void i_count_total_records_in_del_and_db() {
            // later
        }

        @Then("the total record count should match")
        public void the_total_record_count_should_match() {
            // later
        }

        @When("I calculate distinct RTBL_ID counts")
        public void i_calculate_distinct_rtbl_id_counts() {
            // later
        }

        @Then("each RTBL_ID should have matching record counts in DEL and DB")
        public void each_rtbl_id_should_have_matching_record_counts() {
            // later
        }

        @When("I randomly validate up to 100 records for each RTBL_ID")
        public void i_randomly_validate_100_records_per_rtbl_id() {
            // later
        }

        @When("I randomly validate 1000 records")
        public void i_randomly_validate_1000_records() {
            // later
        }

        @Then("all selected records should be found in DB")
        public void all_selected_records_should_be_found_in_db() {
            // later
        }

        @Given("DEL file is loaded")
        public void del_file_is_loaded() {
                // already handled by Hooks
        }

        @Given("DB connection is available")
        public void db_connection_is_available() {
                // already handled by Hooks
        }

        @When("I count total records in DEL file")
        public void i_count_total_records_in_del_file() {
                delRowCount = 0;

                try {
                        BufferedReader reader = TestHooks.delReader;

                        String line;
                        while ((line = reader.readLine()) != null) {
                                if (!line.trim().isEmpty()) {
                                        delRowCount++;
                                }
                        }

                } catch (Exception e) {
                        throw new RuntimeException("Error counting DEL records", e);
                }

                System.out.println("DEL total records: " + delRowCount);
        }

        @When("I count total records in DB table")
        public void i_count_total_records_in_db_table() {
                String sql = "SELECT COUNT(*) FROM ipr1dba1.trt where prev_updt_user_id = 'S24211'";

                try (Statement stmt = TestHooks.connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                        if (rs.next()) {
                                dbRowCount = rs.getInt(1);
                        }

                } catch (Exception e) {
                        throw new RuntimeException("Error counting DB records", e);
                }

                System.out.println("DB total records: " + dbRowCount);
        }



        @Then("DEL record count should match DB record count")
        public void del_record_count_should_match_db_record_count() {
                if (delRowCount != dbRowCount) {
                        throw new AssertionError(
                                "Record count mismatch! DEL=" + delRowCount + ", DB=" + dbRowCount
                        );
                }

                System.out.println("✅ Record counts match: " + delRowCount);

        }

        @When("I fetch distinct RTBL_ID values with record counts")
        public void i_fetch_distinct_rtbl_id_values_with_record_counts() {

                String sql =
                        "SELECT RTBL_ID, COUNT(*) AS CNT FROM ipr1dba1.trt where prev_updt_user_id = 'S24211' GROUP BY RTBL_ID\n";

                try (Statement stmt = TestHooks.connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                        while (rs.next()) {
                                String rtblId = rs.getString("RTBL_ID");
                                int count = rs.getInt("CNT");
                                rtblIdCounts.put(rtblId, count);
                        }

                } catch (Exception e) {
                        throw new RuntimeException("Error fetching RTBL_ID distribution", e);
                }
        }

        @Then("RTBL_ID distribution should be displayed")
        public void rtbl_id_distribution_should_be_displayed() {

                System.out.println("Distinct RTBL_ID count: " + rtblIdCounts.size());

                rtblIdCounts.forEach((id, count) ->
                        System.out.println("RTBL_ID=" + id + " -> records=" + count)
                );

                if (rtblIdCounts.isEmpty()) {
                        throw new AssertionError("No RTBL_ID values found in DB");
                }
        }

        @Given("I analyze RTBL_ID distribution from DEL file")
        public void analyze_rtbl_id_distribution_from_del_file() throws Exception {

                BufferedReader reader = new BufferedReader(
                        new FileReader(delPath)   // same delPath you already use
                );

                String line;
                Set<String> rtblIds = new HashSet<>();

                while ((line = reader.readLine()) != null) {
                        String[] c = line.split(",");
                        rtblIds.add(c[1]); // RTBL_ID
                }

                reader.close();

                System.out.println("Distinct RTBL_ID count = " + rtblIds.size());
        }

        @Given("I select up to 50 random rows per RTBL_ID from DEL file")
        public void select_50_random_rows_per_rtbl_id() throws Exception {

                BufferedReader reader = new BufferedReader(new FileReader(delPath));

                Map<String, List<String[]>> groupedRows = new HashMap<>();
                String line;

                while ((line = reader.readLine()) != null) {
                        String[] c = line.split(",");
                        String rtblId = c[1]; // RTBL_ID

                        groupedRows
                                .computeIfAbsent(rtblId, k -> new ArrayList<>())
                                .add(c);
                }

                reader.close();

                Random random = new Random();

                for (String rtblId : groupedRows.keySet()) {
                        List<String[]> rows = groupedRows.get(rtblId);
                        Collections.shuffle(rows, random);

                        int limit = Math.min(50, rows.size());

                        System.out.println(
                                "RTBL_ID=" + rtblId + " | selected rows=" + limit
                        );
                }


        }

        @Given("random sampled DEL rows exist in DB")
        public void validate_random_sampled_rows_against_db() throws Exception {

                BufferedReader reader = new BufferedReader(new FileReader(delPath));
                Map<String, List<String[]>> groupedRows = new HashMap<>();
                String line;

                while ((line = reader.readLine()) != null) {
                        String[] c = line.split(",");
                        String rtblId = c[1];

                        groupedRows
                                .computeIfAbsent(rtblId, k -> new ArrayList<>())
                                .add(c);
                }
                reader.close();

                Random random = new Random();

                String sql = "SELECT 1 FROM " + TABLE_NAME +
                                " WHERE CO_ID=? AND RTBL_ID=? AND RTBL_RT_TYP_CD=? " +
                                " AND RTBL_SMKR_CD=? AND RTBL_PAR_CD=? AND RTBL_SEX_CD=?";

                PreparedStatement ps = conn.prepareStatement(sql);

                int checked = 0;
                int found = 0;

                for (String rtblId : groupedRows.keySet()) {

                        List<String[]> rows = groupedRows.get(rtblId);
                        Collections.shuffle(rows, random);

                        int limit = Math.min(50, rows.size());

                        for (int i = 0; i < limit; i++) {
                                String[] c = rows.get(i);

                                ps.setString(1, c[0]); // CO_ID
                                ps.setString(2, c[1]); // RTBL_ID
                                ps.setString(3, c[2]);
                                ps.setString(4, c[3]);
                                ps.setString(5, c[4]);
                                ps.setString(6, c[5]);

                                ResultSet rs = ps.executeQuery();
                                checked++;

                                if (rs.next()) {
                                        found++;
                                } else {
                                        throw new AssertionError(
                                                "NOT FOUND in DB for RTBL_ID=" + c[1]
                                        );
                                }
                                rs.close();
                        }
                }

                System.out.println(
                        "Validated rows: " + checked + " | Found in DB: " + found
                );
        }
}
