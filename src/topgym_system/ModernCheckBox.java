/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;

import java.awt.*;
import javax.swing.JCheckBox;

public class ModernCheckBox extends JCheckBox {
 private final Color RED = new Color(220, 0, 0);
    private final Color BORDER = new Color(120, 120, 125);

    public ModernCheckBox() {
        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorder(null);
        setForeground(new Color(200, 200, 205));
        setFont(new Font("Segoe UI", Font.PLAIN, 18));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setIconTextGap(10);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

       int size = 24;
        int y = (getHeight() - size) / 2;

        if (isSelected()) {
            g2.setColor(RED);
            g2.fillRoundRect(0, y, size, size, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{5, 9, 15}, new int[]{y + 10, y + 14, y + 6}, 3);
        } else {
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRoundRect(0, y, size, size, 6, 6);
            g2.setColor(getModel().isRollover() ? Color.WHITE : BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, y, size - 1, size - 1, 6, 6);
        }

        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), size + getIconTextGap(), ty);

        g2.dispose();
    }
}