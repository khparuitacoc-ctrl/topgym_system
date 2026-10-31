/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package topgym_system;
import java.awt.*;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class BgPanel2 extends JPanel {

    private Image img;

    public BgPanel2() {
        java.net.URL url = getClass().getResource("/topgym_system/membersback.png");
        if (url != null) {
            img = new ImageIcon(url).getImage();
        } else {
            System.err.println("members.png not found in /topgym_system/");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (img == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
    }
}