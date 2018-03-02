/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator.model;

import jasper.generator.model.exceptions.EmptyRenderQueueException;
import java.io.File;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * The manager for generating reports.
 * @author Felix
 */
public class RenderManager {
    final CompileManager compiler; //all of the data here will come from the CompileManager
    
    final private Queue<String[]> args; //the queue for rendering. data will come from the compilemanager
    final private Queue<String> destPath;
    
    private int rendered; //counter for the reports generated
    final private int initial; //counter for the amount of arugments initally passed into the queue

    public RenderManager(CompileManager compiler) {
        this.compiler = compiler;
        
        args = new LinkedList<>();
        destPath = new LinkedList<>();
        
        rendered = 0;
        initial = compiler.args.size();
        
        for (int i = 0; i < compiler.args.size(); i++) { //turns the list into a queue
            args.add(compiler.args.get(i));
            destPath.add(compiler.destPath.get(i));
        }
    }
    
    /**
     * Generates the report that on the start of the queue. The dataset to be used is from the Connection argument
     * @param con the Connection object to be used to fetch the dataset
     * @return the Map used as the parameters for the JasperFillManager
     * @throws JRException if a Jaspersoft-related error was encountered
     * @throws EmptyRenderQueueException if this method was called even though the queue is already empty
     */
    public Map<String, Object> render(Connection con) throws JRException, EmptyRenderQueueException {
        Map<String, Object> vals = new HashMap<>(); //map to be passed to the JasperPrint
        String[] tempArgs = this.args.poll(); //dequeues and then passes the values to the tempargs
        String[] tempParams = compiler.params; //passes the param names from the compilemanager
        String dest = destPath.poll(); //dequeues the path from the destPath queue
        
        if (tempArgs == null) { //if triggered, this means the queue is empty
            throw new EmptyRenderQueueException();
        }
        
        for (int i = 0; i < tempParams.length; i++) { //matches the parameters to arguments and then places the in the map
            vals.put(tempParams[i], tempArgs[i]);
        }
        
        vals.put("SUBREPORT_DIR", compiler.subreportPath); //the .jrxml requires this as a parameter. this is where it looks for its subreports
        
        JasperPrint report = JasperFillManager.fillReport(compiler.report, vals, con); //'render' the report with the values
        
        String split[] = dest.split("/");
        new File(String.join("/", Arrays.copyOfRange(split, 0, split.length - 1))).mkdirs();
        //the 2 lines above is in charge of creating the folder
        
        JasperExportManager.exportReportToPdfFile(report, dest); //generates the pdf containing the rendered report
        
        vals.put("DEST_PATH", dest); //adds the render information to the map that can be used as a basis on what was printed
        vals.put("RENDER_NO", ++rendered);
        
        return vals;
    }
    
    /**
     * Generates the report that on the start of the queue. It will use the Connection from the CompileManager to fetch the dataset
     * @return the Map used as the parameters for the JasperFillManager
     * @throws JRException JRException if a Jaspersoft-related error was encountered
     * @throws EmptyRenderQueueException if this method was called even though the queue is already empty
     */
    public Map<String, Object> render() throws JRException, EmptyRenderQueueException {
        return render(compiler.con);
    }
    
    /**
     * Generates all of the reports that are in the queue. It uses the Connection from the CompileManager to fetch the dataset.
     * @throws JRException JRException if a Jaspersoft-related error was encountered
     * @throws EmptyRenderQueueException if this method was called even though the queue is already empty
     */
    public void renderAll() throws JRException, EmptyRenderQueueException {
        for (int i = 0; i < args.size(); i++) {
            render();
        }
    }
    
    public int countInitial() {
        return initial;
    }
    
    public int countRendered() {
        return rendered;
    }
    
    public int countQueued() {
        return args.size();
    }
    
}
