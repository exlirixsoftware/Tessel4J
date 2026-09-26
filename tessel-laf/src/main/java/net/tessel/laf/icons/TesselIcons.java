package net.tessel.laf.icons;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.function.Function;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;
import net.tessel.laf.Palette;
import net.tessel.laf.Severity;
import net.tessel.laf.Tessel;
import net.tessel.laf.ui.UIUtils;

/** Vector icons used by the look and feel. They do not depend on any installed font. */
public final class TesselIcons {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private TesselIcons() {
    }

    public static Icon chevron(Direction direction, int size) {
        return new ChevronIcon(direction, size, p -> p.textSecondary);
    }

    public static Icon chevron(Direction direction, int size, Function<Palette, Color> color) {
        return new ChevronIcon(direction, size, color);
    }

    /** A small "x" glyph. */
    public static Icon close(int size) {
        return new CloseIcon(size);
    }

    /** A filled status icon (info / success / warning / error). */
    public static Icon severity(Severity severity, int size) {
        return new SeverityIcon(severity, size);
    }

    // ------------------------------------------------------------------ base

    private abstract static class VectorIcon implements Icon, UIResource {
        protected final int size;

        VectorIcon(int size) {
            this.size = size;
        }

        @Override
        public final void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.translate(x, y);
                paint(c, g2, Tessel.palette());
            } finally {
                g2.dispose();
            }
        }

        protected abstract void paint(Component c, Graphics2D g, Palette p);

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    // ------------------------------------------------------------------ glyphs

    public static final class ChevronIcon extends VectorIcon {
        private final Direction direction;
        private final Function<Palette, Color> color;

        public ChevronIcon(Direction direction, int size, Function<Palette, Color> color) {
            super(size);
            this.direction = direction;
            this.color = color;
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            double s = size;
            double half = Math.max(3, Math.round(s * 0.28));
            double cx = s / 2.0;
            double cy = s / 2.0;
            Path2D path = new Path2D.Double();
            switch (direction) {
                case DOWN:
                    path.moveTo(cx - half, cy - half / 2);
                    path.lineTo(cx, cy + half / 2);
                    path.lineTo(cx + half, cy - half / 2);
                    break;
                case UP:
                    path.moveTo(cx - half, cy + half / 2);
                    path.lineTo(cx, cy - half / 2);
                    path.lineTo(cx + half, cy + half / 2);
                    break;
                case RIGHT:
                    path.moveTo(cx - half / 2, cy - half);
                    path.lineTo(cx + half / 2, cy);
                    path.lineTo(cx - half / 2, cy + half);
                    break;
                default:
                    path.moveTo(cx + half / 2, cy - half);
                    path.lineTo(cx - half / 2, cy);
                    path.lineTo(cx + half / 2, cy + half);
                    break;
            }
            g.setColor(color.apply(p));
            g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
        }
    }

    public static final class CloseIcon extends VectorIcon {
        public CloseIcon(int size) {
            super(size);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            double m = size * 0.25;
            g.setColor(c != null ? c.getForeground() : p.textSecondary);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Double();
            path.moveTo(m, m);
            path.lineTo(size - m, size - m);
            path.moveTo(size - m, m);
            path.lineTo(m, size - m);
            g.draw(path);
        }
    }

    public static final class SeverityIcon extends VectorIcon {
        private final Severity severity;

        public SeverityIcon(Severity severity, int size) {
            super(size);
            this.severity = severity;
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            double s = size;
            Color col = severity.color(p);
            g.setColor(col);
            Color fg = p.dark ? p.background : Color.WHITE;
            if (severity == Severity.WARNING) {
                Path2D tri = new Path2D.Double();
                tri.moveTo(s / 2, s * 0.06);
                tri.lineTo(s * 0.98, s * 0.92);
                tri.lineTo(s * 0.02, s * 0.92);
                tri.closePath();
                g.setStroke(new BasicStroke((float) (s * 0.06), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.fill(tri);
                g.draw(tri);
                g.setColor(fg);
                g.setStroke(new BasicStroke((float) (s * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Double(s / 2, s * 0.38, s / 2, s * 0.6));
                g.fill(new Ellipse2D.Double(s / 2 - s * 0.06, s * 0.71, s * 0.12, s * 0.12));
                return;
            }
            g.fill(new Ellipse2D.Double(0, 0, s, s));
            g.setColor(fg);
            g.setStroke(new BasicStroke((float) Math.max(1.4, s * 0.1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Double();
            switch (severity) {
                case SUCCESS:
                    path.moveTo(s * 0.29, s * 0.52);
                    path.lineTo(s * 0.44, s * 0.66);
                    path.lineTo(s * 0.71, s * 0.37);
                    g.draw(path);
                    break;
                case ERROR:
                    path.moveTo(s * 0.34, s * 0.34);
                    path.lineTo(s * 0.66, s * 0.66);
                    path.moveTo(s * 0.66, s * 0.34);
                    path.lineTo(s * 0.34, s * 0.66);
                    g.draw(path);
                    break;
                default:
                    g.draw(new java.awt.geom.Line2D.Double(s / 2, s * 0.45, s / 2, s * 0.72));
                    g.fill(new Ellipse2D.Double(s / 2 - s * 0.065, s * 0.24, s * 0.13, s * 0.13));
                    break;
            }
        }
    }

    // ------------------------------------------------------------------ check box / radio

    public static final class CheckBoxIcon extends VectorIcon {
        public CheckBoxIcon() {
            super(20);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean indeterminate = Boolean.TRUE.equals(b.getClientProperty(Tessel.INDETERMINATE));
            boolean on = m.isSelected() || indeterminate;
            boolean hover = m.isRollover() && b.isEnabled();

            Color fill = on ? (hover ? p.accentHover : p.accent) : (hover ? p.controlHover : p.inputBackground);
            Color stroke = on ? fill : (hover ? p.textSecondary : p.textTertiary);
            g.setColor(fill);
            UIUtils.fillRound(g, 1, 1, 18, 18, 4);
            g.setColor(stroke);
            UIUtils.strokeRound(g, 1, 1, 18, 18, 4, 1.5f);

            if (on) {
                g.setColor(p.onAccent);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D path = new Path2D.Double();
                if (indeterminate) {
                    path.moveTo(5.5, 10);
                    path.lineTo(14.5, 10);
                } else {
                    path.moveTo(5.5, 10.2);
                    path.lineTo(8.6, 13.3);
                    path.lineTo(14.6, 6.9);
                }
                g.draw(path);
            }
            if (UIUtils.isFocusVisible(b) && b.isFocusPainted()) {
                UIUtils.paintFocusRing(g, -1, -1, 22, 22, 6);
            }
        }
    }

    public static final class RadioIcon extends VectorIcon {
        public RadioIcon() {
            super(20);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean on = m.isSelected();
            boolean hover = m.isRollover() && b.isEnabled();

            Color fill = on ? p.accent : (hover ? p.controlHover : p.inputBackground);
            Color stroke = on ? p.accent : (hover ? p.textSecondary : p.textTertiary);
            g.setColor(fill);
            g.fill(new Ellipse2D.Double(1, 1, 18, 18));
            g.setColor(stroke);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(1.75, 1.75, 16.5, 16.5));
            if (on) {
                double d = hover ? 10 : 8;
                g.setColor(p.onAccent);
                g.fill(new Ellipse2D.Double(10 - d / 2, 10 - d / 2, d, d));
            }
            if (UIUtils.isFocusVisible(b) && b.isFocusPainted()) {
                g.setColor(p.accent);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Double(-1, -1, 22, 22));
            }
        }
    }

    // ------------------------------------------------------------------ menus

    public static final class MenuCheckIcon extends VectorIcon {
        public MenuCheckIcon() {
            super(16);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected()) return;
            g.setColor(p.accent);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D path = new Path2D.Double();
            path.moveTo(3.5, 8.3);
            path.lineTo(6.5, 11.2);
            path.lineTo(12.5, 4.8);
            g.draw(path);
        }
    }

    public static final class MenuRadioIcon extends VectorIcon {
        public MenuRadioIcon() {
            super(16);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected()) return;
            g.setColor(p.accent);
            g.fill(new Ellipse2D.Double(5, 5, 6, 6));
        }
    }

    // ------------------------------------------------------------------ tree

    public static final class FolderIcon extends VectorIcon {
        public FolderIcon() {
            super(16);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            Path2D path = new Path2D.Double();
            path.moveTo(1.5, 3.5);
            path.lineTo(6, 3.5);
            path.lineTo(7.5, 5);
            path.lineTo(14.5, 5);
            path.lineTo(14.5, 12.5);
            path.lineTo(1.5, 12.5);
            path.closePath();
            g.setColor(p.accent);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
        }
    }

    public static final class FileIcon extends VectorIcon {
        public FileIcon() {
            super(16);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            Path2D path = new Path2D.Double();
            path.moveTo(3.5, 1.5);
            path.lineTo(9.5, 1.5);
            path.lineTo(12.5, 4.5);
            path.lineTo(12.5, 14.5);
            path.lineTo(3.5, 14.5);
            path.closePath();
            path.moveTo(9.5, 1.5);
            path.lineTo(9.5, 4.5);
            path.lineTo(12.5, 4.5);
            g.setColor(p.accent);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
        }
    }

    // ------------------------------------------------------------------ misc

    /** Paints another icon with reduced opacity (used for disabled icons). */
    public static final class AlphaIcon implements Icon, UIResource {
        private final Icon icon;
        private final float alpha;

        public AlphaIcon(Icon icon, float alpha) {
            this.icon = icon;
            this.alpha = alpha;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                icon.paintIcon(c, g2, x, y);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return icon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return icon.getIconHeight();
        }
    }

    /** An invisible icon that only reserves space. */
    public static final class EmptyIcon implements Icon, UIResource {
        private final int w;
        private final int h;

        public EmptyIcon(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }
}
