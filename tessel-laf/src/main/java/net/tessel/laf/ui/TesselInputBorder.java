package net.tessel.laf.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import net.tessel.laf.Palette;
import net.tessel.laf.Tessel;
import net.tessel.laf.icons.FontIcon;

/**
 * Rounded outline of text fields, combo boxes and spinners. The outline turns stronger on hover
 * and accent-colored while the control (or a child) has keyboard focus. Also paints the
 * {@link Tessel#LEADING_ICON}.
 */
public class TesselInputBorder extends AbstractBorder implements UIResource {

    public static final double RADIUS = 6;

    private final Insets insets;

    public TesselInputBorder(int top, int left, int bottom, int right) {
        this.insets = new Insets(top, left, bottom, right);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets i) {
        i.set(insets.top, insets.left, insets.bottom, insets.right);
        Icon icon = leadingIcon(c);
        if (icon != null) i.left += icon.getIconWidth() + 8;
        return i;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Palette p = Tessel.palette();
        Graphics2D g2 = UIUtils.create(g);
        try {
            boolean enabled = c.isEnabled();
            boolean focused = enabled && UIUtils.hasFocusWithin(c) && isEditable(c);
            boolean hover = enabled && UIUtils.isHover(c);
            Color col = focused ? p.accent : hover ? p.borderStrong : p.border;
            if (!enabled) UIUtils.applyEnabledAlpha(g2, c);
            g2.setColor(col);
            UIUtils.strokeRound(g2, x, y, w, h, RADIUS, 1f);
            if (focused) {
                // Fluent-style emphasis: a slightly thicker accent line along the bottom edge.
                g2.setClip(x, y + h - 2, w, 2);
                UIUtils.strokeRound(g2, x, y, w, h, RADIUS, 2f);
                g2.setClip(null);
            }

            Icon icon = leadingIcon(c);
            if (icon != null) {
                int ix = x + insets.left;
                int iy = y + (h - icon.getIconHeight()) / 2;
                if (icon instanceof FontIcon) ((FontIcon) icon).paintIcon(c, g2, ix, iy, p.textSecondary);
                else icon.paintIcon(c, g2, ix, iy);
            }
        } finally {
            g2.dispose();
        }
    }

    private static boolean isEditable(Component c) {
        return !(c instanceof JTextComponent) || ((JTextComponent) c).isEditable();
    }

    private static Icon leadingIcon(Component c) {
        if (c instanceof JComponent) {
            Object icon = ((JComponent) c).getClientProperty(Tessel.LEADING_ICON);
            if (icon instanceof Icon) return (Icon) icon;
        }
        return null;
    }
}
