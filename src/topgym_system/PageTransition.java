package topgym_system;

import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Fast page switch, no dark screen:
 *   PageTransition.go(this, homepage::new);
 *
 * The next page is created right away, then it fades in over the old page
 * (a quick cross-fade) and the old page closes.
 */
public final class PageTransition {

    private static final int FADE_MS = 110;      // lower = faster, 0 = instant
    private static boolean busy = false;         // ignore clicks while a switch is running

    private PageTransition() { }

    public static void go(JFrame from, Supplier<JFrame> next) {
        if (busy) return;
        busy = true;

        JFrame to;
        try {
            to = next.get();
        } catch (RuntimeException ex) {
            busy = false;
            throw ex;
        }

        boolean canFade = FADE_MS > 0;
        if (canFade) {
            try {
                to.setOpacity(0f);               // needs an undecorated window (all your pages are)
            } catch (RuntimeException ex) {
                canFade = false;
            }
        }
        final boolean fade = canFade;

        to.setVisible(true);
        SwingUtilities.invokeLater(() -> {       // let it paint once, then fade in
            if (!fade) {
                from.dispose();
                busy = false;
                return;
            }
            animate(FADE_MS, p -> to.setOpacity((float) p), () -> {
                to.setOpacity(1f);
                from.dispose();
                busy = false;
            });
        });
    }

    // time-based animation: same speed on slow and fast computers
    private static void animate(int ms, DoubleConsumer step, Runnable done) {
        final long start = System.nanoTime();
        final Timer t = new Timer(8, null);
        t.addActionListener(e -> {
            double p = Math.min(1.0, (System.nanoTime() - start) / 1_000_000.0 / ms);
            step.accept(p);
            if (p >= 1.0) {
                t.stop();
                done.run();
            }
        });
        t.start();
    }
}