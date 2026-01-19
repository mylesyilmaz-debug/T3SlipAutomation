import ca.empire.util.Db2Util;

import java.math.BigDecimal;
import java.sql.SQLOutput;

public class Db2ConnectionTest {

    public static void main(String[] args) {

        int count = Db2Util.getRowCount();
        System.out.println("DB2 total rows: " + count);

      /*  double num1 = 0.1;
        double num2 = 0.2;
        double num3 = 0.3;
        double num4 = num1+num2;

        System.out.println(num3==num4);
        System.out.println(num4);

        //output is false
        //output num4 = 0.30000000000000004

        BigDecimal numA= BigDecimal.valueOf(0.1);
        BigDecimal numB= BigDecimal.valueOf(0.2);
        BigDecimal numC= BigDecimal.valueOf(0.3);
        BigDecimal numD= numA.add(numB);

        System.out.println(numC==numD);
        System.out.println(numD);

        //output is false
        //output numD = 0.3


        String numX = String.valueOf(0.1);
        String numY = String.valueOf(0.2);
        String numZ = String.valueOf(0.3);
        String numK = numX + numY;

        System.out.println(numZ==numK);
        System.out.println(numK);

        //output is false
        //output numD = 0.10.2

        */




    }
}