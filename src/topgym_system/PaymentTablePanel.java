package topgym_system;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.*;

/**
 * Payments table with a "Search member..." box and an "All Status" dropdown.
 * Just add this panel to your payment page (see instructions).
 */
public class PaymentTablePanel extends JPanel {

    // ---- Colors (change these to restyle everything) ----
    private static final Color CARD   = new Color(24, 26, 30, 235);
    private static final Color FIELD  = new Color(38, 40, 46);
    private static final Color LINE   = new Color(70, 72, 80);
    private static final Color HEADER = new Color(40, 42, 48);
    private static final Color TEXT   = Color.WHITE;
    private static final Color MUTED  = new Color(175, 177, 185);

    private static final String[] COLUMNS =
        {"Member", "Payment Date", "Amount", "Method", "Status", "Action"};
    private static final int COL_STATUS = 4;
    private static final int COL_ACTION = 5;

    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField txtSearch;
    private final JComboBox<String> cbStatus;
    // ---- date filter ----
    private static final Color DATE_RED = new Color(226, 30, 30);
    private static final int COL_DATE = 1;
    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
    private java.time.LocalDate dateFilter = null;   // null = all dates (use LocalDate.now() to open on today)
    private final JButton btnDate;
    private final java.util.List<Integer> payIds = new java.util.ArrayList<>();    // pay_id of each model row
    private final java.util.List<String> payTypes = new java.util.ArrayList<>();   // "Monthly" or "Session"

    // ---- Default prices shown in the Add Payment dialog (change to your real prices) ----
    private static final String MONTHLY_PRICE  = "500.00";
    private static final String SESSION_PRICE = "50.00";

