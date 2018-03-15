/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator;

import jasper.generator.model.CompileManager;
import jasper.generator.model.RenderManager;
import jasper.generator.threaded.ExportManager;
import jasperreports.models.JdbcHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jasperreports.engine.JRException;

/**
 *
 * @author User
 */
public class MultithreadedGeneratorTest {

    public static void main(String[] args) {
        args = new String[2];
        args[0] = "C:\\Users\\User\\Desktop\\QMO-SSS\\config\\jdbc.txt";
        args[1] = "C:\\Users\\User\\Desktop\\QMO-SSS\\config\\test.txt";
        int i = 0;
        final List<CompileManager> compiled = new ArrayList<>();
        final Set<Thread> threadPool = new HashSet<>();
        try {
            while (i < args.length) {
                switch (i) {
                    case 0:
                        jdbcConfigure(args[i]);
                        System.out.println("finished configuring jdbc");
                        break;
                    default:
                        final String temp = args[i];
                        System.out.printf("reading %s\n", temp);
                        Thread t = new Thread() {
                            public void run() {
                                try (BufferedReader br = new BufferedReader(new FileReader(temp))) {
                                    String reportPath = br.readLine(), subPath = br.readLine();
                                    List<String> params = new ArrayList<>();
                                    String str;
                                    while ((str = br.readLine()) != null) {
                                        params.add(str);
                                    }

                                    compiled.add(new CompileManager(JdbcHelper.getConnection(), reportPath, subPath, params.toArray(new String[params.size()])));
                                    threadPool.remove(this);
                                } catch (IOException ex) {
                                    System.out.println("THREAD IOEXCEPTION");
                                } catch (SQLException ex) {
                                    System.out.println("THREAD SQLEXCEPTION");
                                } catch (JRException ex) {
                                    System.out.println("THREAD JREXCEPTION");
                                } finally {
                                    System.out.printf("finished compiling %s\n", temp);
                                }
                            }
                        };
                        threadPool.add(t);
                        t.start();
                        break;
                }
                i++;
            }
            System.out.println(threadPool.hashCode());
            while (threadPool.size() > 0) {
            }
            System.out.println("finished compiling all threads");
            List<RenderManager> rendered = new ArrayList<>();
            for (CompileManager c : compiled) {
                rendered.add(c.getRenderer());
            }
            
            final ExportManager exporter = new ExportManager(10, rendered.toArray(new RenderManager[rendered.size()]));
            Thread log = new Thread() {
                @Override
                public void run() {
                    while(!exporter.isFinished()) {
                        System.out.printf("\r%d", exporter.exportCount());
                    }
                }
            };
            log.start();
            
            exporter.export();
        } catch (SQLException ex) {
            if (i == 0) {
                System.out.println("SQLEXCEPTION AT " + i);
                return;
            } else {
                //skip lang
            }
        } catch (IOException ex) {
            if (i == 0) {
                System.out.println("IOEXCEPTION AT " + i);
                return;
            } else {

            }
        } catch (ClassNotFoundException ex) {
            System.out.println("CLASSNOTFOUNDEXCEPTION");
            return;
        }
    }

    private static void jdbcConfigure(String filePath) throws IOException, ClassNotFoundException, SQLException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            JdbcHelper.config("oracle.jdbc.driver.OracleDriver", br.readLine(), br.readLine(), br.readLine());
        }
    }

    private static CompileManager jasperCompiler(String filePath) throws IOException, JRException, SQLException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String reportPath = br.readLine(), subPath = br.readLine();
            List<String> params = new ArrayList<>();
            String str;
            while ((str = br.readLine()) != null) {
                params.add(str);
            }

            return new CompileManager(JdbcHelper.getConnection(), filePath, subPath, params.toArray(new String[params.size()]));
        }
    }
}
