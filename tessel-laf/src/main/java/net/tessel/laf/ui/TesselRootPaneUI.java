package net.tessel.laf.ui;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRootPaneUI;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.TesselStrings;
import net.tessel.laf.win.NativeWindowEffects;

/**
 * Root pane delegate that draws window decorations for decorated {@link javax.swing.JDialog}s (and
 * {@link javax.swing.JFrame}s): a themed title bar with icon, title and window buttons, a thin outline,
 * and on Windows real chrome (drop shadow, rounded corners, native dragging and edge resizing, see
 * {@link NativeWindowEffects}). Elsewhere dragging and resizing are done in Java.
 *
 * <p>Swing asks for decorations when {@code JDialog.setDefaultLookAndFeelDecorated(true)} is in effect, which
 * {@link Tessel#setup(net.tessel.laf.TesselTheme)} enables by default (see
 * {@link Tessel#setDecoratedDialogs(boolean)}). Then every {@code JOptionPane}, {@code JFileChooser},
 * {@code JColorChooser} and plain {@code JDialog} gets this chrome; root panes without a decoration style
 * (such as {@code TesselFrame}'s own) are left alone.
 */
public class TesselRootPaneUI extends BasicRootPaneUI {

    /** Height of the title bar. */
    public static final int TITLE_HEIGHT = 36;
    private static final int BUTTON_WIDTH = 46;
    private static final int RESIZE_EDGE = 6;

    private JRootPane root;
    private Decorations decorations;

    public static ComponentUI createUI(JComponent c) {
        return new TesselRootPaneUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        root = (JRootPane) c;
        if (root.getWindowDecorationStyle() != JRootPane.NONE) installDecorations();
    }

    @Override
    public void uninstallUI(JComponent c) {
        uninstallDecorations();
        super.uninstallUI(c);
        root = null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
        if ("windowDecorationStyle".equals(e.getPropertyName()) && root != null) {
            if (root.getWindowDecorationStyle() == JRootPane.NONE) uninstallDecorations();
            else installDecorations();
        }
    }

    private void installDecorations() {
        if (decorations != null) return;
        decorations = new Decorations(root);
        decorations.install();
    }

    private void uninstallDecorations() {
        if (decorations == null) return;
        decorations.uninstall();
        decorations = null;
    }

    /**
     * Closes {@code window} the way its close button does: window listeners get a {@code windowClosing}
     * (so dialogs such as {@code JOptionPane} can record that they were dismissed), then the default close
     * operation of a {@code JDialog} / {@code JFrame} is applied.
     */
    public static void requestClose(Window window) {
        WindowEvent closing = new WindowEvent(window, WindowEvent.WINDOW_CLOSING);
        for (WindowListener l : window.getWindowListeners()) l.windowClosing(closing);
        int operation = window instanceof javax.swing.JDialog ? ((javax.swing.JDialog) window).getDefaultCloseOperation()
                : window instanceof javax.swing.JFrame ? ((javax.swing.JFrame) window).getDefaultCloseOperation()
                : javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
        switch (operation) {
            case javax.swing.WindowConstants.HIDE_ON_CLOSE:
                window.setVisible(false);
                break;
            case javax.swing.WindowConstants.DISPOSE_ON_CLOSE:
                window.dispose();
                break;
            case javax.swing.WindowConstants.EXIT_ON_CLOSE:
                window.dispose();
                System.exit(0);
                break;
            default:
                break;
        }
    }

    private static boolean isResizable(Window w) {
        if (w instanceof Dialog) return ((Dialog) w).isResizable();
        if (w instanceof Frame) return ((Frame) w).isResizable();
        return false;
    }

