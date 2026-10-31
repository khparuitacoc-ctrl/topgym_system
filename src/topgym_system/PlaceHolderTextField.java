/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
import java.awt.*;
import javax.swing.JTextField;

public class PlaceHolderTextField extends JTextField {
    private final String hint;

    public PlaceHolderTextField(String hint) {
        this.hint = hint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(150, 150, 150));
            g2.setFont(getFont());
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int areaH = getHeight() - in.top - in.bottom;
            int y = in.top + (areaH - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(hint, in.left, y);
            g2.dispose();
        }
    }
}