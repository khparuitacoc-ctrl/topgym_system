package topgym_system;
 
/**
 *
 * @author Khastrel Troy Ruita
 */
public class UserSession {
    private static int userId;
    private static String username;
    private static String role;
 
    public static void setUser(int id, String user, String userRole) {
        userId = id;
        username = user;
        role = userRole;
    }
 
    public static int getUserId() {
        return userId;
    }
 
    public static String getUsername() {
        return username;
    }
 
    public static String getRole() {
        return role;
    }
 
    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
 
    public static boolean isStaff() {
        return "staff".equalsIgnoreCase(role);
    }
 
    public static void clearSession() {
        userId = 0;
        username = null;
        role = null;
    }
}