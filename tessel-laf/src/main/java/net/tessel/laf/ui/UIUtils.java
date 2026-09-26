package net.tessel.laf.ui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Painting and state helpers shared by the Tessel UI delegates and components. */
public final class UIUtils {

    /** Client property set while the mouse is over the component. */
    public static final String HOVER = "Tessel.hover";
    /** Client property set while the component has keyboard (traversal) focus. */
    public static final String FOCUS_VISIBLE = "Tessel.focusVisible";

    public static final float DISABLED_ALPHA = 0.45f;

    private static final MouseListener HOVER_LISTENER = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            setHover(e.getComponent(), true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setHover(e.getComponent(), false);
        }
    };

    private static final AWTEventListener FOCUS_TRACKER = UIUtils::onFocusEvent;
    private static boolean focusTrackerInstalled;

    private UIUtils() {
    }

    // ------------------------------------------------------------------ graphics

    /** A copy of {@code g} with antialiasing enabled. Dispose it when done. */
    public static Graphics2D create(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /** Applies the disabled-state transparency when the component is disabled. */
    public static void applyEnabledAlpha(Graphics2D g, Component c) {
        if (c != null && !c.isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(DISABLED_ALPHA));
    }

    public static void fillRound(Graphics2D g, double x, double y, double w, double h, double radius) {
        if (w <= 0 || h <= 0) return;
        double arc = Math.max(0, Math.min(radius * 2, Math.min(w, h)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    public static void strokeRound(Graphics2D g, double x, double y, double w, double h, double radius, float width) {
        if (w <= 0 || h <= 0) return;
        double inset = width / 2.0;
        double arc = Math.max(0, Math.min((radius - inset) * 2, Math.min(w, h)));
        g.setStroke(new BasicStroke(width));
        g.draw(new RoundRectangle2D.Double(x + inset, y + inset, w - width, h - width, arc, arc));
    }

    /** Accent ring drawn around focused controls (inside the given bounds). */
    public static void paintFocusRing(Graphics2D g, double x, double y, double w, double h, double radius) {
        g.setColor(Tessel.palette().accent);
        strokeRound(g, x, y, w, h, radius, 2f);
    }

    /**
     * Soft drop shadow for a rounded rectangle. Paint it before the shape itself;
     * callers reserve {@code size} pixels (plus 2 below) around the shape.
     */
    public static void paintShadow(Graphics2D g, double x, double y, double w, double h, double radius, int size) {
        Palette p = Tessel.palette();
        int base = p.shadow.getAlpha();
        for (int i = size; i >= 1; i--) {
            double t = 1.0 - (double) i / (size + 1);
            int alpha = (int) Math.round(base * t * t * 0.55);
            if (alpha <= 0) continue;
            g.setColor(new Color(0, 0, 0, alpha));
            fillRound(g, x - i, y - i + 2, w + 2.0 * i, h + 2.0 * i, radius + i);
        }
    }

    // ------------------------------------------------------------------ hover

    public static void installHover(JComponent c) {
        c.removeMouseListener(HOVER_LISTENER);
        c.addMouseListener(HOVER_LISTENER);
    }

    public static void uninstallHover(JComponent c) {
        c.removeMouseListener(HOVER_LISTENER);
        c.putClientProperty(HOVER, null);
    }

    public static boolean isHover(Component c) {
        return c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(HOVER));
    }

    private static void setHover(Component source, boolean hover) {
        int depth = 0;
        for (Component c = source; c != null && depth < 4; c = c.getParent(), depth++) {
            if (!(c instanceof JComponent)) continue;
            JComponent jc = (JComponent) c;
            if (c == source || jc.getBorder() instanceof TesselInputBorder) {
                jc.putClientProperty(HOVER, hover ? Boolean.TRUE : null);
                jc.repaint();
            }
        }
    }

    // ------------------------------------------------------------------ focus

    /** True when {@code c} or a descendant owns the keyboard focus. */
    public static boolean hasFocusWithin(Component c) {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return owner != null && (owner == c || SwingUtilities.isDescendingFrom(owner, c));
    }

    /** True when {@code c} received focus through keyboard traversal (Tab), not a mouse click. */
    public static boolean isFocusVisible(Component c) {
        return c instanceof JComponent && c.hasFocus()
                && Boolean.TRUE.equals(((JComponent) c).getClientProperty(FOCUS_VISIBLE));
    }

    public static synchronized void installFocusTracker() {
        if (focusTrackerInstalled) return;
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(FOCUS_TRACKER, AWTEvent.FOCUS_EVENT_MASK);
            focusTrackerInstalled = true;
        } catch (SecurityException ignored) {
            // Focus rings will not distinguish keyboard focus.
        }
    }

    public static synchronized void uninstallFocusTracker() {
        if (!focusTrackerInstalled) return;
        Toolkit.getDefaultToolkit().removeAWTEventListener(FOCUS_TRACKER);
        focusTrackerInstalled = false;
    }

    private static void onFocusEvent(AWTEvent event) {
        if (!(event instanceof FocusEvent)) return;
        FocusEvent fe = (FocusEvent) event;
        Component c = fe.getComponent();
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            if (fe.getID() == FocusEvent.FOCUS_GAINED) {
                jc.putClientProperty(FOCUS_VISIBLE, isTraversal(fe.getCause()) ? Boolean.TRUE : null);
            } else if (fe.getID() == FocusEvent.FOCUS_LOST) {
                jc.putClientProperty(FOCUS_VISIBLE, null);
            }
            jc.repaint();
        }
        // Inputs made of several components (spinner, editable combo) show focus on their outer border.
        int depth = 0;
        for (Container p = c == null ? null : c.getParent(); p != null && depth < 4; p = p.getParent(), depth++) {
            if (p instanceof JComponent && (((JComponent) p).getBorder() instanceof TesselInputBorder
                    || ((JComponent) p).getBorder() instanceof TesselBorders.ScrollPaneBorder)) p.repaint();
        }
    }

    private static boolean isTraversal(FocusEvent.Cause cause) {
        return cause == FocusEvent.Cause.TRAVERSAL || cause == FocusEvent.Cause.TRAVERSAL_FORWARD
                || cause == FocusEvent.Cause.TRAVERSAL_BACKWARD || cause == FocusEvent.Cause.TRAVERSAL_UP
                || cause == FocusEvent.Cause.TRAVERSAL_DOWN;
    }
}
