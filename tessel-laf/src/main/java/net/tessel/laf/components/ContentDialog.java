package net.tessel.laf.components;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;
import net.tessel.laf.ui.UIUtils;

/**
 * A modal dialog drawn over the window content (like Tessel.NET's ContentDialog).
 *
 * <pre>{@code
 * ContentDialog d = new ContentDialog("Delete file?", "This can't be undone.");
 * d.setPrimaryButtonText("Delete");
 * d.setCloseButtonText("Cancel");
 * d.setPrimaryDestructive(true);
 * if (d.showModal(frame) == ContentDialog.Result.PRIMARY) { ... }
 * }</pre>
 */
public class ContentDialog {

    public enum Result { NONE, PRIMARY, SECONDARY }

    private String title;
    private Object content;
    private String primaryText = "";
    private String secondaryText = "";
    private String closeText = "";
    private boolean primaryEnabled = true;
    private boolean primaryDestructive;
    private Predicate<ContentDialog> primaryHandler;
    private Predicate<ContentDialog> secondaryHandler;

    private Overlay overlay;
    private JButton primaryButton;
    private CompletableFuture<Result> future;
    private Component previousFocus;

    public ContentDialog() {
    }

    public ContentDialog(String title, Object content) {
        this.title = title;
        this.content = content;
    }

    // ------------------------------------------------------------------ properties

    public void setTitle(String title) {
        this.title = title;
    }

    /** A string (wrapped text) or any component. */
    public void setContent(Object content) {
        this.content = content;
    }

    public void setPrimaryButtonText(String text) {
        this.primaryText = text == null ? "" : text;
    }

    public void setSecondaryButtonText(String text) {
        this.secondaryText = text == null ? "" : text;
    }

    public void setCloseButtonText(String text) {
        this.closeText = text == null ? "" : text;
    }

    public void setPrimaryButtonEnabled(boolean enabled) {
        this.primaryEnabled = enabled;
        if (primaryButton != null) primaryButton.setEnabled(enabled);
    }

    public boolean isPrimaryButtonEnabled() {
        return primaryEnabled;
    }

    /** Uses the danger (red) style for the primary button. */
    public void setPrimaryDestructive(boolean destructive) {
        this.primaryDestructive = destructive;
    }

    /** Called when the primary button is clicked; return false to keep the dialog open. */
    public void onPrimary(Predicate<ContentDialog> handler) {
        this.primaryHandler = handler;
    }

    /** Called when the secondary button is clicked; return false to keep the dialog open. */
    public void onSecondary(Predicate<ContentDialog> handler) {
        this.secondaryHandler = handler;
    }

    public boolean isOpen() {
        return future != null;
    }

    // ------------------------------------------------------------------ showing

    /** Shows the dialog and returns immediately; the future completes when it closes. */
    public CompletableFuture<Result> showAsync(Component owner) {
        if (future != null) throw new IllegalStateException("The dialog is already open.");
        JRootPane root = rootPane(owner);
        if (root == null) throw new IllegalStateException("No window to show the dialog in.");

        future = new CompletableFuture<>();
        previousFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        overlay = new Overlay(root.getLayeredPane(), buildPanel());
        overlay.open();
        return future;
    }

    /** Shows the dialog and blocks (while still processing events) until it closes. */
    public Result showModal(Component owner) {
        CompletableFuture<Result> f = showAsync(owner);
        if (EventQueue.isDispatchThread()) {
            SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            f.whenComplete((r, e) -> loop.exit());
            if (!f.isDone()) loop.enter();
        }
        return f.join();
    }

    /** Closes the dialog with {@link Result#NONE}. */
    public void hide() {
        hide(Result.NONE);
    }

    public void hide(Result result) {
        if (future == null) return;
        overlay.close();
        overlay = null;
        primaryButton = null;
        CompletableFuture<Result> f = future;
        future = null;
        if (previousFocus != null) previousFocus.requestFocusInWindow();
        previousFocus = null;
        f.complete(result);
    }

    private static JRootPane rootPane(Component owner) {
        if (owner instanceof RootPaneContainer) return ((RootPaneContainer) owner).getRootPane();
        if (owner != null) return SwingUtilities.getRootPane(owner);
        for (Window w : Window.getWindows()) {
            if (w.isActive() && w instanceof RootPaneContainer) return ((RootPaneContainer) w).getRootPane();
        }
        return null;
    }

    // ------------------------------------------------------------------ UI

