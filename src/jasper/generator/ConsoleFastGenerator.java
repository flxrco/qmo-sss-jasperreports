/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.generator;

import jasper.generator.model.BatchRenderManager;
import jasper.generator.model.CompileManager;
import jasper.generator.model.exceptions.EmptyRenderQueueException;
import jasper.generator.model.exceptions.ParameterLengthMismatchException;
import jasperreports.models.JdbcHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.sf.jasperreports.engine.JRException;

/**
 *
 * @author Felix
 */
public class ConsoleFastGenerator {

    public static void main(String args[]) {
        generate(10, args[0], Arrays.copyOfRange(args, 1, args.length));
    }
    
    /**
     * Generates the reports in bulk. Has a nifty progress bar to alleviate
     * impatience.
     *
     * @param threadLimit amount of threads allowed for report generation
     * @param jdbcConfigPath the path of the .txt file that contains the jdbc
     * settings
     * @param jrxmlConfigPath the paths of the .txt files that contain
     * jasperreports settings
     */
    public static void generate(int threadLimit, String jdbcConfigPath, String... jrxmlConfigPath) {
        try {
            try {
                jdbcConfigurator(jdbcConfigPath); //set up the jdbc
            } catch (ClassNotFoundException ex) {
                //this wont happen
            }

            ExecutorService compilePool = Executors.newFixedThreadPool(threadLimit); //create a thread pool for the report compilation stage
            List<Future<CompileManager>> futures = new ArrayList<>(); //store the generated reports in a future list
            for (String str : jrxmlConfigPath) { //compiles every path passed as jrxmlConfigPath parameter
                Callable call = (Callable) () -> { //anonymous class for compiling a jrxml file
                    try {
                        return jrxmlCompiler(str);
                    } catch (IOException | SQLException | JRException ex) {
                        System.out.printf("%s> .jrxml compilation failed.\n", str);
                        return null; //if compilation failed, just return null
                    }
                };
                futures.add(compilePool.submit(call));
            }

            compilePool.shutdown();

            List<CompileManager> compiledList = new ArrayList<>();

            try { //inject all contents of the future array to the compiledList array
                for (Future<CompileManager> future : futures) {
                    CompileManager compiled = future.get();
                    if (compiled != null) {
                        compiledList.add(compiled);
                    }
                }
            } catch (InterruptedException | ExecutionException ex) {
                System.out.println("$> Threads were interrupted. Compilation has been aborted.");
                return;
            }

            System.out.printf("$> Successfully compiled %d .jrxml file%s.\n", compiledList.size(), compiledList.size() == 1 ? "" : "s");
            
            
            //begin report generation phase
            final BatchRenderManager batch = new BatchRenderManager(compiledList.toArray(new CompileManager[compiledList.size()]));
            System.out.printf("$> %d report%s have been added to the render queue.\n", batch.countQueued(), batch.countQueued() == 1 ? "" : "s");

            Thread bar = new Thread() { //anonymous class for printing the progress bar
                @Override
                public void run() {
                    long counter = 0;
                    try {
                        while (!batch.isEmpty()) {
                            counter++;
                            System.out.printf("%s\r", progressBar(counter, batch.countRendered(), batch.countInitial()));
                            Thread.sleep(250);

                        }
                    } catch (InterruptedException ex) {
                        System.out.printf("%s\r", progressBar(-1, batch.countInitial(), batch.countInitial()));
                    } finally {
                        System.out.println("");
                    }
                }
            };

            bar.start(); //start the anonmyouse class/thread

            ExecutorService renderPool = Executors.newFixedThreadPool(threadLimit); //thread pool for the generation of the report .pdfs
            for (int i = 0; i < threadLimit; i++) {
                Runnable run = () -> {
                    try {
                        while (!batch.isEmpty()) {
                            try {
                                batch.render();
                            } catch (JRException | SQLException ex) {

                            }
                        }
                    } catch (EmptyRenderQueueException ex) {

                    }
                };
                renderPool.execute(run);
            }
            renderPool.shutdown();
            try {
                renderPool.awaitTermination(1, TimeUnit.DAYS);
                bar.interrupt();
                System.out.println("$> Report generation completed.");
            } catch (InterruptedException ex) {

            }

        } catch (IOException ex) {
            System.out.printf("$> An IOException was encountered: %s\n", ex.getMessage());
        } catch (SQLException ex) {
            System.out.printf("$> An exception was encountered while setting up the JDBC: %s", ex.getMessage());
        } finally {
            System.out.println("$> Terminating.");
        }
    }

    private static void jdbcConfigurator(String jdbcConfigPath) throws IOException, SQLException, ClassNotFoundException {
        try (BufferedReader br = new BufferedReader(new FileReader(jdbcConfigPath))) {
            String url = br.readLine(), user = br.readLine(), pass = br.readLine();
            JdbcHelper.config("oracle.jdbc.driver.OracleDriver", url, user, pass);

            JdbcHelper.getConnection().close(); //test if connection is valid

            System.out.println("$> JDBC configuration successful");
            System.out.printf("$> URL: %d\n", url);
            System.out.printf("$> Username: %s | Password: %s\n", user, pass);
        }
    }

    private static CompileManager jrxmlCompiler(String jrxmlConfigPath) throws IOException, SQLException, JRException {
        try (BufferedReader br = new BufferedReader(new FileReader(jrxmlConfigPath))) {
            String reportPath = br.readLine(), subreportPath = br.readLine();
            List<String> list = new ArrayList<>();
            String[] params = br.readLine().split(",");

            CompileManager compiled = new CompileManager(JdbcHelper.getConnection(), reportPath, subreportPath, params);

            String str;

            while ((str = br.readLine()) == null) {
                try {
                    String[] temp = str.split(",");
                    compiled.insertArguments(Arrays.copyOfRange(temp, 0, params.length), reportPath);
                } catch (ParameterLengthMismatchException ex) {
                }
            }
            
            return compiled;
        }
    }

    private static String progressBar(long counter, int progress, int total) {
        StringBuilder str = new StringBuilder("|");
        int prog = (int) Math.floor((double) progress / total * 50);
        for (int i = 1; i <= 50; i++) {
            if (prog >= i) {
                str.append("¦");
            } else {
                str.append("-");
            }
        }
        char spinner = ' ';

        if (counter == -1) {
            spinner = ' ';
        } else if (counter % 4 == 0) {
            spinner = '|';
        } else if (counter % 3 == 0) {
            spinner = '-';
        } else if (counter % 2 == 0) {
            spinner = '/';
        } else {
            spinner = '|';
        }

        str.append(String.format("| %d of %d | %c", progress, total, spinner));

        return str.toString();
    }
}
