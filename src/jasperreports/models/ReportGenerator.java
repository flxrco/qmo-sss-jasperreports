/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasperreports.models;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class ReportGenerator {

    private Connection connection;

    private JasperReport report;

    private String[] params;
    private List<Map<String, Object>> args;
    private List<String> destPaths;

    public ReportGenerator(Connection connection, String reportPath, String[] params) throws JRException {
        this.connection = connection;
        report = JasperCompileManager.compileReport(reportPath);
        this.params = params;
        args = new ArrayList<>();
        destPaths = new ArrayList<>();
    }

    public Map<String, Object> insertArguments(String[] args, String destPath) throws ParamLengthMismatchException {
        if (params.length != args.length) {
            throw new ParamLengthMismatchException();
        } else {
            Map<String, Object> argMap = new HashMap<>();

            for (int i = 0; i < params.length; i++) {
                argMap.put(params[i], args[i]);
            }
           
            //testing
            argMap.put("SUBREPORT_DIR", "C:/Users/User/Desktop/8_maydagdag/");

            
            this.args.add(argMap);
            destPaths.add(destPath == null ? "jasper-report-" + argMap.hashCode() : destPath);
            return argMap;
        }
    }
    
    public String generateReport(int index) throws JRException {
        JasperPrint jasperPrint = JasperFillManager.fillReport(report, args.get(index), connection);
        JasperExportManager.exportReportToPdfFile(jasperPrint, destPaths.get(index));
        return destPaths.get(index);
    }
    
    public void generateReports() throws JRException {
        for (int i = 0; i < args.size(); i++) {
            generateReport(i);
        }
    }
    
    public int getReportCount() {
        return args.size();
    }
    
    public int getParamCount() {
        return params.length;
    }
}
