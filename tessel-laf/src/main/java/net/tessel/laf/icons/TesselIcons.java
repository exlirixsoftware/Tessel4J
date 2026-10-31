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

    /** A filled accent circle with a question mark (the "question" icon of option panes). */
    public static Icon question(int size) {
        return new QuestionIcon(size);
    }

    /** A vector glyph that takes the foreground color of the component it is painted on. */
    public static ShapeIcon of(Kind kind, int size) {
        return new ShapeIcon(kind, size, null);
    }

    /** A vector glyph with a palette color that follows theme changes, e.g. {@code p -> p.accent}. */
    public static ShapeIcon of(Kind kind, int size, Function<Palette, Color> color) {
        return new ShapeIcon(kind, size, color);
    }

    /** The glyphs available through {@link #of(Kind, int)}. All are drawn as vectors on a 16 unit grid. */
    public enum Kind {
        PLUS, MINUS, MORE,
        ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
        NEW_FOLDER, VIEW_LIST, VIEW_DETAILS, VIEW_TILES,
        DRIVE, COMPUTER, HOME, DESKTOP, DOCUMENTS, DOWNLOADS, PICTURES, FOLDER, FILE,
        MINIMIZE, MAXIMIZE, RESTORE, CLOSE, WINDOW,
        SEARCH, EYEDROPPER
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

    /** A {@link Kind} glyph. Without an explicit color it uses the foreground of the painted component. */
    public static final class ShapeIcon implements Icon, UIResource {
        private final Kind kind;
        private final int size;
        private final Function<Palette, Color> color;

        ShapeIcon(Kind kind, int size, Function<Palette, Color> color) {
            this.kind = kind;
            this.size = size;
            this.color = color;
        }

        public Kind getKind() {
            return kind;
        }

        public boolean hasOwnColor() {
            return color != null;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            paintIcon(c, g, x, y, null);
        }

        /** Paints with {@code override} as color when the icon has no color of its own. */
        public void paintIcon(Component c, Graphics g, int x, int y, Color override) {
            Palette p = Tessel.palette();
            Color col = color != null ? color.apply(p) : override != null ? override : c != null ? c.getForeground() : p.text;
            Graphics2D g2 = UIUtils.create(g);
            try {
                g2.translate(x, y);
                g2.scale(size / 16.0, size / 16.0);
                g2.setColor(col);
                g2.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g2, p);
            } finally {
                g2.dispose();
            }
        }

        private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
            g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
        }

        private static void poly(Graphics2D g, boolean close, double... xy) {
            Path2D path = new Path2D.Double();
            path.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
            if (close) path.closePath();
            g.draw(path);
        }

        private static void rect(Graphics2D g, double x, double y, double w, double h, double r) {
            g.draw(new java.awt.geom.RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
        }

        private void draw(Graphics2D g, Palette p) {
            switch (kind) {
                case PLUS:
                    line(g, 8, 3, 8, 13);
                    line(g, 3, 8, 13, 8);
                    break;
                case MINUS:
                    line(g, 3, 8, 13, 8);
                    break;
                case MORE:
                    for (double cx : new double[]{3.5, 8, 12.5}) g.fill(new Ellipse2D.Double(cx - 1.25, 6.75, 2.5, 2.5));
                    break;
                case ARROW_UP:
                case ARROW_DOWN:
                case ARROW_LEFT:
                case ARROW_RIGHT: {
                    double angle = kind == Kind.ARROW_UP ? 0 : kind == Kind.ARROW_RIGHT ? Math.PI / 2
                            : kind == Kind.ARROW_DOWN ? Math.PI : -Math.PI / 2;
                    g.rotate(angle, 8, 8);
                    line(g, 8, 13.5, 8, 2.5);
                    poly(g, false, 3.5, 7, 8, 2.5, 12.5, 7);
                    break;
                }
                case NEW_FOLDER:
                    poly(g, true, 1.5, 3.5, 5.5, 3.5, 7, 5, 12.5, 5, 12.5, 7.5, 12.5, 7.5, 1.5, 7.5);
                    poly(g, false, 1.5, 3.5, 1.5, 12.5, 8, 12.5);
                    line(g, 12, 9.5, 12, 14);
                    line(g, 9.75, 11.75, 14.25, 11.75);
                    break;
                case VIEW_LIST:
                    for (double y : new double[]{4, 8, 12}) {
                        g.fill(new Ellipse2D.Double(2, y - 1, 2, 2));
                        line(g, 6, y, 14, y);
                    }
                    break;
                case VIEW_DETAILS:
                    rect(g, 2, 3, 12, 10, 1.5);
                    line(g, 2, 6.5, 14, 6.5);
                    line(g, 2, 9.75, 14, 9.75);
                    line(g, 6.5, 6.5, 6.5, 13);
                    break;
                case VIEW_TILES:
                    rect(g, 2.5, 2.5, 4.5, 4.5, 1);
                    rect(g, 9, 2.5, 4.5, 4.5, 1);
                    rect(g, 2.5, 9, 4.5, 4.5, 1);
                    rect(g, 9, 9, 4.5, 4.5, 1);
                    break;
                case DRIVE:
                    rect(g, 1.5, 8, 13, 5, 1.5);
                    poly(g, false, 2.5, 8, 4, 3.5, 12, 3.5, 13.5, 8);
                    g.fill(new Ellipse2D.Double(10.75, 9.75, 1.5, 1.5));
                    break;
                case COMPUTER:
                case DESKTOP:
                    rect(g, 2, 2.5, 12, 8.5, 1.5);
                    line(g, 8, 11, 8, 13.5);
                    line(g, 5, 13.5, 11, 13.5);
                    break;
                case HOME:
                    poly(g, false, 1.75, 8, 8, 2.5, 14.25, 8);
                    poly(g, false, 3.5, 6.75, 3.5, 13.5, 12.5, 13.5, 12.5, 6.75);
                    poly(g, false, 6.5, 13.5, 6.5, 9.5, 9.5, 9.5, 9.5, 13.5);
                    break;
                case DOCUMENTS:
                    poly(g, false, 9.5, 1.5, 3.5, 1.5, 3.5, 14.5, 12.5, 14.5, 12.5, 4.5, 9.5, 1.5, 9.5, 4.5, 12.5, 4.5);
                    line(g, 5.75, 8, 10.25, 8);
                    line(g, 5.75, 10.5, 10.25, 10.5);
                    break;
                case DOWNLOADS:
                    line(g, 8, 2.5, 8, 10.5);
                    poly(g, false, 4.5, 7.25, 8, 10.75, 11.5, 7.25);
                    poly(g, false, 2.5, 11.5, 2.5, 13.5, 13.5, 13.5, 13.5, 11.5);
                    break;
                case PICTURES:
                    rect(g, 1.75, 3, 12.5, 10, 1.5);
                    poly(g, false, 2.5, 11.5, 6, 7.5, 9, 10.5, 10.75, 9, 13.5, 11.75);
                    g.fill(new Ellipse2D.Double(9.75, 5.25, 2, 2));
                    break;
                case FOLDER:
                    poly(g, true, 1.5, 3.5, 6, 3.5, 7.5, 5, 14.5, 5, 14.5, 12.5, 1.5, 12.5);
                    break;
                case FILE:
                    poly(g, false, 9.5, 1.5, 3.5, 1.5, 3.5, 14.5, 12.5, 14.5, 12.5, 4.5, 9.5, 1.5, 9.5, 4.5, 12.5, 4.5);
                    break;
                case MINIMIZE:
                    line(g, 3.5, 8.5, 12.5, 8.5);
                    break;
                case MAXIMIZE:
                    rect(g, 3.5, 3.5, 9, 9, 1.5);
                    break;
                case RESTORE:
                    rect(g, 3.5, 5.5, 7, 7, 1.25);
                    poly(g, false, 5.75, 3.25, 11.25, 3.25, 12.75, 4.75, 12.75, 10.25);
                    break;
                case CLOSE:
                    line(g, 4, 4, 12, 12);
                    line(g, 12, 4, 4, 12);
                    break;
                case WINDOW:
                    rect(g, 2, 3, 12, 10, 1.75);
                    line(g, 2, 6.25, 14, 6.25);
                    break;
                case SEARCH:
                    g.draw(new Ellipse2D.Double(2.5, 2.5, 8.5, 8.5));
                    line(g, 9.5, 9.5, 13.5, 13.5);
                    break;
                case EYEDROPPER:
                    g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    line(g, 10, 6, 12.5, 3.5);
                    g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    poly(g, false, 9.5, 5.5, 3.5, 11.5, 2.5, 13.5, 4.5, 12.5, 10.5, 6.5);
                    break;
                default:
                    break;
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    public static final class QuestionIcon extends VectorIcon {
        public QuestionIcon(int size) {
            super(size);
        }

        @Override
        protected void paint(Component c, Graphics2D g, Palette p) {
            g.setColor(p.accent);
            g.fill(new Ellipse2D.Double(0, 0, size, size));
            g.setColor(p.onAccent);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Font font = net.tessel.laf.Fonts.semibold((float) (size * 0.62));
            g.setFont(font);
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString("?", (size - fm.stringWidth("?")) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
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
