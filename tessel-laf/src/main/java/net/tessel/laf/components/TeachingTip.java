package net.tessel.laf.components;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.tessel.laf.ButtonStyle;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.TextStyle;
import net.tessel.laf.icons.TesselIcons;
import net.tessel.laf.ui.UIUtils;

/**
 * A small popover that points at a control to explain it.
 *
 * <pre>{@code
 * TeachingTip tip = new TeachingTip("Save your work", "Press Ctrl+S at any time to save.");
 * tip.setTarget(saveButton);
 * tip.setActionButton("Got it", () -> {});
 * tip.show();
 * }</pre>
 *
 * The tip is drawn inside the window (not in a separate native window), picks the side with the most room
 * unless a {@link Placement} is given, and closes on Esc or when the user clicks elsewhere
 * (see {@link #setLightDismiss(boolean)}). Without a tail ({@link #setTailVisible(boolean)}) it works as a
 * plain flyout under its target.
 */
public class TeachingTip {

    public enum Placement { AUTO, TOP, BOTTOM, LEFT, RIGHT }

    public enum CloseReason { CLOSE_BUTTON, ACTION_BUTTON, LIGHT_DISMISS, PROGRAMMATIC }

    private static final int MARGIN = 12;
    private static final int TAIL = 10;
    private static final int RADIUS = 8;
    private static final int PADDING = 16;
    private static final int GAP = 3;

    private String title;
    private String subtitle;
    private Icon icon;
    private Component content;
    private Component target;
    private Placement placement = Placement.AUTO;
    private boolean lightDismiss = true;
    private boolean tailVisible = true;
    private boolean closeButtonVisible = true;
    private String actionText;
    private Runnable actionHandler;
    private String closeText;
    private int width = 320;
    private final List<Consumer<CloseReason>> closeListeners = new ArrayList<>();

    private Bubble bubble;
    private JLayeredPane layeredPane;
    private AWTEventListener dismissListener;
    private ComponentAdapter repositionListener;
    private final List<Component> watched = new ArrayList<>();

    public TeachingTip() {
    }

    public TeachingTip(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    // ------------------------------------------------------------------ properties

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /** An icon shown before the title. */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /** Any component shown below the text. */
    public void setContent(Component content) {
        this.content = content;
    }

    /** The control the tip points at. */
    public void setTarget(Component target) {
        this.target = target;
    }

    public Component getTarget() {
        return target;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement == null ? Placement.AUTO : placement;
    }

    public Placement getPlacement() {
        return placement;
    }

    /** True (the default): Esc and clicks outside close the tip. False: only its buttons close it. */
    public void setLightDismiss(boolean lightDismiss) {
        this.lightDismiss = lightDismiss;
    }

    public boolean isLightDismiss() {
        return lightDismiss;
    }

    /** False removes the pointer, giving a flyout positioned next to the target. */
    public void setTailVisible(boolean tailVisible) {
        this.tailVisible = tailVisible;
    }

    public boolean isTailVisible() {
        return tailVisible;
    }

    /** Shows or hides the "x" button in the corner. */
    public void setCloseButtonVisible(boolean visible) {
        this.closeButtonVisible = visible;
    }

    /** The accent-colored button at the bottom; {@code handler} runs before the tip closes. */
    public void setActionButton(String text, Runnable handler) {
        this.actionText = text;
        this.actionHandler = handler;
    }

    /** A second, plain button at the bottom that only closes the tip. */
    public void setCloseButtonText(String text) {
        this.closeText = text;
    }

    /** Width of the text area in pixels (default 320). */
    public void setContentWidth(int width) {
        this.width = Math.max(160, width);
    }

    public void addCloseListener(Consumer<CloseReason> listener) {
        closeListeners.add(listener);
    }

    public boolean isOpen() {
        return bubble != null;
    }

    // ------------------------------------------------------------------ showing

    /** Shows the tip next to {@code target}. */
    public void show(Component target) {
        this.target = target;
        show();
    }

    /** Shows the tip next to the target set with {@link #setTarget(Component)}. */
    public void show() {
        if (target == null) throw new IllegalStateException("A target is required.");
        if (bubble != null) {
            close(CloseReason.PROGRAMMATIC);
        }
        JRootPane root = SwingUtilities.getRootPane(target);
        if (root == null) throw new IllegalStateException("The target is not in a window.");
        layeredPane = root.getLayeredPane();
        bubble = new Bubble();
        layeredPane.add(bubble, JLayeredPane.POPUP_LAYER);
        place();
        bubble.open();

        repositionListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                place();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                place();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                close(CloseReason.PROGRAMMATIC);
            }
        };
        layeredPane.addComponentListener(repositionListener);
        // Scrolling moves an ancestor of the target, not the target itself.
        watched.clear();
        for (Component c = target; c != null && c != layeredPane; c = c.getParent()) {
            c.addComponentListener(repositionListener);
            watched.add(c);
        }

