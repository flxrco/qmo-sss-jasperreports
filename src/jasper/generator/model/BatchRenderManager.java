/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator.model;

import jasper.generator.model.exceptions.EmptyRenderQueueException;
import jasperreports.models.JdbcHelper;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.jasperreports.engine.JRException;

/**
 *
 * @author Felix
 */
public class BatchRenderManager {

    private final RenderManager[] renderers;
    private final Set<Integer> finished;
    private final int initial;
    
    private int counter, rendered;

    public BatchRenderManager(RenderManager[] renderers) {
        this.renderers = renderers;
        finished = new HashSet<>();
        counter = 0;
        int tempInitial = 0;
        for (RenderManager renderer : renderers) {
            tempInitial += renderer.countInitial();
        }
        initial = tempInitial;
    }

    public BatchRenderManager(CompileManager[] compilers) {
        renderers = new RenderManager[compilers.length];
        for (int i = 0; i < compilers.length; i++) {
            renderers[i] = compilers[i].getRenderer();
        }
        finished = new HashSet<>();
        counter = 0;
        int tempInitial = 0;
        for (RenderManager renderer : renderers) {
            tempInitial += renderer.countInitial();
        }
        initial = tempInitial;
    }

    public Map<String, Object> render() throws JRException, EmptyRenderQueueException, SQLException {
        if (finished.size() == renderers.length) {
            throw new EmptyRenderQueueException();
        }
        
        RenderManager renderer = renderers[counter];
        try {
            Map<String, Object> result =  renderer.render(JdbcHelper.getConnection());
            rendered++;
            return result;
        } finally {
            if (renderer.countQueued() == 0) {
                finished.add(counter);
            }
            
            counter = Math.floorMod(counter + 1, renderers.length);
            
            if (finished.size() < renderers.length) {
                while (finished.contains(counter)) {
                    counter = Math.floorMod(counter + 1, renderers.length);
                }
            }
        }
    }

    public int countRendered() {
        return rendered;
    }

    public int countInitial() {
        return initial;
    }
    
    public int countQueued() {
        return initial - rendered;
    }
    
    public boolean isEmpty() {
        return initial == rendered;
    }
}
