/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.JButton;
public class LoginButton extends JButton{
     private static final int RADIUS = 14;
    private static final int GLOW = 6;
    private static final Color TOP = new Color(235, 25, 25);
    private static final Color BOTTOM = new Color(165, 0, 0);
    private static final Color TOP_HOVER = new Color(255, 55, 55);
    private static final Color BOTTOM_HOVER = new Color(200, 0, 0);

    private float hover = 0f;
    private boolean hovering = false;
    private final javax.swing.Timer fade;

    public LoginButton() {
        this("LOG IN");
    }

    public LoginButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(300, 52));

        fade = new javax.swing.Timer(15, e -> {
            hover += hovering ? 0.12f : -0.12f;
            if (hover >= 1f) { hover = 1f; ((javax.swing.Timer) e.getSource()).stop(); }
            if (hover <= 0f) { hover = 0f; ((javax.swing.Timer) e.getSource()).stop(); }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovering = true;  fade.start(); }
            @Override public void mouseExited(MouseEvent e)  { hovering = false; fade.start(); }
        });
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(
            (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int bx = GLOW, by = GLOW, bw = w - GLOW * 2, bh = h - GLOW * 2;
            int arc = RADIUS * 2;
            boolean pressed = getModel().isPressed();

            // soft glow on hover
            if (hover > 0f && isEnabled()) {
                for (int i = GLOW; i > 0; i--) {
                    g2.setColor(new Color(255, 30, 30, (int) (hover * 14)));
                    g2.fillRoundRect(bx - i, by - i, bw + i * 2, bh + i * 2, arc + i * 2, arc + i * 2);
                }
            }

            // body gradient
            Color top, bottom;
            if (!isEnabled()) {
                top = new Color(90, 90, 90);
                bottom = new Color(60, 60, 60);
            } else if (pressed) {
                top = BOTTOM;
                bottom = new Color(120, 0, 0);
            } else {
                top = blend(TOP, TOP_HOVER, hover);
                bottom = blend(BOTTOM, BOTTOM_HOVER, hover);
            }
            g2.setPaint(new GradientPaint(0, by, top, 0, by + bh, bottom));
            g2.fillRoundRect(bx, by, bw, bh, arc, arc);

            // subtle top highlight edge
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawRoundRect(bx, by, bw - 1, bh - 1, arc, arc);

            // text with letter spacing
            Font f = getFont().deriveFont(Map.of(TextAttribute.TRACKING, 0.08f));
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent() + (pressed ? 1 : 0);
            g2.setColor(new Color(0, 0, 0, 90)); // text shadow
            g2.drawString(text, tx, ty + 1);
            g2.setColor(isEnabled() ? getForeground() : new Color(170, 170, 170));
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }
}
