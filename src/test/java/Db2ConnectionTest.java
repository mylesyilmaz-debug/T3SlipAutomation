import ca.empire.util.Db2Util;

public class Db2ConnectionTest {

    public static void main(String[] args) {

        int count = Db2Util.getRowCount();
        System.out.println("DB2 total rows: " + count);
    }
}