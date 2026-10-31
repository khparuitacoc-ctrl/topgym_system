/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;

 


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Attendance page: name-only cards with a Check In button.
 * Adjust DB_URL / USER / PASS and the members table/column names to match your project
 * .
 */
public class AttendancePanel extends JPanel {


    private static final Color CARD_BG = new Color(28, 28, 28);
    private static final Color RED = new Color(255, 20, 20);
    private static final Color GREEN = new Color(76, 175, 80);

    private final JTextField searchField = new JTextField();
    private final JPanel grid = new JPanel(new GridLayout(0, 4, 15, 15));
    private final Map<Integer, String> members = new LinkedHashMap<>(); // id -> name
    private final Map<Integer, String> present = new LinkedHashMap<>(); // id -> time in

    public AttendancePanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        // Top bar: search + today's date
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        searchField.setBackground(new Color(20, 20, 20));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createLineBorder(RED));
        searchField.setPreferredSize(new Dimension(300, 38));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshCards(); }
            public void removeUpdate(DocumentEvent e) { refreshCards(); }
            public void changedUpdate(DocumentEvent e) { refreshCards(); }
        });

        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        date.setForeground(Color.WHITE);
        date.setFont(new Font("SansSerif", Font.BOLD, 18));

        top.add(searchField, BorderLayout.WEST);
        top.add(date, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Card grid (wrapped in NORTH so rows keep a fixed height)
        grid.setOpaque(false);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(grid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(holder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Skip the database while NetBeans is showing the Design view
        if (!java.beans.Beans.isDesignTime()) {
            loadData();
        }
        refreshCards();
    }

    /** Loads all members and today's attendance from the database. */
    private void loadData() {
        members.clear();
        present.clear();
        try (Connection c = DBConnection.connect()) {
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Could not connect to the database.");
                return;
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT mem_id, CONCAT(first_name, ' ', last_name) AS full_name "
                                 + "FROM members_tbl ORDER BY first_name")) {
                while (rs.next()) members.put(rs.getInt("mem_id"), rs.getString("full_name"));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT mem_id, time_in FROM attendance_tbl WHERE att_date = ?")) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) present.put(rs.getInt("mem_id"), formatTime(rs.getTime("time_in")));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    /** Rebuilds the cards, applying the search filter. */
    private void refreshCards() {
        grid.removeAll();
        String q = searchField.getText().trim().toLowerCase();
        for (Map.Entry<Integer, String> m : members.entrySet()) {
            if (q.isEmpty() || m.getValue().toLowerCase().contains(q)) {
                grid.add(buildCard(m.getKey(), m.getValue()));
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel buildCard(int id, String name) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 60)),
                BorderFactory.createEmptyBorder(25, 15, 25, 15)));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel status = new JLabel();
        status.setFont(new Font("SansSerif", Font.BOLD, 13));
        status.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn = new JButton("Check In");
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(RED);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));

        if (present.containsKey(id)) {
            markPresent(btn, status, present.get(id));
        } else {
            status.setText("Absent");
            status.setForeground(Color.GRAY);
            btn.addActionListener(e -> checkIn(id, btn, status));
        }

        card.add(nameLbl);
        card.add(Box.createVerticalStrut(10));
        card.add(status);
        card.add(Box.createVerticalStrut(15));
        card.add(btn);
        return card;
    }

    private void checkIn(int memberId, JButton btn, JLabel status) {
        // Uses the PC's own date/time so it matches the clock you see
        String sql = "INSERT INTO attendance_tbl (mem_id, att_date, time_in) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.connect()) {
            if (c == null) {
                JOptionPane.showMessageDialog(this, "Could not connect to the database.");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, memberId);
                ps.setDate(2, Date.valueOf(LocalDate.now()));
                ps.setTime(3, Time.valueOf(LocalTime.now().withNano(0)));
                ps.executeUpdate();
            }
            String time = formatTime(new Time(System.currentTimeMillis()));
            present.put(memberId, time);
            // Activity log: who marked this member present
            ActivityLog.log("Marked present",
                    members.get(memberId) + String.format(" (TG-%03d)", memberId));
            markPresent(btn, status, time);
        } catch (SQLIntegrityConstraintViolationException dup) {
            JOptionPane.showMessageDialog(this, "Already checked in today.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void markPresent(JButton btn, JLabel status, String time) {
        status.setText("Present · " + time);
        status.setForeground(GREEN);
        btn.setText("Checked In");
        btn.setEnabled(false);
        for (var l : btn.getActionListeners()) btn.removeActionListener(l);
    }

    private static String formatTime(Time t) {
        return new java.text.SimpleDateFormat("h:mm a").format(t);
    }

}