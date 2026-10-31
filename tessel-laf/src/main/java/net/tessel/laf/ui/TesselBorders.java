package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;

/** Borders used by the Tessel look and feel. */
public final class TesselBorders {

    private TesselBorders() {
    }

    /**
     * Rounded outline around scroll panes. It fills its shape with the background of the
     * scrolled view so square content corners never poke out of the rounded outline.
     */
    public static class ScrollPaneBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            int v = isText(c) ? 2 : 3;
            i.set(v, v, v, v);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                double radius = isText(c) ? 6 : 8;
                Color fill = viewBackground(c);
                if (fill != null) {
                    g2.setColor(fill);
                    UIUtils.fillRound(g2, x, y, w, h, radius);
                }
                Component view = view(c);
                boolean focused = view instanceof JTextComponent && ((JTextComponent) view).isEditable()
                        && UIUtils.hasFocusWithin(view);
                g2.setColor(focused ? p.accent : p.border);
                UIUtils.strokeRound(g2, x, y, w, h, radius, 1f);
            } finally {
                g2.dispose();
            }
        }

        private static Component view(Component c) {
            if (!(c instanceof JScrollPane)) return null;
            JViewport vp = ((JScrollPane) c).getViewport();
            return vp == null ? null : vp.getView();
        }

        private static boolean isText(Component c) {
            return view(c) instanceof JTextComponent;
        }

        private static Color viewBackground(Component c) {
            if (!(c instanceof JScrollPane)) return null;
            JViewport vp = ((JScrollPane) c).getViewport();
            if (vp == null) return null;
            Component view = vp.getView();
            if (view != null && view.isOpaque()) return view.getBackground();
            return vp.isOpaque() ? vp.getBackground() : null;
        }
    }

    /** Bottom separator line plus padding for table header cells. */
    public static class TableHeaderBorder extends AbstractBorder implements UIResource {

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.set(10, 12, 11, 12);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(Tessel.palette().border);
            g.fillRect(x, y + h - 1, w, 1);
        }
    }

    /**
     * Rounded surface of a tool bar, with a grip of dots when the tool bar is floatable.
     * Nothing is painted when {@link JToolBar#isBorderPainted()} is false.
     */
    public static class ToolBarBorder extends AbstractBorder implements UIResource {
        /** Width (or height, for vertical bars) reserved for the grip. */
        static final int GRIP = 12;

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.set(4, 4, 4, 4);
            if (c instanceof JToolBar) {
                JToolBar tb = (JToolBar) c;
                if (tb.isFloatable()) {
                    if (tb.getOrientation() == JToolBar.HORIZONTAL) i.left += GRIP;
                    else i.top += GRIP;
                }
            }
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!(c instanceof JToolBar) || !((JToolBar) c).isBorderPainted()) return;
            JToolBar tb = (JToolBar) c;
            Palette p = Tessel.palette();
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(p.surfaceAlt);
                UIUtils.fillRound(g2, x, y, w, h, 8);
                g2.setColor(p.border);
                UIUtils.strokeRound(g2, x, y, w, h, 8, 1f);
                if (tb.isFloatable()) {
                    g2.setColor(p.textTertiary);
                    boolean horizontal = tb.getOrientation() == JToolBar.HORIZONTAL;
                    // Two lines of three dots across the bar's thickness.
                    for (int along = 0; along < 3; along++) {
                        for (int across = 0; across < 2; across++) {
                            double cx = horizontal ? x + 6 + across * 4 : x + w / 2.0 - 4 + along * 4;
                            double cy = horizontal ? y + h / 2.0 - 4 + along * 4 : y + 6 + across * 4;
                            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - 1, cy - 1, 2, 2));
                        }
                    }
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** Rounded surface and outline of an internal frame, with a soft shadow around it. */
    public static class InternalFrameBorder extends AbstractBorder implements UIResource {
        /** Space reserved around the frame surface (also the resize zone). */
        public static final int TOP = 5;
        public static final int SIDE = 5;
        public static final int BOTTOM = 7;
        public static final double RADIUS = 6;

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.set(TOP, SIDE, BOTTOM, SIDE);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Palette p = Tessel.palette();
            boolean selected = false;
            boolean icon = false;
            if (c instanceof javax.swing.JInternalFrame) {
                selected = ((javax.swing.JInternalFrame) c).isSelected();
                icon = ((javax.swing.JInternalFrame) c).isIcon();
            } else if (c instanceof javax.swing.JInternalFrame.JDesktopIcon) {
                selected = ((javax.swing.JInternalFrame.JDesktopIcon) c).getInternalFrame().isSelected();
                icon = true;
            }
            Graphics2D g2 = UIUtils.create(g);
            try {
                double sx = x + 2;
                double sy = y + 2;
                double sw = w - 4;
                double sh = h - 6;
                UIUtils.paintShadow(g2, sx, sy, sw, sh, RADIUS, selected ? 3 : 2);
                g2.setColor(p.surface);
                UIUtils.fillRound(g2, sx, sy, sw, sh, RADIUS);
                g2.setColor(selected ? (icon ? p.accent : p.borderStrong) : p.border);
                UIUtils.strokeRound(g2, sx, sy, sw, sh, RADIUS, 1f);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A 1px rounded outline (used by titled borders). */
    public static class RoundedLineBorder extends AbstractBorder implements UIResource {
        private final int radius;

        public RoundedLineBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.set(4, 4, 4, 4);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.setColor(Tessel.palette().border);
                UIUtils.strokeRound(g2, x, y, w, h, radius, 1f);
            } finally {
                g2.dispose();
            }
        }
    }

    /** A 1px line with padding inside (popup menus, tooltips). */
    public static class PopupBorder extends AbstractBorder implements UIResource {
        private final Insets padding;

        public PopupBorder(int top, int left, int bottom, int right) {
            padding = new Insets(top, left, bottom, right);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.set(padding.top + 1, padding.left + 1, padding.bottom + 1, padding.right + 1);
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Palette p = Tessel.palette();
            g.setColor(p.dark ? p.borderStrong : p.border);
            g.drawRect(x, y, w - 1, h - 1);
        }
    }
}
