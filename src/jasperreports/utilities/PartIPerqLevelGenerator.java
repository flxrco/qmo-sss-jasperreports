/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasperreports.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author User
 */
public class PartIPerqLevelGenerator {
    public static void main(String args[]) {
        File in = new File("C:/Users/User/Desktop/qmo_course.csv"),
                out = new File("C:/Users/User/Desktop/conf.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(in))) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                String str = null;
                
                while ((str = br.readLine()) != null) {
                    String[] arr = str.split(",");
                    for (int i = 1; i <= 5; i++) {
                        bw.write(String.format("%s,%s,%s,%s,%d,C:/Users/User/Desktop/qmo-sss/generated/%s/%s/%s %d.pdf", "01-JAN-15", arr[1], arr[2], arr[3], i, arr[2], arr[4], arr[4], i));
                        bw.newLine();
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
}
