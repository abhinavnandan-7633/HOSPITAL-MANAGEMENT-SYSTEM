package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DB {
    private static final String URL =
            "jdbc:mysql://localhost:3306/hospital_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Abhi@7633";

    public static Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC driver not found. Add mysql-connector-j to the classpath.");
            return null;
        } catch (SQLException e) {
            System.err.println("Database Connection Error: " + e.getMessage());
            return null;
        }
    }

    public static void ensureSchema() {
        try (Connection conn = connect()) {
            if (conn == null) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users ("
                                + "patient_id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "username VARCHAR(50) UNIQUE NOT NULL,"
                                + "password VARCHAR(100) NOT NULL,"
                                + "name VARCHAR(100) NOT NULL,"
                                + "phone VARCHAR(15),"
                                + "role VARCHAR(20) DEFAULT 'patient'"
                                + ")");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS patients ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "name VARCHAR(100) NOT NULL,"
                                + "age INT NOT NULL,"
                                + "gender VARCHAR(10) NOT NULL,"
                                + "phone VARCHAR(15)"
                                + ")");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS staff ("
                                + "staff_id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "username VARCHAR(50) UNIQUE NOT NULL,"
                                + "password VARCHAR(100) NOT NULL,"
                                + "name VARCHAR(100) NOT NULL,"
                                + "role VARCHAR(20) NOT NULL,"
                                + "specialization VARCHAR(50)"
                                + ")");
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS appointments ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                                + "patient_id INT,"
                                + "doctor_id INT,"
                                + "session VARCHAR(20) NOT NULL,"
                                + "token_no INT NOT NULL,"
                                + "status VARCHAR(20) DEFAULT 'Waiting',"
                                + "diagnosis TEXT,"
                                + "prescription TEXT,"
                                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                                + "FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE"
                                + ")");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, name, phone, role) "
                            + "VALUES ('admin', 'admin123', 'Hospital Admin', '0000000000', 'admin') "
                            + "ON DUPLICATE KEY UPDATE username = username")) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Schema setup error: " + e.getMessage());
        }
    }
}