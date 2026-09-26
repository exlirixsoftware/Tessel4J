package net.tessel.laf.components;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import net.tessel.laf.Fonts;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;
import net.tessel.laf.win.NativeWindowEffects;

/**
 * A frame with a themed, custom-drawn title bar (like Tessel.NET's TesselWindow).
 * Supports dragging, double-click to maximize, edge resizing and taskbar-aware maximizing.
 * Put extra components into the title bar with {@link #addTitleBarComponent(JComponent)}.
 */
public class TesselFrame extends JFrame implements NativeWindowEffects.FrameModel {

    private static final int RESIZE_EDGE = 6;

    private TitleBar titleBar;
    private int iconSize = 16;
    private int titleBarHeight = 36;
    private NativeWindowEffects nativeEffects;
    private final Runnable themeListener = () -> {
        if (nativeEffects != null) {
            nativeEffects.setDarkMode(Tessel.palette().dark);
            // A theme switch repaints everything at once; DWM's glass composition can otherwise
            // leave stale pixels from the previous theme behind (see forceFullRepaint's javadoc).
            SwingUtilities.invokeLater(nativeEffects::forceFullRepaint);
        }
    };

    // Snapshot of everything NativeWindowEffects.FrameModel needs, refreshed only on the EDT
    // (see refreshHitRegions) since FrameModel's methods run on the native message-pump thread.
    private volatile int hitW;
    private volatile int hitH;
    private volatile int hitTitleBarBottom;
    private volatile boolean hitResizable;
    private volatile boolean hitMaximized;
    private volatile boolean hitMaxButtonVisible;
    private volatile Rectangle hitMaxBounds = new Rectangle();
    private volatile Rectangle hitMinBounds = new Rectangle();
    private volatile Rectangle hitCloseBounds = new Rectangle();
    private volatile Rectangle[] hitContentComponentBounds = new Rectangle[0];
    private volatile Dimension hitMinimumSize = new Dimension();

    public TesselFrame() {
        this("");
    }

