package topgym_system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Records what staff do. Call from anywhere:
 *     ActivityLog.log("Added member", "Juan Dela Cruz");
 * The table (activity_tbl) is created automatically.
 * Logging never throws: if it fails, the app keeps working.
 */
public class ActivityLog {

    private static boolean tableReady = false;

    public static void log(String action, String details) {
        try (Connection con = DBConnection.connect()) {
            ensureTable(con);
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO activity_tbl (staff_name, action, details) VALUES (?, ?, ?)")) {
                ps.setString(1, currentUser());
                ps.setString(2, action);
                ps.setString(3, details);
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.err.println("ActivityLog failed: " + ex.getMessage());
        }
    }

    /** Who is logged in. CHANGE THIS LINE if your UserSession uses a different method name. */
    private static String currentUser() {
        try {
            return UserSession.getUsername();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    static void ensureTable(Connection con) throws SQLException {
        if (tableReady) {
            return;
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS activity_tbl ("
                    + "log_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "log_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "staff_name VARCHAR(50) NOT NULL, "
                    + "action VARCHAR(60) NOT NULL, "
                    + "details VARCHAR(255), "
                    + "INDEX idx_log_time (log_time))");
        }
        tableReady = true;
    }
}