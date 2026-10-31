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
 
 
public class attendance extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(attendance.class.getName());
   private java.util.List<MemberInfo> allMembers = new java.util.ArrayList<>();
    private java.util.Set<Integer> checkedInToday = new java.util.HashSet<>();
        private java.util.Map<Integer, java.time.LocalTime> checkInTimes = new java.util.HashMap<>();
   private String filterText = "";
    private java.time.LocalDate currentDate = java.time.LocalDate.now();
private java.time.LocalDate lastToday = java.time.LocalDate.now();
    
    private static final Color RED = new Color(226, 30, 30);
    private static final Color MUTED = new Color(150, 152, 160);
    private JButton dateButton;
    private JButton presentButton;
    private JTextField barSearch;
    private JScrollPane gridScroll;
 
    public attendance() {
        setUndecorated(true);
        initComponents();
 
        // true full screen (covers the taskbar)
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screen.width, screen.height);
        SideMenu.attach(this, "Attendance");
 
        // the old form date label / search box are replaced by the filter bar
        dateLabel.setVisible(false);
        searchField.setVisible(false);
 
        attendanceContainer.setLayout(new BorderLayout(0, 16));
        attendanceContainer.setPreferredSize(new Dimension(1380, 540));
        attendanceContainer.setOpaque(false);
        attendanceContainer.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));   // push the bar lower (raise/lower this number)
        attendanceContainer.add(buildFilterBar(), BorderLayout.NORTH);
 
        allMembers = loadMembers();
        refreshAttendance();
 
        // midnight rollover
        new javax.swing.Timer(60000, e -> {
            java.time.LocalDate now = java.time.LocalDate.now();
            if (!now.equals(lastToday)) {
                boolean viewingToday = currentDate.equals(lastToday);
                lastToday = now;
                if (viewingToday) {
                    currentDate = now;
                    refreshAttendance();
                }
            }
        }).start();
    }
 
    private java.util.List<MemberInfo> loadMembers() {
        java.util.List<MemberInfo> list = new java.util.ArrayList<>();
        // archived members are hidden from the check-in cards
        String sql = "SELECT mem_id, first_name, last_name, age, address, contact_number, gender, end_date "
                + "FROM members_tbl WHERE archived = 0 ORDER BY mem_id";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                MemberInfo m = new MemberInfo(
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("age"),
                        rs.getString("address"),
                        rs.getString("contact_number"));
                java.sql.Date end = rs.getDate("end_date");
                m.setId(rs.getInt("mem_id"));
                m.setGender(rs.getString("gender"));
                if (end != null) {
                    m.setEndDate(end.toLocalDate());
                }
                list.add(m);
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not load members: " + ex.getMessage());
        }
        return list;
    }
    private void loadTodayAttendance() {
    checkedInToday.clear();
    checkInTimes.clear();
    String sql = "SELECT mem_id, check_in FROM attendance_tbl WHERE att_date = ?";
    try (java.sql.Connection con = DBConnection.connect();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setDate(1, java.sql.Date.valueOf(currentDate));
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("mem_id");
                checkedInToday.add(id);
                java.sql.Time t = rs.getTime("check_in");
                if (t != null) checkInTimes.put(id, t.toLocalTime());
            }
        }
    } catch (java.sql.SQLException ex) {
        javax.swing.JOptionPane.showMessageDialog(this, "Could not load attendance: " + ex.getMessage());
    }
}
 
