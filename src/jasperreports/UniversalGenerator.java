/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasperreports;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

/**
 *
 * @author User
 */
public class UniversalGenerator {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            String[] arr = new String[5];

            System.out.print("DB URL> ");
            arr[0] = sc.nextLine();
            System.out.print("USERNAME> ");
            arr[1] = sc.nextLine();
            System.out.print("PASSWORD> ");
            arr[2] = sc.nextLine();

            JdbcHelper.config("oracle.jdbc.driver.OracleDriver", arr[0], arr[1], arr[2]);

            System.out.print(".JRXML FILE> ");
            arr[3] = sc.nextLine();
            System.out.print(".TXT FILE> ");
            arr[4] = sc.nextLine();

            generateReports(arr[3], arr[4]);
        } catch (ClassNotFoundException ex) {
            System.out.println("CLASS NOT FOUND!");
        }

    }

    public static void generateReports(String src, String paramSrc) {
        try (Connection con = JdbcHelper.getCon()) { //establish connection to oracle db
            JasperReport jasperReport = JasperCompileManager.compileReport(src); //compile the .jrxml to be used
            try (BufferedReader br = new BufferedReader(new FileReader(paramSrc))) {
                String str; //cache for the lines that the bufferedreader is reading
                int counter = 0;
                List<String> paramNames = new ArrayList<>(); //paramnames that the .jrxml file uses
                while ((str = br.readLine()) != null) {
                    String[] split = str.split(","); //format: param1,param2,param3,dest

                    if (counter == 0) { //if true, this means that the BR is reading the first line of the .txt file
                        for (String s : split) { //that also means that it SHOULD contain the param names
                            s = s.trim();
                            paramNames.add(s);
                        }
                    } else {
                        String dest = "report" + counter;

                        /*
                        GRAMMAR:
                        <txtline> -> <params>","<txtline><newline>
                        <params> -> <param> | <param>","<params>
                         */
                        Map<String, Object> params = new HashMap<>();

                        int i = 0;
                        for (String s : split) {
                            s = s.trim();
                            if (i + 1 > paramNames.size()) {
                                dest = s;
                                break;
                            } else {
                                params.put(paramNames.get(i), s);
                            }
                        }

                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, con);
                        JasperExportManager.exportReportToPdfFile(jasperPrint, dest);
                    }
                    counter++;
                }
            }
        } catch (Exception oex) {
            System.out.println("Exception encountered: ");
            System.out.println(oex.toString());
        }
    }
    
    //codes below are unused
    
    private String generatePdfReport(Connection connection, JasperReport jasperReport, String params, List<String> paramNames) throws JRException {
        /*
        CFG:
        <txtline> -> <params>","<txtline><newline>
        <params> -> <param> | <param>","<params>
         */
        
        String dest = "report" + params.hashCode();
        
        Map<String, Object> paramMap = new HashMap<>();
        
        int i = 0;
        for (String s : params.split(",")) {
            s = s.trim();
            if (i + 1 > paramNames.size()) {
                dest = s;
                break;
            } else {
                paramMap.put(paramNames.get(i), s);
            }
        }
        
        return generatePdfReport(connection, jasperReport, paramMap, dest);
    }

    private String generatePdfReport(Connection connection, JasperReport jasperReport, Map<String, Object> parameters, String destinationFile) throws JRException {
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);
        JasperExportManager.exportReportToPdfFile(jasperPrint, destinationFile);
        return destinationFile;
    }
}
