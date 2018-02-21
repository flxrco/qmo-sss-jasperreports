/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasperreports;

import jasperreports.models.JdbcHelper;
import jasperreports.models.ParamLengthMismatchException;
import jasperreports.models.ReportGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import net.sf.jasperreports.engine.JRException;

/**
 *
 * @author User
 */
public class UniversalGenerator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        args = new String[1];
        args[0] = "C:\\Users\\User\\Desktop\\test.txt";
        System.out.println("***QMO-SSS Report Generation Automator***");
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                String url, user, pass;
                System.out.print("Database URL> ");
                url = sc.next();
                System.out.print("Username> ");
                user = sc.next();
                System.out.print("Password> ");
                pass = sc.next();

                try {
                    System.out.println("Testing JDBC connection...");
                    //JdbcHelper.config("oracle.jdbc.driver.OracleDriver", url, user, pass);
                    JdbcHelper.config("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@172.20.0.201:1521:USTPRD1", "opqm_user03", "password");
                    if (JdbcHelper.testConnection()) {
                        System.out.println("Connection successful.");
                        break;
                    } else {
                        System.out.println("Connection failed. Would you like to attempt login again? [Y/N]");
                        switch (sc.next().charAt(0)) {
                            case 'Y':
                            case 'y':
                                break;
                            default:
                                System.out.println("Terminating the program.");
                                return;
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    //this will NEVER happen
                }
            }
        }
        
        System.out.println("Commencing report generation.");
        
        /**
         * EXPECTED FORMAT: LINE 1: .jrxml file path LINE 2: param1, param2,
         * param3, param4 LINE ~: arg1, arg2, arg3, arg4 [,filename]
         */
        for (int i = 1; i <= args.length; i++) {
            String path = args[i - 1];
            System.out.printf("[%d/%d] Preparing the report on path %s\n", i, args.length, path);
            try (BufferedReader br = new BufferedReader(new FileReader(new File(path)))) {
                String str, reportPath = null;
                String[] params;
                ReportGenerator report = null;
                int c = 1;
                while ((str = br.readLine()) != null) {
                    try {
                        String[] repArgs = null;
                        switch (c) {
                            case 1: {
                                reportPath = str;
                                break;
                            }
                            case 2: {
                                params = splitAndTrim(str);
                                report = new ReportGenerator(JdbcHelper.getConnection(), reportPath, params);
                                break;
                            }
                            default: {
                                repArgs = splitAndTrim(str);
                                if (repArgs.length == report.getParamCount()) {
                                    report.insertArguments(repArgs, null);
                                } else if (repArgs.length == report.getParamCount() + 1) {
                                    report.insertArguments(Arrays.copyOfRange(repArgs, 0, repArgs.length - 1), repArgs[repArgs.length - 1]);
                                }
                                break;
                            }
                        }
                    } catch (ParamLengthMismatchException ex2) {
                        System.out.printf("There was a mismatch between the parameters and the arguments on line %d \n", c);
                    } catch (SQLException ex) {
                        //will never happen
                    } catch (JRException ex) {
                        System.out.printf("An error with the .jrxml file was encountered: %s", ex.getMessage());
                        System.out.println(i < args.length ? "The program will proceed to generate the next report." : "");
                        break;
                    }

                    c++;
                }
                try {
                    for (int j = 0; j < report.getReportCount(); j++) {
                        System.out.printf("%s: Generating to %s \r", progressBar(15, j + 1, report.getReportCount()), report.generateReport(j));
                    }
                    System.out.printf("%s: Generated\r", progressBar(15, 15, report.getReportCount()));
                } catch (JRException ex) {
                    System.out.println(ex.toString());
                    System.out.printf("\nAn error with the .jrxml file was encountered: %s", ex.getMessage());
                    System.out.println(i < args.length ? "The program will proceed to generate the next report." : "");
                    break;
                }

            } catch (IOException ex) {
                System.out.printf("The .txt file at path %s was not found.", path);
                System.out.println(i < args.length ? "The program will proceed to generate the next report." : "");
            }
        }
        System.out.println("***Generation complete***");
    }

    private static String[] splitAndTrim(String s) {
        String[] arr = s.split(",");
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].trim();
        }
        return arr;
    }

    private static String progressBar(int maxBars, int current, int count) {
        int barCount = (int) Math.floor(((double) current / count) * maxBars);
        StringBuilder str = new StringBuilder("|");
        for (int i = 1; i <= maxBars; i++) {
            if (barCount >= i) {
                str.append("=");
            } else {
                str.append(" ");
            }
        }
        str.append("| ");
        return str.toString();
    }
}
