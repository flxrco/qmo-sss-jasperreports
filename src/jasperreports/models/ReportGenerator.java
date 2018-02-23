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

    private final Connection connection;

    private final JasperReport report;

    private final String[] params;
    private final List<Map<String, Object>> args;
    private final List<String> destPaths;
    private final String subReports;
    
    
    public ReportGenerator(Connection connection, String mainReportPath, String subReportDirPath, String[] params) throws JRException {
        this.connection = connection;
        report = JasperCompileManager.compileReport(mainReportPath);
        this.params = params;
        args = new ArrayList<>();
        destPaths = new ArrayList<>();
        subReports = subReportDirPath;
    }

    public Map<String, Object> insertArguments(String[] args, String destPath) throws ParamLengthMismatchException {
        if (params.length != args.length) {
            throw new ParamLengthMismatchException();
        } else {
            Map<String, Object> argMap = new HashMap<>();

            for (int i = 0; i < params.length; i++) {
                argMap.put(params[i], args[i]);
            }
            
            argMap.put("SUBREPORT_DIR", subReports);
            
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
    
    public String getDestPath(int index) {
        return destPaths.get(index);
    }
    
    public int getReportCount() {
        return args.size();
    }
    
    public int getParamCount() {
        return params.length;
    }
}