private void markAttendance(MemberInfo member) {
    if (!currentDate.equals(java.time.LocalDate.now())) {
        javax.swing.JOptionPane.showMessageDialog(this, "You can only change attendance for today.");
        return;
    }
 
    // already present: offer to undo
    if (checkedInToday.contains(member.getId())) {
        int undo = javax.swing.JOptionPane.showConfirmDialog(this,
                member.getFullName() + " is already checked in.\nUndo this check-in?",
                "Undo Check-In", javax.swing.JOptionPane.YES_NO_OPTION);
        if (undo != javax.swing.JOptionPane.YES_OPTION) return;
 
        String sql = "DELETE FROM attendance_tbl WHERE mem_id = ? AND att_date = ?";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, member.getId());
            ps.setDate(2, java.sql.Date.valueOf(currentDate));
            ps.executeUpdate();
            // Activity log: who undid this check-in
            ActivityLog.log("Undid check-in",
                    member.getFullName() + String.format(" (TG-%03d)", member.getId()));
            loadTodayAttendance();
            setupAttendanceGrid();
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not undo check-in: " + ex.getMessage());
        }
        return;
    }
 
    if (member.isExpired()) {
        javax.swing.JOptionPane.showMessageDialog(this,
                member.getFullName() + "'s membership has expired.\nPlease renew it first.",
                "Membership Expired", javax.swing.JOptionPane.WARNING_MESSAGE);
        return;
    }
 
    int choice = javax.swing.JOptionPane.showConfirmDialog(this,
            "Check in " + member.getFullName() + "?",
            "Confirm Check-In", javax.swing.JOptionPane.YES_NO_OPTION);
    if (choice != javax.swing.JOptionPane.YES_OPTION) return;
 
    String sql = "INSERT INTO attendance_tbl (mem_id, att_date, check_in) VALUES (?, CURDATE(), CURTIME())";
    try (java.sql.Connection con = DBConnection.connect();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setInt(1, member.getId());
        ps.executeUpdate();
        // Activity log: who marked this member present
        ActivityLog.log("Marked present",
                member.getFullName() + String.format(" (TG-%03d)", member.getId()));
        loadTodayAttendance();
        setupAttendanceGrid();
    } catch (java.sql.SQLException ex) {
        javax.swing.JOptionPane.showMessageDialog(this, "Could not record attendance: " + ex.getMessage());
    } }   private javax.swing.Icon createAvatarIcon(int size) {
        return new javax.swing.Icon() {
            @Override
            public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(java.awt.Color.WHITE);
                int head = size * 2 / 5;
                g2.fillOval(x + (size - head) / 2, y + size / 10, head, head);
                int bodyW = size * 3 / 4;
                g2.fillArc(x + (size - bodyW) / 2, y + size * 11 / 20, bodyW, size * 4 / 5, 0, 180);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
    private void updateDateLabel() {
        if (dateButton != null) {
            dateButton.setText(currentDate.format(
                    java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH)));
        }
    }
 
    /** Shows how many people were present on the day currently selected. */
    private void updatePresentButton() {
        if (presentButton != null) {
            presentButton.setText("Present: " + checkedInToday.size());
        }
    }
 
    private void refreshAttendance() {
    updateDateLabel();
    loadTodayAttendance();
    setupAttendanceGrid();
}
 
    private void setupAttendanceGrid() {
    updatePresentButton();
    if (gridScroll != null) attendanceContainer.remove(gridScroll);
 
    javax.swing.JPanel cardsPanel = new javax.swing.JPanel(new java.awt.GridLayout(0, 4, 20, 20));
    cardsPanel.setOpaque(false);
 
    int shown = 0;
    for (MemberInfo m : allMembers) {
        if (m.getFullName().toLowerCase().contains(filterText)) {
            cardsPanel.add(createAttendanceCard(m));
            shown++;
        }
    }
    while (shown > 0 && shown % 6 != 0) {
        javax.swing.JPanel filler = new javax.swing.JPanel();
        filler.setOpaque(false);
        cardsPanel.add(filler);
        shown++;
    }
 
    // FlowLayout keeps the grid at its natural size instead of stretching it
    javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
wrapper.setOpaque(false);
wrapper.add(cardsPanel, java.awt.BorderLayout.NORTH);
 
    javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(wrapper);
    scroll.setPreferredSize(new java.awt.Dimension(1380, 450));
    scroll.getViewport().setScrollMode(javax.swing.JViewport.SIMPLE_SCROLL_MODE);
    scroll.setOpaque(false);
    scroll.getViewport().setOpaque(false);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scroll.getVerticalScrollBar().setUnitIncrement(10);
    scroll.getVerticalScrollBar().setPreferredSize(new java.awt.Dimension(10, 0));
 
    gridScroll = scroll;
    attendanceContainer.add(scroll, java.awt.BorderLayout.CENTER);
    attendanceContainer.revalidate();
    attendanceContainer.repaint();
}
    private javax.swing.JPanel createAttendanceCard(MemberInfo member) {
        final boolean[] hover = {false};
        final boolean present = checkedInToday.contains(member.getId());
        javax.swing.JPanel card = new javax.swing.JPanel(new java.awt.BorderLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(present ? new java.awt.Color(30, 70, 40, 220)
          : hover[0] ? new java.awt.Color(45, 45, 45, 235)
                     : new java.awt.Color(30, 30, 30, 210));
g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
g2.setColor(present ? new java.awt.Color(76, 175, 80)
          : hover[0] ? new java.awt.Color(255, 51, 51)
                     : new java.awt.Color(70, 70, 70));
g2.setStroke(new java.awt.BasicStroke(present || hover[0] ? 2.5f : 1f));
g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new java.awt.Dimension(230, 220));
        card.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
 
        javax.swing.JLabel icon = new javax.swing.JLabel(createAvatarIcon(110), javax.swing.SwingConstants.CENTER);
        javax.swing.JLabel name = new javax.swing.JLabel(member.getFullName(), javax.swing.SwingConstants.CENTER);
        name.setForeground(java.awt.Color.WHITE);
        name.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        name.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 18, 0));
 
        card.add(icon, java.awt.BorderLayout.CENTER);
        card.add(name, java.awt.BorderLayout.SOUTH);
 
        java.awt.event.MouseAdapter ma = new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover[0] = true; card.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                java.awt.Point p = javax.swing.SwingUtilities.convertPoint(
                        (java.awt.Component) e.getSource(), e.getPoint(), card);
                if (!card.contains(p)) { hover[0] = false; card.repaint(); }
            }
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
    markAttendance(member);
            }
        };
        card.addMouseListener(ma);
        icon.addMouseListener(ma);
        name.addMouseListener(ma);
 
        
        return card; }
        
 
    // ================= FILTER BAR (search + present + date) =================
    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 21, 24, 230));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.setColor(new Color(58, 59, 66));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        bar.setPreferredSize(new Dimension(1380, 92));
 
        barSearch = new SearchField("Search member...");
        barSearch.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                filterText = barSearch.getText().trim().toLowerCase();
                setupAttendanceGrid();
            }
            @Override public void insertUpdate(DocumentEvent e)  { update(); }
            @Override public void removeUpdate(DocumentEvent e)  { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
 
        dateButton = new DateButton();
        dateButton.addActionListener(e -> showCalendar());
        updateDateLabel();
 
        presentButton = new PresentButton();
        presentButton.addActionListener(e -> showPresentDialog(currentDate));
        updatePresentButton();
 
        // present count sits just left of the date; the date stays on the far right
        JPanel right = new JPanel(new BorderLayout(12, 0));
        right.setOpaque(false);
        right.add(presentButton, BorderLayout.WEST);
        right.add(dateButton, BorderLayout.EAST);
 
        bar.add(barSearch, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
 
    // ================= PRESENT COUNT =================
    /** Lists everyone who checked in on a day, with the total. Use the arrows to move between days. */
    private void showPresentDialog(java.time.LocalDate startDate) {
        final java.time.LocalDate[] day = {startDate};
        final java.time.format.DateTimeFormatter dateFmt =
                java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH);
        final java.time.format.DateTimeFormatter timeFmt =
                java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH);
 
        JDialog dialog = new JDialog(this, "Present Members", true);
        dialog.setLayout(new BorderLayout());
 
        JLabel title = new JLabel("", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
 
        JLabel total = new JLabel("", SwingConstants.CENTER);
        total.setForeground(new Color(255, 77, 77));
        total.setFont(new Font("Segoe UI", Font.BOLD, 20));
        total.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
 
        String[] cols = {"NAME", "CHECK-IN"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setBackground(new Color(22, 22, 22));
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? new Color(26, 26, 26) : new Color(34, 34, 34));
                l.setForeground(Color.WHITE);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 15));
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 40));
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(14, 14, 14));
                l.setForeground(new Color(255, 77, 77));
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });
 
        // reloads the list for day[0]
        Runnable load = () -> {
            model.setRowCount(0);
            String sql = "SELECT m.first_name, m.last_name, a.check_in FROM attendance_tbl a "
                    + "JOIN members_tbl m ON m.mem_id = a.mem_id "
                    + "WHERE a.att_date = ? ORDER BY a.check_in, m.first_name";
            try (java.sql.Connection con = DBConnection.connect();
                 java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, java.sql.Date.valueOf(day[0]));
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.sql.Time t = rs.getTime("check_in");
                        model.addRow(new Object[]{
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            t == null ? "-" : t.toLocalTime().format(timeFmt)
                        });
                    }
                }
            } catch (java.sql.SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Could not load attendance: " + ex.getMessage());
            }
            title.setText(day[0].format(dateFmt) + (day[0].equals(java.time.LocalDate.now()) ? "  (Today)" : ""));
            int n = model.getRowCount();
            total.setText("Total present: " + n);
        };
 
        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(new Color(204, 0, 0));
        head.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        head.add(navArrow("‹", () -> {
            day[0] = day[0].minusDays(1);
            load.run();
        }), BorderLayout.WEST);
        head.add(title, BorderLayout.CENTER);
        head.add(navArrow("›", () -> {
            if (day[0].isBefore(java.time.LocalDate.now())) {
                day[0] = day[0].plusDays(1);
                load.run();
            }
        }), BorderLayout.EAST);
 
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(22, 22, 22));
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(520, 340));
 
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(51, 51, 51));
        JPanel closeRow = new JPanel();
        closeRow.setOpaque(false);
        closeRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));
        closeRow.add(close);
        bottom.add(total, BorderLayout.NORTH);
        bottom.add(closeRow, BorderLayout.SOUTH);
 
        dialog.add(head, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
 
        load.run();
        dialog.pack();
        dialog.setMinimumSize(new Dimension(520, 360));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
 
    private JLabel navArrow(String text, Runnable action) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 28));
        l.setForeground(Color.WHITE);
        l.setPreferredSize(new Dimension(44, 36));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
        return l;
    }
 
    private void showCalendar() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLineBorder(new Color(70, 71, 79)));
        menu.setBackground(new Color(30, 31, 36));
        menu.add(new CalendarPanel(menu));
        menu.show(dateButton, dateButton.getWidth() - 340, dateButton.getHeight() + 4);   // right edges line up
    }
 
    private void pickDate(java.time.LocalDate d, JPopupMenu menu) {
        currentDate = d;
        menu.setVisible(false);
        refreshAttendance();
    }
 
    // month calendar shown under the date button
    private class CalendarPanel extends JPanel {
        private final JPopupMenu menu;
        private java.time.YearMonth shownMonth;
        private final JLabel title = new JLabel("", SwingConstants.CENTER);
        private final JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));
 
        CalendarPanel(JPopupMenu menu) {
            this.menu = menu;
            shownMonth = java.time.YearMonth.from(currentDate);
            setLayout(new BorderLayout(0, 10));
            setBackground(new Color(30, 31, 36));
            setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
            setPreferredSize(new Dimension(340, 370));
 
            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(Color.WHITE);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(arrow("‹", -1), BorderLayout.WEST);
            top.add(title, BorderLayout.CENTER);
            top.add(arrow("›", 1), BorderLayout.EAST);
 
            grid.setOpaque(false);
 
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(link("Today", () -> pickDate(java.time.LocalDate.now(), menu)), BorderLayout.EAST);
 
            add(top, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
            rebuild();
        }
 
        private JLabel arrow(String text, int delta) {
            JLabel l = new JLabel(text, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 26));
            l.setForeground(Color.WHITE);
            l.setPreferredSize(new Dimension(36, 30));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    shownMonth = shownMonth.plusMonths(delta);
                    rebuild();
                }
            });
            return l;
        }
 
        private JLabel link(String text, Runnable action) {
            JLabel l = new JLabel(text);
            l.setFont(new Font("Segoe UI", Font.BOLD, 15));
            l.setForeground(new Color(255, 77, 77));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
            });
            return l;
        }
 
        private void rebuild() {
            title.setText(shownMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
                    + " " + shownMonth.getYear());
            grid.removeAll();
            for (String d : new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"}) {
                JLabel l = new JLabel(d, SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setForeground(MUTED);
                grid.add(l);
            }
            int lead = shownMonth.atDay(1).getDayOfWeek().getValue() % 7;   // Sunday = 0
            int cells = 0;
            for (int i = 0; i < lead; i++, cells++) grid.add(new JLabel());
            for (int day = 1; day <= shownMonth.lengthOfMonth(); day++, cells++) {
                grid.add(dayCell(shownMonth.atDay(day)));
            }
            while (cells < 42) { grid.add(new JLabel()); cells++; }
            grid.revalidate();
            grid.repaint();
        }
 
        private JComponent dayCell(java.time.LocalDate date) {
            final boolean selected = date.equals(currentDate);
            final boolean today = date.equals(java.time.LocalDate.now());
            JLabel l = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER) {
                boolean hover;
                {
                    addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                        @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) { pickDate(date, menu); }
                    });
                }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int sz = Math.min(getWidth(), getHeight()) - 2;
                    int x = (getWidth() - sz) / 2, y = (getHeight() - sz) / 2;
                    if (selected) {
                        g2.setColor(RED);
                        g2.fillOval(x, y, sz, sz);
                    } else if (hover) {
                        g2.setColor(new Color(60, 61, 68));
                        g2.fillOval(x, y, sz, sz);
                    } else if (today) {
                        g2.setColor(RED);
                        g2.drawOval(x, y, sz, sz);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            l.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 16));
            l.setForeground(Color.WHITE);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return l;
        }
    }
 
    // rounded search box with a magnifier and hint text
    private static class SearchField extends JTextField {
        private final String hint;
 
        SearchField(String hint) {
            this.hint = hint;
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 20));
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(0, 58, 0, 16));
            setPreferredSize(new Dimension(650, 56));
        }
 
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(34, 35, 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(hasFocus() ? new Color(226, 30, 30) : new Color(70, 71, 79));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
 
            // magnifier
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = getHeight() / 2;
            g2.drawOval(22, cy - 11, 16, 16);
            g2.drawLine(35, cy + 4, 42, cy + 11);
 
            if (getText().isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, 58, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    // date pill: calendar icon, date text, chevron
    private static class DateButton extends JButton {
        DateButton() {
            super("");
            setFont(new Font("Segoe UI", Font.PLAIN, 20));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 64, 0, 56));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(320, 56));
        }
 
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(34, 35, 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(getModel().isRollover() ? new Color(110, 112, 124) : new Color(70, 71, 79));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
 
            // calendar icon
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ix = 22, iy = (getHeight() - 24) / 2;
            g2.drawRoundRect(ix, iy + 3, 24, 21, 5, 5);
            g2.drawLine(ix, iy + 10, ix + 24, iy + 10);
            g2.drawLine(ix + 7, iy, ix + 7, iy + 6);
            g2.drawLine(ix + 17, iy, ix + 17, iy + 6);
            g2.fillRect(ix + 6, iy + 14, 3, 3);
            g2.fillRect(ix + 11, iy + 14, 3, 3);
            g2.fillRect(ix + 16, iy + 14, 3, 3);
 
            // chevron
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = getHeight() / 2;
            int cx = getWidth() - 34;
            g2.drawLine(cx - 8, cy - 3, cx, cy + 5);
            g2.drawLine(cx, cy + 5, cx + 8, cy - 3);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    // present-count pill: "Present: 5"
    private static class PresentButton extends JButton {
        PresentButton() {
            super("Present: 0");
            setFont(new Font("Segoe UI", Font.BOLD, 20));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("See who is present on this day");
            setPreferredSize(new Dimension(220, 56));
        }
 
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hot = getModel().isRollover();
            g2.setColor(hot ? new Color(204, 0, 0) : new Color(34, 35, 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(hot ? new Color(255, 77, 77) : new Color(70, 71, 79));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgPanel31 = new topgym_system.BgPanel3();
        attendanceContainer = new javax.swing.JPanel();
        dateLabel = new javax.swing.JLabel();
        searchField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout attendanceContainerLayout = new javax.swing.GroupLayout(attendanceContainer);
        attendanceContainer.setLayout(attendanceContainerLayout);
        attendanceContainerLayout.setHorizontalGroup(
            attendanceContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1430, Short.MAX_VALUE)
        );
        attendanceContainerLayout.setVerticalGroup(
            attendanceContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 321, Short.MAX_VALUE)
        );

        dateLabel.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        searchField.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        searchField.addActionListener(this::searchFieldActionPerformed);

        javax.swing.GroupLayout bgPanel31Layout = new javax.swing.GroupLayout(bgPanel31);
        bgPanel31.setLayout(bgPanel31Layout);
        bgPanel31Layout.setHorizontalGroup(
            bgPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bgPanel31Layout.createSequentialGroup()
                .addGap(171, 171, 171)
                .addComponent(dateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 468, Short.MAX_VALUE)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 179, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(442, 442, 442))
            .addGroup(bgPanel31Layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addComponent(attendanceContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        bgPanel31Layout.setVerticalGroup(
            bgPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bgPanel31Layout.createSequentialGroup()
                .addContainerGap(237, Short.MAX_VALUE)
                .addGroup(bgPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bgPanel31Layout.createSequentialGroup()
                        .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(51, 51, 51))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bgPanel31Layout.createSequentialGroup()
                        .addComponent(dateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(34, 34, 34)))
                .addComponent(attendanceContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(68, 68, 68))
        );

        getContentPane().add(bgPanel31, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchFieldActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
    try {
        javax.swing.UIManager.setLookAndFeel(
            javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception ex) {
        logger.log(java.util.logging.Level.SEVERE, null, ex);
    }
    java.awt.EventQueue.invokeLater(() -> new attendance().setVisible(true));
}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel attendanceContainer;
    private topgym_system.BgPanel3 bgPanel31;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables
}
