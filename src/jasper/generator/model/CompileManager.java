/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator.model;

import jasper.generator.model.exceptions.ParameterLengthMismatchException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;

/**
 *
 * @author Felix
 */
public class CompileManager {

    protected final Connection con; //connected to the target db. assumes that it is working
    protected final JasperReport report; //the compiled report will be stored here

    protected final String[] params; //stores the parameters
    protected final List<String[]> args; //stores many string arrays that will be paired up with the params. their indeces will determine the pairing
    protected final List<String> destPath; //stores the path where the report with the same index should be generated
    protected final String subreportPath; //contains the path of the dir that holds the .jrxml's subreports

    /**
     * Creates a new CompileManager object.
     * Note: you may experience a major slowdown while creating the constructor because the report layout is being compiled.
     * @param con a Connection object to the database assuming that it is a valid Connection
     * @param reportPath the path (preferably absolute) to the .jrxml file of the main report
     * @param subreportPath the path (preferably absolute) to the directory of the sub reports of the main report
     * @param params the parameters specified in the .jrxml file
     * @throws JRException when the .jrxml is not found or an error was encountered during the compiling process
     */
    public CompileManager(Connection con, String reportPath, String subreportPath, String... params) throws JRException {
        report = JasperCompileManager.compileReport(reportPath); //compiles the .jrxml from the reportPath
        this.con = con;
        this.params = params;
        this.subreportPath = subreportPath;
        args = new ArrayList<>();
        destPath = new ArrayList();
    }

    /**
     * Inserts a set of arguments into the arguments list of the class
     * @param args the set of arguments represented using a String array
     * @param destPath the path (preferably absolute) where the report .pdf should be placed
     * @throws ParameterLengthMismatchException when the number of arguments does not match the number of parameters that was passed in the consturctor
     */
    public void insertArguments(String[] args, String destPath) throws ParameterLengthMismatchException {
        if (params.length != args.length) {
            throw new ParameterLengthMismatchException();
        } else {
            this.args.add(args);
            this.destPath.add(destPath);
        }
    }
    
    /**
     * Retrieves the number of sets of arguments in this class' argument list
     * @return an int representing the number of the args in the argument list
     */
    public int countArguments() {
        return args.size();
    }
    
    /**
     * Retrieves the number of parameters that were passed in the constructor
     * @return an int representing the number of parameters
     */
    public int countParameters() {
        return params.length;
    }
    
    /**
     * Creates a RenderManager object to be manipulated to render the actual reports in .pdf format (separately)
     * @return a RenderManager object
     */
    public RenderManager getRenderer() {
        return new RenderManager(this);
    }
}
