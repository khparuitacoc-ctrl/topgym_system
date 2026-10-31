package topgym_system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Admin-only list of accounts (username + role). Passwords are never shown.
 * "Archive" hides a staff account and blocks it from logging in (nothing is deleted).
 * "Show Archived" lets you see archived accounts and Restore them.
 */
public class StaffListDialog {

    private static final Color RED = new Color(204, 0, 0);
    private static final Color PANEL = new Color(51, 51, 51);
    private static final Color FIELD = new Color(30, 30, 30);

    public static void show(Window owner) {
        if (!UserSession.isAdmin()) {
            JOptionPane.showMessageDialog(owner, "Only an admin can view the staff list.");
            return;
        }

        final boolean[] showArchived = {false};

        JDialog d = new JDialog(owner, "Staff List", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());

        JLabel title = new JLabel("Staff List", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(RED);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        DefaultTableModel model = new DefaultTableModel(new String[]{"Username", "Role"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        table.setBackground(FIELD);
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 70));
        table.setSelectionBackground(new Color(120, 20, 20));
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(RED);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(FIELD);
        scroll.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        scroll.setBackground(PANEL);

        JLabel count = new JLabel(" ", SwingConstants.CENTER);
        count.setForeground(Color.WHITE);
        count.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        count.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JButton actionBtn = new JButton("Archive");
        actionBtn.setBackground(RED);
        actionBtn.setForeground(Color.WHITE);
        actionBtn.setFocusPainted(false);
        JButton toggleBtn = new JButton("Show Archived");
        JButton close = new JButton("Close");
        close.addActionListener(e -> d.dispose());

        // reloads the table for the current view (active or archived)
        Runnable load = () -> {
            model.setRowCount(0);
            try (Connection con = DBConnection.connect()) {
                ensureArchivedColumn(con);
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT uname, role FROM ad_tbl WHERE archived = ? AND role = 'staff' ORDER BY uname")) {
                    ps.setInt(1, showArchived[0] ? 1 : 0);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String role = rs.getString("role");
                            model.addRow(new Object[]{
                                rs.getString("uname"),
                                role == null || role.isEmpty() ? ""
                                    : role.substring(0, 1).toUpperCase() + role.substring(1)
                            });
                        }
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Could not load staff: " + ex.getMessage());
            }
            title.setText(showArchived[0] ? "Archived Staff" : "Staff List");
            actionBtn.setText(showArchived[0] ? "Restore" : "Archive");
            toggleBtn.setText(showArchived[0] ? "Show Active" : "Show Archived");
            count.setText(model.getRowCount() + (showArchived[0] ? " archived account(s)" : " account(s)"));
        };

        toggleBtn.addActionListener(e -> {
            showArchived[0] = !showArchived[0];
            load.run();
        });

        actionBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(d, "Select an account first.");
                return;
            }
            String uname = String.valueOf(model.getValueAt(row, 0));
            String role = String.valueOf(model.getValueAt(row, 1));

            if (!showArchived[0]) {
                // ---- archive ----
                if ("admin".equalsIgnoreCase(role)) {
                    JOptionPane.showMessageDialog(d, "Admin accounts can't be archived.");
                    return;
                }
                int c = JOptionPane.showConfirmDialog(d,
                        "Archive \"" + uname + "\"?\nThey will no longer be able to log in.",
                        "Archive Staff", JOptionPane.YES_NO_OPTION);
                if (c != JOptionPane.YES_OPTION) {
                    return;
                }
                if (setArchived(d, uname, true)) {
                    ActivityLog.log("Archived staff account", uname);
                    load.run();
                }
            } else {
                // ---- restore ----
                int c = JOptionPane.showConfirmDialog(d,
                        "Restore \"" + uname + "\"?\nThey will be able to log in again.",
                        "Restore Staff", JOptionPane.YES_NO_OPTION);
                if (c != JOptionPane.YES_OPTION) {
                    return;
                }
                if (setArchived(d, uname, false)) {
                    ActivityLog.log("Restored staff account", uname);
                    load.run();
                }
            }
        });

        JPanel buttons = new JPanel();
        buttons.setBackground(PANEL);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        buttons.add(actionBtn);
        buttons.add(toggleBtn);
        buttons.add(close);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(PANEL);
        south.add(count, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);

        d.add(title, BorderLayout.NORTH);
        d.add(scroll, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        d.getContentPane().setBackground(PANEL);
        d.setPreferredSize(new Dimension(500, 440));
        d.pack();
        d.setLocationRelativeTo(owner);

        load.run();
        d.setVisible(true);
    }

    /** Archives or restores one account. Admin accounts can never be archived here. */
    private static boolean setArchived(JDialog d, String uname, boolean archived) {
        String sql = "UPDATE ad_tbl SET archived = ? WHERE uname = ? AND role = 'staff'";
        if (!archived) {
            sql = "UPDATE ad_tbl SET archived = 0 WHERE uname = ? AND role = 'staff'";
        }
        try (Connection con = DBConnection.connect()) {
            ensureArchivedColumn(con);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (archived) {
                    ps.setInt(1, 1);
                    ps.setString(2, uname);
                } else {
                    ps.setString(1, uname);
                }
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(d, "Could not update account: " + ex.getMessage());
            return false;
        }
    }

    /** Adds the "archived" column to ad_tbl the first time it is needed. Safe to call every time. */
    static void ensureArchivedColumn(Connection con) throws SQLException {
        try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, "ad_tbl", "archived")) {
            if (rs.next()) {
                return;
            }
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE ad_tbl ADD COLUMN archived TINYINT(1) NOT NULL DEFAULT 0");
        }
    }
}