    private static boolean isMaximized(Window w) {
        return w instanceof Frame && (((Frame) w).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    // ------------------------------------------------------------------ decorations

    /** Everything that exists only while the root pane is decorated. */
    private final class Decorations implements NativeWindowEffects.FrameModel {
        private final JRootPane pane;
        private final TitlePane title = new TitlePane();
        private Border oldBorder;
        private LayoutManager oldLayout;
        private Window window;
        private NativeWindowEffects effects;
        private Resizer resizer;

        // Snapshot read by the native message thread (see NativeWindowEffects.FrameModel).
        private volatile int hitW;
        private volatile int hitH;
        private volatile int hitTitleBottom;
        private volatile boolean hitResizable;
        private volatile boolean hitMaximized;
        private volatile Rectangle hitMin = new Rectangle();
        private volatile Rectangle hitMax = new Rectangle();
        private volatile Rectangle hitClose = new Rectangle();
        private volatile boolean hitMaxVisible;
        private volatile boolean hitMinVisible;
        private volatile Dimension hitMinimumSize = new Dimension();

        private final HierarchyListener hierarchyListener = this::hierarchyChanged;
        private final ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshHit();
            }
        };
        private final WindowAdapter windowListener = new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                title.repaint();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                title.repaint();
            }

            @Override
            public void windowStateChanged(WindowEvent e) {
                title.updateButtons();
                pane.revalidate();
                pane.repaint();
            }
        };
        private final Runnable themeListener = () -> {
            if (effects != null) {
                effects.setDarkMode(Tessel.palette().dark);
                SwingUtilities.invokeLater(effects::forceFullRepaint);
            }
        };

        Decorations(JRootPane pane) {
            this.pane = pane;
        }

        void install() {
            oldBorder = pane.getBorder();
            oldLayout = pane.getLayout();
            pane.getLayeredPane().add(title, JLayeredPane.FRAME_CONTENT_LAYER);
            pane.setBorder(new OutlineBorder());
            pane.setLayout(new DecoratedLayout(pane, title));
            pane.addHierarchyListener(hierarchyListener);
            Tessel.addThemeListener(themeListener);
            attach(SwingUtilities.getWindowAncestor(pane));
            pane.revalidate();
        }

        void uninstall() {
            detach();
            pane.removeHierarchyListener(hierarchyListener);
            Tessel.removeThemeListener(themeListener);
            pane.getLayeredPane().remove(title);
            pane.setBorder(oldBorder);
            pane.setLayout(oldLayout);
            pane.revalidate();
            pane.repaint();
        }

        private void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & (HierarchyEvent.PARENT_CHANGED | HierarchyEvent.DISPLAYABILITY_CHANGED)) == 0) return;
            Window w = SwingUtilities.getWindowAncestor(pane);
            if (w != window) {
                detach();
                attach(w);
            } else if (w != null && pane.isDisplayable() && effects == null && resizer == null) {
                installNative();
            } else if (!pane.isDisplayable() && effects != null) {
                effects.uninstall();
                effects = null;
            }
        }

        private void attach(Window w) {
            window = w;
            if (w == null) return;
            w.addWindowListener(windowListener);
            w.addWindowStateListener(windowListener);
            w.addComponentListener(resizeListener);
            title.updateButtons();
            refreshHit();
            if (pane.isDisplayable()) installNative();
        }

        private void detach() {
            if (window != null) {
                window.removeWindowListener(windowListener);
                window.removeWindowStateListener(windowListener);
                window.removeComponentListener(resizeListener);
            }
            if (effects != null) {
                effects.uninstall();
                effects = null;
            }
            if (resizer != null) {
                resizer.uninstall();
                resizer = null;
            }
            window = null;
        }

        private void installNative() {
            refreshHit();
            // Dialogs get only the shadow and rounded corners: native frame styles are what lets Windows flash
            // its own title bar while the window fades in and out. Frames keep full native chrome.
            boolean dialog = window instanceof Dialog;
            effects = dialog ? NativeWindowEffects.installVisualOnly(window) : NativeWindowEffects.install(window, this);
            if (effects != null) effects.setDarkMode(Tessel.palette().dark);
            if (effects == null || dialog) {
                resizer = new Resizer(window);
                resizer.install();
            }
        }

        /** Recomputes the snapshot for the native hit-testing. EDT only. */
        void refreshHit() {
            if (window == null) return;
            hitW = window.getWidth();
            hitH = window.getHeight();
            hitResizable = isResizable(window);
            hitMaximized = isMaximized(window);
            hitMinimumSize = window.getMinimumSize();
            Insets in = pane.getInsets();
            hitTitleBottom = in.top + title.getHeight();
            hitMinVisible = title.minimize.isVisible();
            hitMaxVisible = title.maximize.isVisible();
            hitMin = physical(title.minimize, in);
            hitMax = physical(title.maximize, in);
            hitClose = physical(title.close, in);
        }

        private Rectangle physical(Component c, Insets in) {
            return new Rectangle(in.left + c.getX(), in.top + c.getY(), c.getWidth(), c.getHeight());
        }

        // ---- NativeWindowEffects.FrameModel (native thread: cached fields only)

        @Override
        public NativeWindowEffects.Hit hitTest(int x, int y) {
            if (hitResizable && !hitMaximized) {
                boolean n = y < RESIZE_EDGE;
                boolean s = y >= hitH - RESIZE_EDGE;
                boolean w = x < RESIZE_EDGE;
                boolean e = x >= hitW - RESIZE_EDGE;
                if (n && w) return NativeWindowEffects.Hit.TOP_LEFT;
                if (n && e) return NativeWindowEffects.Hit.TOP_RIGHT;
                if (s && w) return NativeWindowEffects.Hit.BOTTOM_LEFT;
                if (s && e) return NativeWindowEffects.Hit.BOTTOM_RIGHT;
                if (n) return NativeWindowEffects.Hit.TOP;
                if (s) return NativeWindowEffects.Hit.BOTTOM;
                if (w) return NativeWindowEffects.Hit.LEFT;
                if (e) return NativeWindowEffects.Hit.RIGHT;
            }
            if (y >= hitTitleBottom) return null;
            if (hitMaxVisible && hitMax.contains(x, y)) return NativeWindowEffects.Hit.MAX_BUTTON;
            if (hitMinVisible && hitMin.contains(x, y)) return NativeWindowEffects.Hit.MIN_BUTTON;
            if (hitClose.contains(x, y)) return NativeWindowEffects.Hit.CLOSE_BUTTON;
            return NativeWindowEffects.Hit.CAPTION;
        }

        @Override
        public Dimension resizeMinimumSize() {
            return hitMinimumSize;
        }

        @Override
        public void onButtonHover(NativeWindowEffects.Hit hovered) {
            SwingUtilities.invokeLater(() -> {
                title.maximize.getModel().setRollover(hovered == NativeWindowEffects.Hit.MAX_BUTTON);
                title.minimize.getModel().setRollover(hovered == NativeWindowEffects.Hit.MIN_BUTTON);
                title.close.getModel().setRollover(hovered == NativeWindowEffects.Hit.CLOSE_BUTTON);
            });
        }

        // ---- title bar

        private final class TitlePane extends JComponent {
            final ChromeButton minimize = new ChromeButton(ChromeButton.MINIMIZE);
            final ChromeButton maximize = new ChromeButton(ChromeButton.MAXIMIZE);
            final ChromeButton close = new ChromeButton(ChromeButton.CLOSE);
            private Point pressPoint;

            TitlePane() {
                setOpaque(true);
                add(minimize);
                add(maximize);
                add(close);
                minimize.addActionListener(e -> {
                    if (window instanceof Frame) ((Frame) window).setExtendedState(((Frame) window).getExtendedState() | Frame.ICONIFIED);
                });
                maximize.addActionListener(e -> toggleMaximize());
                close.addActionListener(e -> {
                    if (window != null) requestClose(window);
                });
                MouseAdapter drag = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        pressPoint = SwingUtilities.isLeftMouseButton(e) && !isMaximized(window) ? e.getPoint() : null;
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (pressPoint == null || window == null) return;
                        Point screen = e.getLocationOnScreen();
                        window.setLocation(screen.x - pressPoint.x - pane.getInsets().left, screen.y - pressPoint.y - pane.getInsets().top);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        pressPoint = null;
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && maximize.isVisible()) toggleMaximize();
                    }
                };
                addMouseListener(drag);
                addMouseMotionListener(drag);
            }

            private void toggleMaximize() {
                if (!(window instanceof Frame)) return;
                Frame f = (Frame) window;
                f.setExtendedState(isMaximized(f) ? f.getExtendedState() & ~Frame.MAXIMIZED_BOTH : f.getExtendedState() | Frame.MAXIMIZED_BOTH);
            }

            void updateButtons() {
                boolean frame = window instanceof Frame;
                boolean resizable = window != null && isResizable(window);
                minimize.setVisible(frame);
                maximize.setVisible(frame && resizable);
                maximize.kind = isMaximized(window) ? ChromeButton.RESTORE : ChromeButton.MAXIMIZE;
                maximize.setToolTipText(TesselStrings.get(isMaximized(window) ? "restore" : "maximize"));
                minimize.setToolTipText(TesselStrings.get("minimize"));
                close.setToolTipText(TesselStrings.get("close"));
                revalidate();
                repaint();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, TITLE_HEIGHT);
            }

            @Override
            public void doLayout() {
                int x = getWidth();
                for (ChromeButton b : new ChromeButton[]{close, maximize, minimize}) {
                    if (!b.isVisible()) continue;
                    x -= BUTTON_WIDTH;
                    b.setBounds(x, 0, BUTTON_WIDTH, getHeight());
                }
                refreshHit();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Palette p = Tessel.palette();
                g.setColor(p.surface);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (window == null) return;
                Graphics2D g2 = UIUtils.create(g);
                try {
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int x = 14;
                    Image icon = pickIcon();
                    if (icon != null) {
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2.drawImage(icon, x, (getHeight() - 16) / 2, 16, 16, null);
                        x += 26;
                    }
                    String text = windowTitle();
                    g2.setFont(Fonts.regular(12));
                    g2.setColor(window.isActive() ? p.textSecondary : p.textTertiary);
                    FontMetrics fm = g2.getFontMetrics();
                    int right = getWidth() - 8;
                    for (ChromeButton b : new ChromeButton[]{minimize, maximize, close}) {
                        if (b.isVisible()) right = Math.min(right, b.getX() - 4);
                    }
                    g2.drawString(clip(text, fm, right - x), x, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                } finally {
                    g2.dispose();
                }
            }

            private String windowTitle() {
                if (window instanceof Dialog) return ((Dialog) window).getTitle() == null ? "" : ((Dialog) window).getTitle();
                if (window instanceof Frame) return ((Frame) window).getTitle() == null ? "" : ((Frame) window).getTitle();
                return "";
            }

            private Image pickIcon() {
                List<Image> images = window.getIconImages();
                if (images.isEmpty()) return null;
                Image best = images.get(0);
                for (Image img : images) {
                    if (img.getWidth(null) > best.getWidth(null)) best = img;
                }
                return best;
            }

            private String clip(String s, FontMetrics fm, int width) {
                if (fm.stringWidth(s) <= width) return s;
                int end = s.length();
                while (end > 0 && fm.stringWidth(s.substring(0, end) + "…") > width) end--;
                return s.substring(0, end) + "…";
            }
        }
    }

    // ------------------------------------------------------------------ pieces

    /** One pixel outline around the window, hidden while maximized. */
    private static final class OutlineBorder extends AbstractBorder implements javax.swing.plaf.UIResource {
        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            int v = isMaximized(SwingUtilities.getWindowAncestor(c)) ? 0 : 1;
            i.set(v, v, v, v);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (isMaximized(SwingUtilities.getWindowAncestor(c))) return;
            Palette p = Tessel.palette();
            g.setColor(p.dark ? p.borderStrong : p.border);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    /** Title bar on top, then the menu bar and the content pane. */
    private static final class DecoratedLayout implements LayoutManager2 {
        private final JRootPane pane;
        private final JComponent title;

        DecoratedLayout(JRootPane pane, JComponent title) {
            this.pane = pane;
            this.title = title;
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets i = pane.getInsets();
            int w = parent.getWidth() - i.left - i.right;
            int h = parent.getHeight() - i.top - i.bottom;
            pane.getLayeredPane().setBounds(i.left, i.top, w, h);
            if (pane.getGlassPane() != null) pane.getGlassPane().setBounds(i.left, i.top, w, h);
            int y = 0;
            title.setBounds(0, 0, w, TITLE_HEIGHT);
            y += TITLE_HEIGHT;
            JComponent menu = pane.getJMenuBar();
            if (menu != null && menu.isVisible()) {
                int mh = menu.getPreferredSize().height;
                menu.setBounds(0, y, w, mh);
                y += mh;
            }
            if (pane.getContentPane() != null) pane.getContentPane().setBounds(0, y, w, Math.max(0, h - y));
        }

        private Dimension size(Dimension content) {
            Insets i = pane.getInsets();
            int w = content.width;
            int h = content.height + TITLE_HEIGHT;
            JComponent menu = pane.getJMenuBar();
            if (menu != null && menu.isVisible()) {
                Dimension m = menu.getPreferredSize();
                h += m.height;
                w = Math.max(w, m.width);
            }
            return new Dimension(w + i.left + i.right, h + i.top + i.bottom);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Container c = pane.getContentPane();
            return size(c == null ? new Dimension() : c.getPreferredSize());
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Container c = pane.getContentPane();
            return size(c == null ? new Dimension() : c.getMinimumSize());
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }
    }

    /** Minimize / maximize / restore / close. */
    private static final class ChromeButton extends JButton {
        static final int MINIMIZE = 0;
        static final int MAXIMIZE = 1;
        static final int RESTORE = 2;
        static final int CLOSE = 3;

        int kind;

        ChromeButton(int kind) {
            this.kind = kind;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(javax.swing.BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isArmed() && getModel().isPressed();
            if (hover || pressed) {
                g.setColor(kind == CLOSE ? (pressed ? new Color(0xB3261A) : new Color(0xC42B1C)) : pressed ? p.controlPressed : p.controlHover);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            Window w = SwingUtilities.getWindowAncestor(this);
            boolean active = w == null || w.isActive();
            Color fg = kind == CLOSE && (hover || pressed) ? Color.WHITE : active ? p.text : p.textTertiary;
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(1f));
                double cx = Math.floor(getWidth() / 2.0) + 0.5;
                double cy = Math.floor(getHeight() / 2.0) + 0.5;
                switch (kind) {
                    case MINIMIZE:
                        g2.draw(new Line2D.Double(cx - 5, cy, cx + 5, cy));
                        break;
                    case MAXIMIZE:
                        g2.draw(new Rectangle2D.Double(cx - 5, cy - 5, 10, 10));
                        break;
                    case RESTORE:
                        g2.draw(new Rectangle2D.Double(cx - 5, cy - 3, 8, 8));
                        g2.draw(new Line2D.Double(cx - 3, cy - 3, cx - 3, cy - 5));
                        g2.draw(new Line2D.Double(cx - 3, cy - 5, cx + 5, cy - 5));
                        g2.draw(new Line2D.Double(cx + 5, cy - 5, cx + 5, cy + 3));
                        g2.draw(new Line2D.Double(cx + 5, cy + 3, cx + 3, cy + 3));
                        break;
                    default:
                        g2.draw(new Line2D.Double(cx - 5, cy - 5, cx + 5, cy + 5));
                        g2.draw(new Line2D.Double(cx + 5, cy - 5, cx - 5, cy + 5));
                        break;
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Edge resizing for decorated windows when there is no native chrome. */
    private static final class Resizer implements AWTEventListener {
        private static final int N = 1;
        private static final int S = 2;
        private static final int W = 4;
        private static final int E = 8;

        private final Window window;
        private int dragEdges;
        private Point pressScreen;
        private Rectangle pressBounds;
        private boolean cursorSet;

        Resizer(Window window) {
            this.window = window;
        }

        void install() {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }

        void uninstall() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        }

        private JRootPane rootPane() {
            return window instanceof javax.swing.RootPaneContainer ? ((javax.swing.RootPaneContainer) window).getRootPane() : null;
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            Component src = me.getComponent();
            if (src == null || (src != window && SwingUtilities.getWindowAncestor(src) != window)) return;
            if (!isResizable(window) || isMaximized(window)) {
                resetCursor();
                return;
            }
            Point p = SwingUtilities.convertPoint(src, me.getPoint(), window);
            switch (me.getID()) {
                case MouseEvent.MOUSE_MOVED:
                    updateCursor(edgesAt(p));
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    int edges = edgesAt(p);
                    if (edges != 0 && SwingUtilities.isLeftMouseButton(me)) {
                        dragEdges = edges;
                        pressScreen = me.getLocationOnScreen();
                        pressBounds = window.getBounds();
                        me.consume();
                    }
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    if (dragEdges != 0) {
                        resize(me.getLocationOnScreen());
                        me.consume();
                    }
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    dragEdges = 0;
                    break;
                default:
                    break;
            }
        }

        private int edgesAt(Point p) {
            int edges = 0;
            if (p.y < RESIZE_EDGE) edges |= N;
            if (p.y >= window.getHeight() - RESIZE_EDGE) edges |= S;
            if (p.x < RESIZE_EDGE) edges |= W;
            if (p.x >= window.getWidth() - RESIZE_EDGE) edges |= E;
            return edges;
        }

        private void updateCursor(int edges) {
            int type;
            switch (edges) {
                case N: type = Cursor.N_RESIZE_CURSOR; break;
                case S: type = Cursor.S_RESIZE_CURSOR; break;
                case W: type = Cursor.W_RESIZE_CURSOR; break;
                case E: type = Cursor.E_RESIZE_CURSOR; break;
                case N | W: type = Cursor.NW_RESIZE_CURSOR; break;
                case N | E: type = Cursor.NE_RESIZE_CURSOR; break;
                case S | W: type = Cursor.SW_RESIZE_CURSOR; break;
                case S | E: type = Cursor.SE_RESIZE_CURSOR; break;
                default: type = -1; break;
            }
            JRootPane rp = rootPane();
            if (rp == null) return;
            if (type < 0) {
                resetCursor();
            } else {
                rp.setCursor(Cursor.getPredefinedCursor(type));
                cursorSet = true;
            }
        }

        private void resetCursor() {
            JRootPane rp = rootPane();
            if (cursorSet && rp != null) {
                rp.setCursor(null);
                cursorSet = false;
            }
        }

        private void resize(Point screen) {
            int dx = screen.x - pressScreen.x;
            int dy = screen.y - pressScreen.y;
            Rectangle r = new Rectangle(pressBounds);
            Dimension min = window.getMinimumSize();
            if ((dragEdges & E) != 0) r.width = Math.max(min.width, pressBounds.width + dx);
            if ((dragEdges & S) != 0) r.height = Math.max(min.height, pressBounds.height + dy);
            if ((dragEdges & W) != 0) {
                r.width = Math.max(min.width, pressBounds.width - dx);
                r.x = pressBounds.x + pressBounds.width - r.width;
            }
            if ((dragEdges & N) != 0) {
                r.height = Math.max(min.height, pressBounds.height - dy);
                r.y = pressBounds.y + pressBounds.height - r.height;
            }
            window.setBounds(r);
            window.validate();
        }
    }
}
