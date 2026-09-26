package net.tessel.gallery;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.tessel.laf.Tessel;
import net.tessel.laf.TextStyle;

/** Small layout helpers used by the gallery pages. */
public final class Layouts {

    private Layouts() {
    }

    /** A transparent panel. */
    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /** Components left to right, wrapping onto new lines when the width runs out. */
    public static JPanel row(Component... children) {
        JPanel p = panel(new WrapLayout(8, 8));
        for (Component c : children) p.add(c);
        return p;
    }

    /** A vertical stack of full-width children. */
    public static JPanel stack(int gap, Component... children) {
        JPanel p = panel(new StackLayout(gap, Integer.MAX_VALUE));
        for (Component c : children) p.add(c);
        return p;
    }

    /** Equal-width columns. */
    public static JPanel columns(int gap, Component... children) {
        JPanel p = panel(new GridLayout(1, children.length, gap, 0));
        for (Component c : children) p.add(c);
        return p;
    }

    /** A form field: a label above the input. */
    public static JPanel field(String header, JComponent input) {
        JPanel p = panel(new BorderLayout(0, 6));
        JLabel label = Tessel.label(header, TextStyle.BODY);
        label.setLabelFor(input);
        p.add(label, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    /** Stacks children vertically at the container width (optionally capped). */
    public static final class StackLayout implements LayoutManager {
        private final int gap;
        private final int maxWidth;

        public StackLayout(int gap, int maxWidth) {
            this.gap = gap;
            this.maxWidth = maxWidth;
        }

        private int width(Container parent) {
            Insets in = parent.getInsets();
            int available = parent.getWidth() - in.left - in.right;
            if (available <= 0) available = 800;
            return Math.min(available, maxWidth);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            int w = width(parent);
            int h = 0;
            int prefW = 0;
            int visible = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                c.setSize(w, Math.max(1, c.getHeight()));
                Dimension d = c.getPreferredSize();
                h += d.height;
                prefW = Math.max(prefW, d.width);
                visible++;
            }
            h += Math.max(0, visible - 1) * gap;
            return new Dimension(Math.min(prefW, maxWidth) + in.left + in.right, h + in.top + in.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Insets in = parent.getInsets();
            return new Dimension(in.left + in.right, preferredLayoutSize(parent).height);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int w = width(parent);
            int y = in.top;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) continue;
                c.setSize(w, Math.max(1, c.getHeight()));
                int h = c.getPreferredSize().height;
                c.setBounds(in.left, y, w, h);
                y += h + gap;
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    /** Flow layout that wraps and reports the wrapped height. */
    public static final class WrapLayout implements LayoutManager {
        private final int hgap;
        private final int vgap;

        public WrapLayout(int hgap, int vgap) {
            this.hgap = hgap;
            this.vgap = vgap;
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return layout(parent, false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Dimension d = layout(parent, false);
            return new Dimension(0, d.height);
        }

        @Override
        public void layoutContainer(Container parent) {
            layout(parent, true);
        }

        private Dimension layout(Container parent, boolean apply) {
            Insets in = parent.getInsets();
            int maxW = parent.getWidth() > 0 ? parent.getWidth() - in.left - in.right : Integer.MAX_VALUE;
            int x = 0;
            int y = 0;
            int rowH = 0;
            int widest = 0;
            int rowStart = 0;
            Component[] cs = parent.getComponents();
            for (int i = 0; i < cs.length; i++) {
                Component c = cs[i];
                if (!c.isVisible()) continue;
                Dimension d = c.getPreferredSize();
                if (x > 0 && x + d.width > maxW) {
                    if (apply) alignRow(cs, rowStart, i, in, y, rowH);
                    y += rowH + vgap;
                    x = 0;
                    rowH = 0;
                    rowStart = i;
                }
                if (apply) c.setBounds(in.left + x, in.top + y, d.width, d.height);
                x += d.width + hgap;
                rowH = Math.max(rowH, d.height);
                widest = Math.max(widest, x - hgap);
            }
            if (apply) alignRow(cs, rowStart, cs.length, in, y, rowH);
            return new Dimension(widest + in.left + in.right, y + rowH + in.top + in.bottom);
        }

        private static void alignRow(Component[] cs, int from, int to, Insets in, int y, int rowH) {
            for (int i = from; i < to; i++) {
                Component c = cs[i];
                if (!c.isVisible()) continue;
                c.setLocation(c.getX(), in.top + y + (rowH - c.getHeight()) / 2);
            }
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }
}