    public PaymentTablePanel() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 20));

        // ---------- Table model ----------
        model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ---------- Search box + status dropdown ----------
        txtSearch = createSearchField();
        cbStatus = createStatusCombo();
        btnDate = createDateButton();

        RoundedPanel filterBar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 20, 13));
        txtSearch.setPreferredSize(new Dimension(520, 46));
        cbStatus.setPreferredSize(new Dimension(300, 46));
        btnDate.setPreferredSize(new Dimension(320, 46));
        filterBar.add(txtSearch);
        filterBar.add(cbStatus);
        filterBar.add(btnDate);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        cbStatus.addActionListener(e -> applyFilter());

        // ---------- Table look ----------
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(10, 12, 10, 12));
        scroll.setViewportBorder(null);
        scroll.getViewport().setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        RoundedPanel tableCard = new RoundedPanel(new BorderLayout());
        tableCard.add(scroll, BorderLayout.CENTER);

        add(filterBar, BorderLayout.NORTH);
        add(tableCard, BorderLayout.CENTER);

        loadPayments();
    }

    // =====================================================
    //  Database
    // =====================================================

    /** Reloads every row from payments_tbl. Call this any time the data changes. */
    public final void loadPayments() {
        model.setRowCount(0);
        payIds.clear();
        payTypes.clear();

        // members show their name; walk-ins show the name typed at the front desk + "(Session)"
        String sql = "SELECT p.pay_id, p.pay_type, "
                + "CASE WHEN p.mem_id IS NULL "
                + "THEN CONCAT(COALESCE(NULLIF(p.payer_name, ''), 'Walk-in'), ' (Session)') "
                + "ELSE CONCAT(m.first_name, ' ', m.last_name) END AS member, "
                + "p.pay_date, p.amount, p.method, p.status "
                + "FROM payments_tbl p LEFT JOIN members_tbl m ON m.mem_id = p.mem_id "
                + "ORDER BY p.pay_date DESC, p.pay_id DESC";
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);

        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                payIds.add(rs.getInt("pay_id"));
                payTypes.add(rs.getString("pay_type"));
                model.addRow(new Object[]{
                    rs.getString("member"),
                    rs.getDate("pay_date").toLocalDate().format(fmt),
                    String.format("\u20B1%,.2f", rs.getBigDecimal("amount")),
                    rs.getString("method"),
                    rs.getString("status"),
                    "View"});
            }
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load payments: " + ex.getMessage());
        }
    }

    /** memId = null means a walk-in (day pass) customer; payerName is then the name typed in. */
    private boolean insertPayment(Integer memId, String payerName, String payType,
                                  java.time.LocalDate date, java.math.BigDecimal amount,
                                  String method, String status) {
        String sql = "INSERT INTO payments_tbl (mem_id, payer_name, pay_type, pay_date, amount, method, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            if (memId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, memId);
            if (payerName == null) ps.setNull(2, java.sql.Types.VARCHAR); else ps.setString(2, payerName);
            ps.setString(3, payType);
            ps.setDate(4, java.sql.Date.valueOf(date));
            ps.setBigDecimal(5, amount);
            ps.setString(6, method);
            ps.setString(7, status);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not save payment: " + ex.getMessage());
            return false;
        }
    }

    private boolean runUpdate(String sql, int payId, String errorText) {
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, payId);
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, errorText + ex.getMessage());
            return false;
        }
    }

    // =====================================================
    //  Filtering (search + status together)
    // =====================================================
    private void applyFilter() {
        String query = txtSearch.getText().trim();
        String status = (String) cbStatus.getSelectedItem();

        java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();
        if (!query.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(query), 0));
        }
        if (status != null && !status.equals("All Status")) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(status) + "$", COL_STATUS));
        }

        if (dateFilter != null) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(dateFilter.format(DATE_FMT)) + "$", COL_DATE));
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.<DefaultTableModel, Integer>andFilter(filters));
        }
    }

    // =====================================================
    //  Date filter (calendar dropdown)
    // =====================================================
    private void updateDateButton() {
        btnDate.setText(dateFilter == null ? "All Dates"
                : dateFilter.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", java.util.Locale.ENGLISH)));
    }

    private void showCalendar() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(new LineBorder(LINE, 1));
        menu.setBackground(FIELD);
        menu.add(new CalendarPanel(menu));
        menu.show(btnDate, 0, btnDate.getHeight() + 4);
    }

    private void pickDate(java.time.LocalDate d, JPopupMenu menu) {
        dateFilter = d;
        updateDateButton();
        menu.setVisible(false);
        applyFilter();
    }

    private JButton createDateButton() {
        JButton b = new JButton("All Dates") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(getModel().isRollover() ? MUTED : LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // calendar icon
                g2.setColor(TEXT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int ix = 16, iy = (getHeight() - 20) / 2;
                g2.drawRoundRect(ix, iy + 3, 20, 17, 4, 4);
                g2.drawLine(ix, iy + 9, ix + 20, iy + 9);
                g2.drawLine(ix + 6, iy, ix + 6, iy + 5);
                g2.drawLine(ix + 14, iy, ix + 14, iy + 5);
                g2.fillRect(ix + 5, iy + 12, 3, 3);
                g2.fillRect(ix + 9, iy + 12, 3, 3);
                g2.fillRect(ix + 13, iy + 12, 3, 3);

                // chevron
                g2.setStroke(new BasicStroke(2f));
                int cx = getWidth() - 26, cy = getHeight() / 2;
                g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
                g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        b.setForeground(TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(0, 52, 0, 46));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> showCalendar());
        return b;
    }

    /** Month calendar shown under the date button. */
    private class CalendarPanel extends JPanel {
        private final JPopupMenu menu;
        private java.time.YearMonth shownMonth;
        private final JLabel title = new JLabel("", SwingConstants.CENTER);
        private final JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));

        CalendarPanel(JPopupMenu menu) {
            this.menu = menu;
            shownMonth = java.time.YearMonth.from(dateFilter != null ? dateFilter : java.time.LocalDate.now());
            setLayout(new BorderLayout(0, 10));
            setBackground(FIELD);
            setBorder(new EmptyBorder(14, 14, 12, 14));
            setPreferredSize(new Dimension(340, 370));

            title.setFont(new Font("Segoe UI", Font.BOLD, 18));
            title.setForeground(TEXT);
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(arrow("\u2039", -1), BorderLayout.WEST);
            top.add(title, BorderLayout.CENTER);
            top.add(arrow("\u203A", 1), BorderLayout.EAST);

            grid.setOpaque(false);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setOpaque(false);
            bottom.add(link("All dates", () -> pickDate(null, menu)), BorderLayout.WEST);
            bottom.add(link("Today", () -> pickDate(java.time.LocalDate.now(), menu)), BorderLayout.EAST);

            add(top, BorderLayout.NORTH);
            add(grid, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
            rebuild();
        }

        private JLabel arrow(String text, int delta) {
            JLabel l = new JLabel(text, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 26));
            l.setForeground(TEXT);
            l.setPreferredSize(new Dimension(36, 30));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
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
            l.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { action.run(); }
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
            final boolean selected = date.equals(dateFilter);
            final boolean today = date.equals(java.time.LocalDate.now());
            JLabel l = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER) {
                boolean hover;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                        @Override public void mouseClicked(MouseEvent e) { pickDate(date, menu); }
                    });
                }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int sz = Math.min(getWidth(), getHeight()) - 2;
                    int x = (getWidth() - sz) / 2, y = (getHeight() - sz) / 2;
                    if (selected) {
                        g2.setColor(DATE_RED);
                        g2.fillOval(x, y, sz, sz);
                    } else if (hover) {
                        g2.setColor(new Color(60, 61, 68));
                        g2.fillOval(x, y, sz, sz);
                    } else if (today) {
                        g2.setColor(DATE_RED);
                        g2.drawOval(x, y, sz, sz);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            l.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 16));
            l.setForeground(TEXT);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return l;
        }
    }

    // =====================================================
    //  Table styling
    // =====================================================
    private void styleTable() {
        table.setRowHeight(64);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setOpaque(false);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 17));

        int[] widths = {230, 190, 150, 150, 170, 170};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Header
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(0, 62));
        header.setBackground(HEADER);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                JLabel l = new JLabel(String.valueOf(v));
                l.setOpaque(true);
                l.setBackground(HEADER);
                l.setForeground(TEXT);
                l.setFont(new Font("Segoe UI", Font.BOLD, 17));
                l.setBorder(new EmptyBorder(0, 20, 0, 10));
                if (col == COL_ACTION) l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        // Cells
        TableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                                                           boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, row, col);
                l.setOpaque(false);
                l.setForeground(col == 0 ? TEXT : MUTED);
                l.setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 17));
                l.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, LINE),
                        new EmptyBorder(0, 20, 0, 10)));
                return l;
            }
        };
        for (int i = 0; i < COL_STATUS; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(textRenderer);
        }
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new PillRenderer(false));
        table.getColumnModel().getColumn(COL_ACTION).setCellRenderer(new PillRenderer(true));

        // Click on "View"
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == COL_ACTION) {
                    showDetails(table.convertRowIndexToModel(row));
                }
            }
        });
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                table.setCursor(col == COL_ACTION
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });
    }

    // =====================================================
    //  Dialogs (same look as your Members dialogs)
    // =====================================================
    private static final Color DIALOG_BG = new Color(51, 51, 51);
    private static final Color DIALOG_RED = new Color(204, 0, 0);
    private static final Color DIALOG_KEY = new Color(255, 77, 77);

    private JDialog newDialog(String title) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        d.setLayout(new BorderLayout());
        JLabel head = new JLabel(title, SwingConstants.CENTER);
        head.setForeground(Color.WHITE);
        head.setFont(new Font("Segoe UI", Font.BOLD, 24));
        head.setOpaque(true);
        head.setBackground(DIALOG_RED);
        head.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        d.add(head, BorderLayout.NORTH);
        return d;
    }

    private JLabel keyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(DIALOG_KEY);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return l;
    }

    private JLabel valueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        return l;
    }

    private JTextField darkField(String text) {
        JTextField f = new JTextField(text, 18);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        f.setBackground(new Color(30, 30, 30));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        return f;
    }

    private JButton redDialogButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(DIALOG_RED);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    private void finishDialog(JDialog d, JPanel form, JPanel bottom, int minW, int minH) {
        form.setBackground(DIALOG_BG);
        form.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));
        bottom.setBackground(DIALOG_BG);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        d.add(form, BorderLayout.CENTER);
        d.add(bottom, BorderLayout.SOUTH);
        d.pack();
        d.setMinimumSize(new Dimension(minW, minH));
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    /** "View" button: shows the payment and lets you mark it Paid or delete it. */
    private void showDetails(int modelRow) {
        final int payId = payIds.get(modelRow);
        final String memberName = String.valueOf(model.getValueAt(modelRow, 0));
        final String status = String.valueOf(model.getValueAt(modelRow, 4));

        JDialog dialog = newDialog("Payment Details");

        String[][] rows = {
            {"Member:", memberName},
            {"Type:",   payTypes.get(modelRow)},
            {"Date:",   String.valueOf(model.getValueAt(modelRow, 1))},
            {"Amount:", String.valueOf(model.getValueAt(modelRow, 2))},
            {"Method:", String.valueOf(model.getValueAt(modelRow, 3))},
            {"Status:", status}
        };
        JPanel info = new JPanel(new GridLayout(rows.length, 2, 10, 15));
        for (String[] r : rows) {
            info.add(keyLabel(r[0]));
            info.add(valueLabel(r[1]));
        }

        JPanel bottom = new JPanel();
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        bottom.add(close);

        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(dialog,
                    "Delete this payment of " + memberName + "?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION
                    && runUpdate("DELETE FROM payments_tbl WHERE pay_id = ?", payId,
                                 "Could not delete payment: ")) {
                loadPayments();
                dialog.dispose();
            }
        });
        bottom.add(delete);

        if (!"Paid".equals(status)) {
            JButton markPaid = redDialogButton("Mark as Paid");
            markPaid.addActionListener(e -> {
                if (runUpdate("UPDATE payments_tbl SET status = 'Paid' WHERE pay_id = ?", payId,
                              "Could not update payment: ")) {
                    loadPayments();
                    dialog.dispose();
                }
            });
            bottom.add(markPaid);
        }

        finishDialog(dialog, info, bottom, 420, 420);
    }

    /** Opens the "Add Payment" dialog. Call this from your own "+ Add Payment" button. */
    public void showAddPaymentDialog() {
        java.util.List<MemberItem> members = loadMemberItems();

        JDialog dialog = newDialog("Add Payment");

        JComboBox<String> cbType = new JComboBox<>(new String[]{"Monthly Membership", "1-Day Session (Walk-in)"});
        cbType.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        // the "Member" row switches between a member list and a free-text name
        JComboBox<MemberItem> cbMember = new JComboBox<>(members.toArray(new MemberItem[0]));
        cbMember.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JTextField fWalkIn = darkField("");
        CardLayout cards = new CardLayout();
        JPanel whoPanel = new JPanel(cards);
        whoPanel.setOpaque(false);
        whoPanel.add(cbMember, "member");
        whoPanel.add(fWalkIn, "walkin");
        JLabel whoLabel = keyLabel("Member:");

        JTextField fDate = darkField(java.time.LocalDate.now().toString());   // yyyy-mm-dd
        fDate.setEditable(false);   // payments can only be recorded for today
        fDate.setToolTipText("Payments can only be recorded for today's date.");
        JTextField fAmount = darkField(MONTHLY_PRICE);
        JComboBox<String> cbMethod = new JComboBox<>(new String[]{"Cash", "GCash"});
        cbMethod.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JComboBox<String> cbPayStatus = new JComboBox<>(new String[]{"Paid", "Pending"});
        cbPayStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        cbType.addActionListener(e -> {
            boolean walkIn = cbType.getSelectedIndex() == 1;
            whoLabel.setText(walkIn ? "Customer name:" : "Member:");
            cards.show(whoPanel, walkIn ? "walkin" : "member");
            fAmount.setText(walkIn ? SESSION_PRICE : MONTHLY_PRICE);
        });

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 15));
        form.add(keyLabel("Type:"));               form.add(cbType);
        form.add(whoLabel);                        form.add(whoPanel);
        form.add(keyLabel("Date (today only):"));  form.add(fDate);
        form.add(keyLabel("Amount:"));             form.add(fAmount);
        form.add(keyLabel("Method:"));             form.add(cbMethod);
        form.add(keyLabel("Status:"));             form.add(cbPayStatus);

        JButton save = redDialogButton("Save");
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        save.addActionListener(e -> {
            boolean walkIn = cbType.getSelectedIndex() == 1;

            Integer memId = null;
            String payerName = null;
            if (walkIn) {
                payerName = fWalkIn.getText().trim();      // optional: blank is saved as "Walk-in"
            } else {
                MemberItem who = (MemberItem) cbMember.getSelectedItem();
                if (who == null) {
                    JOptionPane.showMessageDialog(dialog, "There are no members yet. Add a member first.");
                    return;
                }
                memId = who.id;
            }

            java.time.LocalDate date;
            try {
                date = java.time.LocalDate.parse(fDate.getText().trim());
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Date must look like 2026-10-05.");
                return;
            }
            if (!date.equals(java.time.LocalDate.now())) {
                JOptionPane.showMessageDialog(dialog,
                        "Payments can only be recorded for today (" + java.time.LocalDate.now() + ").\n"
                        + "The date you entered is " + date + ".",
                        "Wrong date", JOptionPane.WARNING_MESSAGE);
                return;
            }
            java.math.BigDecimal amount;
            try {
                amount = new java.math.BigDecimal(fAmount.getText().trim().replace(",", ""));
                if (amount.signum() <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Amount must be a number greater than 0.");
                return;
            }

            if (insertPayment(memId, payerName, walkIn ? "Session" : "Monthly", date, amount,
                    cbMethod.getSelectedItem().toString(),
                    cbPayStatus.getSelectedItem().toString())) {
                loadPayments();
                JOptionPane.showMessageDialog(dialog, "Payment added.");
                dialog.dispose();
            }
        });

        JPanel bottom = new JPanel();
        bottom.add(save);
        bottom.add(cancel);
        finishDialog(dialog, form, bottom, 480, 480);
    }

    private java.util.List<MemberItem> loadMemberItems() {
        java.util.List<MemberItem> list = new java.util.ArrayList<>();
        String sql = "SELECT mem_id, first_name, last_name FROM members_tbl ORDER BY first_name, last_name";
        try (java.sql.Connection con = DBConnection.connect();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MemberItem(rs.getInt("mem_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name")));
            }
        } catch (java.sql.SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load members: " + ex.getMessage());
        }
        return list;
    }

    // =====================================================
    //  Search field (with magnifier icon + placeholder)
    // =====================================================
    private JTextField createSearchField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();

                super.paintComponent(g);   // the typed text

                Graphics2D h = (Graphics2D) g.create();
                h.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cy = getHeight() / 2;
                h.setColor(TEXT);
                h.setStroke(new BasicStroke(2f));
                h.drawOval(16, cy - 9, 13, 13);          // magnifier circle
                h.drawLine(27, cy + 3, 32, cy + 8);      // magnifier handle
                if (getText().isEmpty()) {
                    h.setColor(MUTED);
                    h.setFont(getFont());
                    FontMetrics fm = h.getFontMetrics();
                    h.drawString("Search member...", 46,
                            (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                }
                h.dispose();
            }
        };
        f.setUI(new javax.swing.plaf.basic.BasicTextFieldUI());  // ignore look-and-feel colors
        f.setOpaque(false);
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        f.setBorder(new EmptyBorder(0, 46, 0, 12));
        return f;
    }

    // =====================================================
    //  Status dropdown
    // =====================================================
    private JComboBox<String> createStatusCombo() {
        JComboBox<String> cb = new JComboBox<>(new String[]{"All Status", "Paid", "Pending"});
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        cb.setBackground(FIELD);
        cb.setForeground(TEXT);
        cb.setBorder(new LineBorder(LINE, 1, true));
        cb.setFocusable(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cb.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton();
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusable(false);
                return b;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle r, boolean focus) {
                g.setColor(FIELD);
                g.fillRect(0, 0, comboBox.getWidth(), comboBox.getHeight());
                // chevron on the right side
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT);
                g2.setStroke(new BasicStroke(2f));
                int cx = comboBox.getWidth() - 26, cy = comboBox.getHeight() / 2;
                g2.drawLine(cx - 5, cy - 2, cx, cy + 3);
                g2.drawLine(cx, cy + 3, cx + 5, cy - 2);
                g2.dispose();
            }
        });

        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                l.setBorder(new EmptyBorder(8, 14, 8, 14));
                l.setForeground(TEXT);
                l.setBackground(selected && index >= 0 ? new Color(90, 25, 25) : FIELD);
                return l;
            }
        });
        return cb;
    }

    // =====================================================
    //  Helper classes
    // =====================================================

    /** Item shown in the "Member" dropdown of the Add Payment dialog. */
    private static class MemberItem {
        final int id;
        final String name;
        MemberItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    /** Dark rounded card used for the filter bar and the table. */
    private static class RoundedPanel extends JPanel {
        RoundedPanel(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.setColor(LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A label drawn as a rounded pill (used for Paid / Pending / View). */
    private static class Pill extends JLabel {
        private final Color fill, border;
        private final boolean fullyRound;

        Pill(String text, Color fill, Color border, Color fg, boolean fullyRound) {
            super(text, SwingConstants.CENTER);
            this.fill = fill;
            this.border = border;
            this.fullyRound = fullyRound;
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
            setBorder(new EmptyBorder(6, 18, 6, 18));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = fullyRound ? getHeight() : 12;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Renders the Status column (pills) or the Action column (View button). */
    private static class PillRenderer implements TableCellRenderer {
        private final boolean actionColumn;

        PillRenderer(boolean actionColumn) { this.actionColumn = actionColumn; }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(false);
            p.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, 0, LINE),
                    new EmptyBorder(0, actionColumn ? 0 : 20, 0, 0)));

            String s = String.valueOf(value);
            Pill pill;
            if (actionColumn) {
                pill = new Pill("View", new Color(48, 50, 56), LINE, TEXT, false);
                pill.setBorder(new EmptyBorder(9, 28, 9, 28));
            } else if (s.equals("Paid")) {
                pill = new Pill(s, new Color(30, 70, 45), new Color(60, 130, 80),
                        new Color(140, 230, 160), true);
            } else {
                pill = new Pill(s, new Color(85, 65, 20), new Color(170, 130, 40),
                        new Color(245, 205, 90), true);
            }

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1;
            gbc.anchor = actionColumn ? GridBagConstraints.CENTER : GridBagConstraints.WEST;
            p.add(pill, gbc);
            return p;
        }
    }
}