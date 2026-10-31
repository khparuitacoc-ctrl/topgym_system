/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
import java.awt.*;
import javax.swing.*;
public class StatusDialog {

    private enum Type { SUCCESS, ERROR, WARNING }

    private static final Color BG = new Color(18, 18, 20);

    private static final Color GREEN      = new Color(40, 200, 90);
    private static final Color GREEN_DARK = new Color(20, 130, 55);
    private static final Color RED        = new Color(220, 0, 0);
    private static final Color RED_DARK   = new Color(150, 0, 0);
    private static final Color AMBER      = new Color(255, 170, 0);
    private static final Color AMBER_DARK = new Color(200, 110, 0);

    // green: login worked
    public static void success(Component parent, String msg) {
        show(parent, msg, Type.SUCCESS);
    }

    // red: wrong username or password
    public static void error(Component parent, String msg) {
        show(parent, msg, Type.ERROR);
    }

    // amber: nothing typed in
    public static void warning(Component parent, String msg) {
        show(parent, msg, Type.WARNING);
    }

    private static void show(Component parent, String msg, Type type) {
        final Color accent;
        final Color accentDark;
        switch (type) {
            case SUCCESS: accent = GREEN; accentDark = GREEN_DARK; break;
            case WARNING: accent = AMBER; accentDark = AMBER_DARK; break;
            default:      accent = RED;   accentDark = RED_DARK;   break;
        }

        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 40, 24, 40));

        JComponent icon = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(90, 90); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(2, 2, 86, 86);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));                
                switch (type) {
                    case SUCCESS:
                        g2.drawLine(25, 47, 39, 61);
                        g2.drawLine(39, 61, 66, 30);
                        break;
                    case WARNING:
                        g2.drawLine(45, 24, 45, 50);
                        g2.fillOval(40, 59, 10, 10);
                        break;
                    default:
                        g2.drawLine(29, 29, 61, 61);
                        g2.drawLine(61, 29, 29, 61);
                }
                g2.dispose();
            }
        };
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        icon.setMaximumSize(new Dimension(90, 90));
        icon.setMinimumSize(new Dimension(90, 90));

        JLabel label = new JLabel(msg, SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn = new JButton("OK") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? accentDark : accent;
                Color bot = getModel().isPressed() ? accentDark.darker() : accentDark;
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(135, 38));
        btn.setMaximumSize(new Dimension(135, 38));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> d.dispose());

        card.add(icon);
        card.add(Box.createVerticalStrut(14));
        card.add(label);
        card.add(Box.createVerticalStrut(18));
        card.add(btn);

        d.setContentPane(card);
        d.getRootPane().setDefaultButton(btn);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();
        d.setLocationRelativeTo(parent);

        if (type == Type.SUCCESS) {
            Timer t = new Timer(1300, e -> d.dispose());
            t.setRepeats(false);
            t.start();
        }
        d.setVisible(true);
    }
}
