/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
public class SideMenu {
 
    private final javax.swing.JFrame frame;
    private final String current;
    private final int w = 260;
 
    private javax.swing.JPanel sidebar;
    private javax.swing.JPanel overlay;
    private boolean menuOpen = false;
    private float dim = 0f;   // how dark the page behind the menu is (0..1)
    private javax.swing.Timer slideTimer;
 
    public static void attach(javax.swing.JFrame frame, String current) {
        new SideMenu(frame, current).build();
    }
 
    private SideMenu(javax.swing.JFrame frame, String current) {
        this.frame = frame;
        this.current = current;
    }
 
    // ---------- build ----------
    private void build() {
        int h = frame.getHeight();
 
        sidebar = new javax.swing.JPanel(null) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                g.setColor(new java.awt.Color(210, 20, 20));
                g.fillRect(getWidth() - 3, 0, 3, getHeight());      // red edge
                g.setColor(new java.awt.Color(60, 60, 60));
                g.fillRect(25, 385, getWidth() - 50, 1);            // divider
            }
        };
        sidebar.setBackground(new java.awt.Color(15, 15, 15));
        sidebar.setBounds(-w, 0, w, h);                             // hidden off-screen
 
        javax.swing.JLabel title = new javax.swing.JLabel(
                "<html><span style='color:white'>TOP</span><span style='color:#E01414'>GYM</span></html>",
                javax.swing.SwingConstants.CENTER);
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD | java.awt.Font.ITALIC, 38));
        title.setBounds(0, 40, w, 60);
        sidebar.add(title);
 
        MenuLabel home       = item("Home",       130, "Home".equals(current));
        MenuLabel members    = item("Members",    190, "Members".equals(current));
        MenuLabel attendance = item("Attendance", 250, "Attendance".equals(current));
        MenuLabel payment    = item("Payment",    310, "Payment".equals(current));
        MenuLabel settings   = item("Settings",   405, false);
        MenuLabel logout     = item("Logout",     465, false);
 
        home.setIcon(new NavIcon("home", 24));
        members.setIcon(new NavIcon("members", 24));
        attendance.setIcon(new NavIcon("attendance", 24));
        payment.setIcon(new NavIcon("payment", 24));
        settings.setIcon(icon("/topgym_system/Settings.png", 24));
        logout.setIcon(icon("/topgym_system/Logout.png", 24));
 
        onClick(home,       () -> go("Home"));
        onClick(members,    () -> go("Members"));
        onClick(attendance, () -> go("Attendance"));
        onClick(payment,    () -> go("Payment"));
          onClick(settings,   () -> SettingsDialog.show(frame));
        onClick(logout,     () -> confirmLogout());
 
        javax.swing.JLabel menuIcon = new javax.swing.JLabel(new MenuBarsIcon());
        menuIcon.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        menuIcon.setBounds(25, 15, 50, 50);
        menuIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { toggle(); }
        });
 
        // invisible layer that catches "click away" while the menu is open
        overlay = new javax.swing.JPanel(null) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                g.setColor(new java.awt.Color(0f, 0f, 0f, 0.45f * dim));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, frame.getWidth(), h);
        overlay.setVisible(false);
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { if (menuOpen) toggle(); }
        });
 
        javax.swing.JLayeredPane lp = frame.getLayeredPane();
        lp.add(overlay, javax.swing.JLayeredPane.MODAL_LAYER);
        lp.add(menuIcon, javax.swing.JLayeredPane.POPUP_LAYER);
        lp.add(sidebar, javax.swing.JLayeredPane.DRAG_LAYER);
    }
 
    private MenuLabel item(String text, int y, boolean active) {
        MenuLabel l = new MenuLabel(text, active);
        l.setBounds(25, y, w - 50, 46);
        sidebar.add(l);
        return l;
    }
 
    // ---------- behaviour ----------
    private void go(String name) {
        if (name.equals(current)) { toggle(); return; }   // already on this page
        java.util.function.Supplier<javax.swing.JFrame> next;
        switch (name) {
            case "Home":       next = homepage::new;   break;
            case "Members":    next = members::new;    break;
            case "Attendance": next = attendance::new; break;
            case "Payment":    next = payment::new;    break;
            default: return;
        }
        PageTransition.go(frame, next);   // smooth fade between pages
    }

    private void confirmLogout() {
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                frame, "Are you sure you want to log out?", "Logout",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);
        if (choice == javax.swing.JOptionPane.YES_OPTION) {
            PageTransition.go(frame, login::new);
        }
    }

    // time-based slide with easing. Only the menu moves each frame; the dim layer is
    // switched on/off once (repainting the whole full-screen page 60x a second is what made it laggy).
    private void toggle() {
        if (slideTimer != null && slideTimer.isRunning()) return;
        final int startX = sidebar.getX();
        final int target = menuOpen ? -w : 0;
        final boolean opening = !menuOpen;
        dim = opening ? 1f : 0f;
        overlay.setVisible(opening);   // opening: dim + catch clicks. closing: dim off right away
        final long start = System.nanoTime();
        final int duration = 200;   // milliseconds
        slideTimer = new javax.swing.Timer(8, null);
        slideTimer.addActionListener(e -> {
            double p = Math.min(1.0, (System.nanoTime() - start) / 1_000_000.0 / duration);
            double eased = 1 - Math.pow(1 - p, 3);   // fast start, soft landing
            sidebar.setLocation((int) Math.round(startX + (target - startX) * eased), 0);
            if (p >= 1.0) {
                slideTimer.stop();
                menuOpen = opening;
            }
        });
        slideTimer.start();
    }

    private static void onClick(javax.swing.JLabel label, Runnable action) {
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); }
        });
    }
 
    private static javax.swing.ImageIcon icon(String path, int height) {
        javax.swing.ImageIcon raw = new javax.swing.ImageIcon(SideMenu.class.getResource(path));
        java.awt.Image scaled = raw.getImage().getScaledInstance(-1, height, java.awt.Image.SCALE_SMOOTH);
        return new javax.swing.ImageIcon(scaled);
    }
 
    // ---------- one menu button ----------
    private static class MenuLabel extends javax.swing.JLabel {
        boolean active, hover;
 
        MenuLabel(String text, boolean active) {
            super(text, javax.swing.SwingConstants.CENTER);
            this.active = active;
            setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
            setForeground(java.awt.Color.WHITE);
            setIconTextGap(10);
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }
 
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor((active || hover) ? new java.awt.Color(210, 20, 20)
                                          : new java.awt.Color(38, 38, 38));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            g2.dispose();
            super.paintComponent(g);
        }
    }
 
    // ---------- icons drawn in code ----------
    private static class NavIcon implements javax.swing.Icon {
        private final String type;
        private final int size;
 
        NavIcon(String type, int size) {
            this.type = type;
            this.size = size;
        }
 
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
 
        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(java.awt.Color.WHITE);
            g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            int s = size;
            switch (type) {
                case "home":
                    g2.drawPolyline(new int[]{2, s / 2, s - 2}, new int[]{s / 2, 3, s / 2}, 3);
                    g2.drawPolyline(new int[]{5, 5, s - 5, s - 5}, new int[]{s / 2, s - 3, s - 3, s / 2}, 4);
                    g2.drawRect(s / 2 - 3, s - 10, 6, 7);
                    break;
                case "members":
                    g2.drawOval(s / 2 - 4, 3, 8, 8);
                    g2.drawArc(3, 13, s - 6, 16, 0, 180);
                    break;
                case "attendance":
                    g2.drawRoundRect(3, 5, s - 6, s - 8, 4, 4);
                    g2.drawLine(3, 10, s - 3, 10);
                    g2.drawLine(8, 2, 8, 6);
                    g2.drawLine(s - 8, 2, s - 8, 6);
                    g2.drawPolyline(new int[]{8, 11, 16}, new int[]{16, 19, 13}, 3);
                    break;
                case "payment":
                    g2.drawRoundRect(2, 5, s - 4, s - 10, 4, 4);
                    g2.drawLine(2, 10, s - 2, 10);
                    g2.drawLine(5, 15, 10, 15);
                    break;
            }
            g2.dispose();
        }
    }
 
    private static class MenuBarsIcon implements javax.swing.Icon {
        @Override public int getIconWidth()  { return 36; }
        @Override public int getIconHeight() { return 28; }
 
        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(java.awt.Color.WHITE);
            g2.fillRoundRect(x, y,      36, 4, 4, 4);
            g2.fillRoundRect(x, y + 12, 36, 4, 4, 4);
            g2.fillRoundRect(x, y + 24, 36, 4, 4, 4);
            g2.dispose();
        }
    }
}