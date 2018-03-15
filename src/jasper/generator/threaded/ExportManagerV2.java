/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator.threaded;

import jasper.generator.model.RenderManager;
import jasper.generator.model.exceptions.EmptyRenderQueueException;
import jasperreports.models.JdbcHelper;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import net.sf.jasperreports.engine.JRException;

/**
 * The manager class for multi-threaded report generation.
 * @author Felix
 */
public class ExportManagerV2 {

    private RenderManager[] renderers;
    final private Set<Thread> pool;
    
    private boolean finished = false;
    protected int exported, failed;
    private final int limit, init;

    /**
     * Constructor for a an ExportManager object;
     * @param limit int that represents how many active threads at an time an instance can have
     * @param renderers an array that contains RenderManager objects
     */
    public ExportManagerV2(int limit, RenderManager... renderers) {
        pool = new HashSet<>();
        this.renderers = renderers;
        this.limit = limit;
        exported = 0;
        failed = 0;
        int initLocal = 0;
        for (RenderManager rm : renderers) {
            initLocal += rm.countInitial();
        }
        this.init = initLocal;
    }

    /**
     * Begins the exporting process of all of the RenderManagers that were input in the constructor.
     */
    public void export() {
        Set<Integer> finished = new HashSet<>();

        for (int i = 0; finished.size() < renderers.length; i++) { //loop until all objects in the array are in the set
            
            if (!finished.contains(i)) { //if the renderer object is in the finished set, skip it
                while (pool.size() >= limit) { //keep looping until the pool has a vacant spot
                    //loop forever
                }

                final RenderManager renderer = renderers[i]; //get the reference of the current RenderManager
                final int index = i;
                
                Thread thread = new Thread() {
                    
                    @Override
                    public void run() {
                        try {
                            renderer.render(JdbcHelper.getConnection());
                            exported++;
                            if (renderer.countQueued() == 0) {
                                finished.add(index);
                            }
                        } catch (JRException ex) {
                            failed++;
                        } catch (EmptyRenderQueueException ex) {
                            //this will never happen
                        } catch (SQLException ex) {
                            failed++;
                        } finally {
                            pool.remove(this);
                        }
                    }
                };
                
                pool.add(thread);
                thread.start();
            }
        }
        this.finished = true;
    }
    
    public int initCount() {
        return init;
    }
    
    public int exportCount() {
        return exported;
    }
    
    public int failedCount() {
        return failed;
    }
    
    public boolean isFinished() {
        return finished;
    }
}
