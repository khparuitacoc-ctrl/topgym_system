package topgym_system;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Admin-only viewer for activity_tbl. Browse day by day. */
public class ActivityLogDialog {

    private static final Color RED = new Color(204, 0, 0);
    private static final Color PANEL = new Color(51, 51, 51);
    private static final Color FIELD = new Color(30, 30, 30);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm:ss a");

    private static LocalDate current;

    public static void show(Window owner) {
        if (!UserSession.isAdmin()) {
            JOptionPane.showMessageDialog(owner, "Only an admin can view the activity log.");
            return;
        }
        current = LocalDate.now();

        JDialog d = new JDialog(owner, "Activity Log", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());

        JLabel title = new JLabel("Activity Log", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(RED);
        title.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // ---- date navigation ----
        JButton prev = redButton("<  Prev");
        JButton next = redButton("Next  >");
        JButton today = redButton("Today");
        JTextField dateField = new JTextField(current.format(DAY), 9);
        dateField.setHorizontalAlignment(SwingConstants.CENTER);
        dateField.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dateField.setBackground(FIELD);
        dateField.setForeground(Color.WHITE);
        dateField.setCaretColor(Color.WHITE);
        JButton go = redButton("Go");

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        nav.setBackground(PANEL);
        nav.add(prev);
        nav.add(dateField);
        nav.add(go);
        nav.add(next);
        nav.add(today);

        // ---- table ----
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Time", "Staff", "Action", "Details"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(FIELD);
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 70));
        table.setSelectionBackground(new Color(120, 20, 20));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(RED);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(550);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(FIELD);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        scroll.setBackground(PANEL);

        JLabel count = new JLabel(" ", SwingConstants.CENTER);
        count.setForeground(Color.WHITE);
        count.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        count.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JButton close = new JButton("Close");
        close.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(PANEL);
        south.add(count, BorderLayout.NORTH);
        JPanel closeRow = new JPanel();
        closeRow.setBackground(PANEL);
        closeRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        closeRow.add(close);
        south.add(closeRow, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout());
        top.add(title, BorderLayout.NORTH);
        top.add(nav, BorderLayout.CENTER);

        Runnable refresh = () -> {
            dateField.setText(current.format(DAY));
            load(d, model, count, current);
        };

        prev.addActionListener(e -> { current = current.minusDays(1); refresh.run(); });
        next.addActionListener(e -> { current = current.plusDays(1); refresh.run(); });
        today.addActionListener(e -> { current = LocalDate.now(); refresh.run(); });
        go.addActionListener(e -> {
            try {
                current = LocalDate.parse(dateField.getText().trim(), DAY);
                refresh.run();
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(d, "Use the format yyyy-MM-dd, e.g. 2026-10-13");
            }
        });
        dateField.addActionListener(e -> go.doClick());

        d.add(top, BorderLayout.NORTH);
        d.add(scroll, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        d.getContentPane().setBackground(PANEL);
        d.setPreferredSize(new Dimension(1280, 520));
        d.pack();
        d.setLocationRelativeTo(owner);

        refresh.run();
        d.setVisible(true);
    }

    private static void load(JDialog d, DefaultTableModel model, JLabel count, LocalDate day) {
        model.setRowCount(0);
        String sql = "SELECT log_time, staff_name, action, details FROM activity_tbl "
                + "WHERE log_time >= ? AND log_time < ? ORDER BY log_time DESC";
        try (Connection con = DBConnection.connect()) {
            ActivityLog.ensureTable(con);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(day.atStartOfDay()));
                ps.setTimestamp(2, Timestamp.valueOf(day.plusDays(1).atStartOfDay()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getTimestamp(1).toLocalDateTime().format(TIME),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4) == null ? "" : rs.getString(4)
                        });
                    }
                }
            }
            count.setText(model.getRowCount() == 0
                    ? "No activity on " + day.format(DAY)
                    : model.getRowCount() + " action(s) on " + day.format(DAY));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(d, "Could not load log: " + ex.getMessage());
        }
    }

    private static JButton redButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }
}