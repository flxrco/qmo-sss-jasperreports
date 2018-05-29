/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jasper.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author User
 */
public class JdbcHelper {
    private static String url;
    private static Properties prop;
    
    public static void config(String driverName, String url, String username, String password) throws ClassNotFoundException {
        Class.forName(driverName);
        prop = new Properties();
        prop.put("user", username);
        prop.put("password", password);
        JdbcHelper.url = url;
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, prop);
    }
    
    public static boolean testConnection() {
        try (Connection con = getConnection()) {
            return true;
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }
}
