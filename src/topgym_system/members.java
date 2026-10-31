
 
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package topgym_system;
 
/**
 *
 * @author Khastrel Troy Ruita
 */
public class members extends javax.swing.JFrame {
 
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(members.class.getName());
    private java.util.List<MemberInfo> allMembers = new java.util.ArrayList<>();
    private java.util.List<MemberInfo> visibleMembers = new java.util.ArrayList<>();
    private String filterText = "";
 
    // ---- table ----
    private static final int COL_ID = 0, COL_NAME = 1, COL_AGE = 2, COL_GENDER = 3,
            COL_ADDRESS = 4, COL_CONTACT = 5, COL_STATUS = 6, COL_ACTIONS = 7;
    private static final int EDIT_X = 10, EDIT_W = 70, RENEW_X = 90, RENEW_W = 130, BTN_H = 32;
 
    private javax.swing.JTable membersTable;
    private javax.swing.table.DefaultTableModel tableModel;
    private int hoverRow = -1;
 
    public members() {
        setUndecorated(true);
        initComponents();
 
        // true full screen + side menu
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screen.width, screen.height);
        SideMenu.attach(this, "Members");
        addExpiringButton();
        addArchiveButton();
        addMemberBtn.setBackground(new java.awt.Color(204, 0, 0));
        addMemberBtn.setForeground(java.awt.Color.WHITE);
        addMemberBtn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        addMemberBtn.setFocusPainted(false);
        addMemberBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        addMemberBtn.addActionListener(e -> showAddMemberDialog());
 
        membersContainer.setLayout(new java.awt.BorderLayout());
        searchField.setBackground(new java.awt.Color(30, 30, 30));
        searchField.setForeground(java.awt.Color.WHITE);
        searchField.setCaretColor(java.awt.Color.WHITE);
        searchField.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        searchField.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 51, 51)),
                javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)));
 
        buildTable();
        allMembers = loadMembers();
        setupMembersGrid();
 
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void update() {
                filterText = searchField.getText().trim().toLowerCase();
                setupMembersGrid();
            }
 
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
 
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
 
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
        });
    }
 
    // ================= DATA =================
    private java.util.List<MemberInfo> loadMembers() {
        java.util.List<MemberInfo> list = new java.util.ArrayList<>();
        String sql = "SELECT mem_id, first_name, last_name, age, address, contact_number, gender, end_date "
                + "FROM members_tbl WHERE archived = 0 ORDER BY mem_id";
        try (java.sql.Connection con = DBConnection.connect(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {
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
 
    // ================= TABLE =================
    private void buildTable() {
        String[] cols = {"ID", "NAME", "AGE", "GENDER", "ADDRESS", "CONTACT", "MEMBERSHIP", "ACTIONS"};
        tableModel = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        membersTable = new javax.swing.JTable(tableModel);
        membersTable.setRowHeight(58);
        membersTable.setShowGrid(false);
        membersTable.setIntercellSpacing(new java.awt.Dimension(0, 0));
        membersTable.setRowSelectionAllowed(false);
        membersTable.setFocusable(false);
        membersTable.setBackground(new java.awt.Color(22, 22, 22));
        membersTable.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        membersTable.setFillsViewportHeight(true);
 
        CellRenderer cellRenderer = new CellRenderer();
        for (int i = 0; i < COL_ACTIONS; i++) {
            membersTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        membersTable.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(new ActionsRenderer());
 
        int[] widths = {95, 230, 70, 120, 270, 190, 270, 135};
        
        for (int i = 0; i < widths.length; i++) {
            membersTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
 
        javax.swing.table.JTableHeader header = membersTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new java.awt.Dimension(100, 46));
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(new java.awt.Color(14, 14, 14));
                l.setForeground(new java.awt.Color(255, 77, 77));
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });
 
        membersTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int r = membersTable.rowAtPoint(e.getPoint());
                if (r != hoverRow) {
                    hoverRow = r;
                    membersTable.repaint();
                }
            }
        });
        membersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoverRow = -1;
                membersTable.repaint();
            }
 
            @Override