    public TesselFrame(String title) {
        super(title);
        setUndecorated(true);
        titleBar = new TitleBar();
        ((TesselRootPane) getRootPane()).setTitleBar(titleBar);
        getRootPane().setBorder(new FrameBorder());
        new WindowResizer(this).install();
        addWindowStateListener(e -> {
            getRootPane().revalidate();
            titleBar.updateButtons();
            repaint();
        });
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                titleBar.repaint();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                titleBar.repaint();
            }
        });
        addPropertyChangeListener("title", e -> titleBar.repaint());
        addPropertyChangeListener("iconImage", e -> titleBar.repaint());
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                refreshHitRegions();
            }
        });
        Tessel.addThemeListener(themeListener);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Tessel.removeThemeListener(themeListener);
                if (nativeEffects != null) nativeEffects.uninstall();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (nativeEffects == null) {
            refreshHitRegions();
            nativeEffects = NativeWindowEffects.install(this, this);
            if (nativeEffects != null) {
                nativeEffects.setDarkMode(Tessel.palette().dark);
                // Re-apply the icon after the native chrome install changes the window's styles and
                // forces a frame recalculation: the taskbar button is created from whatever icon is
                // current at that point, and doesn't always pick up on it changing later even though
                // WM_GETICON itself reports it correctly - a fresh setIconImages nudges Windows to
                // pick it up for the taskbar too, not just the title bar and Alt+Tab.
                List<Image> icons = getIconImages();
                if (!icons.isEmpty()) setIconImages(icons);
            }
        }
    }

    @Override
    public void setResizable(boolean resizable) {
        super.setResizable(resizable);
        refreshHitRegions();
        if (titleBar != null) titleBar.updateButtons();
    }

    /**
     * Recomputes the plain-field snapshot {@link NativeWindowEffects.FrameModel} reads. Only call on the EDT.
     *
     * <p>Deliberately avoids {@code SwingUtilities.convertPoint(..., this)} / {@code getLocationOnScreen()}:
     * once the native hook adds real {@code WS_CAPTION}/{@code WS_THICKFRAME} styles to this "undecorated"
     * peer (see {@link NativeWindowEffects}), AWT's own {@code Frame.getInsets()} tracking can pick up a
     * phantom native caption/frame inset that has nothing to do with the actual (zero) gap on screen, which
     * throws off any conversion routed through the Window. Instead this walks the one parent chain we author
     * ourselves - rootPane's own {@link FrameBorder} inset, then titleBar's direct children - which always
     * matches the physical window exactly, by construction.
     */
    private void refreshHitRegions() {
        if (titleBar == null) return;
        hitW = getWidth();
        hitH = getHeight();
        hitResizable = isResizable();
        hitMaximized = isMaximized();
        hitMinimumSize = getMinimumSize();
        Insets border = getRootPane().getInsets();
        hitTitleBarBottom = border.top + titleBar.getHeight();
        hitMaxButtonVisible = titleBar.maximize.isVisible();
        hitMaxBounds = physicalBounds(titleBar.maximize, border);
        hitMinBounds = physicalBounds(titleBar.minimize, border);
        hitCloseBounds = physicalBounds(titleBar.close, border);
        // titleBar.content spans most of the draggable middle of the title bar (it right-aligns extra
        // buttons behind glue); only its real children should block the drag, not the glue itself.
        Rectangle contentBounds = titleBar.content.getBounds();
        Component[] children = titleBar.content.getComponents();
        Rectangle[] bounds = new Rectangle[children.length];
        int count = 0;
        for (Component c : children) {
            if (c instanceof Box.Filler || !c.isVisible()) continue;
            Rectangle r = c.getBounds();
            bounds[count++] = new Rectangle(border.left + contentBounds.x + r.x, border.top + contentBounds.y + r.y, r.width, r.height);
        }
        hitContentComponentBounds = Arrays.copyOf(bounds, count);
    }

    /** {@code child}'s bounds (a direct child of titleBar) translated into physical window coordinates. */
    private static Rectangle physicalBounds(Component child, Insets rootBorder) {
        return new Rectangle(rootBorder.left + child.getX(), rootBorder.top + child.getY(), child.getWidth(), child.getHeight());
    }

    // -------------------------------------------------------- NativeWindowEffects.FrameModel
    // Called from the native message-pump thread: read only the cached hit* fields above, never
    // a live Swing/AWT getter (see the warning on NativeWindowEffects.FrameModel).

    @Override
    public NativeWindowEffects.Hit hitTest(int x, int y) {
        if (hitResizable && !hitMaximized) {
            NativeWindowEffects.Hit edge = edgeHit(x, y);
            if (edge != null) return edge;
        }
        if (y >= hitTitleBarBottom) return null;
        if (hitMaxButtonVisible && hitMaxBounds.contains(x, y)) return NativeWindowEffects.Hit.MAX_BUTTON;
        if (hitMinBounds.contains(x, y)) return NativeWindowEffects.Hit.MIN_BUTTON;
        if (hitCloseBounds.contains(x, y)) return NativeWindowEffects.Hit.CLOSE_BUTTON;
        for (Rectangle r : hitContentComponentBounds) {
            if (r.contains(x, y)) return null;
        }
        return NativeWindowEffects.Hit.CAPTION;
    }

    @Override
    public Dimension resizeMinimumSize() {
        return hitMinimumSize;
    }

    /**
     * All three buttons are plain {@code HTCLIENT} and also receive ordinary Swing mouse events, but
     * under fast real mouse movement Swing's own rollover tracking can occasionally desync (miss a
     * leave) and get stuck hovered. Rather than layer a second "is it hovered" signal on top of
     * Swing's, this resyncs the actual ButtonModel to ground truth on every native hit-test - which
     * fires on essentially every mouse move - so it self-heals regardless of what caused a desync
     * instead of just reflecting our own possibly-also-stale state.
     */
    @Override
    public void onButtonHover(NativeWindowEffects.Hit hoveredButton) {
        SwingUtilities.invokeLater(() -> {
            titleBar.maximize.getModel().setRollover(hoveredButton == NativeWindowEffects.Hit.MAX_BUTTON);
            titleBar.minimize.getModel().setRollover(hoveredButton == NativeWindowEffects.Hit.MIN_BUTTON);
            titleBar.close.getModel().setRollover(hoveredButton == NativeWindowEffects.Hit.CLOSE_BUTTON);
        });
    }

    private NativeWindowEffects.Hit edgeHit(int x, int y) {
        boolean n = y < RESIZE_EDGE;
        boolean s = y >= hitH - RESIZE_EDGE;
        boolean w0 = x < RESIZE_EDGE;
        boolean e = x >= hitW - RESIZE_EDGE;
        if (n && w0) return NativeWindowEffects.Hit.TOP_LEFT;
        if (n && e) return NativeWindowEffects.Hit.TOP_RIGHT;
        if (s && w0) return NativeWindowEffects.Hit.BOTTOM_LEFT;
        if (s && e) return NativeWindowEffects.Hit.BOTTOM_RIGHT;
        if (n) return NativeWindowEffects.Hit.TOP;
        if (s) return NativeWindowEffects.Hit.BOTTOM;
        if (w0) return NativeWindowEffects.Hit.LEFT;
        if (e) return NativeWindowEffects.Hit.RIGHT;
        return null;
    }

    @Override
    protected JRootPane createRootPane() {
        TesselRootPane rp = new TesselRootPane();
        rp.setOpaque(true);
        return rp;
    }

    /** Width and height of the icon in the title bar (default 16). */
    public void setIconSize(int size) {
        this.iconSize = size;
        titleBar.revalidate();
        titleBar.repaint();
    }

    public int getIconSize() {
        return iconSize;
    }

    public void setTitleBarHeight(int height) {
        this.titleBarHeight = height;
        getRootPane().revalidate();
    }

    public int getTitleBarHeight() {
        return titleBarHeight;
    }

    /** The container at the right side of the title bar (before the window buttons). */
    public JPanel getTitleBarContent() {
        return titleBar.content;
    }

    /** Adds a component to the right side of the title bar. */
    public void addTitleBarComponent(JComponent component) {
        component.setAlignmentY(0.5f);
        titleBar.content.add(component);
        titleBar.content.add(Box.createHorizontalStrut(8));
        titleBar.content.revalidate();
    }

    public boolean isMaximized() {
        return (getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
    }

    /** Maximizes to the work area of the current screen (keeps the taskbar visible). */
    public void maximize() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle b = gc.getBounds();
        Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        setMaximizedBounds(new Rectangle(b.x + in.left, b.y + in.top, b.width - in.left - in.right, b.height - in.top - in.bottom));
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);
    }

    public void restore() {
        setExtendedState(getExtendedState() & ~Frame.MAXIMIZED_BOTH);
    }

    public void toggleMaximize() {
        if (isMaximized()) restore();
        else maximize();
    }

    /**
     * Invoked by the close button. Notifies {@link WindowListener}s the same way a real
     * {@code WINDOW_CLOSING} event would (so an app can still veto closing, e.g. for unsaved
     * changes), then applies {@link #getDefaultCloseOperation()} directly - {@code dispatchEvent}
     * with a synthetic {@code WINDOW_CLOSING} does not reliably reach {@code JFrame}'s own
     * close-operation handling on this frame once native chrome is installed.
     */
    private void requestClose() {
        WindowEvent closing = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
        for (java.awt.event.WindowListener l : getWindowListeners()) {
            l.windowClosing(closing);
        }
        switch (getDefaultCloseOperation()) {
            case HIDE_ON_CLOSE:
                setVisible(false);
                break;
            case DISPOSE_ON_CLOSE:
                dispose();
                break;
            case EXIT_ON_CLOSE:
                dispose();
                System.exit(0);
                break;
            case DO_NOTHING_ON_CLOSE:
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ root pane

    /** Root pane that places the title bar above the menu bar and content pane. */
    private static final class TesselRootPane extends JRootPane {
        private JComponent titleBar;

        void setTitleBar(JComponent titleBar) {
            this.titleBar = titleBar;
            getLayeredPane().add(titleBar, JLayeredPane.FRAME_CONTENT_LAYER);
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            // JFrame captures the \"control\" color once; always paint the current theme background.
            g.setColor(Tessel.palette().background);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        @Override
        protected LayoutManager createRootLayout() {
            return new Layout();
        }

        private final class Layout implements LayoutManager2 {
            @Override
            public void layoutContainer(Container parent) {
                Rectangle b = parent.getBounds();
                Insets i = getInsets();
                int w = b.width - i.left - i.right;
                int h = b.height - i.top - i.bottom;
                if (layeredPane != null) layeredPane.setBounds(i.left, i.top, w, h);
                if (glassPane != null) glassPane.setBounds(i.left, i.top, w, h);
                int y = 0;
                if (titleBar != null && titleBar.isVisible()) {
                    int th = titleBar.getPreferredSize().height;
                    titleBar.setBounds(0, 0, w, th);
                    y += th;
                }
                if (menuBar != null && menuBar.isVisible()) {
                    int mh = menuBar.getPreferredSize().height;
                    menuBar.setBounds(0, y, w, mh);
                    y += mh;
                }
                if (contentPane != null) contentPane.setBounds(0, y, w, Math.max(0, h - y));
            }

            private Dimension size(Dimension content) {
                Insets i = getInsets();
                int w = content.width;
                int h = content.height;
                if (titleBar != null && titleBar.isVisible()) h += titleBar.getPreferredSize().height;
                if (menuBar != null && menuBar.isVisible()) {
                    Dimension m = menuBar.getPreferredSize();
                    h += m.height;
                    w = Math.max(w, m.width);
                }
                return new Dimension(w + i.left + i.right, h + i.top + i.bottom);
            }

            @Override
            public Dimension preferredLayoutSize(Container parent) {
                return size(contentPane == null ? new Dimension() : contentPane.getPreferredSize());
            }

            @Override
            public Dimension minimumLayoutSize(Container parent) {
                return size(contentPane == null ? new Dimension() : contentPane.getMinimumSize());
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
    }

    /** 1px window outline, hidden while maximized. */
    private final class FrameBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            int v = isMaximized() ? 0 : 1;
            i.set(v, v, v, v);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (isMaximized()) return;
            Palette p = Tessel.palette();
            g.setColor(p.dark ? p.borderStrong : p.border);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }

    // ------------------------------------------------------------------ title bar

    private final class TitleBar extends JComponent {
        final JPanel content = new JPanel();
        final WindowButton minimize = new WindowButton(WindowButton.MINIMIZE);
        final WindowButton maximize = new WindowButton(WindowButton.MAXIMIZE);
        final WindowButton close = new WindowButton(WindowButton.CLOSE);
        private Image scaledIcon;
        private Image scaledSource;
        private int scaledSize;
        private Point pressPoint;

        TitleBar() {
            setOpaque(true);
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
            content.add(Box.createHorizontalGlue());
            add(content);
            add(minimize);
            add(maximize);
            add(close);
            minimize.addActionListener(e -> setExtendedState(getExtendedState() | Frame.ICONIFIED));
            maximize.addActionListener(e -> toggleMaximize());
            close.addActionListener(e -> requestClose());

            MouseAdapter drag = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e) || WindowResizer.isResizing(TesselFrame.this)) {
                        pressPoint = null;
                        return;
                    }
                    pressPoint = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (pressPoint == null) return;
                    Point screen = e.getLocationOnScreen();
                    if (isMaximized()) {
                        double ratio = pressPoint.x / (double) Math.max(1, getWidth());
                        restore();
                        Rectangle normal = TesselFrame.this.getBounds();
                        pressPoint = new Point((int) (normal.width * ratio), pressPoint.y);
                    }
                    setLocation(screen.x - pressPoint.x - getRootPane().getInsets().left,
                            screen.y - pressPoint.y - getRootPane().getInsets().top);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressPoint = null;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && isResizable()) toggleMaximize();
                }
            };
            addMouseListener(drag);
            addMouseMotionListener(drag);
            updateButtons();
        }

        void updateButtons() {
            maximize.kind = isMaximized() ? WindowButton.RESTORE : WindowButton.MAXIMIZE;
            maximize.setToolTipText(isMaximized() ? "Restore Down" : "Maximize");
            maximize.setVisible(isResizable());
            maximize.repaint();
            TesselFrame.this.refreshHitRegions();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, titleBarHeight);
        }

        private int textStart() {
            return getIconImages().isEmpty() && getIconImage() == null ? 14 : 14 + iconSize + 10;
        }

        @Override
        public void doLayout() {
            int h = getHeight();
            int x = getWidth();
            for (WindowButton b : new WindowButton[]{close, maximize, minimize}) {
                if (!b.isVisible()) continue;
                x -= 46;
                b.setBounds(x, 0, 46, h);
            }
            int titleWidth = getFontMetrics(Fonts.regular(12)).stringWidth(getTitle() == null ? "" : getTitle());
            int start = textStart() + titleWidth + 16;
            content.setBounds(start, 0, Math.max(0, x - start), h);
            TesselFrame.this.refreshHitRegions();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            g.setColor(p.background);
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2 = UIUtils.create(g);
            try {
                boolean active = isActive();
                Image icon = pickIcon();
                if (icon != null) paintIcon(g2, icon, 14, (getHeight() - iconSize) / 2);

                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(Fonts.regular(12));
                g2.setColor(active ? p.textSecondary : p.textTertiary);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String title = getTitle() == null ? "" : getTitle();
                g2.drawString(title, textStart(), (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            } finally {
                g2.dispose();
            }
        }

        private Image pickIcon() {
            List<Image> images = getIconImages();
            if (images.isEmpty()) return getIconImage();
            Image best = images.get(0);
            for (Image img : images) {
                if (img.getWidth(null) > best.getWidth(null)) best = img;
            }
            return best;
        }

        /** Draws the icon pre-scaled to device pixels for crisp HiDPI rendering. */
        private void paintIcon(Graphics2D g2, Image src, int x, int y) {
            AffineTransform t = g2.getTransform();
            double scale = t.getScaleX();
            int target = (int) Math.round(iconSize * scale);
            int iw = src.getWidth(null);
            int ih = src.getHeight(null);
            if (iw <= 0 || ih <= 0) return;
            if (scaledIcon == null || scaledSource != src || scaledSize != target) {
                double f = Math.min(target / (double) iw, target / (double) ih);
                int w = Math.max(1, (int) Math.round(iw * f));
                int h = Math.max(1, (int) Math.round(ih * f));
                scaledIcon = downscale(src, w, h);
                scaledSource = src;
                scaledSize = target;
            }
            int w = scaledIcon.getWidth(null);
            int h = scaledIcon.getHeight(null);
            Graphics2D g = (Graphics2D) g2.create();
            try {
                double dx = t.getTranslateX() + (x + (iconSize - w / scale) / 2.0) * scale;
                double dy = t.getTranslateY() + (y + (iconSize - h / scale) / 2.0) * scale;
                g.setTransform(new AffineTransform());
                g.drawImage(scaledIcon, (int) Math.round(dx), (int) Math.round(dy), null);
            } finally {
                g.dispose();
            }
        }
    }

    /** Progressive bilinear downscaling for good quality at small sizes. */
    private static Image downscale(Image src, int w, int h) {
        int cw = src.getWidth(null);
        int ch = src.getHeight(null);
        BufferedImage current = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = current.createGraphics();
        g0.drawImage(src, 0, 0, null);
        g0.dispose();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            current = resize(current, cw, ch);
        }
        return resize(current, w, h);
    }

    private static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /** Minimize / maximize / restore / close button with vector glyphs. */
    private static final class WindowButton extends JButton {
        static final int MINIMIZE = 0;
        static final int MAXIMIZE = 1;
        static final int RESTORE = 2;
        static final int CLOSE = 3;

        int kind;

        WindowButton(int kind) {
            this.kind = kind;
            setFocusable(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText(kind == MINIMIZE ? "Minimize" : kind == CLOSE ? "Close" : "Maximize");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Palette p = Tessel.palette();
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isArmed() && getModel().isPressed();
            if (hover || pressed) {
                g.setColor(kind == CLOSE ? (pressed ? new Color(0xB3261A) : new Color(0xC42B1C))
                        : pressed ? p.controlPressed : p.controlHover);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            Window w = SwingUtilities.getWindowAncestor(this);
            boolean active = w == null || w.isActive();
            Color fg = kind == CLOSE && (hover || pressed) ? Color.WHITE : active ? p.text : p.textTertiary;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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

    // ------------------------------------------------------------------ resizing

    /** Resizes an undecorated frame from its edges. */
    private static final class WindowResizer implements AWTEventListener {
        private static final int EDGE = 6;
        private static final int N = 1;
        private static final int S = 2;
        private static final int W = 4;
        private static final int E = 8;

        private final TesselFrame frame;
        private int dragEdges;
        private Point pressScreen;
        private Rectangle pressBounds;
        private boolean cursorSet;

        WindowResizer(TesselFrame frame) {
            this.frame = frame;
        }

        static boolean isResizing(TesselFrame frame) {
            return frame.resizer != null && frame.resizer.dragEdges != 0;
        }

        void install() {
            frame.resizer = this;
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(WindowResizer.this);
                }
            });
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (!(event instanceof MouseEvent)) return;
            MouseEvent me = (MouseEvent) event;
            Component src = me.getComponent();
            if (src == null || (src != frame && SwingUtilities.getWindowAncestor(src) != frame)) return;
            if (!frame.isResizable() || frame.isMaximized()) {
                resetCursor();
                return;
            }
            Point p = SwingUtilities.convertPoint(src, me.getPoint(), frame);
            switch (me.getID()) {
                case MouseEvent.MOUSE_MOVED:
                    updateCursor(edgesAt(p));
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    int edges = edgesAt(p);
                    if (edges != 0 && SwingUtilities.isLeftMouseButton(me)) {
                        dragEdges = edges;
                        pressScreen = me.getLocationOnScreen();
                        pressBounds = frame.getBounds();
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
                case MouseEvent.MOUSE_EXITED:
                    if (dragEdges == 0 && src == frame) resetCursor();
                    break;
                default:
                    break;
            }
        }

        private int edgesAt(Point p) {
            int w = frame.getWidth();
            int h = frame.getHeight();
            int edges = 0;
            if (p.y < EDGE) edges |= N;
            if (p.y >= h - EDGE) edges |= S;
            if (p.x < EDGE) edges |= W;
            if (p.x >= w - EDGE) edges |= E;
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
            if (type < 0) {
                resetCursor();
            } else {
                frame.getRootPane().setCursor(Cursor.getPredefinedCursor(type));
                cursorSet = true;
            }
        }

        private void resetCursor() {
            if (cursorSet) {
                frame.getRootPane().setCursor(null);
                cursorSet = false;
            }
        }

        private void resize(Point screen) {
            int dx = screen.x - pressScreen.x;
            int dy = screen.y - pressScreen.y;
            Rectangle r = new Rectangle(pressBounds);
            Dimension min = frame.getMinimumSize();
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
            frame.setBounds(r);
            frame.validate();
        }
    }

    private WindowResizer resizer;
}
