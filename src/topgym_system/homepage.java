package topgym_system;
 
/**
 *
 * @author Khastrel Troy Ruita
 */
public class homepage extends javax.swing.JFrame {
    
private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(homepage.class.getName());
 
    private int countMembers() {
        int count = 0;
        try (java.sql.Connection conn = DBConnection.connect();
             java.sql.PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM members_tbl");
             java.sql.ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
            System.out.println("Connected to: " + conn.getCatalog() + ", count = " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
    
 
    // members who checked in today (same data the Attendance page uses)
    private int countPresentToday() {
        int count = 0;
        String sql = "SELECT COUNT(DISTINCT mem_id) FROM attendance_tbl WHERE att_date = CURDATE()";
        try (java.sql.Connection conn = DBConnection.connect();
             java.sql.PreparedStatement pst = conn.prepareStatement(sql);
             java.sql.ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
 
    // total money received today (only payments marked "Paid")
    private java.math.BigDecimal sumPaymentsToday() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments_tbl "
                   + "WHERE pay_date = CURDATE() AND status = 'Paid'";
        try (java.sql.Connection conn = DBConnection.connect();
             java.sql.PreparedStatement pst = conn.prepareStatement(sql);
             java.sql.ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                total = rs.getBigDecimal(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
 
    private javax.swing.JLabel lblActive;
 
    private void placeActive() {
        java.awt.Point p = javax.swing.SwingUtilities.convertPoint(
                lblTotal.getParent(), lblTotal.getLocation(), getLayeredPane());
        lblActive.setBounds(p.x + 632, p.y, 300, lblTotal.getHeight());
    }
 
    // ---- today's payments amount ----
    // lblPayments (from the form) only marks the spot; this label draws the amount there
    // with a width that is not limited by the form's layout.
    private javax.swing.JLabel lblPaymentsAmount;
 
    private void placePaymentsAmount() {
        if (lblPaymentsAmount == null || lblPayments.getParent() == null) return;
        java.awt.Point p = javax.swing.SwingUtilities.convertPoint(
                lblPayments.getParent(), lblPayments.getLocation(), getLayeredPane());
        lblPaymentsAmount.setBounds(p.x, p.y, 340, lblPayments.getHeight());
    }
 
    public homepage() {
        setUndecorated(true);   // must come BEFORE initComponents()
        initComponents();
        lblTotal.setText(String.valueOf(countMembers()));
        lblPresent.setText(String.valueOf(countPresentToday()));
        UserName.load(UserSession.getUserId(), UserSession.getUsername());
        if (!UserName.get().isEmpty()) jLabel1.setText(UserName.get().toUpperCase());
 
        // true full screen (covers the taskbar)
        java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screen.width, screen.height);
 
        SideMenu.attach(this, "Home");   // shared side menu
 
        // today's total money on the Payments card
        lblPayments.setText(" ");   // keeps its place in the layout; the amount is drawn by lblPaymentsAmount
        lblPaymentsAmount = new javax.swing.JLabel(String.format("\u20B1%,.2f", sumPaymentsToday()));
        lblPaymentsAmount.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 54));
        lblPaymentsAmount.setForeground(java.awt.Color.WHITE);
        lblPaymentsAmount.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        getLayeredPane().add(lblPaymentsAmount, javax.swing.JLayeredPane.PALETTE_LAYER);
 
        java.awt.event.ComponentAdapter follow = new java.awt.event.ComponentAdapter() {
            @Override public void componentMoved(java.awt.event.ComponentEvent e)   { placePaymentsAmount(); }
            @Override public void componentResized(java.awt.event.ComponentEvent e) { placePaymentsAmount(); }
            @Override public void componentShown(java.awt.event.ComponentEvent e)   { placePaymentsAmount(); }
        };
        lblPayments.addComponentListener(follow);
        bgPanel12.addComponentListener(follow);
        javax.swing.SwingUtilities.invokeLater(this::placePaymentsAmount);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgPanel12 = new topgym_system.BgPanel1();
        lblTotal = new javax.swing.JLabel();
        lblPresent = new javax.swing.JLabel();
        lblPayments = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 60)); // NOI18N
        lblTotal.setForeground(new java.awt.Color(255, 255, 255));
        lblTotal.setText("0");

        lblPresent.setFont(new java.awt.Font("Segoe UI", 1, 60)); // NOI18N
        lblPresent.setForeground(new java.awt.Color(255, 255, 255));
        lblPresent.setText("s");

        lblPayments.setFont(new java.awt.Font("Segoe UI", 1, 60)); // NOI18N
        lblPayments.setForeground(new java.awt.Color(255, 255, 255));
        lblPayments.setText("t");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("ADMIN");

        javax.swing.GroupLayout bgPanel12Layout = new javax.swing.GroupLayout(bgPanel12);
        bgPanel12.setLayout(bgPanel12Layout);
        bgPanel12Layout.setHorizontalGroup(
            bgPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bgPanel12Layout.createSequentialGroup()
                .addGroup(bgPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bgPanel12Layout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(lblTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(350, 350, 350)
                        .addComponent(lblPresent, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(371, 371, 371)
                        .addComponent(lblPayments, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(bgPanel12Layout.createSequentialGroup()
                        .addGap(326, 326, 326)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(422, Short.MAX_VALUE))
        );
        bgPanel12Layout.setVerticalGroup(
            bgPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bgPanel12Layout.createSequentialGroup()
                .addGap(127, 127, 127)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 327, Short.MAX_VALUE)
                .addGroup(bgPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotal)
                    .addComponent(lblPresent)
                    .addComponent(lblPayments))
                .addGap(241, 241, 241))
        );

        getContentPane().add(bgPanel12, java.awt.BorderLayout.CENTER);

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
        java.awt.EventQueue.invokeLater(() -> new homepage().setVisible(true));
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private topgym_system.BgPanel1 bgPanel12;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblPayments;
    private javax.swing.JLabel lblPresent;
    private javax.swing.JLabel lblTotal;
    // End of variables declaration//GEN-END:variables
}