    private JComponent buildPanel() {
        DialogPanel panel = new DialogPanel();

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        if (title != null && !title.isEmpty()) {
            JLabel t = Tessel.label(title, TextStyle.SUBTITLE);
            body.add(t, BorderLayout.NORTH);
        }
        if (content instanceof Component) body.add((Component) content, BorderLayout.CENTER);
        else if (content != null) body.add(new TextBlock(content.toString(), TextStyle.BODY), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 0, 8, 0));
        buttons.setOpaque(false);
        if (!primaryText.isEmpty()) {
            primaryButton = new JButton(primaryText);
            Tessel.style(primaryButton, primaryDestructive ? ButtonStyle.DANGER : ButtonStyle.ACCENT);
            primaryButton.setEnabled(primaryEnabled);
            primaryButton.addActionListener(e -> {
                if (primaryHandler == null || primaryHandler.test(this)) hide(Result.PRIMARY);
            });
            buttons.add(primaryButton);
        }
        if (!secondaryText.isEmpty()) {
            JButton b = new JButton(secondaryText);
            b.addActionListener(e -> {
                if (secondaryHandler == null || secondaryHandler.test(this)) hide(Result.SECONDARY);
            });
            buttons.add(b);
        }
        if (!closeText.isEmpty()) {
            JButton b = new JButton(closeText);
            b.addActionListener(e -> hide(Result.NONE));
            buttons.add(b);
        }

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        footer.add(buttons, BorderLayout.CENTER);

        panel.add(body, BorderLayout.CENTER);
        panel.add(footer, BorderLayout.SOUTH);
        panel.footer = footer;
        return panel;
    }

    /** Rounded dialog surface with a tinted footer. */
    private static final class DialogPanel extends JPanel {
        JPanel footer;

        DialogPanel() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                int w = getWidth();
                int h = getHeight();
                g2.setColor(p.surface);
                UIUtils.fillRound(g2, 0, 0, w, h, 8);
                if (footer != null) {
                    int fy = footer.getY();
                    java.awt.Shape old = g2.getClip();
                    g2.clipRect(0, fy, w, h - fy);
                    g2.setColor(p.surfaceAlt);
                    UIUtils.fillRound(g2, 0, 0, w, h, 8);
                    g2.setClip(old);
                    g2.setColor(p.border);
                    g2.fillRect(1, fy, w - 2, 1);
                }
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, 0, 0, w, h, 8, 1f);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Full-window smoke layer that blocks input and centers the dialog. */
    private final class Overlay extends JPanel {
        private final JLayeredPane layeredPane;
        private final JComponent dialog;
        private float alpha;
        private final ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                revalidate();
            }
        };

        Overlay(JLayeredPane layeredPane, JComponent dialog) {
            this.layeredPane = layeredPane;
            this.dialog = dialog;
            setOpaque(false);
            setLayout(new CenterLayout());
            add(dialog);
            // Swallow mouse input so nothing behind the smoke can be clicked.
            MouseAdapter blocker = new MouseAdapter() {
            };
            addMouseListener(blocker);
            addMouseMotionListener(blocker);
            addMouseWheelListener(blocker);

            setFocusCycleRoot(true);
            setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());

            getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
            getActionMap().put("close", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ContentDialog.this.hide(Result.NONE);
                }
            });
        }

        void open() {
            setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            layeredPane.add(this, JLayeredPane.MODAL_LAYER);
            layeredPane.addComponentListener(resizeListener);
            revalidate();
            layeredPane.repaint();
            animate();
            SwingUtilities.invokeLater(() -> {
                Component first = getFocusTraversalPolicy().getDefaultComponent(this);
                if (!(first instanceof javax.swing.text.JTextComponent) && primaryButton != null && primaryButton.isEnabled()) {
                    first = primaryButton;
                }
                if (first != null) first.requestFocusInWindow();
            });
        }

        void close() {
            layeredPane.removeComponentListener(resizeListener);
            layeredPane.remove(this);
            layeredPane.repaint();
        }

        private void animate() {
            long start = System.currentTimeMillis();
            Timer t = new Timer(15, null);
            t.addActionListener(e -> {
                float k = Math.min(1f, (System.currentTimeMillis() - start) / 180f);
                alpha = 1f - (1f - k) * (1f - k);
                repaint();
                if (k >= 1f) t.stop();
            });
            t.start();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
                g2.setColor(Tessel.palette().overlay);
                g2.fillRect(0, 0, getWidth(), getHeight());
                double scale = 0.96 + 0.04 * alpha;
                double cx = dialog.getX() + dialog.getWidth() / 2.0;
                double cy = dialog.getY() + dialog.getHeight() / 2.0;
                g2.translate(cx, cy);
                g2.scale(scale, scale);
                g2.translate(-cx, -cy);
                Graphics2D sg = UIUtils.create(g2);
                UIUtils.paintShadow(sg, dialog.getX(), dialog.getY(), dialog.getWidth(), dialog.getHeight(), 8, 14);
                sg.dispose();
                super.paintChildren(g2);
            } finally {
                g2.dispose();
            }
        }
    }

    /** Centers the single child with a width between 320 and 548. */
    private static final class CenterLayout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return parent.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() == 0) return;
            Component c = parent.getComponent(0);
            Insets in = parent.getInsets();
            int available = parent.getWidth() - 48;
            int w = Math.max(320, Math.min(548, c.getPreferredSize().width));
            w = Math.min(w, Math.max(200, available));
            c.setSize(w, c.getHeight() == 0 ? 10 : c.getHeight());
            int h = Math.min(c.getPreferredSize().height, parent.getHeight() - 48);
            c.setBounds(in.left + (parent.getWidth() - w) / 2, in.top + (parent.getHeight() - h) / 2, w, h);
        }
    }
}
