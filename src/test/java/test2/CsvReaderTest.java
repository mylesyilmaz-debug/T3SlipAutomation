package test2;

import ca.empire.util.CsvReaderUtil;
import java.util.List;

public class CsvReaderTest {

    public static void main(String[] args) {

        String csvPath = "C:/Users/citmxy/temp/test.csv";

        List<String[]> records = CsvReaderUtil.readCsv(csvPath);

        System.out.println("Total rows: " + records.size());
        System.out.println(String.join(" | ", records.get(0)));
    }
}
