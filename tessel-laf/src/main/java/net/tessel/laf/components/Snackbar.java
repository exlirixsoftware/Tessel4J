package net.tessel.laf.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.tessel.laf.Severity;

/**
 * Transient notifications stacked in the bottom-right corner of a window.
 *
 * <pre>{@code Snackbar.show(frame, "Saved", "Your changes were saved.", Severity.SUCCESS);}</pre>
 */
public final class Snackbar {

    private static final String HOST_KEY = "Tessel.snackbarHost";
    private static final int WIDTH = 380;
    private static final int MARGIN = 24;

    /** Default display time in milliseconds. */
    public static int defaultDuration = 4000;

    private Snackbar() {
    }

    public static InfoBar show(Component owner, String title, String message) {
        return show(owner, title, message, Severity.INFORMATIONAL, defaultDuration);
    }

    public static InfoBar show(Component owner, String title, String message, Severity severity) {
        return show(owner, title, message, severity, defaultDuration);
    }

    /**
     * Shows a snackbar in the window containing {@code owner}.
     *
     * @param durationMillis time before it fades out; hovering keeps it open
     */
    public static InfoBar show(Component owner, String title, String message, Severity severity, int durationMillis) {
        JRootPane root = rootPane(owner);
        if (root == null) return null;
        Host host = host(root);

        InfoBar bar = new InfoBar(severity, title, message);
        bar.setElevated(true);
        bar.setSurfaceBackground(true);
        bar.setAlpha(0f);
        bar.addActionListener(e -> host.removeBar(bar));
        host.add(bar);
        host.relayout();

        Animator.fade(bar, 0f, 1f, null);

        boolean[] hovered = {false};
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered[0] = bar.getMousePosition(true) != null;
            }
        });

        Timer dismiss = new Timer(durationMillis, null);
        dismiss.addActionListener(e -> {
            if (hovered[0]) return;
            dismiss.stop();
            Animator.fade(bar, 1f, 0f, bar::close);
        });
        dismiss.start();
        return bar;
    }

    private static JRootPane rootPane(Component owner) {
        if (owner instanceof RootPaneContainer) return ((RootPaneContainer) owner).getRootPane();
        if (owner instanceof JRootPane) return (JRootPane) owner;
        if (owner != null) return SwingUtilities.getRootPane(owner);
        for (Window w : Window.getWindows()) {
            if (w.isActive() && w instanceof RootPaneContainer) return ((RootPaneContainer) w).getRootPane();
        }
        return null;
    }

    private static Host host(JRootPane root) {
        Object existing = root.getClientProperty(HOST_KEY);
        if (existing instanceof Host) return (Host) existing;
        Host host = new Host(root.getLayeredPane());
        root.putClientProperty(HOST_KEY, host);
        return host;
    }

    /** Transparent container in the layered pane holding the bars. */
    private static final class Host extends JPanel {
        private final JLayeredPane layeredPane;

        Host(JLayeredPane layeredPane) {
            this.layeredPane = layeredPane;
            setOpaque(false);
            setLayout(new StackLayout());
            layeredPane.add(this, JLayeredPane.POPUP_LAYER);
            layeredPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    relayout();
                }
            });
        }

        void removeBar(InfoBar bar) {
            remove(bar);
            relayout();
        }

        void relayout() {
            Dimension d = getPreferredSize();
            setBounds(layeredPane.getWidth() - d.width - MARGIN + 6, layeredPane.getHeight() - d.height - MARGIN + 8, d.width, d.height);
            revalidate();
            repaint();
            layeredPane.repaint();
        }
    }

    /** Stacks children vertically at a fixed width. */
    private static final class StackLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int h = 0;
            for (Component c : parent.getComponents()) {
                c.setSize(WIDTH, Math.max(1, c.getHeight()));
                h += c.getPreferredSize().height;
            }
            return new Dimension(WIDTH, h);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            int y = 0;
            for (Component c : parent.getComponents()) {
                c.setSize(WIDTH, Math.max(1, c.getHeight()));
                int h = c.getPreferredSize().height;
                c.setBounds(0, y, WIDTH, h);
                y += h;
            }
        }
    }

    /** Small fade helper. */
    static final class Animator {
        static void fade(InfoBar bar, float from, float to, Runnable done) {
            long start = System.currentTimeMillis();
            Timer t = new Timer(15, null);
            t.addActionListener(e -> {
                float k = Math.min(1f, (System.currentTimeMillis() - start) / 200f);
                float eased = 1f - (1f - k) * (1f - k);
                bar.setAlpha(from + (to - from) * eased);
                if (k >= 1f) {
                    t.stop();
                    if (done != null) done.run();
                }
            });
            t.start();
        }
    }
}