public void mouseClicked(java.awt.event.MouseEvent e) {
    int row = membersTable.rowAtPoint(e.getPoint());
    int col = membersTable.columnAtPoint(e.getPoint());
    if (row < 0 || col < 0 || row >= visibleMembers.size()) {
        return;
    }
    MemberInfo m = visibleMembers.get(row);
    if (col == COL_ACTIONS) {
        java.awt.Rectangle r = membersTable.getCellRect(row, col, false);
        int relX = e.getX() - r.x;
        if (relX >= EDIT_X && relX <= EDIT_X + EDIT_W) {
            showEditMemberDialog(m, null);
        }
    } else {
        showMemberDetails(m);
    }
 
            }
        });
 
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(membersTable);
        scroll.getViewport().setBackground(new java.awt.Color(22, 22, 22));
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(70, 70, 70)));
        scroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new java.awt.Dimension(10, 0));
 
        membersContainer.setOpaque(false);
        membersContainer.add(scroll, java.awt.BorderLayout.CENTER);
    }
 
    private static java.awt.Color rowColor(int row, int hoverRow) {
        if (row == hoverRow) {
            return new java.awt.Color(52, 52, 52);
        }
        return (row % 2 == 0) ? new java.awt.Color(26, 26, 26) : new java.awt.Color(34, 34, 34);
    }
 
    private class CellRenderer extends javax.swing.table.DefaultTableCellRenderer {
 
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setOpaque(true);
            setBackground(rowColor(row, hoverRow));
            setForeground(java.awt.Color.WHITE);
            setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
            if (col == COL_ID) {
                setForeground(new java.awt.Color(255, 77, 77));
                setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
            }
            if (col == COL_STATUS && row < visibleMembers.size()) {
                setForeground(visibleMembers.get(row).getStatusColor());
                setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
            }
            return this;
        }
    }
 
    private class ActionsRenderer extends javax.swing.JPanel implements javax.swing.table.TableCellRenderer {
 
        private int row;
 
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            this.row = row;
            return this;
        }
 
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(rowColor(row, hoverRow));
            g2.fillRect(0, 0, getWidth(), getHeight());
            int y = (getHeight() - BTN_H) / 2;
            drawButton(g2, "Edit", EDIT_X, y, EDIT_W, new java.awt.Color(70, 70, 70));
            
            g2.dispose();
        }
 
        private void drawButton(java.awt.Graphics2D g2, String text, int x, int y, int w, java.awt.Color c) {
            g2.setColor(c);
            g2.fillRoundRect(x, y, w, BTN_H, 14, 14);
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, x + (w - fm.stringWidth(text)) / 2,
                    y + (BTN_H - fm.getHeight()) / 2 + fm.getAscent());
        }
    }
 
    /** Re-fills the table from allMembers, applying the search filter. */
    private void setupMembersGrid() {
        tableModel.setRowCount(0);
        visibleMembers.clear();
        for (MemberInfo m : allMembers) {
            String haystack = (m.getFullName() + " " + m.getAge() + " " + m.getAddress() + " "
                    + m.getContactNumber() + " " + m.getGender()).toLowerCase();
            if (haystack.contains(filterText)) {
                visibleMembers.add(m);
                tableModel.addRow(new Object[]{
                    String.format("TG-%03d", m.getId()),
                    m.getFullName(),
                    m.getAge(),
                    m.getGender(),
                    m.getAddress(),
                    m.getContactNumber(),
                    m.getStatusText(),
                    ""
                });
            }
        }
    }
 private void addExpiringButton() {
        javax.swing.JButton btn = new javax.swing.JButton() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hot = getModel().isRollover();
                g2.setColor(hot ? new java.awt.Color(204, 0, 0) : new java.awt.Color(34, 35, 40));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(hot ? new java.awt.Color(255, 77, 77) : new java.awt.Color(90, 91, 99));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(java.awt.Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(3f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 4, cy - 9, cx + 5, cy);
                g2.drawLine(cx + 5, cy, cx - 4, cy + 9);
                g2.dispose();
            }
        };
        btn.setRolloverEnabled(true);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setToolTipText("Memberships ending within 1 month");
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showExpiringDialog());
        // x = distance from the left, y = distance from the top, then width and height
        btn.setBounds(getWidth() - 142, 62, 48, 48);
        getLayeredPane().add(btn, javax.swing.JLayeredPane.PALETTE_LAYER);
    }
 
    /** Lists every member whose membership ends between today and 1 month from today. */
    private void showExpiringDialog() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
 
        String[] cols = {"NAME", "CONTACT", "EXPIRES ON", "DAYS LEFT"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        final java.util.List<Long> daysLeft = new java.util.ArrayList<>();
 
        String sql = "SELECT first_name, last_name, contact_number, end_date FROM members_tbl "
                + "WHERE archived = 0 AND end_date BETWEEN ? AND ? ORDER BY end_date, first_name";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(today));
            ps.setDate(2, java.sql.Date.valueOf(today.plusMonths(1)));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.time.LocalDate end = rs.getDate("end_date").toLocalDate();
                    long d = java.time.temporal.ChronoUnit.DAYS.between(today, end);
                    daysLeft.add(d);
                    model.addRow(new Object[]{
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("contact_number"),
                        end.format(fmt),
                        d == 0 ? "Today" : (d == 1 ? "1 day" : d + " days")
                    });
                }
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not load expiring members: " + ex.getMessage());
            return;
        }
 
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Expiring Soon", true);
        dialog.setLayout(new java.awt.BorderLayout());
 
        javax.swing.JLabel title = new javax.swing.JLabel(
                "Expiring Soon (" + model.getRowCount() + ")", javax.swing.SwingConstants.CENTER);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(new java.awt.Color(204, 0, 0));
        title.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 15, 0));
        dialog.add(title, java.awt.BorderLayout.NORTH);
 
        if (model.getRowCount() == 0) {
            javax.swing.JLabel none = new javax.swing.JLabel(
                    "No memberships are ending within the next month.", javax.swing.SwingConstants.CENTER);
            none.setForeground(java.awt.Color.WHITE);
            none.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 17));
            none.setOpaque(true);
            none.setBackground(new java.awt.Color(51, 51, 51));
            none.setBorder(javax.swing.BorderFactory.createEmptyBorder(50, 40, 50, 40));
            dialog.add(none, java.awt.BorderLayout.CENTER);
        } else {
            javax.swing.JTable table = new javax.swing.JTable(model);
            table.setRowHeight(44);
            table.setShowGrid(false);
            table.setIntercellSpacing(new java.awt.Dimension(0, 0));
            table.setRowSelectionAllowed(false);
            table.setFocusable(false);
            table.setBackground(new java.awt.Color(22, 22, 22));
            table.setFillsViewportHeight(true);
            int[] widths = {260, 180, 160, 140};
            for (int i = 0; i < widths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
 
            table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                        boolean sel, boolean foc, int row, int col) {
                    javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                    l.setOpaque(true);
                    l.setBackground(rowColor(row, -1));
                    l.setForeground(java.awt.Color.WHITE);
                    l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15));
                    l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
                    if (col == 3) {   // red when a week or less is left, orange otherwise
                        long d = daysLeft.get(row);
                        l.setForeground(d <= 7 ? new java.awt.Color(255, 77, 77) : new java.awt.Color(255, 170, 60));
                        l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
                    }
                    return l;
                }
            });
 
            javax.swing.table.JTableHeader header = table.getTableHeader();
            header.setReorderingAllowed(false);
            header.setResizingAllowed(false);
            header.setPreferredSize(new java.awt.Dimension(100, 42));
            header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                        boolean sel, boolean foc, int row, int col) {
                    javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                    l.setOpaque(true);
                    l.setBackground(new java.awt.Color(14, 14, 14));
                    l.setForeground(new java.awt.Color(255, 77, 77));
                    l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
                    l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
                    return l;
                }
            });
 
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(table);
            scroll.getViewport().setBackground(new java.awt.Color(22, 22, 22));
            scroll.setBorder(null);
            scroll.setPreferredSize(new java.awt.Dimension(760, 380));
            dialog.add(scroll, java.awt.BorderLayout.CENTER);
        }
 
        javax.swing.JButton close = new javax.swing.JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        javax.swing.JPanel bottom = new javax.swing.JPanel();
        bottom.setBackground(new java.awt.Color(51, 51, 51));
        bottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 12, 0));
        bottom.add(close);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
 
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(480, 260));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    // ================= DIALOGS =================
    private void confirmRenew(MemberInfo member, java.awt.Component parent) {
        int choice = javax.swing.JOptionPane.showConfirmDialog(parent,
                "Renew " + member.getFullName() + " for 1 year?\nOnly continue if payment was received.",
                "Confirm Renewal", javax.swing.JOptionPane.YES_NO_OPTION);
        if (choice == javax.swing.JOptionPane.YES_OPTION && renewMember(member.getId())) {
            allMembers = loadMembers();
            setupMembersGrid();
            javax.swing.JOptionPane.showMessageDialog(this, "Membership renewed for 1 year.");
        }
    }
 
    private void showMemberDetails(MemberInfo member) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Member Information", true);
        dialog.setLayout(new java.awt.BorderLayout());
        dialog.setMinimumSize(new java.awt.Dimension(420, 300));
        javax.swing.JPanel info = new javax.swing.JPanel(new java.awt.GridLayout(7, 2, 10, 15));
        info.setBackground(new java.awt.Color(51, 51, 51));
        info.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
 
        String[][] rows = {
            {"First Name:", member.getFirstName()},
            {"Last Name:", member.getLastName()},
            {"Age:", member.getAge()},
            {"Gender:", member.getGender()},
            {"Address:", member.getAddress()},
            {"Contact Number:", member.getContactNumber()},
            {"Membership:", member.getStatusText()}
        };
        for (String[] row : rows) {
            javax.swing.JLabel key = new javax.swing.JLabel(row[0]);
            key.setForeground(new java.awt.Color(255, 77, 77));
            key.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
            javax.swing.JLabel value = new javax.swing.JLabel(row[1]);
            value.setForeground(java.awt.Color.WHITE);
            value.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
            info.add(key);
            info.add(value);
        }
 
        javax.swing.JButton close = new javax.swing.JButton("Close");
        close.addActionListener(e -> dialog.dispose());
 
        javax.swing.JButton renew = new javax.swing.JButton("Renew 1 Year");
        renew.setBackground(new java.awt.Color(204, 0, 0));
        renew.setForeground(java.awt.Color.WHITE);
        renew.setFocusPainted(false);
        renew.addActionListener(e -> {
            int choice = javax.swing.JOptionPane.showConfirmDialog(dialog,
                    "Renew " + member.getFullName() + " for 1 year?\nOnly continue if payment was received.",
                    "Confirm Renewal", javax.swing.JOptionPane.YES_NO_OPTION);
            if (choice == javax.swing.JOptionPane.YES_OPTION && renewMember(member.getId())) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(this, "Membership renewed for 1 year.");
                dialog.dispose();
            }
        });
 
        javax.swing.JButton edit = new javax.swing.JButton("Edit Info");
        edit.addActionListener(e -> showEditMemberDialog(member, dialog));
 
        javax.swing.JButton undo = new javax.swing.JButton("Undo Renewal");
        undo.addActionListener(e -> {
            int undoChoice = javax.swing.JOptionPane.showConfirmDialog(dialog,
                    "Undo the last renewal for " + member.getFullName() + "?\nTheir previous dates will be restored.",
                    "Confirm Undo", javax.swing.JOptionPane.YES_NO_OPTION);
            if (undoChoice != javax.swing.JOptionPane.YES_OPTION) {
                return;
            }
            int result = undoRenewal(member.getId());
            if (result == 1) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(this, "Renewal undone.");
                dialog.dispose();
            } else if (result == 0) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "There is no renewal to undo.");
            }
        });
 
        javax.swing.JPanel bottom = new javax.swing.JPanel();
        bottom.setBackground(new java.awt.Color(51, 51, 51));
        bottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 15, 0));
        bottom.add(close);
        bottom.add(edit);
        bottom.add(undo);
        dialog.add(info, java.awt.BorderLayout.CENTER);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(420, 260));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
 
    // ================= DATABASE =================
    /** Membership lasts 1 year. */
    private boolean renewMember(int memId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        try (java.sql.Connection con = DBConnection.connect()) {
            java.time.LocalDate currentEnd = null;
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                    "SELECT end_date FROM members_tbl WHERE mem_id = ?")) {
                ps.setInt(1, memId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getDate(1) != null) {
                        currentEnd = rs.getDate(1).toLocalDate();
                    }
                }
            }
            boolean expired = currentEnd == null || currentEnd.isBefore(today);
            java.time.LocalDate newEnd = expired ? today.plusYears(1).minusDays(1)
                    : currentEnd.plusYears(1);
 
            String sql = expired
                    ? "UPDATE members_tbl SET prev_start_date = start_date, prev_end_date = end_date, "
                    + "start_date = ?, end_date = ? WHERE mem_id = ?"
                    : "UPDATE members_tbl SET prev_start_date = start_date, prev_end_date = end_date, "
                    + "end_date = ? WHERE mem_id = ?";
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                int i = 1;
                if (expired) {
                    ps.setDate(i++, java.sql.Date.valueOf(today));
                }
                ps.setDate(i++, java.sql.Date.valueOf(newEnd));
                ps.setInt(i, memId);
                return ps.executeUpdate() > 0;
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not renew membership: " + ex.getMessage());
            return false;
        }
    }
 
    private boolean insertMember(String first, String last, int age, String address, String contact, String gender) {
        String sql = "INSERT INTO members_tbl (first_name, last_name, age, address, contact_number, gender, start_date, end_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        java.time.LocalDate today = java.time.LocalDate.now();
        try (java.sql.Connection con = DBConnection.connect(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, first);
            ps.setString(2, last);
            ps.setInt(3, age);
            ps.setString(4, address);
            ps.setString(5, contact);
            ps.setString(6, gender);
            ps.setDate(7, java.sql.Date.valueOf(today));
            ps.setDate(8, java.sql.Date.valueOf(today));   
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not add member: " + ex.getMessage());
            return false;
        }
    }
 
    private boolean updateMember(int memId, String first, String last, int age, String address, String contact, String gender) {
        String sql = "UPDATE members_tbl SET first_name = ?, last_name = ?, age = ?, "
                + "address = ?, contact_number = ?, gender = ? WHERE mem_id = ?";
        try (java.sql.Connection con = DBConnection.connect(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, first);
            ps.setString(2, last);
            ps.setInt(3, age);
            ps.setString(4, address);
            ps.setString(5, contact);
            ps.setString(6, gender);
            ps.setInt(7, memId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                javax.swing.JOptionPane.showMessageDialog(null,
                        "No row was updated. mem_id = " + memId);
            }
            return rows > 0;
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(null, "Could not update member: " + ex.getMessage());
            return false;
        }
    }
 
    private int undoRenewal(int memId) {
        String sql = "UPDATE members_tbl SET start_date = prev_start_date, end_date = prev_end_date, "
                + "prev_start_date = NULL, prev_end_date = NULL "
                + "WHERE mem_id = ? AND prev_end_date IS NOT NULL";
        try (java.sql.Connection con = DBConnection.connect(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memId);
            return ps.executeUpdate() > 0 ? 1 : 0;
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not undo renewal: " + ex.getMessage());
            return -1;
        }
    }
    // ================= ARCHIVE =================
    /** Hides the member from the list instead of deleting. Attendance and payment records stay. */
    private boolean archiveMember(int memId) {
        String sql = "UPDATE members_tbl SET archived = 1, archived_date = ? WHERE mem_id = ?";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(2, memId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not archive member: " + ex.getMessage());
            return false;
        }
    }
 
    private boolean restoreMember(int memId) {
        String sql = "UPDATE members_tbl SET archived = 0, archived_date = NULL WHERE mem_id = ?";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, memId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not restore member: " + ex.getMessage());
            return false;
        }
    }
 
    /** "Archive" button at the top right, next to the expiring-soon button. */
    private void addArchiveButton() {
        javax.swing.JButton btn = new javax.swing.JButton("Archive");
        btn.setBackground(new java.awt.Color(34, 35, 40));
        btn.setForeground(java.awt.Color.WHITE);
        btn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(90, 91, 99)));
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.setToolTipText("View archived members");
        btn.addActionListener(e -> showArchiveDialog());
        // x = distance from the left, y = distance from the top, then width and height
        btn.setBounds(getWidth() - 282, 62, 120, 48);
        getLayeredPane().add(btn, javax.swing.JLayeredPane.PALETTE_LAYER);
    }
 
    /** Lists archived members and lets you restore one. */
    private void showArchiveDialog() {
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
 
        String[] cols = {"ID", "NAME", "CONTACT", "ARCHIVED ON"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        final java.util.List<Integer> ids = new java.util.ArrayList<>();
 
        String sql = "SELECT mem_id, first_name, last_name, contact_number, archived_date FROM members_tbl "
                + "WHERE archived = 1 ORDER BY archived_date DESC, first_name";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date archivedOn = rs.getDate("archived_date");
                ids.add(rs.getInt("mem_id"));
                model.addRow(new Object[]{
                    String.format("TG-%03d", rs.getInt("mem_id")),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("contact_number"),
                    archivedOn == null ? "-" : archivedOn.toLocalDate().format(fmt)
                });
            }
        } catch (java.sql.SQLException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not load archive: " + ex.getMessage());
            return;
        }
 
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Archive", true);
        dialog.setLayout(new java.awt.BorderLayout());
 
        javax.swing.JLabel title = new javax.swing.JLabel(
                "Archive (" + model.getRowCount() + ")", javax.swing.SwingConstants.CENTER);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(new java.awt.Color(204, 0, 0));
        title.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 15, 0));
        dialog.add(title, java.awt.BorderLayout.NORTH);
 
        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(new java.awt.Color(22, 22, 22));
        table.setFillsViewportHeight(true);
        int[] widths = {100, 260, 170, 170};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(sel ? new java.awt.Color(120, 0, 0) : rowColor(row, -1));
                l.setForeground(col == 0 ? new java.awt.Color(255, 77, 77) : java.awt.Color.WHITE);
                if (sel && col == 0) {
                    l.setForeground(java.awt.Color.WHITE);
                }
                l.setFont(new java.awt.Font("Segoe UI", col == 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 15));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });
 
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new java.awt.Dimension(100, 42));
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(true);
                l.setBackground(new java.awt.Color(14, 14, 14));
                l.setForeground(new java.awt.Color(255, 77, 77));
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
                l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 8));
                return l;
            }
        });
 
        javax.swing.JPanel bottom = new javax.swing.JPanel();
        bottom.setBackground(new java.awt.Color(51, 51, 51));
        bottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 12, 0));
 
        if (model.getRowCount() == 0) {
            javax.swing.JLabel none = new javax.swing.JLabel(
                    "No archived members.", javax.swing.SwingConstants.CENTER);
            none.setForeground(java.awt.Color.WHITE);
            none.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 17));
            none.setOpaque(true);
            none.setBackground(new java.awt.Color(51, 51, 51));
            none.setBorder(javax.swing.BorderFactory.createEmptyBorder(50, 40, 50, 40));
            dialog.add(none, java.awt.BorderLayout.CENTER);
        } else {
            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(table);
            scroll.getViewport().setBackground(new java.awt.Color(22, 22, 22));
            scroll.setBorder(null);
            scroll.setPreferredSize(new java.awt.Dimension(700, 360));
            dialog.add(scroll, java.awt.BorderLayout.CENTER);
 
            javax.swing.JButton restore = new javax.swing.JButton("Restore Selected");
            restore.setBackground(new java.awt.Color(204, 0, 0));
            restore.setForeground(java.awt.Color.WHITE);
            restore.setFocusPainted(false);
            restore.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r < 0) {
                    javax.swing.JOptionPane.showMessageDialog(dialog, "Select a member to restore.");
                    return;
                }
                int choice = javax.swing.JOptionPane.showConfirmDialog(dialog,
                        "Restore " + model.getValueAt(r, 1) + " to the members list?",
                        "Confirm Restore", javax.swing.JOptionPane.YES_NO_OPTION);
                if (choice == javax.swing.JOptionPane.YES_OPTION && restoreMember(ids.get(r))) {
                    allMembers = loadMembers();
                    setupMembersGrid();
                    model.removeRow(r);
                    ids.remove(r);
                    title.setText("Archive (" + model.getRowCount() + ")");
                    javax.swing.JOptionPane.showMessageDialog(dialog, "Member restored.");
                    if (model.getRowCount() == 0) {
                        dialog.dispose();
                    }
                }
            });
            bottom.add(restore);
        }
 
        javax.swing.JButton close = new javax.swing.JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
 
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(480, 260));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
 
    // ================= DUPLICATE CHECK =================
    /**
     * Returns a message when another member (archived ones included) already has the same
     * full name or the same contact number, otherwise null.
     * excludeId = the member being edited (so they don't clash with themselves); use -1 when adding.
     */
    private String findDuplicate(String first, String last, String contact, int excludeId) {
        String sql = "SELECT mem_id, first_name, last_name, contact_number, archived FROM members_tbl "
                + "WHERE mem_id <> ? AND ((LOWER(TRIM(first_name)) = LOWER(?) AND LOWER(TRIM(last_name)) = LOWER(?)) "
                + "OR TRIM(contact_number) = ?) ORDER BY (LOWER(TRIM(first_name)) = LOWER(?) "
                + "AND LOWER(TRIM(last_name)) = LOWER(?)) DESC LIMIT 1";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, excludeId);
            ps.setString(2, first.trim());
            ps.setString(3, last.trim());
            ps.setString(4, contact.trim());
            ps.setString(5, first.trim());
            ps.setString(6, last.trim());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = String.format("TG-%03d", rs.getInt("mem_id"));
                    String name = rs.getString("first_name") + " " + rs.getString("last_name");
                    boolean sameName = name.trim().equalsIgnoreCase((first.trim() + " " + last.trim()));
                    String where = rs.getInt("archived") == 1
                            ? "\nThey are in the Archive - restore them from there instead." : "";
                    if (sameName) {
                        return "A member named " + name + " (" + id + ") already exists." + where;
                    }
                    return "That contact number already belongs to " + name + " (" + id + ")." + where;
                }
            }
        } catch (java.sql.SQLException ex) {
            return "Could not check for duplicates: " + ex.getMessage();
        }
        return null;
    }
 
    // ================= CONTACT NUMBER LIMIT =================
    /** Lets the field take digits only, up to 11 characters (typing and pasting). */
    private void limitContactField(javax.swing.JTextField field) {
        ((javax.swing.text.AbstractDocument) field.getDocument()).setDocumentFilter(
            new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text,
                        javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                    replace(fb, offset, 0, text, attr);
                }
 
                @Override
                public void replace(FilterBypass fb, int offset, int length, String text,
                        javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
                    if (text == null) {
                        return;
                    }
                    String digits = text.replaceAll("[^0-9]", "");
                    int room = 11 - (fb.getDocument().getLength() - length);
                    if (room <= 0 || digits.isEmpty()) {
                        return;
                    }
                    super.replace(fb, offset, length, digits.substring(0, Math.min(room, digits.length())), attrs);
                }
            });
    }
 
    // ================= EDIT / ADD DIALOGS =================
    private void showEditMemberDialog(MemberInfo member, javax.swing.JDialog detailsDialog) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Edit Member", true);
        dialog.setLayout(new java.awt.BorderLayout());
 
        javax.swing.JLabel title = new javax.swing.JLabel("Edit Member", javax.swing.SwingConstants.CENTER);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(new java.awt.Color(204, 0, 0));
        title.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 15, 0));
 
        String[] labels = {"First Name:", "Last Name:", "Age:", "Address:", "Contact Number:"};
        String[] current = {member.getFirstName(), member.getLastName(), member.getAge(),
            member.getAddress(), member.getContactNumber()};
        javax.swing.JTextField[] fields = new javax.swing.JTextField[labels.length];
 
        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(labels.length + 1, 2, 10, 15));
        form.setBackground(new java.awt.Color(51, 51, 51));
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
 
        for (int i = 0; i < labels.length; i++) {
            javax.swing.JLabel key = new javax.swing.JLabel(labels[i]);
            key.setForeground(new java.awt.Color(255, 77, 77));
            key.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
 
            fields[i] = new javax.swing.JTextField(current[i], 18);
            fields[i].setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
            fields[i].setBackground(new java.awt.Color(30, 30, 30));
            fields[i].setForeground(java.awt.Color.WHITE);
            fields[i].setCaretColor(java.awt.Color.WHITE);
 
            form.add(key);
            form.add(fields[i]);
        }
        limitContactField(fields[4]);
 
        javax.swing.JLabel genderLabel = new javax.swing.JLabel("Gender:");
        genderLabel.setForeground(new java.awt.Color(255, 77, 77));
        genderLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        javax.swing.JComboBox<String> genderBox = new javax.swing.JComboBox<>(new String[]{"Male", "Female", "Other"});
        genderBox.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        genderBox.setSelectedItem(member.getGender());
        form.add(genderLabel);
        form.add(genderBox);
 
        javax.swing.JButton save = new javax.swing.JButton("Save Changes");
        save.setBackground(new java.awt.Color(204, 0, 0));
        save.setForeground(java.awt.Color.WHITE);
        save.setFocusPainted(false);
 
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        javax.swing.JButton remove = new javax.swing.JButton("Archive Member");
        remove.setBackground(new java.awt.Color(120, 0, 0));
        remove.setForeground(java.awt.Color.WHITE);
        remove.setFocusPainted(false);
        remove.addActionListener(e -> {
            int choice = javax.swing.JOptionPane.showConfirmDialog(dialog,
                    "Move " + member.getFullName() + " to the archive?\n"
                    + "Their attendance and payment records are kept.\nYou can restore them later from the Archive.",
                    "Confirm Archive", javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE);
            if (choice == javax.swing.JOptionPane.YES_OPTION && archiveMember(member.getId())) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(dialog, "Member archived.");
                dialog.dispose();
                if (detailsDialog != null) {
                    detailsDialog.dispose();
                }
            }
        });
        javax.swing.JButton undo = new javax.swing.JButton("Undo Renewal");
        undo.addActionListener(e -> {
            int undoChoice = javax.swing.JOptionPane.showConfirmDialog(dialog,
                    "Undo the last renewal for " + member.getFullName() + "?\nTheir previous dates will be restored.",
                    "Confirm Undo", javax.swing.JOptionPane.YES_NO_OPTION);
            if (undoChoice != javax.swing.JOptionPane.YES_OPTION) {
                return;
            }
            int result = undoRenewal(member.getId());
            if (result == 1) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(dialog, "Renewal undone.");
                dialog.dispose();
                if (detailsDialog != null) {
                    detailsDialog.dispose();
                }
            } else if (result == 0) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "There is no renewal to undo.");
            }
        });
 
        save.addActionListener(e -> {
            String first = fields[0].getText().trim();
            String last = fields[1].getText().trim();
            String ageText = fields[2].getText().trim();
            String address = fields[3].getText().trim();
            String contact = fields[4].getText().trim();
 
            if (first.isEmpty() || last.isEmpty() || ageText.isEmpty()
                    || address.isEmpty() || contact.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Please fill in all fields.");
                return;
            }
            int age;
            try {
                age = Integer.parseInt(ageText);
                if (age < 1 || age > 120) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Age must be a number between 1 and 120.");
                return;
            }
            if (!contact.matches("[0-9]{7,11}")) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Contact number must be 7 to 11 digits.");
                return;
            }
 
            String dup = findDuplicate(first, last, contact, member.getId());
            if (dup != null) {
                javax.swing.JOptionPane.showMessageDialog(dialog, dup, "Duplicate member",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
 
            if (updateMember(member.getId(), first, last, age, address, contact, genderBox.getSelectedItem().toString())) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(dialog, "Member updated.");
                dialog.dispose();
                if (detailsDialog != null) {
                    detailsDialog.dispose();
                }
            }
        });
 
        javax.swing.JPanel bottom = new javax.swing.JPanel();
        bottom.setBackground(new java.awt.Color(51, 51, 51));
        bottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 15, 0));
         bottom.add(save);
        bottom.add(undo);
        bottom.add(remove);
        bottom.add(cancel);
 
        dialog.add(title, java.awt.BorderLayout.NORTH);
        dialog.add(form, java.awt.BorderLayout.CENTER);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(460, 420));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
 
    private void showAddMemberDialog() {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, "Add Member", true);
        dialog.setLayout(new java.awt.BorderLayout());
 
        javax.swing.JLabel title = new javax.swing.JLabel("Add New Member", javax.swing.SwingConstants.CENTER);
        title.setForeground(java.awt.Color.WHITE);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        title.setOpaque(true);
        title.setBackground(new java.awt.Color(204, 0, 0));
        title.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 0, 15, 0));
 
        String[] labels = {"First Name:", "Last Name:", "Age:", "Address:", "Contact Number:"};
        javax.swing.JTextField[] fields = new javax.swing.JTextField[labels.length];
 
        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridLayout(labels.length + 1, 2, 10, 15));
        form.setBackground(new java.awt.Color(51, 51, 51));
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(30, 30, 20, 30));
 
        for (int i = 0; i < labels.length; i++) {
            javax.swing.JLabel key = new javax.swing.JLabel(labels[i]);
            key.setForeground(new java.awt.Color(255, 77, 77));
            key.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
 
            fields[i] = new javax.swing.JTextField(18);
            fields[i].setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
            fields[i].setBackground(new java.awt.Color(30, 30, 30));
            fields[i].setForeground(java.awt.Color.WHITE);
            fields[i].setCaretColor(java.awt.Color.WHITE);
 
            form.add(key);
            form.add(fields[i]);
        }
        limitContactField(fields[4]);
 
        javax.swing.JLabel genderLabel = new javax.swing.JLabel("Gender:");
        genderLabel.setForeground(new java.awt.Color(255, 77, 77));
        genderLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        javax.swing.JComboBox<String> genderBox = new javax.swing.JComboBox<>(new String[]{"Male", "Female", "Other"});
        genderBox.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 16));
        form.add(genderLabel);
        form.add(genderBox);
 
        javax.swing.JButton save = new javax.swing.JButton("Save");
        save.setBackground(new java.awt.Color(204, 0, 0));
        save.setForeground(java.awt.Color.WHITE);
        save.setFocusPainted(false);
 
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
 
        save.addActionListener(e -> {
            String first = fields[0].getText().trim();
            String last = fields[1].getText().trim();
            String ageText = fields[2].getText().trim();
            String address = fields[3].getText().trim();
            String contact = fields[4].getText().trim();
 
            if (first.isEmpty() || last.isEmpty() || ageText.isEmpty()
                    || address.isEmpty() || contact.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Please fill in all fields.");
                return;
            }
 
            int age;
            try {
                age = Integer.parseInt(ageText);
                if (age < 1 || age > 120) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Age must be a number between 1 and 120.");
                return;
            }
 
            if (!contact.matches("[0-9]{7,11}")) {
                javax.swing.JOptionPane.showMessageDialog(dialog, "Contact number must be 7 to 11 digits.");
                return;
            }
 
            String dup = findDuplicate(first, last, contact, -1);
            if (dup != null) {
                javax.swing.JOptionPane.showMessageDialog(dialog, dup, "Duplicate member",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
 
            if (insertMember(first, last, age, address, contact, genderBox.getSelectedItem().toString())) {
                allMembers = loadMembers();
                setupMembersGrid();
                javax.swing.JOptionPane.showMessageDialog(this, "Member added.");
                dialog.dispose();
            }
        });
 
        javax.swing.JPanel bottom = new javax.swing.JPanel();
        bottom.setBackground(new java.awt.Color(51, 51, 51));
        bottom.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 15, 0));
        bottom.add(save);
        bottom.add(cancel);
 
        dialog.add(title, java.awt.BorderLayout.NORTH);
        dialog.add(form, java.awt.BorderLayout.CENTER);
        dialog.add(bottom, java.awt.BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new java.awt.Dimension(460, 420));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is alwaysss
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgPanel21 = new topgym_system.BgPanel2();
        addMemberBtn = new javax.swing.JButton();
        searchField = new javax.swing.JTextField();
        membersContainer = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        bgPanel21.setPreferredSize(new java.awt.Dimension(1536, 864));

        addMemberBtn.setText("+ Add Member");

        searchField.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        membersContainer.setPreferredSize(new java.awt.Dimension(1380, 480));
        membersContainer.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout bgPanel21Layout = new javax.swing.GroupLayout(bgPanel21);
        bgPanel21.setLayout(bgPanel21Layout);
        bgPanel21Layout.setHorizontalGroup(
            bgPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bgPanel21Layout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addGroup(bgPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(bgPanel21Layout.createSequentialGroup()
                        .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(932, 932, 932)
                        .addComponent(addMemberBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(membersContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        bgPanel21Layout.setVerticalGroup(
            bgPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bgPanel21Layout.createSequentialGroup()
                .addContainerGap(294, Short.MAX_VALUE)
                .addGroup(bgPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addMemberBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(membersContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32))
        );

        getContentPane().add(bgPanel21, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new members().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addMemberBtn;
    private topgym_system.BgPanel2 bgPanel21;
    private javax.swing.JPanel membersContainer;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables
}
