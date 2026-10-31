package topgym_system;

/**
 * Settings popup: Change Password, Add Staff (admin only), Gym Prices, Activity Log, Backup Members (CSV).
 * Open it from anywhere with:  SettingsDialog.show(frame);
 */
public class SettingsDialog {

    private static final java.awt.Color RED = new java.awt.Color(204, 0, 0);
    private static final java.awt.Color LABEL_RED = new java.awt.Color(255, 77, 77);
    private static final java.awt.Color PANEL = new java.awt.Color(51, 51, 51);
    private static final java.awt.Color FIELD = new java.awt.Color(30, 30, 30);

    // ================= MAIN WINDOW =================
    public static void show(javax.swing.JFrame parent) {
        // Settings is locked: only admins can open it
        if (!UserSession.isAdmin()) {
            javax.swing.JOptionPane.showMessageDialog(parent,
                    "Settings is for admins only.",
                    "Access denied",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        javax.swing.JDialog d = new javax.swing.JDialog(parent, "Settings", true);
        d.setLayout(new java.awt.BorderLayout());
        d.add(header("Settings"), java.awt.BorderLayout.NORTH);

        javax.swing.JPanel body = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 14));
        body.setBackground(PANEL);
        body.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 40, 30, 40));

        body.add(bigButton("Change Password", RED, () -> changePassword(d)));

        // Only admins even see this button
        if (UserSession.isAdmin()) {
            body.add(bigButton("Add Staff Account", RED, () -> addStaff(d)));
            body.add(bigButton("Staff List", RED, () -> StaffListDialog.show(d)));
        }

        body.add(bigButton("Gym Prices", RED, () -> prices(d)));
        body.add(bigButton("Activity Log", RED, () -> ActivityLogDialog.show(d)));
        body.add(bigButton("Backup Members (CSV)", RED, () -> backup(d)));
        body.add(bigButton("Close", new java.awt.Color(90, 90, 90), d::dispose));

        d.add(body, java.awt.BorderLayout.CENTER);
        d.pack();
        d.setMinimumSize(new java.awt.Dimension(420, 0));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }

    // ================= ADD STAFF (ADMIN ONLY) =================
    private static void addStaff(java.awt.Window owner) {
        // Second check, in case this method is ever called from somewhere else
        if (!UserSession.isAdmin()) {
            javax.swing.JOptionPane.showMessageDialog(owner, "Only an admin can add staff accounts.");
            return;
        }

        javax.swing.JDialog d = new javax.swing.JDialog(owner, "Add Staff Account", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new java.awt.BorderLayout());
        d.add(header("Add Staff Account"), java.awt.BorderLayout.NORTH);

        javax.swing.JTextField user = new javax.swing.JTextField(18);
        javax.swing.JPasswordField pass = new javax.swing.JPasswordField(18);
        javax.swing.JPasswordField confirm = new javax.swing.JPasswordField(18);
        styleField(user);
        styleField(pass);
        styleField(confirm);

        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(3, 2, 10, 15));
        form.setBackground(PANEL);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
        form.add(label("Username:"));         form.add(user);
        form.add(label("Password:"));         form.add(pass);
        form.add(label("Confirm Password:")); form.add(confirm);

        javax.swing.JButton save = redButton("Create");
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        cancel.addActionListener(e -> d.dispose());

        save.addActionListener(e -> {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            String cf = new String(confirm.getPassword());

            if (u.isEmpty() || p.isEmpty() || cf.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(d, "Please fill in all fields.");
                return;
            }
            if (!p.equals(cf)) {
                javax.swing.JOptionPane.showMessageDialog(d, "Password and confirmation do not match.");
                return;
            }
            if (p.length() < 4) {
                javax.swing.JOptionPane.showMessageDialog(d, "Password must be at least 4 characters.");
                return;
            }

            String check = "SELECT 1 FROM ad_tbl WHERE uname = ?";
            // The role is always 'staff' here, so nobody can create another admin from this form
            String insert = "INSERT INTO ad_tbl (uname, password, role) VALUES (?, ?, 'staff')";

            try (java.sql.Connection con = DBConnection.connect()) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(check)) {
                    ps.setString(1, u);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            javax.swing.JOptionPane.showMessageDialog(d, "That username already exists.");
                            return;
                        }
                    }
                }
                try (java.sql.PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setString(1, u);
                    ps.setString(2, p);
                    ps.executeUpdate();
                }
                ActivityLog.log("Created staff account", u);
                javax.swing.JOptionPane.showMessageDialog(d, "Staff account \"" + u + "\" created.");
                d.dispose();
            } catch (java.sql.SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(d, "Could not create account: " + ex.getMessage());
            }
        });

        d.add(form, java.awt.BorderLayout.CENTER);
        d.add(buttonRow(save, cancel), java.awt.BorderLayout.SOUTH);
        d.pack();
        d.setMinimumSize(new java.awt.Dimension(460, 300));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    // ================= CHANGE PASSWORD =================
    private static void changePassword(java.awt.Window owner) {
        javax.swing.JDialog d = new javax.swing.JDialog(owner, "Change Password", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new java.awt.BorderLayout());
        d.add(header("Change Password"), java.awt.BorderLayout.NORTH);

        javax.swing.JTextField user = new javax.swing.JTextField(18);
        javax.swing.JPasswordField current = new javax.swing.JPasswordField(18);
        javax.swing.JPasswordField newPass = new javax.swing.JPasswordField(18);
        javax.swing.JPasswordField confirm = new javax.swing.JPasswordField(18);
        styleField(user);
        styleField(current);
        styleField(newPass);
        styleField(confirm);

        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(4, 2, 10, 15));
        form.setBackground(PANEL);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
        form.add(label("Username:"));         form.add(user);
        form.add(label("Current Password:")); form.add(current);
        form.add(label("New Password:"));     form.add(newPass);
        form.add(label("Confirm Password:")); form.add(confirm);

        javax.swing.JButton save = redButton("Save");
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        cancel.addActionListener(e -> d.dispose());

        save.addActionListener(e -> {
            String u = user.getText().trim();
            String cur = new String(current.getPassword());
            String np = new String(newPass.getPassword());
            String cf = new String(confirm.getPassword());

            if (u.isEmpty() || cur.isEmpty() || np.isEmpty() || cf.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(d, "Please fill in all fields.");
                return;
            }
            if (!np.equals(cf)) {
                javax.swing.JOptionPane.showMessageDialog(d, "New password and confirmation do not match.");
                return;
            }
            if (np.length() < 4) {
                javax.swing.JOptionPane.showMessageDialog(d, "New password must be at least 4 characters.");
                return;
            }
            // The database may ignore upper/lower case when searching, so the exact
            // match (capital letters included) is checked here in Java instead.
            String find = "SELECT user_id, uname, password FROM ad_tbl WHERE uname = ?";
            try (java.sql.Connection con = DBConnection.connect();
                 java.sql.PreparedStatement ps = con.prepareStatement(find)) {
                ps.setString(1, u);
                int matchId = -1;
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (u.equals(rs.getString("uname")) && cur.equals(rs.getString("password"))) {
                            matchId = rs.getInt("user_id");
                            break;
                        }
                    }
                }
                if (matchId < 0) {
                    javax.swing.JOptionPane.showMessageDialog(d, "Username or current password is incorrect.");
                    return;
                }
                try (java.sql.PreparedStatement up = con.prepareStatement(
                        "UPDATE ad_tbl SET password = ? WHERE user_id = ?")) {
                    up.setString(1, np);
                    up.setInt(2, matchId);
                    up.executeUpdate();
                }
                ActivityLog.log("Changed password", "Account: " + u);
                javax.swing.JOptionPane.showMessageDialog(d, "Password changed.");
                d.dispose();
            } catch (java.sql.SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(d, "Could not change password: " + ex.getMessage());
            }
        });

        d.add(form, java.awt.BorderLayout.CENTER);
        d.add(buttonRow(save, cancel), java.awt.BorderLayout.SOUTH);
        d.pack();
        d.setMinimumSize(new java.awt.Dimension(460, 340));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    // ================= GYM PRICES =================
    private static void prices(java.awt.Window owner) {
        javax.swing.JDialog d = new javax.swing.JDialog(owner, "Gym Prices", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new java.awt.BorderLayout());
        d.add(header("Gym Prices"), java.awt.BorderLayout.NORTH);

        javax.swing.JTextField member = new javax.swing.JTextField(String.valueOf(getInt("price_member", 500)), 18);
        javax.swing.JTextField nonMember = new javax.swing.JTextField(String.valueOf(getInt("price_nonmember", 700)), 18);
        javax.swing.JTextField student = new javax.swing.JTextField(String.valueOf(getInt("price_student", 500)), 18);
        javax.swing.JTextField days = new javax.swing.JTextField(String.valueOf(getInt("expiring_days", 7)), 18);
        styleField(member);
        styleField(nonMember);
        styleField(student);
        styleField(days);

        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(4, 2, 10, 15));
        form.setBackground(PANEL);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
        form.add(label("Member (per month):"));      form.add(member);
        form.add(label("Non-member (per month):"));  form.add(nonMember);
        form.add(label("Student (per month):"));     form.add(student);
        form.add(label("Expiring soon (days):"));    form.add(days);

        javax.swing.JButton save = redButton("Save");
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        cancel.addActionListener(e -> d.dispose());

        save.addActionListener(e -> {
            try {
                int m = Integer.parseInt(member.getText().trim());
                int n = Integer.parseInt(nonMember.getText().trim());
                int s = Integer.parseInt(student.getText().trim());
                int x = Integer.parseInt(days.getText().trim());
                if (m < 0 || n < 0 || s < 0 || x < 0) {
                    throw new NumberFormatException();
                }
                saveSetting("price_member", String.valueOf(m));
                saveSetting("price_nonmember", String.valueOf(n));
                saveSetting("price_student", String.valueOf(s));
                saveSetting("expiring_days", String.valueOf(x));
                ActivityLog.log("Changed prices",
                        "Member " + m + ", Non-member " + n + ", Student " + s + ", Expiring days " + x);
                javax.swing.JOptionPane.showMessageDialog(d, "Prices saved.");
                d.dispose();
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(d, "Please enter whole numbers (0 or more).");
            } catch (java.sql.SQLException ex) {
                javax.swing.JOptionPane.showMessageDialog(d, "Could not save: " + ex.getMessage());
            }
        });

        d.add(form, java.awt.BorderLayout.CENTER);
        d.add(buttonRow(save, cancel), java.awt.BorderLayout.SOUTH);
        d.pack();
        d.setMinimumSize(new java.awt.Dimension(460, 340));
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    // ---- settings storage (settings_tbl, created automatically) ----
    private static void ensureTable(java.sql.Connection con) throws java.sql.SQLException {
        try (java.sql.Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS settings_tbl ("
                    + "setting_key VARCHAR(50) PRIMARY KEY, "
                    + "setting_value VARCHAR(100) NOT NULL)");
        }
    }

    /** Other pages can call this, e.g. SettingsDialog.getInt("price_member", 500). */
    public static int getInt(String key, int fallback) {
        try (java.sql.Connection con = DBConnection.connect()) {
            ensureTable(con);
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                    "SELECT setting_value FROM settings_tbl WHERE setting_key = ?")) {
                ps.setString(1, key);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Integer.parseInt(rs.getString(1));
                    }
                }
            }
        } catch (Exception ex) {
            // fall through to the default
        }
        return fallback;
    }

    private static void saveSetting(String key, String value) throws java.sql.SQLException {
        try (java.sql.Connection con = DBConnection.connect()) {
            ensureTable(con);
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO settings_tbl (setting_key, setting_value) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
        }
    }

    // ================= BACKUP (CSV) =================
    private static void backup(java.awt.Window owner) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Save members backup");
        chooser.setSelectedFile(new java.io.File("topgym_members_" + java.time.LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(owner) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new java.io.File(file.getPath() + ".csv");
        }
        if (file.exists() && javax.swing.JOptionPane.showConfirmDialog(owner,
                "That file already exists. Replace it?", "Replace file",
                javax.swing.JOptionPane.YES_NO_OPTION) != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "SELECT mem_id, first_name, last_name, age, address, contact_number, gender, start_date, end_date "
                + "FROM members_tbl ORDER BY mem_id";
        int count = 0;
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery();
             java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                     new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {

            pw.print('\uFEFF');   // lets Excel read the file as UTF-8
            pw.println("ID,First Name,Last Name,Age,Address,Contact Number,Gender,Start Date,End Date");
            while (rs.next()) {
                pw.println(String.join(",",
                        csv(String.format("TG-%03d", rs.getInt("mem_id"))),
                        csv(rs.getString("first_name")),
                        csv(rs.getString("last_name")),
                        csv(rs.getString("age")),
                        csv(rs.getString("address")),
                        "=\"" + nz(rs.getString("contact_number")) + "\"",   // keeps the leading 0 in Excel
                        csv(rs.getString("gender")),
                        csv(rs.getString("start_date")),
                        csv(rs.getString("end_date"))));
                count++;
            }
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(owner, "Backup failed: " + ex.getMessage());
            return;
        }
        ActivityLog.log("Backup", "Exported " + count + " members");
        javax.swing.JOptionPane.showMessageDialog(owner,
                "Saved " + count + " members to:\n" + file.getAbsolutePath());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String csv(String s) {
        return "\"" + nz(s).replace("\"", "\"\"") + "\"";
    }

    // ================= STYLE HELPERS =================
    private static javax.swing.JLabel header(String text) {
        javax.swing.JLabel title = new javax.swing.JLabel(text, javax.swing.SwingConstants.CENTER);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(RED);
        title.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 15, 0));
        return title;
    }

    private static javax.swing.JLabel label(String text) {
        javax.swing.JLabel l = new javax.swing.JLabel(text);
        l.setForeground(LABEL_RED);
        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        return l;
    }

    private static void styleField(javax.swing.JTextField f) {
        f.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        f.setBackground(FIELD);
        f.setForeground(java.awt.Color.WHITE);
        f.setCaretColor(java.awt.Color.WHITE);
    }

    private static javax.swing.JButton redButton(String text) {
        javax.swing.JButton b = new javax.swing.JButton(text);
        b.setBackground(RED);
        b.setForeground(java.awt.Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private static javax.swing.JButton bigButton(String text, java.awt.Color color, Runnable action) {
        javax.swing.JButton b = new javax.swing.JButton(text);
        b.setBackground(color);
        b.setForeground(java.awt.Color.WHITE);
        b.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        b.setFocusPainted(false);
        b.setPreferredSize(new java.awt.Dimension(340, 52));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static javax.swing.JPanel buttonRow(javax.swing.JButton... buttons) {
        javax.swing.JPanel p = new javax.swing.JPanel();
        p.setBackground(PANEL);
        p.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 15, 0));
        for (javax.swing.JButton b : buttons) {
            p.add(b);
        }
        return p;
    }
}