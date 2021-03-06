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
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import net.sf.jasperreports.engine.JRException;

/**
 *
 * @author User
 */
public class UniversalGenerator {

    public static void main(String args[]) {
        args = new String[2];
        args[0] = "C:\\Users\\User\\Desktop\\qmo-sss\\jdbc-config.txt";
        args[1] = "C:\\Users\\User\\Desktop\\qmo-sss\\peryear.txt";
        try {
            for (int i = 0; i < args.length; i++) {
                String path = args[i];

                switch (i) {
                    case 0:
                        registerDatabase(path);
                        break;
                    default: {
                        generateReport(path);
                        break;
                    }

                }
            }
        } catch (SQLException ex) {
            System.out.printf("$> An error was encountered while configuring JDBC: %s Terminating.\n", ex.getMessage());
        } catch (IOException ex) {
            System.out.printf("$> The JDBC config file was not found on path %s.\n", args[0]);
        }

        System.out.println("$> The program has finished executing.");
    }

    @SuppressWarnings("unchecked")
    private static void generateReport(String path) {
        System.out.printf("%s> File has been loaded. \n", path);
        try (Connection con = JdbcHelper.getConnection()) {
            ReportGenerator rep = null;
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String str = null, mainPath = null, subPath = null;
                int cnt = 0;
                while ((str = br.readLine()) != null) {
                    try {
                        switch (cnt) {
                            case 0: {
                                mainPath = str;
                                break;
                            }
                            case 1: {
                                subPath = str;
                                break;
                            }
                            case 2: {
                                System.out.printf("%s> Compiling .jrxml at path %s\r", path, mainPath);
                                rep = new ReportGenerator(con, mainPath, subPath, splitAndTrim(str));
                                System.out.printf("%s> Successfully compiled .jrxml at path %s\n", path, mainPath);
                                break;
                            }
                            default: {
                                System.out.printf("%s> Reading parameters on line %d (%s)\r", path, cnt + 1, str);
                                String[] args = splitAndTrim(str);
                                if (args.length == rep.getParamCount()) {
                                    rep.insertArguments(args, null);
                                } else if (args.length == rep.getParamCount() + 1) {
                                    rep.insertArguments(Arrays.copyOfRange(args, 0, args.length - 1), args[args.length - 1]);
                                }
                                System.out.printf("%s> Successfully registered parameters on line %d.\r", path, cnt + 1);
                                break;
                            }

                        }
                    } catch (ParamLengthMismatchException ex) {
                        System.out.printf("%s> Skipping line %d. A mismatch between args and params was encountered.\r", path, cnt + 1);
                    }

                    cnt++;
                }
                
                System.out.printf("%s> Reached the end of file after line %d \n", path, cnt);
            }
            
            System.out.printf("%s> Starting report generation...\n", path);
            for (int i = 0; i < rep.getReportCount(); i++) {
                System.out.printf("%s> %s | (%d / %d) Generating report to path %s\r", path, progressBar(50, i + 1, rep.getReportCount()), i + 1, rep.getReportCount(), rep.getDestPath(i));
                rep.generateReport(i);
            }
            System.out.printf("%s> Report generation completed.\n", path);

        } catch (JRException ex) {
            System.out.printf("%s> .jrxml compilation has failed. Aborting.\n", path);
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.printf("%s> This file is either missing or invalid. Aborting.\n", path);
        } catch (SQLException ex) {
            //will never happen
        }
    }

    private static void registerDatabase(String path) throws IOException, SQLException {
        String url = null, user = null, pass = null;
        int cnt = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String str = null;
            url = br.readLine();
            user = br.readLine();
            pass = br.readLine();
            System.out.println("Starting JDBC connection.");
            System.out.printf("$> Database URL: %s\n", url);
            System.out.printf("$> Username: %s\n", user);
            System.out.printf("$> Password: %s\n", pass.replaceAll(".", "*"));
            JdbcHelper.config("oracle.jdbc.driver.OracleDriver", url, user, pass);
        } catch (ClassNotFoundException ex) {
            //this will never happen
        }
        
        try (Connection con = JdbcHelper.getConnection()) {
            System.out.println("JDBC connection established.");
            //just for testing the connectivity
        }
    }

    private static String[] splitAndTrim(String s) {
        String[] arr = s.split(",");
        for (int i = 0; i < arr.length; i++) {
            arr[i] = arr[i].trim();
        }
        return arr;
    }

    private static String progressBar(int maxBars, int current, int count) {
        double mult = ((double) current) / count;
        int barCount = (int) Math.floor(mult * maxBars);
        StringBuilder str = new StringBuilder("|");
        for (int i = 1; i <= maxBars; i++) {
            if (barCount >= i) {
                str.append("=");
            } else {
                str.append(" ");
            }
        }
        str.append("|");
        return str.toString();
    }
}
