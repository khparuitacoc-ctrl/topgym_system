package topgym_system;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * Remembers the logged-in person's name (read from ad_tbl) for the "Welcome back" text.
 *   after login:   UserName.load(userId, username);
 *   on any page:   UserName.get()
 * It looks for first_name + last_name, or a full_name / name column.
 * If the table has none of those, it falls back to the username they typed.
 */
public final class UserName {

    private static String name = "";

    private UserName() { }

    public static String get() {
        return name;
    }

    public static void load(int userId, String username) {
        name = username == null ? "" : username;   // fallback
        try (Connection con = DBConnection.connect();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM ad_tbl WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;

                ResultSetMetaData md = rs.getMetaData();
                Map<String, String> cols = new HashMap<>();   // lowercase -> real column name
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    cols.put(md.getColumnLabel(i).toLowerCase(), md.getColumnLabel(i));
                }

                String first = value(rs, cols, "first_name", "firstname", "fname");
                String last = value(rs, cols, "last_name", "lastname", "lname");
                if (first != null) {
                    name = last != null ? first + " " + last : first;
                    return;
                }
                String full = value(rs, cols, "full_name", "fullname", "name", "admin_name", "fname");
                if (full != null) name = full;
            }
        } catch (Exception ex) {
            ex.printStackTrace();   // keep the username fallback
        }
    }

    private static String value(ResultSet rs, Map<String, String> cols, String... candidates) throws Exception {
        for (String c : candidates) {
            String real = cols.get(c);
            if (real != null) {
                String v = rs.getString(real);
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return null;
    }
}