        if (lightDismiss) {
            Bubble own = bubble;
            dismissListener = event -> {
                if (bubble != own) return;
                if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_PRESSED) {
                    Component source = ((MouseEvent) event).getComponent();
                    Window sourceWindow = source == null ? null : SwingUtilities.getWindowAncestor(source);
                    Window ownWindow = SwingUtilities.getWindowAncestor(own);
                    boolean inside = source != null && SwingUtilities.isDescendingFrom(source, own);
                    if (!inside && sourceWindow == ownWindow) close(CloseReason.LIGHT_DISMISS);
                } else if (event instanceof KeyEvent && event.getID() == KeyEvent.KEY_PRESSED
                        && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_ESCAPE) {
                    close(CloseReason.LIGHT_DISMISS);
                }
            };
            try {
                Toolkit.getDefaultToolkit().addAWTEventListener(dismissListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
            } catch (SecurityException ignored) {
                dismissListener = null;
            }
        }
    }

    public void hide() {
        close(CloseReason.PROGRAMMATIC);
    }

    private void close(CloseReason reason) {
        if (bubble == null) return;
        Bubble old = bubble;
        bubble = null;
        if (dismissListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(dismissListener);
            dismissListener = null;
        }
        if (repositionListener != null) {
            layeredPane.removeComponentListener(repositionListener);
            for (Component c : watched) c.removeComponentListener(repositionListener);
            watched.clear();
            repositionListener = null;
        }
        old.dispose();
        Rectangle b = old.getBounds();
        layeredPane.remove(old);
        layeredPane.repaint(b);
        for (Consumer<CloseReason> l : new ArrayList<>(closeListeners)) l.accept(reason);
    }

    // ------------------------------------------------------------------ placement

    /** Positions the bubble and its tail around the target. */
    private void place() {
        if (bubble == null || target == null) return;
        if (!target.isShowing()) {
            close(CloseReason.PROGRAMMATIC);
            return;
        }
        Point origin = SwingUtilities.convertPoint(target, 0, 0, layeredPane);
        Rectangle t = new Rectangle(origin.x, origin.y, target.getWidth(), target.getHeight());
        Dimension body = bubble.bodySize();
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        int tail = tailVisible ? TAIL : 0;
        int gap = tailVisible ? GAP : 6;

        Placement p = placement;
        if (p == Placement.AUTO) {
            int below = h - t.y - t.height - 8;
            int above = t.y - 8;
            int right = w - t.x - t.width - 8;
            int left = t.x - 8;
            if (below >= body.height + tail + gap) p = Placement.BOTTOM;
            else if (above >= body.height + tail + gap) p = Placement.TOP;
            else if (right >= body.width + tail + gap) p = Placement.RIGHT;
            else if (left >= body.width + tail + gap) p = Placement.LEFT;
            else p = below >= above ? Placement.BOTTOM : Placement.TOP;
        }
        int bx;
        int by;
        switch (p) {
            case TOP:
                bx = clamp(t.x + t.width / 2 - body.width / 2, 8, w - body.width - 8);
                by = t.y - tail - gap - body.height;
                break;
            case LEFT:
                bx = t.x - tail - gap - body.width;
                by = clamp(t.y + t.height / 2 - body.height / 2, 8, h - body.height - 8);
                break;
            case RIGHT:
                bx = t.x + t.width + tail + gap;
                by = clamp(t.y + t.height / 2 - body.height / 2, 8, h - body.height - 8);
                break;
            default:
                bx = clamp(t.x + t.width / 2 - body.width / 2, 8, w - body.width - 8);
                by = t.y + t.height + tail + gap;
                break;
        }
        bubble.setPlacement(p, new Point(t.x + t.width / 2, t.y + t.height / 2), t);
        bubble.setBounds(bx - MARGIN, by - MARGIN, body.width + 2 * MARGIN, body.height + 2 * MARGIN);
        bubble.revalidate();
        bubble.repaint();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(Math.max(min, max), v));
    }

    // ------------------------------------------------------------------ the bubble

    private final class Bubble extends JPanel {
        private final JPanel body = new JPanel();
        private Placement placed = Placement.BOTTOM;
        private Point targetCenter = new Point();
        private Rectangle targetRect = new Rectangle();
        private float alpha;
        private Timer animation;

        Bubble() {
            super(null);
            setOpaque(false);
            body.setOpaque(false);
            body.setLayout(new Column());
            body.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
            build();
            add(body);
            setFocusCycleRoot(true);
        }

        private void build() {
            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setOpaque(false);
            boolean hasTitle = title != null && !title.isEmpty();
            if (hasTitle) {
                JLabel label = Tessel.label(title, TextStyle.BODY_STRONG);
                if (icon != null) label.setIcon(icon);
                label.setIconTextGap(8);
                header.add(label, BorderLayout.CENTER);
            }
            if (closeButtonVisible) {
                JButton close = new JButton(TesselIcons.close(14));
                Tessel.style(close, ButtonStyle.ICON);
                close.setToolTipText(TesselStrings.get("close"));
                close.setPreferredSize(new Dimension(28, 28));
                close.getAccessibleContext().setAccessibleName(TesselStrings.get("close"));
                close.addActionListener(e -> close(CloseReason.CLOSE_BUTTON));
                JPanel holder = new JPanel(new BorderLayout());
                holder.setOpaque(false);
                holder.add(close, BorderLayout.NORTH);
                header.add(holder, BorderLayout.EAST);
                // Pull the close button into the padding so the title keeps its line.
                header.setBorder(BorderFactory.createEmptyBorder(hasTitle ? 0 : 0, 0, 0, -8));
            }
            if (hasTitle || closeButtonVisible) body.add(header);
            if (subtitle != null && !subtitle.isEmpty()) body.add(new TextBlock(subtitle, TextStyle.SECONDARY));
            if (content != null) body.add(content);
            boolean hasAction = actionText != null && !actionText.isEmpty();
            boolean hasClose = closeText != null && !closeText.isEmpty();
            if (hasAction || hasClose) {
                JPanel buttons = new JPanel(new java.awt.GridLayout(1, 0, 8, 0));
                buttons.setOpaque(false);
                if (hasAction) {
                    JButton action = Tessel.style(new JButton(actionText), ButtonStyle.ACCENT);
                    action.addActionListener(e -> {
                        if (actionHandler != null) actionHandler.run();
                        close(CloseReason.ACTION_BUTTON);
                    });
                    buttons.add(action);
                }
                if (hasClose) {
                    JButton close = new JButton(closeText);
                    close.addActionListener(e -> close(CloseReason.CLOSE_BUTTON));
                    buttons.add(close);
                }
                body.add(buttons);
            }
        }

        /** Natural size of the body for the configured width. */
        Dimension bodySize() {
            Insets in = body.getInsets();
            int contentWidth = width;
            body.setSize(contentWidth + in.left + in.right, 10);
            Dimension d = body.getLayout().preferredLayoutSize(body);
            return new Dimension(contentWidth + in.left + in.right, d.height);
        }

        void setPlacement(Placement p, Point center, Rectangle rect) {
            placed = p;
            targetCenter = SwingUtilities.convertPoint(layeredPane, center, this);
            targetRect = new Rectangle(SwingUtilities.convertPoint(layeredPane, rect.getLocation(), this), rect.getSize());
        }

        @Override
        public void doLayout() {
            body.setBounds(MARGIN, MARGIN, Math.max(0, getWidth() - 2 * MARGIN), Math.max(0, getHeight() - 2 * MARGIN));
        }

        void open() {
            alpha = 0f;
            long start = System.currentTimeMillis();
            animation = new Timer(15, null);
            animation.addActionListener(e -> {
                float k = Math.min(1f, (System.currentTimeMillis() - start) / 160f);
                alpha = 1f - (1f - k) * (1f - k);
                repaint();
                if (k >= 1f) animation.stop();
            });
            animation.start();
        }

        void dispose() {
            if (animation != null) animation.stop();
        }

        private Dimension bodyDimension() {
            return new Dimension(getWidth() - 2 * MARGIN, getHeight() - 2 * MARGIN);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                Dimension b = bodyDimension();
                double x = MARGIN;
                double y = MARGIN;
                UIUtils.paintShadow(g2, x, y, b.width, b.height, RADIUS, 9);
                Area shape = new Area(new RoundRectangle2D.Double(x, y, b.width, b.height, RADIUS * 2, RADIUS * 2));
                if (tailVisible) {
                    Path2D tail = new Path2D.Double();
                    double tx;
                    double ty;
                    switch (placed) {
                        case TOP: {
                            tx = clampD(targetCenter.x, x + RADIUS + TAIL, x + b.width - RADIUS - TAIL);
                            ty = y + b.height;
                            tail.moveTo(tx - TAIL, ty - 1);
                            tail.lineTo(tx, ty + TAIL);
                            tail.lineTo(tx + TAIL, ty - 1);
                            break;
                        }
                        case LEFT: {
                            tx = x + b.width;
                            ty = clampD(targetCenter.y, y + RADIUS + TAIL, y + b.height - RADIUS - TAIL);
                            tail.moveTo(tx - 1, ty - TAIL);
                            tail.lineTo(tx + TAIL, ty);
                            tail.lineTo(tx - 1, ty + TAIL);
                            break;
                        }
                        case RIGHT: {
                            tx = x;
                            ty = clampD(targetCenter.y, y + RADIUS + TAIL, y + b.height - RADIUS - TAIL);
                            tail.moveTo(tx + 1, ty - TAIL);
                            tail.lineTo(tx - TAIL, ty);
                            tail.lineTo(tx + 1, ty + TAIL);
                            break;
                        }
                        default: {
                            tx = clampD(targetCenter.x, x + RADIUS + TAIL, x + b.width - RADIUS - TAIL);
                            ty = y;
                            tail.moveTo(tx - TAIL, ty + 1);
                            tail.lineTo(tx, ty - TAIL);
                            tail.lineTo(tx + TAIL, ty + 1);
                            break;
                        }
                    }
                    tail.closePath();
                    shape.add(new Area(tail));
                }
                g2.setColor(p.surface);
                g2.fill(shape);
                g2.setColor(p.dark ? p.borderStrong : p.border);
                g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g2.draw(shape);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
                // Slide in from the side of the target.
                double slide = (1 - alpha) * 8;
                switch (placed) {
                    case TOP:
                        g2.translate(0, slide);
                        break;
                    case LEFT:
                        g2.translate(slide, 0);
                        break;
                    case RIGHT:
                        g2.translate(-slide, 0);
                        break;
                    default:
                        g2.translate(0, -slide);
                        break;
                }
                super.paint(g2);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public boolean contains(int x, int y) {
            // The shadow margin does not swallow clicks.
            return x >= MARGIN - 2 && y >= MARGIN - 2 && x < getWidth() - MARGIN + 2 && y < getHeight() - MARGIN + 2;
        }
    }

    private static double clampD(double v, double min, double max) {
        return Math.max(min, Math.min(Math.max(min, max), v));
    }

    /** Stacks children at the body width with a small gap. */
    private static final class Column implements LayoutManager {
        private static final int GAP_Y = 8;

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = Math.max(1, parent.getWidth() - in.left - in.right);
            int h = in.top + in.bottom;
            int count = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                // Wrapping text needs its final width (and some height) to report the wrapped height.
                c.setSize(w, Short.MAX_VALUE);
                h += c.getPreferredSize().height;
                count++;
            }
            h += Math.max(0, count - 1) * GAP_Y;
            return new Dimension(w + in.left + in.right, h);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int w = Math.max(1, parent.getWidth() - in.left - in.right);
            int y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                c.setSize(w, Short.MAX_VALUE);
                int h = c.getPreferredSize().height;
                c.setBounds(in.left, y, w, h);
                y += h + GAP_Y;
            }
        }
    }
}
