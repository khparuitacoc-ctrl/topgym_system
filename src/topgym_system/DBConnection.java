/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Khastrel Troy Ruita
 */
public class DBConnection {
     public static Connection connect() {
       
      try {           
            String url = "jdbc:MySQL://localhost:3306/topgym_system";
            String user = "root";
            String pass = "";   
            Connection conn = DriverManager.getConnection(url, user, pass);
         return conn;        
      }catch (SQLException e) {
          
          System.out.println("Database connection failed: " + e.getMessage());
      }
      return null;
     }
}
